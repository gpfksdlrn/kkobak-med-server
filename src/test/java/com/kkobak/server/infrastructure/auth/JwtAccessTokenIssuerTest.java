package com.kkobak.server.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.kkobak.server.application.auth.AccessToken;
import com.kkobak.server.global.config.AuthProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

class JwtAccessTokenIssuerTest {

    private static final SecretKey SECRET = key("test-secret-key-must-be-at-least-32-bytes!!");
    private static final SecretKey OTHER_SECRET = key("other-secret-key-must-be-at-least-32-bytes!");

    private static SecretKey key(String value) {
        return new SecretKeySpec(value.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private JwtAccessTokenIssuer issuerAt(Instant now) {
        AuthProperties properties = new AuthProperties("client-id", "unused", Duration.ofMinutes(30), Duration.ofDays(14),
                true, "Lax", Arrays.asList("http://localhost:3000"));
        return new JwtAccessTokenIssuer(new NimbusJwtEncoder(new ImmutableSecret<>(SECRET)), properties,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private NimbusJwtDecoder decoderWith(SecretKey secretKey) {
        return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    // 잡는 버그: 토큰에 사용자 id(sub)를 잘못 넣거나 만료 시각을 잘못 계산하는 것
    @Test
    void 토큰에는_사용자_id와_30분_뒤_만료_시각이_들어간다() {
        Instant now = Instant.parse("2026-11-01T00:00:00Z");
        AccessToken accessToken = issuerAt(now).issue(42L);

        NimbusJwtDecoder decoder = decoderWith(SECRET);
        decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success()); // 고정 시각이라 만료 검사는 끄고 내용만 본다
        Jwt jwt = decoder.decode(accessToken.getValue());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getExpiresAt()).isEqualTo(Instant.parse("2026-11-01T00:30:00Z"));
        assertThat(accessToken.getExpiresAt()).isEqualTo(jwt.getExpiresAt());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(JwtAccessTokenIssuer.ISSUER);
    }

    // 잡는 버그: 다른 비밀키로 서명된(위조된) 토큰이 검증을 통과하는 것
    @Test
    void 다른_비밀키로는_검증되지_않는다() {
        AccessToken accessToken = issuerAt(Instant.now()).issue(42L);

        assertThatThrownBy(() -> decoderWith(OTHER_SECRET).decode(accessToken.getValue()))
                .isInstanceOf(JwtException.class);
    }

    // 잡는 버그: 만료된 Access Token 이 계속 통과하는 것
    @Test
    void 만료된_토큰은_검증에서_거절된다() {
        AccessToken expired = issuerAt(Instant.now().minus(Duration.ofHours(1))).issue(42L);

        assertThatThrownBy(() -> decoderWith(SECRET).decode(expired.getValue()))
                .isInstanceOf(JwtException.class);
    }

    // 잡는 버그: 토큰 내용(sub)을 바꿔치기해도 통과하는 것
    @Test
    void 내용을_바꾼_토큰은_서명이_맞지_않아_거절된다() {
        String token = issuerAt(Instant.now()).issue(42L).getValue();
        String[] parts = token.split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"1\",\"exp\":" + (Instant.now().getEpochSecond() + 600) + "}").getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> decoderWith(SECRET).decode(forged)).isInstanceOf(JwtException.class);
    }
}
