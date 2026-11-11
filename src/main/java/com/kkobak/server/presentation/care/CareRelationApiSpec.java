package com.kkobak.server.presentation.care;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 가족 연결 API 명세 (Swagger 문서 전용).
 * 주의: 여기서 ApiResponse 는 Swagger 어노테이션이고, 우리 봉투 클래스는 전체 경로로 쓴다.
 */
@Tag(name = "가족 연결", description = "환자가 가족을 이메일로 초대하고, 가족이 수락하면 연결된다. "
        + "연결된(ACCEPTED) 가족은 약/복용 기록 API 에 patientId 를 넣어 환자의 데이터를 조회·수정할 수 있다.")
public interface CareRelationApiSpec {

    String INVITE_REQUEST_EXAMPLE = "{\"email\": \"family@gmail.com\"}";
    String RELATION_EXAMPLE = "{\"success\": true, \"data\": {\"id\": 101, "
            + "\"patient\": {\"id\": 5, \"email\": \"patient@gmail.com\", \"name\": \"환자\"}, "
            + "\"caregiver\": {\"id\": 7, \"email\": \"family@gmail.com\", \"name\": \"가족\"}, "
            + "\"status\": \"PENDING\", \"createdAt\": \"2026-11-02T01:00:00Z\", \"acceptedAt\": null}, \"error\": null}";
    String RELATION_LIST_EXAMPLE = "{\"success\": true, \"data\": [{\"id\": 101, "
            + "\"patient\": {\"id\": 5, \"email\": \"patient@gmail.com\", \"name\": \"환자\"}, "
            + "\"caregiver\": {\"id\": 7, \"email\": \"family@gmail.com\", \"name\": \"가족\"}, "
            + "\"status\": \"ACCEPTED\", \"createdAt\": \"2026-11-02T01:00:00Z\", \"acceptedAt\": \"2026-11-02T01:05:00Z\"}], \"error\": null}";
    String REMOVE_EXAMPLE = "{\"success\": true, \"data\": null, \"error\": null}";
    String USER_NOT_FOUND_EXAMPLE = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"USER_NOT_FOUND\", "
            + "\"message\": \"사용자를 찾을 수 없습니다.\"}}";
    String SELF_EXAMPLE = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"CANNOT_INVITE_SELF\", "
            + "\"message\": \"자기 자신은 초대할 수 없습니다.\"}}";
    String ALREADY_EXISTS_EXAMPLE = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"CARE_RELATION_ALREADY_EXISTS\", "
            + "\"message\": \"이미 초대했거나 연결된 가족입니다.\"}}";
    String NOT_FOUND_EXAMPLE = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"CARE_RELATION_NOT_FOUND\", "
            + "\"message\": \"가족 연결을 찾을 수 없습니다.\"}}";

    @Operation(summary = "가족 초대",
            description = "내가 환자가 되어, 이메일의 주인을 가족으로 초대합니다. 초대받은 사람이 먼저 꼬박약에 Google 로그인으로 가입해 있어야 합니다. "
                    + "수락 전(PENDING)에는 아무 권한이 없습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = INVITE_REQUEST_EXAMPLE))))
    @ApiResponse(responseCode = "201", description = "초대 성공 (PENDING)",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RELATION_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "자기 자신을 초대함 (CANNOT_INVITE_SELF), 이메일 형식 오류 (INVALID_INPUT)",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = SELF_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "가입하지 않은 이메일",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = USER_NOT_FOUND_EXAMPLE)))
    @ApiResponse(responseCode = "409", description = "이미 초대했거나 연결된 가족",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = ALREADY_EXISTS_EXAMPLE)))
    com.kkobak.server.global.response.ApiResponse<CareRelationResponse> invite(
            @Parameter(hidden = true) Long userId, CareInvitationRequest request);

    @Operation(summary = "내 가족 연결 목록",
            description = "내가 환자인 연결(내가 초대한 가족)과 내가 가족인 연결(나를 초대한 환자)을 모두 보여줍니다. 받은 초대(PENDING)도 여기에 나옵니다.")
    @ApiResponse(responseCode = "200", description = "성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RELATION_LIST_EXAMPLE)))
    com.kkobak.server.global.response.ApiResponse<List<CareRelationResponse>> getMyRelations(@Parameter(hidden = true) Long userId);

    @Operation(summary = "초대 수락", description = "초대받은 가족만 수락할 수 있습니다. 수락하면 ACCEPTED 가 되고 환자의 데이터를 다룰 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "수락 성공 (ACCEPTED)",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = RELATION_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "없는 연결, 또는 내가 초대받은 가족이 아님",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    com.kkobak.server.global.response.ApiResponse<CareRelationResponse> accept(
            @Parameter(hidden = true) Long userId, @Parameter(description = "가족 연결 id", example = "101") Long careRelationId);

    @Operation(summary = "초대 거절 / 취소 / 연결 끊기",
            description = "연결을 삭제합니다. 환자와 가족 누구나 할 수 있습니다. 삭제하면 가족은 즉시 환자의 데이터에 접근할 수 없습니다.")
    @ApiResponse(responseCode = "200", description = "삭제 성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = REMOVE_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "없는 연결, 또는 내가 당사자가 아님",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    com.kkobak.server.global.response.ApiResponse<Void> remove(
            @Parameter(hidden = true) Long userId, @Parameter(description = "가족 연결 id", example = "101") Long careRelationId);
}
