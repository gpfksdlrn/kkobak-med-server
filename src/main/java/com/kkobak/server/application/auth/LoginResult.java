package com.kkobak.server.application.auth;

import com.kkobak.server.application.user.UserResult;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LoginResult {

    private final TokenResult tokens;
    private final UserResult user;
}
