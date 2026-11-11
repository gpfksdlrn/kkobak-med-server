package com.kkobak.server.presentation.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kkobak.server.application.user.UserFacade;
import com.kkobak.server.global.response.ApiResponse;
import com.kkobak.server.global.security.LoginUserId;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApiSpec {

    private final UserFacade userFacade;

    @Override
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(@LoginUserId Long userId) {
        return ApiResponse.success(UserResponse.from(userFacade.getMe(userId)));
    }
}
