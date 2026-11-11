package com.kkobak.server.presentation.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kkobak.server.application.health.HealthFacade;
import com.kkobak.server.domain.health.HealthCheckResult;
import com.kkobak.server.domain.health.HealthStatus;
import com.kkobak.server.global.exception.ErrorCode;
import com.kkobak.server.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController implements HealthApiSpec {

    private final HealthFacade healthFacade;

    @Override
    @GetMapping
    public ResponseEntity<?> health() {
        HealthCheckResult result = healthFacade.checkHealth();

        if (result.getStatus() == HealthStatus.UP) {
            return ResponseEntity.ok(ApiResponse.success(HealthResponse.from(result)));
        }
        ErrorCode errorCode = ErrorCode.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode, "데이터베이스에 연결할 수 없습니다."));
    }
}
