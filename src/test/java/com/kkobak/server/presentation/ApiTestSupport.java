package com.kkobak.server.presentation;

import org.springframework.beans.factory.annotation.Autowired;

import com.kkobak.server.application.auth.AccessTokenIssuerPort;

// 인증이 필요한 API 테스트의 공통 부모. 서버와 같은 발급기로 진짜 서명된 Access Token 을 만들어 보안 필터를 통과한다.
public abstract class ApiTestSupport {

    @Autowired
    private AccessTokenIssuerPort accessTokenIssuer;

    // Authorization 헤더 값 ("Bearer {Access Token}")
    protected String bearer(Long userId) {
        return "Bearer " + accessTokenIssuer.issue(userId).getValue();
    }
}
