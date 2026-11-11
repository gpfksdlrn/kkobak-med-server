package com.kkobak.server.presentation.medication;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 약 API 명세 (Swagger 문서 전용).
 * 운영 코드(MedicationController)에 문서용 어노테이션이 섞이지 않도록 명세는 이 인터페이스에만 적는다.
 * 주의: 여기서 ApiResponse 는 Swagger 의 응답 명세 어노테이션이고,
 *       우리 봉투 클래스는 이름이 같아서 com.kkobak.server.global.response.ApiResponse 로 전체 경로를 쓴다.
 */
@Tag(name = "약", description = "약 등록/조회/수정/삭제. 복용 시간(schedules)을 함께 다룬다.")
public interface MedicationApiSpec {

    String PATIENT_ID_DESCRIPTION = "가족이 돌보는 환자의 약을 다룰 때 그 환자의 사용자 id. 내 약이면 비워 둔다. "
            + "수락된 가족 연결이 없으면 404 PATIENT_NOT_FOUND";

    String REQUEST_EXAMPLE = "{\"name\": \"타이레놀\", \"dosageText\": \"1정\", \"mealTiming\": \"AFTER_MEAL\", "
            + "\"startDate\": \"2026-11-01\", \"endDate\": \"2026-11-30\", \"times\": [\"08:00\", \"20:00\"]}";

    String MEDICATION_EXAMPLE = "{\"success\": true, \"data\": {\"id\": 1, \"name\": \"타이레놀\", \"dosageText\": \"1정\", "
            + "\"mealTiming\": \"AFTER_MEAL\", \"startDate\": \"2026-11-01\", \"endDate\": \"2026-11-30\", "
            + "\"schedules\": [{\"id\": 2, \"time\": \"08:00\"}, {\"id\": 1, \"time\": \"20:00\"}], "
            + "\"createdAt\": \"2026-11-01T13:47:42.768Z\", \"updatedAt\": \"2026-11-01T13:47:42.768Z\"}, \"error\": null}";

    String MEDICATION_LIST_EXAMPLE = "{\"success\": true, \"data\": [{\"id\": 1, \"name\": \"타이레놀\", \"dosageText\": \"1정\", "
            + "\"mealTiming\": \"AFTER_MEAL\", \"startDate\": \"2026-11-01\", \"endDate\": null, "
            + "\"schedules\": [{\"id\": 2, \"time\": \"08:00\"}], "
            + "\"createdAt\": \"2026-11-01T13:47:42.768Z\", \"updatedAt\": \"2026-11-01T13:47:42.768Z\"}], \"error\": null}";

    String DELETE_EXAMPLE = "{\"success\": true, \"data\": null, \"error\": null}";

    String NOT_FOUND_EXAMPLE = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"MEDICATION_NOT_FOUND\", "
            + "\"message\": \"약을 찾을 수 없습니다.\"}}";
    String INVALID_INPUT_EXAMPLE = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"INVALID_INPUT\", "
            + "\"message\": \"약 이름을 입력해주세요.\"}}";
    String UNAUTHORIZED_EXAMPLE = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"UNAUTHORIZED\", "
            + "\"message\": \"로그인이 필요합니다.\"}}";

    @Operation(summary = "약 목록",
            description = "약 전체를 최근 등록순으로 조회합니다. patientId 를 비우면 내 약, 넣으면 수락된 가족으로서 돌보는 환자의 약입니다.")
    @ApiResponse(responseCode = "200", description = "성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = MEDICATION_LIST_EXAMPLE)))
    @ApiResponse(responseCode = "401", description = "Access Token 없음/만료/위조",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = UNAUTHORIZED_EXAMPLE)))
    com.kkobak.server.global.response.ApiResponse<List<MedicationResponse>> getMedications(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = PATIENT_ID_DESCRIPTION, example = "7") Long patientId);

    @Operation(summary = "특정 날짜에 복용 중인 약",
            description = "시작일 <= date <= 종료일(종료일 없으면 시작일 이후 전부)인 약과 복용 시간을 조회합니다. 홈 화면의 '오늘 먹을 약'에 씁니다.")
    @ApiResponse(responseCode = "200", description = "성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = MEDICATION_LIST_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "date 누락 또는 형식 오류 (yyyy-MM-dd)",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = INVALID_INPUT_EXAMPLE)))
    com.kkobak.server.global.response.ApiResponse<List<MedicationResponse>> getActiveMedications(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = PATIENT_ID_DESCRIPTION, example = "7") Long patientId,
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", example = "2026-11-01") LocalDate date);

    @Operation(summary = "약 상세", description = "약 한 건을 조회합니다. 내 약이거나, 수락된 가족으로서 돌보는 환자의 약이면 볼 수 있습니다. 그 밖의 약과 없는 약은 똑같이 404 로 응답합니다.")
    @ApiResponse(responseCode = "200", description = "성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = MEDICATION_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "없는 약 또는 남의 약",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    com.kkobak.server.global.response.ApiResponse<MedicationResponse> getMedication(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "약 id", example = "1") Long medicationId);

    @Operation(summary = "약 등록",
            description = "약과 복용 시간을 함께 등록합니다. times 는 \"HH:mm\" 형식이고 1개 이상 필요합니다. "
                    + "mealTiming: BEFORE_MEAL(식전), AFTER_MEAL(식후), EMPTY_STOMACH(공복), NONE(상관없음).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = REQUEST_EXAMPLE))))
    @ApiResponse(responseCode = "201", description = "등록 성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = MEDICATION_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "입력값 오류: 이름 비어 있음, 복용 시간 없음, 형식 오류 등은 INVALID_INPUT, 종료일 < 시작일은 INVALID_MEDICATION_PERIOD",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = INVALID_INPUT_EXAMPLE)))
    com.kkobak.server.global.response.ApiResponse<MedicationResponse> createMedication(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = PATIENT_ID_DESCRIPTION, example = "7") Long patientId,
            MedicationRequest request);

    @Operation(summary = "약 수정",
            description = "약 정보와 복용 시간을 수정합니다. 복용 시간은 바뀐 것만 반영되어, 안 바뀐 시간의 복용 기록은 유지됩니다. "
                    + "빠진 시간의 복용 기록은 함께 삭제됩니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = REQUEST_EXAMPLE))))
    @ApiResponse(responseCode = "200", description = "수정 성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = MEDICATION_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "입력값 오류 (INVALID_INPUT), 종료일 < 시작일 (INVALID_MEDICATION_PERIOD)",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = INVALID_INPUT_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "없는 약 또는 남의 약",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    com.kkobak.server.global.response.ApiResponse<MedicationResponse> updateMedication(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "약 id", example = "1") Long medicationId,
            MedicationRequest request);

    @Operation(summary = "약 삭제", description = "약을 삭제합니다. 복용 시간과 복용 기록도 함께 삭제됩니다.")
    @ApiResponse(responseCode = "200", description = "삭제 성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = DELETE_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "없는 약 또는 남의 약",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = NOT_FOUND_EXAMPLE)))
    com.kkobak.server.global.response.ApiResponse<Void> deleteMedication(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "약 id", example = "1") Long medicationId);
}
