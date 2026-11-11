package com.kkobak.server.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 응답의 error.code 로 내려가는 값. 프론트는 message 가 아니라 이 code 로 분기한다.
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "JSON 응답만 제공합니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "요청 본문은 JSON(application/json)으로 보내주세요."),
    CONFLICT(HttpStatus.CONFLICT, "다른 요청과 충돌했습니다. 다시 시도해주세요."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서비스를 일시적으로 사용할 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 인증 / 회원
    INVALID_GOOGLE_TOKEN(HttpStatus.UNAUTHORIZED, "Google 로그인 정보를 확인할 수 없습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해주세요."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // 약
    MEDICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "약을 찾을 수 없습니다."),
    INVALID_MEDICATION_PERIOD(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "복용 시간을 찾을 수 없습니다."),

    // 가족 연결
    CANNOT_INVITE_SELF(HttpStatus.BAD_REQUEST, "자기 자신은 초대할 수 없습니다."),
    CARE_RELATION_NOT_FOUND(HttpStatus.NOT_FOUND, "가족 연결을 찾을 수 없습니다."),
    CARE_RELATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 초대했거나 연결된 가족입니다."),
    PATIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "연결된 환자를 찾을 수 없습니다."),

    // 복용 기록
    DOSE_DATE_OUT_OF_PERIOD(HttpStatus.BAD_REQUEST, "복용 기간이 아닌 날짜입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
