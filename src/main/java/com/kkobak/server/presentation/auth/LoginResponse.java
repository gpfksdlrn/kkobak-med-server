package com.kkobak.server.presentation.auth;

import com.kkobak.server.application.auth.LoginResult;
import com.kkobak.server.presentation.user.UserResponse;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LoginResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final UserResponse user;

    public static LoginResponse from(LoginResult result) {
        TokenResponse token = TokenResponse.from(result.getTokens());
        return new LoginResponse(token.getAccessToken(), token.getTokenType(), token.getExpiresIn(),
                UserResponse.from(result.getUser()));
    }
}
