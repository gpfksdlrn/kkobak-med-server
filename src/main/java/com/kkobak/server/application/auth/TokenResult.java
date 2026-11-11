package com.kkobak.server.application.auth;

import java.time.Duration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 로그인/재발급 결과로 나가는 두 토큰. 리프레시 토큰 원문은 쿠키로만 내보내고 응답 본문에는 넣지 않는다.
@Getter
@RequiredArgsConstructor
public class TokenResult {

    private final String accessToken;
    private final Duration accessTokenValidity;
    private final String refreshToken;
    private final Duration refreshTokenValidity;
}
