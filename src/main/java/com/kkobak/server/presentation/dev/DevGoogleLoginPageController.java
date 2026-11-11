package com.kkobak.server.presentation.dev;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kkobak.server.global.config.AuthProperties;

import lombok.RequiredArgsConstructor;

/**
 * 로컬 개발 전용: 진짜 Google 로그인으로 Access Token 을 받아 보는 테스트 페이지.
 * 받은 Access Token 을 Swagger 의 Authorize 에 넣어 인증이 필요한 API 를 직접 시험하는 용도다.
 * @Profile("local"): local 프로필에서만 이 컨트롤러가 만들어진다. 운영에서는 주소 자체가 없다(404).
 */
@Profile("local")
@RestController
@RequiredArgsConstructor
public class DevGoogleLoginPageController {

    private static final String PAGE_PATH = "dev/google-login.html";
    private static final String CLIENT_ID_PLACEHOLDER = "{{GOOGLE_CLIENT_ID}}";

    private final AuthProperties authProperties;

    @GetMapping(value = "/dev/google-login", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> googleLoginPage() throws IOException {
        InputStream inputStream = new ClassPathResource(PAGE_PATH).getInputStream();
        String html;
        try {
            html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            inputStream.close();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html.replace(CLIENT_ID_PLACEHOLDER, authProperties.getGoogleClientId()));
    }
}
