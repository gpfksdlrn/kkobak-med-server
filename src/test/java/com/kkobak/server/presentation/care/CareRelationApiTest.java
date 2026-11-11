package com.kkobak.server.presentation.care;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.kkobak.server.TestcontainersConfiguration;
import com.kkobak.server.presentation.ApiTestSupport;
import com.kkobak.server.domain.user.User;
import com.kkobak.server.domain.user.UserRepository;

// 가족 연결과 그에 따른 약/복용 기록 권한을 실제 HTTP 요청으로 검증한다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CareRelationApiTest extends ApiTestSupport {

    private static final Instant USER_CREATED_AT = Instant.parse("2026-11-01T00:00:00Z");
    private static final String MEDICATION_JSON = "{\"name\": \"%s\", \"dosageText\": \"1정\", \"mealTiming\": \"AFTER_MEAL\", "
            + "\"startDate\": \"2026-11-01\", \"endDate\": \"2026-11-30\", \"times\": [\"08:00\"]}";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private Long patientId;
    private Long caregiverId;
    private Long strangerId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        patientId = userRepository.save(new User("patient@example.com", "환자", "sub-patient", USER_CREATED_AT)).getId();
        caregiverId = userRepository.save(new User("family@example.com", "가족", "sub-family", USER_CREATED_AT)).getId();
        strangerId = userRepository.save(new User("stranger@example.com", "남", "sub-stranger", USER_CREATED_AT)).getId();
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, Long userId) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    private ResultActions invite(Long inviterId, String email) throws Exception {
        return perform(post("/api/care-relations/invitations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\"}"), inviterId);
    }

    private Long readLong(MvcResult result, String path) throws Exception {
        Number value = JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), path);
        return value.longValue();
    }

    // 환자가 가족을 초대하고 가족이 수락해서 연결을 만든다. 연결 id 를 돌려준다.
    private Long connect() throws Exception {
        Long relationId = readLong(invite(patientId, "family@example.com").andExpect(status().isCreated()).andReturn(), "$.data.id");
        perform(post("/api/care-relations/" + relationId + "/accept"), caregiverId).andExpect(status().isOk());
        return relationId;
    }

    // 약을 등록하고 응답을 돌려준다 (patientId 가 있으면 그 환자의 약으로)
    private MvcResult createMedication(Long actorId, Long targetPatientId, String name) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/medications").contentType(MediaType.APPLICATION_JSON)
                .content(String.format(MEDICATION_JSON, name));
        if (targetPatientId != null) {
            request.param("patientId", String.valueOf(targetPatientId));
        }
        return perform(request, actorId).andExpect(status().isCreated()).andReturn();
    }

    @Test
    void 환자가_이메일로_초대하고_가족이_수락하면_연결된다() throws Exception {
        MvcResult invited = invite(patientId, "family@example.com")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.patient.email").value("patient@example.com"))
                .andExpect(jsonPath("$.data.caregiver.email").value("family@example.com"))
                .andReturn();
        Long relationId = readLong(invited, "$.data.id");

        // 가족의 목록에 받은 초대가 보인다
        perform(get("/api/care-relations"), caregiverId)
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        perform(post("/api/care-relations/" + relationId + "/accept"), caregiverId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.acceptedAt").exists());
    }

    // 잡는 버그: 초대만 해도(수락 전) 가족이 환자의 약·복용 기록에 접근하는 것
    @Test
    void 수락_전에는_가족이_환자_데이터에_접근할_수_없다() throws Exception {
        invite(patientId, "family@example.com").andExpect(status().isCreated());
        MvcResult medication = createMedication(patientId, null, "환자약");
        Long medicationId = readLong(medication, "$.data.id");
        Long scheduleId = readLong(medication, "$.data.schedules[0].id");

        perform(get("/api/medications").param("patientId", String.valueOf(patientId)), caregiverId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATIENT_NOT_FOUND"));
        perform(get("/api/medications/" + medicationId), caregiverId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MEDICATION_NOT_FOUND"));
        perform(put("/api/dose-logs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"scheduleId\": " + scheduleId + ", \"scheduledDate\": \"2026-11-02\"}"), caregiverId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"));
    }

    // 잡는 버그: 수락된 가족이 환자 대신 약을 관리하거나 복용 체크를 못 하는 것, 누가 체크했는지 안 남는 것
    @Test
    void 수락된_가족은_환자의_약을_등록_조회_수정_삭제하고_대신_체크할_수_있다() throws Exception {
        connect();

        // 가족이 환자 대신 등록 → 환자의 약 목록에 나온다
        MvcResult created = createMedication(caregiverId, patientId, "가족이등록한약");
        Long medicationId = readLong(created, "$.data.id");
        Long scheduleId = readLong(created, "$.data.schedules[0].id");
        perform(get("/api/medications"), patientId)
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("가족이등록한약"));

        // 가족이 patientId 로 조회·수정
        perform(get("/api/medications").param("patientId", String.valueOf(patientId)), caregiverId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
        perform(put("/api/medications/" + medicationId).contentType(MediaType.APPLICATION_JSON)
                .content(String.format(MEDICATION_JSON, "가족이수정한약")), caregiverId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("가족이수정한약"));

        // 가족이 대신 체크 → 환자가 보는 기록에 "가족이 체크함"이 남는다
        perform(put("/api/dose-logs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"scheduleId\": " + scheduleId + ", \"scheduledDate\": \"2026-11-02\"}"), caregiverId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedBy").value(caregiverId));
        perform(get("/api/dose-logs").param("date", "2026-11-02"), patientId)
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].checkedBy").value(caregiverId));
        perform(get("/api/dose-logs").param("date", "2026-11-02").param("patientId", String.valueOf(patientId)), caregiverId)
                .andExpect(jsonPath("$.data", hasSize(1)));

        perform(delete("/api/medications/" + medicationId), caregiverId).andExpect(status().isOk());
        perform(get("/api/medications"), patientId).andExpect(jsonPath("$.data", hasSize(0)));
    }

    // 잡는 버그: 가족이 patientId 없이 조회했는데 환자 약이 섞여 나와 "내 약" 화면이 오염되는 것
    @Test
    void 가족이_patientId_없이_조회하면_자기_약만_나온다() throws Exception {
        connect();
        createMedication(patientId, null, "환자약");
        createMedication(caregiverId, null, "가족본인약");

        perform(get("/api/medications"), caregiverId)
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("가족본인약"));
    }

    // 잡는 버그: 연결 방향을 무시해서, 환자도 자기를 돌보는 가족의 약을 볼 수 있게 되는 것
    @Test
    void 연결은_한_방향이라_환자는_가족의_데이터를_볼_수_없다() throws Exception {
        connect();
        Long caregiverMedicationId = readLong(createMedication(caregiverId, null, "가족본인약"), "$.data.id");

        perform(get("/api/medications").param("patientId", String.valueOf(caregiverId)), patientId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PATIENT_NOT_FOUND"));
        perform(get("/api/medications/" + caregiverMedicationId), patientId)
                .andExpect(status().isNotFound());
    }

    // 잡는 버그: 연결을 끊은 뒤에도 가족이 환자 데이터에 계속 접근하는 것
    @Test
    void 연결을_끊으면_가족은_바로_환자_데이터에_접근할_수_없다() throws Exception {
        Long relationId = connect();
        Long medicationId = readLong(createMedication(patientId, null, "환자약"), "$.data.id");
        perform(get("/api/medications/" + medicationId), caregiverId).andExpect(status().isOk());

        perform(delete("/api/care-relations/" + relationId), patientId).andExpect(status().isOk());

        perform(get("/api/medications/" + medicationId), caregiverId).andExpect(status().isNotFound());
        perform(get("/api/care-relations"), caregiverId).andExpect(jsonPath("$.data", hasSize(0)));
    }

    // 잡는 버그: 제3자가 남의 연결을 수락/삭제하거나, 환자가 스스로 수락해 상대 동의 없이 연결하는 것
    @Test
    void 초대받은_가족이_아니면_수락할_수_없고_당사자가_아니면_삭제할_수_없다() throws Exception {
        Long relationId = readLong(invite(patientId, "family@example.com").andReturn(), "$.data.id");

        perform(post("/api/care-relations/" + relationId + "/accept"), patientId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CARE_RELATION_NOT_FOUND"));
        perform(post("/api/care-relations/" + relationId + "/accept"), strangerId)
                .andExpect(status().isNotFound());
        perform(delete("/api/care-relations/" + relationId), strangerId)
                .andExpect(status().isNotFound());
        perform(get("/api/care-relations"), caregiverId)
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void 초대_오류는_각각_알맞은_코드로_응답한다() throws Exception {
        invite(patientId, "nobody@example.com")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
        invite(patientId, "patient@example.com")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CANNOT_INVITE_SELF"));
        invite(patientId, "not-an-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

        invite(patientId, "family@example.com").andExpect(status().isCreated());
        invite(patientId, "family@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CARE_RELATION_ALREADY_EXISTS"));
    }
}
