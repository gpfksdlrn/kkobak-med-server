package com.kkobak.server.global.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.kkobak.server.global.config.AuthProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import lombok.RequiredArgsConstructor;

/**
 * 보안 설정.
 * - 세션을 쓰지 않는다(STATELESS). 요청마다 Authorization: Bearer {Access Token} 으로 누구인지 확인한다.
 * - 아래 PUBLIC_PATHS 만 로그인 없이 열고, 나머지는 전부 로그인 필요.
 * - 인증 실패(401)/권한 없음(403)도 다른 API 처럼 봉투(ApiResponse) 모양으로 응답한다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final int MIN_SECRET_BYTES = 32;

    private static final String[] PUBLIC_PATHS = {
            "/api/health",
            "/api/auth/google",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/actuator/health",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/dev/google-login",   // 로컬 전용 테스트 페이지 (local 프로필이 아니면 페이지 자체가 없어서 404)
    };

    private final AuthProperties authProperties;
    private final JsonSecurityErrorHandler jsonSecurityErrorHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 일반 API 는 쿠키가 아니라 Authorization 헤더의 토큰으로 인증하므로 CSRF 공격 대상이 아니다.
                // 쿠키(kkobak_refresh)로 인증하는 건 /api/auth/refresh, logout 뿐이다. 이 쿠키는 HttpOnly + Path=/api/auth 이고
                // SameSite 는 설정(kkobak.auth.refresh-cookie-same-site, 기본 Lax)을 따른다. Lax 면 다른 사이트에서 시작된 POST 에는
                // 쿠키가 안 실린다. 그래서 CSRF 토큰 대신 이 쿠키 설정에 기대어 CSRF 보호를 끈다 (SameSite 를 None 으로 바꾸면 다시 따져야 한다).
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                // Authorization: Bearer 헤더의 JWT 를 아래 jwtDecoder 로 검증한다
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(jsonSecurityErrorHandler)
                        .accessDeniedHandler(jsonSecurityErrorHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jsonSecurityErrorHandler)
                        .accessDeniedHandler(jsonSecurityErrorHandler));
        return http.build();
    }

    // Access Token 서명/검증에 쓰는 비밀키. 너무 짧으면 서버를 띄우지 않는다 (HS256 은 32바이트 이상 권장).
    @Bean
    public SecretKey accessTokenSecretKey() {
        byte[] secretBytes = authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("kkobak.auth.jwt-secret 은 32바이트 이상이어야 합니다.");
        }
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey accessTokenSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(accessTokenSecretKey));
    }

    // 서명, 만료(exp)를 확인한다
    @Bean
    public JwtDecoder jwtDecoder(SecretKey accessTokenSecretKey) {
        return NimbusJwtDecoder.withSecretKey(accessTokenSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /**
     * CORS: 브라우저가 다른 주소(프론트 localhost:3000)에서 이 서버를 부를 수 있게 허용한다.
     * allowCredentials: 리프레시 토큰 쿠키를 주고받으려면 필요하다. 이때는 허용 주소를 "*" 로 둘 수 없고 정확히 적어야 한다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(authProperties.getAllowedOrigins());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
