package com.kkobak.server.application.health;

import org.springframework.stereotype.Component;

import com.kkobak.server.domain.health.HealthCheckResult;

import lombok.RequiredArgsConstructor;

// 헬스체크 유스케이스의 입구. 지금은 Service 하나만 부르지만, 점검 대상이 늘면 여기서 조합한다.
@Component
@RequiredArgsConstructor
public class HealthFacade {

    private final HealthService healthService;

    public HealthCheckResult checkHealth() {
        return healthService.checkHealth();
    }
}
