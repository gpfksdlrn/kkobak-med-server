package com.kkobak.server.presentation.health;

import java.time.Instant;

import com.kkobak.server.domain.health.HealthCheckResult;
import com.kkobak.server.domain.health.HealthStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// HTTP 응답으로 내려가는 모양. 도메인 객체를 그대로 노출하지 않고 이 DTO 로 한 번 감싼다.
@Getter
@RequiredArgsConstructor
public class HealthResponse {

    private final HealthStatus status;
    private final HealthStatus database;
    private final Instant checkedAt;

    public static HealthResponse from(HealthCheckResult result) {
        return new HealthResponse(result.getStatus(), result.getDatabase(), result.getCheckedAt());
    }
}
