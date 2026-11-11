package com.kkobak.server.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

// Swagger(OpenAPI) 문서의 맨 위 소개 부분. 각 API 의 상세 명세는 presentation 의 XxxApiSpec 인터페이스에 있다.
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    private static final String DESCRIPTION = ""
            + "꼬박약(복약 관리 PWA) 백엔드 API.\n\n"
            + "## 응답 형식 (봉투)\n"
            + "모든 응답은 같은 모양으로 감싸서 내려갑니다.\n"
            + "- 성공: `{ \"success\": true, \"data\": {...}, \"error\": null }`\n"
            + "- 실패: `{ \"success\": false, \"data\": null, \"error\": { \"code\": \"...\", \"message\": \"...\" } }`\n\n"
            + "프론트는 `error.message`(문장)가 아니라 `error.code`(고정값)로 분기합니다.\n\n"
            + "## 인증\n"
            + "1. 프론트가 Google 로그인으로 받은 ID 토큰을 `POST /api/auth/google` 로 보냅니다.\n"
            + "2. 응답의 `accessToken` 을 이후 요청에 `Authorization: Bearer {accessToken}` 으로 보냅니다. (오른쪽 위 Authorize 버튼)\n"
            + "3. Access Token 이 만료되어 401 이 오면 `POST /api/auth/refresh` 로 재발급합니다. Refresh Token 은 HttpOnly 쿠키라 브라우저가 알아서 보냅니다.\n";

    @Bean
    public OpenAPI openApi() {
        // Swagger UI 의 Authorize 버튼에 Access Token 을 넣으면 모든 요청에 Authorization: Bearer 가 붙는다
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
        return new OpenAPI()
                .info(new Info().title("꼬박약 API").version("v1").description(DESCRIPTION))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
