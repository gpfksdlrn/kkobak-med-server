package com.kkobak.server.global.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// application.yml 의 kkobak.auth.* 값을 담는다. 생성자로 한 번 채우면 바뀌지 않는다.
@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "kkobak.auth")
public class AuthProperties {

    private final String googleClientId;
    private final String jwtSecret;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;
    private final List<String> allowedOrigins;
}
