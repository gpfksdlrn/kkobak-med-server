package com.kkobak.server.application.auth;

// "이 Google ID 토큰이 진짜인지 확인하고 사용자 정보를 꺼내줘"라는 요구. 구현(Google 공개키 검증)은 infrastructure 가 한다.
public interface GoogleTokenVerifierPort {

    // 진짜가 아니면(서명/발급자/대상/만료/이메일 미인증) BusinessException(INVALID_GOOGLE_TOKEN) 을 던진다
    GoogleAccount verify(String idToken);
}
