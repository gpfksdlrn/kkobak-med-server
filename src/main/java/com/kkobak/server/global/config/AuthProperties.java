package com.kkobak.server.global.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// application.yml 의 kkobak.auth.* 값을 담는다. 생성자로 한 번 채우면 바뀌지 않는다.
@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "kkobak.auth")
@Validated
public class AuthProperties {

    @NotBlank
    private final String googleClientId;
    @NotBlank
    private final String jwtSecret;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;
    // 운영에서 ALLOWED_ORIGINS 를 빼먹으면 서버는 뜨는데 프론트 요청이 전부 CORS 에 막힌다 → 시작할 때 막는다
    @NotEmpty
    private final List<String> allowedOrigins;
}
