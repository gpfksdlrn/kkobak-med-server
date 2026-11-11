package com.kkobak.server.application.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.kkobak.server.domain.health.HealthCheckResult;
import com.kkobak.server.domain.health.HealthStatus;

// Spring 도 DB 도 없이, 순수 자바로만 하는 단위 테스트. 포트를 가짜로 바꿔 끼울 수 있는 것이 클린 아키텍처의 이점이다.
class HealthServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-01T00:00:00Z");

    private final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Test
    void 데이터베이스가_응답하면_전체_상태는_UP이다() {
        HealthService healthService = new HealthService(new FakeDatabasePingPort(true), fixedClock);

        HealthCheckResult result = healthService.checkHealth();

        assertThat(result.getStatus()).isEqualTo(HealthStatus.UP);
        assertThat(result.getDatabase()).isEqualTo(HealthStatus.UP);
    }

    @Test
    void 데이터베이스가_응답하지_않으면_전체_상태는_DOWN이다() {
        HealthService healthService = new HealthService(new FakeDatabasePingPort(false), fixedClock);

        HealthCheckResult result = healthService.checkHealth();

        assertThat(result.getStatus()).isEqualTo(HealthStatus.DOWN);
        assertThat(result.getDatabase()).isEqualTo(HealthStatus.DOWN);
    }

    @Test
    void 점검_시각은_주입된_Clock_기준이다() {
        HealthService healthService = new HealthService(new FakeDatabasePingPort(true), fixedClock);

        HealthCheckResult result = healthService.checkHealth();

        assertThat(result.getCheckedAt()).isEqualTo(FIXED_NOW);
    }

    private static class FakeDatabasePingPort implements DatabasePingPort {

        private final boolean isReachable;

        FakeDatabasePingPort(boolean isReachable) {
            this.isReachable = isReachable;
        }

        @Override
        public boolean isDatabaseReachable() {
            return isReachable;
        }
    }
}
