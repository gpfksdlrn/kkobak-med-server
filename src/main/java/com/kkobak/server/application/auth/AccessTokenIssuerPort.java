package com.kkobak.server.application.auth;

// Access Token(JWT) 발급 요구. 서명 방식은 infrastructure 가 정한다.
public interface AccessTokenIssuerPort {

    AccessToken issue(Long userId);
}
