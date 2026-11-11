package com.kkobak.server.presentation.health;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kkobak.server.application.health.DatabasePingPort;
import com.kkobak.server.application.health.HealthFacade;
import com.kkobak.server.application.health.HealthService;

// Spring 컨텍스트 없이 Controller 를 직접 만들어 HTTP 요청/응답을 검증한다. DB 대신 가짜 포트를 끼운다.
class HealthControllerTest {

    private MockMvc createMockMvc(final boolean isDatabaseReachable) {
        DatabasePingPort fakePort = new DatabasePingPort() {
            @Override
            public boolean isDatabaseReachable() {
                return isDatabaseReachable;
            }
        };
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC);
        HealthFacade healthFacade = new HealthFacade(new HealthService(fakePort, fixedClock));
        return MockMvcBuilders.standaloneSetup(new HealthController(healthFacade)).build();
    }

    @Test
    void 서버가_정상이면_200과_성공_봉투에_UP을_담아_응답한다() throws Exception {
        MockMvc mockMvc = createMockMvc(true);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.database").value("UP"))
                .andExpect(jsonPath("$.data.checkedAt").exists())
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void 데이터베이스가_죽어있으면_503과_실패_봉투를_응답한다() throws Exception {
        MockMvc mockMvc = createMockMvc(false);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"));
    }
}
