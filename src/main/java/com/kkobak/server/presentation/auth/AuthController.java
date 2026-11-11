package com.kkobak.server.presentation.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kkobak.server.application.auth.AuthFacade;
import com.kkobak.server.application.auth.LoginResult;
import com.kkobak.server.application.auth.TokenResult;
import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;
import com.kkobak.server.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApiSpec {

    private final AuthFacade authFacade;
    private final RefreshTokenCookies refreshTokenCookies;

    @Override
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        LoginResult result = authFacade.loginWithGoogle(request.getIdToken());
        TokenResult tokens = result.getTokens();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshTokenCookies.create(tokens.getRefreshToken(), tokens.getRefreshTokenValidity()).toString())
                .body(ApiResponse.success(LoginResponse.from(result)));
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @CookieValue(name = RefreshTokenCookies.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        TokenResult tokens = authFacade.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshTokenCookies.create(tokens.getRefreshToken(), tokens.getRefreshTokenValidity()).toString())
                .body(ApiResponse.success(TokenResponse.from(tokens)));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = RefreshTokenCookies.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authFacade.logout(refreshToken);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.expire().toString())
                .body(ApiResponse.success());
    }
}
