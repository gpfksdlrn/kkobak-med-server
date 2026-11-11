package com.kkobak.server.presentation.health;

import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 헬스체크 API 명세 (Swagger 문서 전용).
 * 운영 코드(HealthController)에 문서용 어노테이션이 섞이지 않도록 명세는 이 인터페이스에만 적는다.
 */
@Tag(name = "헬스체크", description = "서버와 데이터베이스 상태 확인")
@SecurityRequirements
public interface HealthApiSpec {

    String UP_EXAMPLE = "{\"success\": true, \"data\": {\"status\": \"UP\", \"database\": \"UP\", "
            + "\"checkedAt\": \"2026-11-01T13:31:57.243Z\"}, \"error\": null}";
    String DOWN_EXAMPLE = "{\"success\": false, \"data\": null, \"error\": {\"code\": \"SERVICE_UNAVAILABLE\", "
            + "\"message\": \"데이터베이스에 연결할 수 없습니다.\"}}";

    @Operation(summary = "서버 상태 확인", description = "서버가 살아 있고 데이터베이스에 연결되는지 확인합니다. 인증이 필요 없습니다.")
    @ApiResponse(responseCode = "200", description = "정상",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = UP_EXAMPLE)))
    @ApiResponse(responseCode = "503", description = "데이터베이스 연결 실패",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = DOWN_EXAMPLE)))
    ResponseEntity<?> health();
}
