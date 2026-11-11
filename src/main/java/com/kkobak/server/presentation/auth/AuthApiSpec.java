package com.kkobak.server.presentation.auth;

import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 인증 API 명세 (Swagger 문서 전용).
 * 주의: 여기서 ApiResponse 는 Swagger 어노테이션이다.
 */
@Tag(name = "인증", description = "Google 로그인, 토큰 재발급, 로그아웃. Refresh Token 은 HttpOnly 쿠키(kkobak_refresh)로 주고받는다.")
@SecurityRequirements
public interface AuthApiSpec {

    String LOGIN_REQUEST_EXAMPLE = "{\"idToken\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6Ij...\"}";
    String LOGIN_EXAMPLE = "{\"success\": true, \"data\": {\"accessToken\": \"eyJhbGciOiJIUzI1NiJ9...\", \"tokenType\": \"Bearer\", "
            + "\"expiresIn\": 1800, \"user\": {\"id\": 1, \"email\": \"hong@gmail.com\", \"name\": \"홍길동\"}}, \"error\": null}";
    String TOKEN_EXAMPLE = "{\"success\": true, \"data\": {\"accessToken\": \"eyJhbGciOiJIUzI1NiJ9...\", \"tokenType\": \"Bearer\", "
            + "\"expiresIn\": 1800}, \"error\": null}";
    String LOGOUT_EXAMPLE = "{\"success\": true, \"data\": null, \"error\": null}";
    String INVALID_GOOGLE_TOKEN_EXAMPLE = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"INVALID_GOOGLE_TOKEN\", "
            + "\"message\": \"Google 로그인 정보를 확인할 수 없습니다.\"}}";
    String INVALID_REFRESH_TOKEN_EXAMPLE = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"INVALID_REFRESH_TOKEN\", "
            + "\"message\": \"로그인이 만료되었습니다. 다시 로그인해주세요.\"}}";

    @Operation(summary = "Google 로그인",
            description = "프론트가 Google 로그인으로 받은 ID 토큰을 보내면, 검증 후 Access Token(본문)과 Refresh Token(Set-Cookie)을 발급합니다. "
                    + "처음 로그인한 사용자는 회원으로 자동 가입됩니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = LOGIN_REQUEST_EXAMPLE))))
    @ApiResponse(responseCode = "200", description = "로그인 성공 (Set-Cookie: kkobak_refresh)",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = LOGIN_EXAMPLE)))
    @ApiResponse(responseCode = "401", description = "Google ID 토큰이 유효하지 않음 (서명/만료/대상 앱/이메일 미인증)",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = INVALID_GOOGLE_TOKEN_EXAMPLE)))
    ResponseEntity<com.kkobak.server.global.response.ApiResponse<LoginResponse>> loginWithGoogle(GoogleLoginRequest request);

    @Operation(summary = "토큰 재발급",
            description = "kkobak_refresh 쿠키의 Refresh Token 으로 새 Access Token 과 새 Refresh Token(쿠키)을 발급합니다. "
                    + "쓴 Refresh Token 은 폐기됩니다(교체). 이미 폐기된 토큰이 다시 오면 탈취로 보고 그 사용자의 토큰을 전부 폐기합니다. "
                    + "프론트는 API 가 401 을 주면 이 API 를 부르고, 이것도 401 이면 다시 로그인시킵니다.")
    @ApiResponse(responseCode = "200", description = "재발급 성공 (Set-Cookie: 새 kkobak_refresh)",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = TOKEN_EXAMPLE)))
    @ApiResponse(responseCode = "401", description = "쿠키 없음, 만료/폐기된 토큰",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = INVALID_REFRESH_TOKEN_EXAMPLE)))
    ResponseEntity<com.kkobak.server.global.response.ApiResponse<TokenResponse>> refresh(
            @Parameter(hidden = true) String refreshToken);

    @Operation(summary = "로그아웃", description = "Refresh Token 을 폐기하고 쿠키를 지웁니다. 쿠키가 없거나 이미 로그아웃했어도 성공으로 응답합니다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = LOGOUT_EXAMPLE)))
    ResponseEntity<com.kkobak.server.global.response.ApiResponse<Void>> logout(@Parameter(hidden = true) String refreshToken);
}
