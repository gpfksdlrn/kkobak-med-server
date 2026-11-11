package com.kkobak.server.application.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 검증된 Google ID 토큰에서 꺼낸 사용자 정보
@Getter
@RequiredArgsConstructor
public class GoogleAccount {

    private final String googleSub;   // Google 이 주는 사용자 고유 ID (이메일은 바뀔 수 있어서 이걸로 회원을 찾는다)
    private final String email;
    private final String name;
}
