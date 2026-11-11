package com.kkobak.server.presentation.medication;

import java.time.Instant;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
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

import jakarta.persistence.EntityManager;

// 보안 경계: 남의 약 id 를 넣고 수정/삭제/조회했을 때 막히고, 막힌 뒤에도 약이 그대로인지 DB 에서 다시 읽어 확인한다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class MedicationOwnershipApiTest extends ApiTestSupport {

    private static final Instant USER_CREATED_AT = Instant.parse("2026-11-01T00:00:00Z");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private Long ownerId;
    private Long intruderId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        ownerId = userRepository.save(new User("owner-api@example.com", "주인", "sub-owner-api", USER_CREATED_AT)).getId();
        intruderId = userRepository.save(new User("intruder-api@example.com", "침입자", "sub-intruder-api", USER_CREATED_AT)).getId();
    }

    private String json(String name, String startDate, String endDate, String times) {
        String endDateValue = endDate == null ? "null" : "\"" + endDate + "\"";
        return "{\"name\": \"" + name + "\", \"dosageText\": \"1정\", \"mealTiming\": \"AFTER_MEAL\","
                + "\"startDate\": \"" + startDate + "\", \"endDate\": " + endDateValue + ","
                + "\"times\": [" + times + "]}";
    }

    private ResultActions create(Long userId, String json) throws Exception {
        return mockMvc.perform(post("/api/medications")
                .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions update(Long userId, Long medicationId, String json) throws Exception {
        return mockMvc.perform(put("/api/medications/" + medicationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private Long createAndGetId(Long userId, String json) throws Exception {
        MvcResult result = create(userId, json).andExpect(status().isCreated()).andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    // 영속성 컨텍스트 캐시를 비워서, 다음 조회가 메모리가 아니라 진짜 DB 에서 읽게 한다
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 남의_약을_수정하면_404이고_약은_원래_값_그대로다() throws Exception {
        Long medicationId = createAndGetId(ownerId, json("타이레놀", "2026-11-01", "2026-11-30", "\"08:00\""));
        flushAndClear();

        update(intruderId, medicationId, json("해킹된약", "2026-12-01", null, "\"23:00\""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MEDICATION_NOT_FOUND"));
        flushAndClear();

        mockMvc.perform(get("/api/medications/" + medicationId).header(HttpHeaders.AUTHORIZATION, bearer(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("타이레놀"))
                .andExpect(jsonPath("$.data.startDate").value("2026-11-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-11-30"))
                .andExpect(jsonPath("$.data.schedules[*].time", contains("08:00")));
    }

    @Test
    void 남의_약을_삭제하면_404이고_약은_계속_남아있다() throws Exception {
        Long medicationId = createAndGetId(ownerId, json("타이레놀", "2026-11-01", null, "\"08:00\""));
        flushAndClear();

        mockMvc.perform(delete("/api/medications/" + medicationId).header(HttpHeaders.AUTHORIZATION, bearer(intruderId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MEDICATION_NOT_FOUND"));
        flushAndClear();

        mockMvc.perform(get("/api/medications/" + medicationId).header(HttpHeaders.AUTHORIZATION, bearer(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("타이레놀"));
    }

    // 남의 약과 없는 약의 응답이 한 글자라도 다르면, 그 차이로 "그 id 의 약이 존재한다"는 사실이 새어 나간다
    @Test
    void 남의_약과_없는_약은_조회_수정_삭제_모두_똑같은_응답을_준다() throws Exception {
        Long othersMedicationId = createAndGetId(ownerId, json("타이레놀", "2026-11-01", null, "\"08:00\""));
        Long missingMedicationId = 999999L;
        String body = json("수정", "2026-11-01", null, "\"09:00\"");

        String othersGet = mockMvc.perform(get("/api/medications/" + othersMedicationId).header(HttpHeaders.AUTHORIZATION, bearer(intruderId)))
                .andReturn().getResponse().getContentAsString();
        String missingGet = mockMvc.perform(get("/api/medications/" + missingMedicationId).header(HttpHeaders.AUTHORIZATION, bearer(intruderId)))
                .andReturn().getResponse().getContentAsString();
        MvcResult othersPut = update(intruderId, othersMedicationId, body).andReturn();
        MvcResult missingPut = update(intruderId, missingMedicationId, body).andReturn();
        MvcResult othersDelete = mockMvc.perform(delete("/api/medications/" + othersMedicationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(intruderId))).andReturn();
        MvcResult missingDelete = mockMvc.perform(delete("/api/medications/" + missingMedicationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(intruderId))).andReturn();

        assertThat(othersGet).isEqualTo(missingGet);
        assertThat(othersPut.getResponse().getStatus()).isEqualTo(404).isEqualTo(missingPut.getResponse().getStatus());
        assertThat(othersPut.getResponse().getContentAsString()).isEqualTo(missingPut.getResponse().getContentAsString());
        assertThat(othersDelete.getResponse().getStatus()).isEqualTo(404).isEqualTo(missingDelete.getResponse().getStatus());
        assertThat(othersDelete.getResponse().getContentAsString()).isEqualTo(missingDelete.getResponse().getContentAsString());
    }

    @Test
    void 남의_약은_날짜별_복용_중_목록에도_나오지_않는다() throws Exception {
        createAndGetId(ownerId, json("주인의약", "2026-11-01", null, "\"08:00\""));
        createAndGetId(intruderId, json("침입자의약", "2026-11-01", null, "\"09:00\""));

        mockMvc.perform(get("/api/medications/active").header(HttpHeaders.AUTHORIZATION, bearer(intruderId)).param("date", "2026-11-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("침입자의약"));
    }

    @Test
    void 수정할_때_종료일이_시작일보다_빠르면_400이고_약은_원래_값_그대로다() throws Exception {
        Long medicationId = createAndGetId(ownerId, json("타이레놀", "2026-11-01", "2026-11-30", "\"08:00\""));
        flushAndClear();

        update(ownerId, medicationId, json("바뀐이름", "2026-11-10", "2026-11-01", "\"09:00\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_MEDICATION_PERIOD"));
        flushAndClear();

        mockMvc.perform(get("/api/medications/" + medicationId).header(HttpHeaders.AUTHORIZATION, bearer(ownerId)))
                .andExpect(jsonPath("$.data.name").value("타이레놀"))
                .andExpect(jsonPath("$.data.schedules[*].time", contains("08:00")));
    }

    // 경계값: 시작일과 종료일이 같은 하루짜리 복용은 허용되어야 한다 (DB CHECK 도 end_date >= start_date)
    @Test
    void 시작일과_종료일이_같은_하루짜리_약은_등록된다() throws Exception {
        create(ownerId, json("하루약", "2026-11-05", "2026-11-05", "\"08:00\""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.startDate").value("2026-11-05"))
                .andExpect(jsonPath("$.data.endDate").value("2026-11-05"));
    }

    // 회귀(버그 노트 2번): "08:00", "08:00:00", "08:00:45" 는 같은 복용 시간이다. 프론트에서 중복으로 보이면 안 된다.
    @Test
    void 초가_붙은_시간을_보내도_같은_시간은_하나로_합쳐지고_HH_mm으로_응답한다() throws Exception {
        create(ownerId, json("타이레놀", "2026-11-01", null, "\"08:00\", \"08:00:00\", \"08:00:45\", \"20:00:10\""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.schedules", hasSize(2)))
                .andExpect(jsonPath("$.data.schedules[*].time", contains("08:00", "20:00")));
    }
}
