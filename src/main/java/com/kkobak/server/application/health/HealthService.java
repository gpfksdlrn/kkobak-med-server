package com.kkobak.server.application.health;

import java.time.Clock;

import org.springframework.stereotype.Service;

import com.kkobak.server.domain.health.HealthCheckResult;
import com.kkobak.server.domain.health.HealthStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HealthService {

    private final DatabasePingPort databasePingPort;
    private final Clock clock;

    public HealthCheckResult checkHealth() {
        HealthStatus databaseStatus = HealthStatus.DOWN;
        if (databasePingPort.isDatabaseReachable()) {
            databaseStatus = HealthStatus.UP;
        }

        // DB 가 죽어 있으면 서버 전체도 정상이 아니다
        return new HealthCheckResult(databaseStatus, databaseStatus, clock.instant());
    }
}
