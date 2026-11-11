package com.kkobak.server.presentation.doselog;

import java.time.Instant;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.kkobak.server.TestcontainersConfiguration;
import com.kkobak.server.presentation.ApiTestSupport;
import com.kkobak.server.domain.user.User;
import com.kkobak.server.domain.user.UserRepository;

// @Transactional 을 쓰지 않는다. 다른 API 테스트는 테스트 전체가 트랜잭션 하나라서 "커밋 시점의 DB 동작"
// (동시 요청의 UNIQUE 충돌, ON DELETE CASCADE 의 범위)을 볼 수 없기 때문이다. 대신 끝나면 직접 지운다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DoseLogCommitApiTest extends ApiTestSupport {

    private static final Instant USER_CREATED_AT = Instant.parse("2026-11-01T00:00:00Z");

    private static final String EMAIL_PREFIX = "commit-test-";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private Long firstUserId;
    private Long secondUserId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        firstUserId = userRepository.save(new User(EMAIL_PREFIX + "1@example.com", "첫째", "commit-sub-1", USER_CREATED_AT)).getId();
        secondUserId = userRepository.save(new User(EMAIL_PREFIX + "2@example.com", "둘째", "commit-sub-2", USER_CREATED_AT)).getId();
    }

    @AfterEach
    void cleanUp() {
        // users 를 지우면 ON DELETE CASCADE 로 약, 복용 시간, 복용 기록이 함께 지워진다
        jdbcTemplate.update("delete from users where email like ?", EMAIL_PREFIX + "%");
    }

    private Long createMedicationAndGetScheduleId(Long userId, String name) throws Exception {
        String json = "{\"name\": \"" + name + "\", \"mealTiming\": \"NONE\", \"startDate\": \"2026-11-01\","
                + "\"times\": [\"08:00\"]}";
        MvcResult result = mockMvc.perform(post("/api/medications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        List<Number> scheduleIds = JsonPath.read(result.getResponse().getContentAsString(), "$.data.schedules[*].id");
        return scheduleIds.get(0).longValue();
    }

    private MvcResult checkDose(Long userId, Long scheduleId, String date) throws Exception {
        String json = "{\"scheduleId\": " + scheduleId + ", \"scheduledDate\": \"" + date + "\"}";
        return mockMvc.perform(put("/api/dose-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();
    }

    private int countDoseLogs(Long scheduleId) {
        return jdbcTemplate.queryForObject("select count(*) from dose_logs where schedule_id = ?", Integer.class,
                scheduleId);
    }

    // 복용 체크를 연타하면 같은 요청이 거의 동시에 서버에 들어온다.
    // 어느 쪽이 먼저 이기든 결과는 200 아니면 409(다시 시도) 뿐이어야 하고, 500 이나 중복 기록이 생기면 안 된다.
    @Test
    void 같은_복용_체크를_동시에_여러_번_보내도_모두_성공하고_기록은_하나다() throws Exception {
        Long scheduleId = createMedicationAndGetScheduleId(firstUserId, "타이레놀");
        int requestCount = 8;
        CountDownLatch readyToGo = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            futures.add(executor.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    readyToGo.await();
                    return checkDose(firstUserId, scheduleId, "2026-11-15").getResponse().getStatus();
                }
            }));
        }
        readyToGo.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> future : futures) {
            statuses.add(future.get());
        }
        executor.shutdown();

        // DB upsert 로 바꾼 뒤에는 동시 요청끼리 부딪혀도 409 없이 전부 성공해야 한다
        assertThat(statuses).as("각 요청의 HTTP 상태").hasSize(requestCount).allMatch(code -> code == 200);
        assertThat(countDoseLogs(scheduleId)).isEqualTo(1);
    }

    // 회귀: 약 삭제의 연쇄(CASCADE)가 "그 약의 것만" 지우는지. 다른 사람의 약과 기록이 같이 지워지면 안 된다.
    @Test
    void 약을_삭제하면_그_약의_복용_기록만_지워지고_다른_사람의_기록은_남는다() throws Exception {
        Long firstScheduleId = createMedicationAndGetScheduleId(firstUserId, "첫째의약");
        Long secondScheduleId = createMedicationAndGetScheduleId(secondUserId, "둘째의약");
        assertThat(checkDose(firstUserId, firstScheduleId, "2026-11-15").getResponse().getStatus()).isEqualTo(200);
        assertThat(checkDose(secondUserId, secondScheduleId, "2026-11-15").getResponse().getStatus()).isEqualTo(200);
        Long firstMedicationId = jdbcTemplate.queryForObject(
                "select medication_id from schedules where id = ?", Long.class, firstScheduleId);

        mockMvc.perform(delete("/api/medications/" + firstMedicationId).header(HttpHeaders.AUTHORIZATION, bearer(firstUserId)))
                .andExpect(status().isOk());

        assertThat(countDoseLogs(firstScheduleId)).isZero();
        assertThat(countDoseLogs(secondScheduleId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from schedules where id = ?", Integer.class,
                secondScheduleId)).isEqualTo(1);
    }
}
