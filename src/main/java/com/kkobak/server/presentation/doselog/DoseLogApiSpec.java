package com.kkobak.server.presentation.doselog;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 복용 기록 API 명세 (Swagger 문서 전용).
 * 운영 코드(DoseLogController)에 문서용 어노테이션이 섞이지 않도록 명세는 이 인터페이스에만 적는다.
 * 주의: 여기서 ApiResponse 는 Swagger 어노테이션이고, 우리 봉투 클래스는 전체 경로로 쓴다.
 */
@Tag(name = "복용 기록", description = "날짜별 복용 기록 조회, 복용 체크")
public interface DoseLogApiSpec {

    String PATIENT_ID_DESCRIPTION = "가족이 돌보는 환자의 기록을 볼 때 그 환자의 사용자 id. 내 기록이면 비워 둔다. "
            + "수락된 가족 연결이 없으면 404 PATIENT_NOT_FOUND";

    String CHECK_REQUEST_EXAMPLE = "{\"scheduleId\": 2, \"scheduledDate\": \"2026-11-01\"}";

    String DOSE_LOG_EXAMPLE = "{\"success\": true, \"data\": {\"id\": 1, \"scheduleId\": 2, "
            + "\"scheduledDate\": \"2026-11-01\", \"takenAt\": \"2026-11-01T14:08:11.353Z\", \"checkedBy\": 5}, \"error\": null}";
    String DOSE_LOG_LIST_EXAMPLE = "{\"success\": true, \"data\": [{\"id\": 1, \"scheduleId\": 2, "
            + "\"scheduledDate\": \"2026-11-01\", \"takenAt\": \"2026-11-01T14:08:11.353Z\", \"checkedBy\": 5}], \"error\": null}";

    String SCHEDULE_NOT_FOUND_EXAMPLE = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"SCHEDULE_NOT_FOUND\", "
            + "\"message\": \"복용 시간을 찾을 수 없습니다.\"}}";
    String OUT_OF_PERIOD_EXAMPLE = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"DOSE_DATE_OUT_OF_PERIOD\", "
            + "\"message\": \"복용 기간이 아닌 날짜입니다.\"}}";

    @Operation(summary = "날짜별 복용 기록",
            description = "그 날짜의 복용 기록을 조회합니다. patientId 를 비우면 내 기록, 넣으면 수락된 가족으로서 돌보는 환자의 기록입니다. "
                    + "기록이 없는 복용 시간은 아직 안 먹은 것입니다. "
                    + "(복용 상태 '완료/놓침/예정'은 저장하지 않고 takenAt 과 현재 시각으로 프론트에서 계산)")
    @ApiResponse(responseCode = "200", description = "성공 (기록이 없으면 빈 배열)",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = DOSE_LOG_LIST_EXAMPLE)))
    com.kkobak.server.global.response.ApiResponse<List<DoseLogResponse>> getDoseLogs(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = PATIENT_ID_DESCRIPTION, example = "7") Long patientId,
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", example = "2026-11-01") LocalDate date);

    @Operation(summary = "복용 체크",
            description = "그 복용 시간(scheduleId)의 그 날짜 약을 먹었다고 기록합니다. "
                    + "내 약이거나 수락된 가족으로서 돌보는 환자의 약이면 체크할 수 있고, 체크한 사람(checkedBy)이 기록됩니다. "
                    + "기록이 없으면 새로 만들고, 있으면 복용 시각과 체크한 사람을 갱신합니다(upsert). 여러 번, 동시에 보내도 모두 성공하고 기록은 하나만 남습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = CHECK_REQUEST_EXAMPLE))))
    @ApiResponse(responseCode = "200", description = "체크 성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = DOSE_LOG_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "복용 기간이 아닌 날짜, 또는 입력값 오류",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = OUT_OF_PERIOD_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "없는 복용 시간 또는 남의 복용 시간",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = SCHEDULE_NOT_FOUND_EXAMPLE)))
    com.kkobak.server.global.response.ApiResponse<DoseLogResponse> checkDose(
            @Parameter(hidden = true) Long userId,
            DoseLogCheckRequest request);
}
