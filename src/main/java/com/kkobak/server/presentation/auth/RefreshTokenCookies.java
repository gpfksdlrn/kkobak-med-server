package com.kkobak.server.presentation.auth;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.kkobak.server.global.config.AuthProperties;

import lombok.RequiredArgsConstructor;

/**
 * 리프레시 토큰 쿠키 만들기.
 * - HttpOnly: 자바스크립트(document.cookie)로 읽을 수 없다 → XSS 로 토큰을 훔쳐가기 어렵다
 * - Secure: HTTPS 에서만 보낸다 (로컬 http 개발 때만 끈다)
 * - SameSite: 다른 사이트에서 시작된 요청에 쿠키를 실을지 정한다 (Lax: 대부분 안 실음)
 * - Path=/api/auth: 재발급/로그아웃 요청에만 실리고, 다른 API 요청에는 실리지 않는다
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookies {

    public static final String COOKIE_NAME = "kkobak_refresh";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthProperties authProperties;

    public ResponseCookie create(String refreshToken, Duration validity) {
        return build(refreshToken, validity);
    }

    // 로그아웃: 같은 이름/경로로 만료 시간 0 인 쿠키를 보내면 브라우저가 지운다
    public ResponseCookie expire() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(authProperties.isRefreshCookieSecure())
                .sameSite(authProperties.getRefreshCookieSameSite())
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
