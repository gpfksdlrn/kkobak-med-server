package com.kkobak.server.global.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.kkobak.server.global.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 보안 필터 단계의 실패를 봉투 모양으로 응답한다.
 * 이 실패는 컨트롤러에 도착하기 전에 나기 때문에 GlobalExceptionHandler 가 잡지 못한다. 그래서 따로 둔다.
 * - 토큰 없음/만료/위조 → 401 UNAUTHORIZED (프론트는 이걸 받으면 /api/auth/refresh 로 재발급을 시도한다)
 * - 권한 없음 → 403 FORBIDDEN
 */
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, ErrorCode.UNAUTHORIZED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, ErrorCode.FORBIDDEN);
    }

    // 메시지는 ErrorCode 에 고정된 문장이라(따옴표 없음) 문자열로 바로 JSON 을 만든다
    private void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"success\":false,\"data\":null,\"error\":{\"code\":\"" + errorCode.name()
                + "\",\"message\":\"" + errorCode.getMessage() + "\"}}");
    }
}
