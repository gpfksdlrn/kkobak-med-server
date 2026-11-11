package com.kkobak.server.application.user;

import com.kkobak.server.domain.user.User;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserResult {

    private final Long id;
    private final String email;
    private final String name;

    public static UserResult from(User user) {
        return new UserResult(user.getId(), user.getEmail(), user.getName());
    }
}
