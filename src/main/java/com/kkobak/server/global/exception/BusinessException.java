package com.kkobak.server.global.exception;

import lombok.Getter;

// 업무 규칙 위반을 알리는 예외. ErrorCode 를 담아서 던지면 GlobalExceptionHandler 가 알맞은 응답으로 바꿔 준다.
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
