package com.kkobak.server.presentation.auth;

import com.kkobak.server.application.auth.TokenResult;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 응답 본문에는 Access Token 만 담는다. Refresh Token 은 HttpOnly 쿠키로만 내려간다 (자바스크립트가 못 읽게).
@Getter
@RequiredArgsConstructor
public class TokenResponse {

    private static final String BEARER = "Bearer";

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;   // Access Token 유효 시간(초)

    public static TokenResponse from(TokenResult result) {
        return new TokenResponse(result.getAccessToken(), BEARER, result.getAccessTokenValidity().getSeconds());
    }
}
