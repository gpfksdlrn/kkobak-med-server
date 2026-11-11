package com.kkobak.server.domain.health;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 헬스체크 결과. 순수 자바 객체라서 Spring 이나 DB 를 전혀 모른다.
@Getter
@RequiredArgsConstructor
public class HealthCheckResult {

    private final HealthStatus status;
    private final HealthStatus database;
    private final Instant checkedAt;
}
