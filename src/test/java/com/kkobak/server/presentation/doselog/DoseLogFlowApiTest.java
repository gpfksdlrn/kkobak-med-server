package com.kkobak.server.presentation.doselog;

import java.time.Instant;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.kkobak.server.TestcontainersConfiguration;
import com.kkobak.server.presentation.ApiTestSupport;
import com.kkobak.server.domain.user.User;
import com.kkobak.server.domain.user.UserRepository;

import jakarta.persistence.EntityManager;

// 사용자가 화면에서 하는 순서 그대로 API 를 이어서 호출한다. (버그 노트 3번: API 별 테스트만으로는 놓친 흐름 버그)
// 약은 도메인 객체로 직접 만들지 않고 반드시 API 로 만들어서, "응답에 다음 API 에 필요한 id 가 들어 있는가"까지 검증한다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class DoseLogFlowApiTest extends ApiTestSupport {

    private static final Instant USER_CREATED_AT = Instant.parse("2026-11-01T00:00:00Z");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private Long myUserId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        myUserId = userRepository.save(new User("flow-me@example.com", "나", "sub-flow-me", USER_CREATED_AT)).getId();
        otherUserId = userRepository.save(new User("flow-other@example.com", "남", "sub-flow-other", USER_CREATED_AT)).getId();
    }

    private String medicationJson(String startDate, String endDate, String times) {
        String endDateValue = endDate == null ? "null" : "\"" + endDate + "\"";
        return "{\"name\": \"타이레놀\", \"dosageText\": \"1정\", \"mealTiming\": \"AFTER_MEAL\","
                + "\"startDate\": \"" + startDate + "\", \"endDate\": " + endDateValue + ","
                + "\"times\": [" + times + "]}";
    }

    private Long createMedication(Long userId, String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/medications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    private ResultActions updateMedication(Long userId, Long medicationId, String json) throws Exception {
        return mockMvc.perform(put("/api/medications/" + medicationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    // 화면이 하는 것처럼 "그날 복용 중인 약" 응답에서 복용 시간 id 를 꺼낸다
    private Long scheduleIdAt(Long userId, String date, String time) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/medications/active")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId)).param("date", date))
                .andExpect(status().isOk())
                .andReturn();
        List<Number> ids = JsonPath.read(result.getResponse().getContentAsString(),
                "$.data[*].schedules[?(@.time=='" + time + "')].id");
        assertThat(ids).as("%s 에 복용 중인 약의 %s 복용 시간", date, time).hasSize(1);
        return ids.get(0).longValue();
    }

    private ResultActions checkDose(Long userId, Long scheduleId, String date) throws Exception {
        String json = "{\"scheduleId\": " + scheduleId + ", \"scheduledDate\": \"" + date + "\"}";
        return mockMvc.perform(put("/api/dose-logs")
                .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 약_등록_후_목록에서_받은_복용_시간_id로_체크하고_날짜별로_다시_조회할_수_있다() throws Exception {
        createMedication(myUserId, medicationJson("2026-11-01", null, "\"08:00\", \"20:00\""));

        Long eveningScheduleId = scheduleIdAt(myUserId, "2026-11-15", "20:00");
        checkDose(myUserId, eveningScheduleId, "2026-11-15")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduleId").value(eveningScheduleId));
        flushAndClear();

        // 저녁만 체크했으므로 그날 기록은 저녁 하나뿐이다 (아침은 기록 없음 = 아직 안 먹음)
        mockMvc.perform(get("/api/dose-logs").header(HttpHeaders.AUTHORIZATION, bearer(myUserId)).param("date", "2026-11-15"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].scheduleId").value(eveningScheduleId))
                .andExpect(jsonPath("$.data[0].takenAt").exists());
    }

    // 남의 복용 시간이면 날짜가 기간 밖이어도 404 여야 한다.
    // 400(기간 아님)이 먼저 나가면, 남의 약의 복용 기간이 어떤지 알려주는 셈이다.
    @Test
    void 남의_복용_시간에_기간_밖_날짜로_체크해도_400이_아니라_404를_응답한다() throws Exception {
        createMedication(otherUserId, medicationJson("2026-11-01", "2026-11-30", "\"08:00\""));
        Long othersScheduleId = scheduleIdAt(otherUserId, "2026-11-15", "08:00");

        checkDose(myUserId, othersScheduleId, "2030-01-01")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"));
    }

    @Test
    void 남의_복용_시간을_체크하려다_막혀도_남의_기록은_생기지_않는다() throws Exception {
        createMedication(otherUserId, medicationJson("2026-11-01", null, "\"08:00\""));
        Long othersScheduleId = scheduleIdAt(otherUserId, "2026-11-15", "08:00");

        checkDose(myUserId, othersScheduleId, "2026-11-15").andExpect(status().isNotFound());
        flushAndClear();

        mockMvc.perform(get("/api/dose-logs").header(HttpHeaders.AUTHORIZATION, bearer(otherUserId)).param("date", "2026-11-15"))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void 종료일_당일은_체크되고_종료일_다음날은_400이다() throws Exception {
        createMedication(myUserId, medicationJson("2026-11-01", "2026-11-30", "\"08:00\""));
        Long scheduleId = scheduleIdAt(myUserId, "2026-11-30", "08:00");

        checkDose(myUserId, scheduleId, "2026-11-30").andExpect(status().isOk());
        checkDose(myUserId, scheduleId, "2026-12-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DOSE_DATE_OUT_OF_PERIOD"));
    }

    @Test
    void 시작일_당일은_체크되고_시작일_전날은_400이다() throws Exception {
        createMedication(myUserId, medicationJson("2026-11-10", null, "\"08:00\""));
        Long scheduleId = scheduleIdAt(myUserId, "2026-11-10", "08:00");

        checkDose(myUserId, scheduleId, "2026-11-10").andExpect(status().isOk());
        checkDose(myUserId, scheduleId, "2026-11-09")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DOSE_DATE_OUT_OF_PERIOD"));
    }

    // 회귀: "안 바뀐 시간의 dose_logs 가 CASCADE 로 지워지면 안 된다" (프로젝트 결정사항)를 API 흐름 전체로 확인한다.
    // 프론트가 "08:00" 대신 "08:00:00" 으로 보내도 같은 시간으로 보아야 기록이 유지된다 (버그 노트 2번).
    @Test
    void 약을_수정해도_안_바뀐_시간의_복용_기록은_남고_빠진_시간의_기록만_사라진다() throws Exception {
        Long medicationId = createMedication(myUserId, medicationJson("2026-11-01", null, "\"08:00\", \"20:00\""));
        Long morningScheduleId = scheduleIdAt(myUserId, "2026-11-15", "08:00");
        Long eveningScheduleId = scheduleIdAt(myUserId, "2026-11-15", "20:00");
        checkDose(myUserId, morningScheduleId, "2026-11-15").andExpect(status().isOk());
        checkDose(myUserId, eveningScheduleId, "2026-11-15").andExpect(status().isOk());
        flushAndClear();

        // 20:00 을 빼고 12:30 을 추가, 08:00 은 "08:00:00" 형태로 보낸다
        updateMedication(myUserId, medicationId, medicationJson("2026-11-01", null, "\"08:00:00\", \"12:30\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schedules", hasSize(2)));
        flushAndClear();

        // 아침 복용 시간은 같은 id 로 유지되고 기록도 남아 있다
        assertThat(scheduleIdAt(myUserId, "2026-11-15", "08:00")).isEqualTo(morningScheduleId);
        mockMvc.perform(get("/api/dose-logs").header(HttpHeaders.AUTHORIZATION, bearer(myUserId)).param("date", "2026-11-15"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].scheduleId").value(morningScheduleId));
    }

    @Test
    void 약을_수정하면_새로_생긴_복용_시간으로_바로_체크할_수_있다() throws Exception {
        Long medicationId = createMedication(myUserId, medicationJson("2026-11-01", null, "\"08:00\""));
        MvcResult updated = updateMedication(myUserId, medicationId, medicationJson("2026-11-01", null, "\"08:00\", \"12:30\""))
                .andExpect(status().isOk())
                .andReturn();
        List<Number> newScheduleIds = JsonPath.read(updated.getResponse().getContentAsString(),
                "$.data.schedules[?(@.time=='12:30')].id");
        assertThat(newScheduleIds).hasSize(1);

        checkDose(myUserId, newScheduleIds.get(0).longValue(), "2026-11-15").andExpect(status().isOk());
    }

    @Test
    void 약을_삭제하면_그_약의_복용_기록도_조회되지_않는다() throws Exception {
        Long medicationId = createMedication(myUserId, medicationJson("2026-11-01", null, "\"08:00\""));
        Long scheduleId = scheduleIdAt(myUserId, "2026-11-15", "08:00");
        checkDose(myUserId, scheduleId, "2026-11-15").andExpect(status().isOk());
        flushAndClear();

        mockMvc.perform(delete("/api/medications/" + medicationId).header(HttpHeaders.AUTHORIZATION, bearer(myUserId)))
                .andExpect(status().isOk());
        flushAndClear();

        mockMvc.perform(get("/api/dose-logs").header(HttpHeaders.AUTHORIZATION, bearer(myUserId)).param("date", "2026-11-15"))
                .andExpect(jsonPath("$.data", hasSize(0)));
        checkDose(myUserId, scheduleId, "2026-11-15").andExpect(status().isNotFound());
    }
}
