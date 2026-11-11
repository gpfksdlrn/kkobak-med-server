package com.kkobak.server.presentation.medication;

import java.time.Instant;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

// 서버 전체를 띄우고(실제 DB 포함) HTTP 요청부터 DB 까지 한 번에 검증한다.
// @Transactional: 각 테스트가 끝나면 DB 변경을 되돌린다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class MedicationApiTest extends ApiTestSupport {

    private static final Instant USER_CREATED_AT = Instant.parse("2026-11-01T00:00:00Z");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private Long myUserId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        myUserId = userRepository.save(new User("me@example.com", "나", "sub-me", USER_CREATED_AT)).getId();
        otherUserId = userRepository.save(new User("other@example.com", "남", "sub-other", USER_CREATED_AT)).getId();
    }

    private String medicationJson(String name, String startDate, String endDate, String times) {
        String endDateValue = endDate == null ? "null" : "\"" + endDate + "\"";
        return "{"
                + "\"name\": \"" + name + "\","
                + "\"dosageText\": \"1정\","
                + "\"mealTiming\": \"AFTER_MEAL\","
                + "\"startDate\": \"" + startDate + "\","
                + "\"endDate\": " + endDateValue + ","
                + "\"times\": [" + times + "]"
                + "}";
    }

    private ResultActions createMedication(Long userId, String json) throws Exception {
        return mockMvc.perform(post("/api/medications")
                .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private Long createMedicationAndGetId(Long userId, String name) throws Exception {
        MvcResult result = createMedication(userId, medicationJson(name, "2026-11-01", null, "\"08:00\""))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    @Test
    void 약을_등록하면_201과_성공_봉투에_등록된_약을_담아_응답한다() throws Exception {
        createMedication(myUserId, medicationJson("타이레놀", "2026-11-01", "2026-11-30", "\"20:00\", \"08:00\""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("타이레놀"))
                .andExpect(jsonPath("$.data.mealTiming").value("AFTER_MEAL"))
                .andExpect(jsonPath("$.data.startDate").value("2026-11-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-11-30"))
                // 순서대로 정렬되고, 초 없이 HH:mm 으로 내려온다
                .andExpect(jsonPath("$.data.schedules[*].time", contains("08:00", "20:00")))
                // 복용 체크에 쓸 복용 시간 id 도 함께 내려온다
                .andExpect(jsonPath("$.data.schedules[0].id").value(notNullValue()))
                .andExpect(jsonPath("$.data.schedules[1].id").value(notNullValue()));
    }

    @Test
    void 내_약_목록은_최근_등록순이고_남의_약은_보이지_않는다() throws Exception {
        createMedicationAndGetId(myUserId, "먼저등록");
        createMedicationAndGetId(myUserId, "나중등록");
        createMedicationAndGetId(otherUserId, "남의약");

        mockMvc.perform(get("/api/medications").header(HttpHeaders.AUTHORIZATION, bearer(myUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("나중등록"))
                .andExpect(jsonPath("$.data[1].name").value("먼저등록"));
    }

    @Test
    void 해당_날짜에_복용_중인_약만_조회된다() throws Exception {
        createMedication(myUserId, medicationJson("복용중", "2026-11-01", "2026-11-30", "\"08:00\""));
        createMedication(myUserId, medicationJson("끝난약", "2026-10-01", "2026-10-31", "\"08:00\""));

        mockMvc.perform(get("/api/medications/active").header(HttpHeaders.AUTHORIZATION, bearer(myUserId)).param("date", "2026-11-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("복용중"));
    }

    @Test
    void 약을_수정하면_값과_복용_시간이_바뀐다() throws Exception {
        Long medicationId = createMedicationAndGetId(myUserId, "타이레놀");

        mockMvc.perform(put("/api/medications/" + medicationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(myUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(medicationJson("이부프로펜", "2026-11-02", null, "\"08:00\", \"12:30\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("이부프로펜"))
                .andExpect(jsonPath("$.data.startDate").value("2026-11-02"))
                .andExpect(jsonPath("$.data.schedules[*].time", contains("08:00", "12:30")))
                // 수정 중 새로 추가된 12:30 에도 id 가 있어야 바로 복용 체크를 할 수 있다
                .andExpect(jsonPath("$.data.schedules[1].id").value(notNullValue()));
    }

    @Test
    void 약을_삭제하면_다시_조회할_수_없다() throws Exception {
        Long medicationId = createMedicationAndGetId(myUserId, "타이레놀");

        mockMvc.perform(delete("/api/medications/" + medicationId).header(HttpHeaders.AUTHORIZATION, bearer(myUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()));

        mockMvc.perform(get("/api/medications/" + medicationId).header(HttpHeaders.AUTHORIZATION, bearer(myUserId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MEDICATION_NOT_FOUND"));
    }

    @Test
    void 남의_약을_조회하면_404와_실패_봉투를_응답한다() throws Exception {
        Long othersMedicationId = createMedicationAndGetId(otherUserId, "남의약");

        mockMvc.perform(get("/api/medications/" + othersMedicationId).header(HttpHeaders.AUTHORIZATION, bearer(myUserId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("MEDICATION_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("약을 찾을 수 없습니다."));
    }

    @Test
    void 이름이_비어있으면_400과_검증_메시지를_응답한다() throws Exception {
        createMedication(myUserId, medicationJson(" ", "2026-11-01", null, "\"08:00\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.message").value("약 이름을 입력해주세요."));
    }

    @Test
    void 복용_시간이_없으면_400을_응답한다() throws Exception {
        createMedication(myUserId, medicationJson("타이레놀", "2026-11-01", null, ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("복용 시간을 하나 이상 추가해주세요."));
    }

    @Test
    void 종료일이_시작일보다_빠르면_400을_응답한다() throws Exception {
        createMedication(myUserId, medicationJson("타이레놀", "2026-11-10", "2026-11-01", "\"08:00\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_MEDICATION_PERIOD"))
                .andExpect(jsonPath("$.error.message").value("종료일은 시작일보다 빠를 수 없습니다."));
    }

    @Test
    void 시간_형식이_틀리면_400을_응답한다() throws Exception {
        createMedication(myUserId, medicationJson("타이레놀", "2026-11-01", null, "\"25:99\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void Access_Token이_없으면_401과_실패_봉투를_응답한다() throws Exception {
        mockMvc.perform(get("/api/medications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
