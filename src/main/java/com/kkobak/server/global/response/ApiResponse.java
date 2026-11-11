package com.kkobak.server.global.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.kkobak.server.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 모든 API 응답을 같은 모양으로 감싸는 봉투.
 * 성공: { "success": true,  "data": {...}, "error": null }
 * 실패: { "success": false, "data": null,  "error": { "code": "...", "message": "..." } }
 */
// JSON 필드 순서를 success → data → error 로 고정한다 (기본은 알파벳순이라 data 가 먼저 나옴)
@JsonPropertyOrder({"success", "data", "error"})
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorDetail error;

    private ApiResponse(boolean success, T data, ErrorDetail error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // 돌려줄 데이터가 없는 성공 (예: 삭제)
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(errorCode.name(), message));
    }

    @Getter
    @RequiredArgsConstructor
    public static class ErrorDetail {

        private final String code;
        private final String message;
    }
}
