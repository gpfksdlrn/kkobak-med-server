package com.kkobak.server.presentation.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID 토큰이 필요합니다.")
    private String idToken;
}
