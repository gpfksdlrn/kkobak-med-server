package com.kkobak.server.presentation.doselog;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

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
import com.kkobak.server.domain.medication.MealTiming;
import com.kkobak.server.domain.medication.Medication;
import com.kkobak.server.domain.medication.MedicationRepository;
import com.kkobak.server.domain.user.User;
import com.kkobak.server.domain.user.UserRepository;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class DoseLogApiTest extends ApiTestSupport {

    private static final Instant USER_CREATED_AT = Instant.parse("2026-11-01T00:00:00Z");

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    private MockMvc mockMvc;
    private Long myUserId;
    private Long otherUserId;
    private Long myScheduleId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        myUserId = userRepository.save(new User("me@example.com", "나", "sub-me", USER_CREATED_AT)).getId();
        otherUserId = userRepository.save(new User("other@example.com", "남", "sub-other", USER_CREATED_AT)).getId();
        // 내 약: 11월 한 달 동안 08:00 에 복용
        myScheduleId = saveScheduleOf(myUserId, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
    }

    private Long saveScheduleOf(Long userId, LocalDate startDate, LocalDate endDate) {
        Medication medication = new Medication(userId, "타이레놀", null, MealTiming.NONE, startDate, endDate, NOW);
        medication.replaceScheduleTimes(Arrays.asList(LocalTime.of(8, 0)), NOW);
        return medicationRepository.save(medication).getSchedules().get(0).getId();
    }

    private ResultActions checkDose(Long userId, Long scheduleId, String scheduledDate) throws Exception {
        String json = "{\"scheduleId\": " + scheduleId + ", \"scheduledDate\": \"" + scheduledDate + "\"}";
        return mockMvc.perform(put("/api/dose-logs")
                .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void 복용_체크를_하면_복용_시각이_기록된_기록을_응답한다() throws Exception {
        checkDose(myUserId, myScheduleId, "2026-11-15")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.scheduleId").value(myScheduleId))
                .andExpect(jsonPath("$.data.scheduledDate").value("2026-11-15"))
                .andExpect(jsonPath("$.data.takenAt").value(notNullValue()));
    }

    @Test
    void 같은_날_두_번_체크해도_기록은_하나만_남는다() throws Exception {
        MvcResult first = checkDose(myUserId, myScheduleId, "2026-11-15").andReturn();
        MvcResult second = checkDose(myUserId, myScheduleId, "2026-11-15").andExpect(status().isOk()).andReturn();

        Number firstId = JsonPath.read(first.getResponse().getContentAsString(), "$.data.id");
        Number secondId = JsonPath.read(second.getResponse().getContentAsString(), "$.data.id");
        org.assertj.core.api.Assertions.assertThat(secondId.longValue()).isEqualTo(firstId.longValue());

        mockMvc.perform(get("/api/dose-logs").header(HttpHeaders.AUTHORIZATION, bearer(myUserId)).param("date", "2026-11-15"))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void 날짜별_조회는_그_날짜의_내_기록만_보여준다() throws Exception {
        Long otherScheduleId = saveScheduleOf(otherUserId, LocalDate.of(2026, 11, 1), null);
        checkDose(myUserId, myScheduleId, "2026-11-15");
        checkDose(myUserId, myScheduleId, "2026-11-16");
        checkDose(otherUserId, otherScheduleId, "2026-11-15");

        mockMvc.perform(get("/api/dose-logs").header(HttpHeaders.AUTHORIZATION, bearer(myUserId)).param("date", "2026-11-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].scheduleId").value(myScheduleId))
                .andExpect(jsonPath("$.data[0].scheduledDate").value("2026-11-15"));
    }

    @Test
    void 기록이_없는_날짜는_빈_목록을_응답한다() throws Exception {
        mockMvc.perform(get("/api/dose-logs").header(HttpHeaders.AUTHORIZATION, bearer(myUserId)).param("date", "2026-11-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void 남의_복용_시간을_체크하면_404를_응답한다() throws Exception {
        Long otherScheduleId = saveScheduleOf(otherUserId, LocalDate.of(2026, 11, 1), null);

        checkDose(myUserId, otherScheduleId, "2026-11-15")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"));
    }

    @Test
    void 없는_복용_시간을_체크하면_404를_응답한다() throws Exception {
        checkDose(myUserId, 999999L, "2026-11-15")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"));
    }

    @Test
    void 복용_기간이_아닌_날짜를_체크하면_400을_응답한다() throws Exception {
        checkDose(myUserId, myScheduleId, "2026-12-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DOSE_DATE_OUT_OF_PERIOD"))
                .andExpect(jsonPath("$.error.message").value("복용 기간이 아닌 날짜입니다."));
    }

    @Test
    void 날짜가_없으면_400과_검증_메시지를_응답한다() throws Exception {
        mockMvc.perform(put("/api/dose-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(myUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\": " + myScheduleId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.message").value("복용 날짜를 입력해주세요."));
    }

    @Test
    void 조회할_날짜를_빼먹으면_400을_응답한다() throws Exception {
        mockMvc.perform(get("/api/dose-logs").header(HttpHeaders.AUTHORIZATION, bearer(myUserId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }
}
