package com.kkobak.server.presentation.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 회원 API 명세 (Swagger 문서 전용).
 * 주의: 여기서 ApiResponse 는 Swagger 어노테이션이고, 우리 봉투 클래스는 전체 경로로 쓴다.
 */
@Tag(name = "회원", description = "내 정보 조회. 회원은 Google 로 처음 로그인할 때 자동으로 만들어진다.")
public interface UserApiSpec {

    String ME_EXAMPLE = "{\"success\": true, \"data\": {\"id\": 1, \"email\": \"hong@gmail.com\", \"name\": \"홍길동\"}, \"error\": null}";
    String UNAUTHORIZED_EXAMPLE = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"UNAUTHORIZED\", "
            + "\"message\": \"로그인이 필요합니다.\"}}";

    @Operation(summary = "내 정보", description = "로그인한 사용자의 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = ME_EXAMPLE)))
    @ApiResponse(responseCode = "401", description = "Access Token 없음/만료/위조",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    com.kkobak.server.global.response.ApiResponse<UserResponse> getMe(@Parameter(hidden = true) Long userId);
}
