package com.kkobak.server.presentation;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.kkobak.server.TestcontainersConfiguration;

// 프론트는 error.code 로 분기한다. 그래서 "잘못된 요청"이 어떤 경로로 들어와도
// 500 이 아니라 봉투(success=false + error.code)로 돌아와야 한다. 실제 스프링 설정(핸들러 포함)으로 검증한다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ErrorResponseApiTest extends ApiTestSupport {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Test
    void 없는_주소를_호출하면_404와_NOT_FOUND_봉투를_응답한다() throws Exception {
        mockMvc.perform(get("/api/없는-주소").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void 지원하지_않는_메서드로_호출하면_405와_METHOD_NOT_ALLOWED_봉투를_응답한다() throws Exception {
        mockMvc.perform(delete("/api/medications").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void 약_id가_숫자가_아니면_400과_INVALID_INPUT_봉투를_응답한다() throws Exception {
        mockMvc.perform(get("/api/medications/abc").header(HttpHeaders.AUTHORIZATION, bearer(1L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    // 잡는 버그: 위조/깨진 토큰이 통과하거나, 401 이 봉투가 아닌 빈 응답으로 나가는 것
    @Test
    void 위조된_Access_Token이면_401과_실패_봉투를_응답한다() throws Exception {
        mockMvc.perform(get("/api/medications").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void JSON_형식이_깨진_본문은_400과_INVALID_INPUT_봉투를_응답한다() throws Exception {
        mockMvc.perform(post("/api/medications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 본문이_아예_없으면_400을_응답한다() throws Exception {
        mockMvc.perform(put("/api/dose-logs").header(HttpHeaders.AUTHORIZATION, bearer(1L)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 정의되지_않은_복용_방법_값은_400을_응답한다() throws Exception {
        String json = "{\"name\": \"약\", \"mealTiming\": \"AFTER_DINNER\", \"startDate\": \"2026-11-01\","
                + "\"times\": [\"08:00\"]}";

        mockMvc.perform(post("/api/medications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    // 클라이언트 실수(Content-Type)를 서버 오류(500)로 알리면 안 된다. 정확한 코드(415)는 정하지 않았으므로 "4xx + 실패 봉투"만 본다.
    @Test
    void JSON이_아닌_Content_Type으로_보내도_500이_아니라_4xx_실패_봉투를_응답한다() throws Exception {
        mockMvc.perform(put("/api/dose-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1L))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("scheduleId=1"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    // 잡는 버그: Accept 에 JSON 이 없을 때 406 이 "빈 본문"으로 나가서 봉투 형식이 깨지는 것
    @Test
    void JSON을_받지_않겠다고_하면_406이지만_본문은_JSON_실패_봉투다() throws Exception {
        mockMvc.perform(get("/api/medications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1L))
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_ACCEPTABLE"));
    }
}
