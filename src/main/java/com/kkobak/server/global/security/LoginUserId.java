package com.kkobak.server.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 로그인한 사용자의 id(Long)가 들어온다.
 * 예: public ApiResponse<...> getMedications(@LoginUserId Long userId)
 * 값은 검증된 Access Token 의 sub(사용자 id)에서 꺼낸다 (LoginUserIdArgumentResolver).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUserId {
}
