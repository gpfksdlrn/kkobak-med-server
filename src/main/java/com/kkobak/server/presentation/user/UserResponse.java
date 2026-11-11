package com.kkobak.server.presentation.user;

import com.kkobak.server.application.user.UserResult;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserResponse {

    private final Long id;
    private final String email;
    private final String name;

    public static UserResponse from(UserResult result) {
        return new UserResponse(result.getId(), result.getEmail(), result.getName());
    }
}
