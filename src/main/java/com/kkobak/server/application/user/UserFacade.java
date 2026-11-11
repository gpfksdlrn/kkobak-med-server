package com.kkobak.server.application.user;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;

    @Transactional(readOnly = true)
    public UserResult getMe(Long userId) {
        return UserResult.from(userService.getUser(userId));
    }
}
