package com.kkobak.server.infrastructure.auth;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.kkobak.server.application.auth.AccessToken;
import com.kkobak.server.application.auth.AccessTokenIssuerPort;
import com.kkobak.server.global.config.AuthProperties;

import lombok.RequiredArgsConstructor;

/**
 * Access Token(JWT) 발급. HS256(공유 비밀키)으로 서명한다.
 * 토큰 안에는 "누구(sub = 사용자 id)인지, 언제까지 유효(exp)한지"만 넣는다. 개인정보는 넣지 않는다 (JWT 는 누구나 디코딩할 수 있다).
 */
@Component
@RequiredArgsConstructor
public class JwtAccessTokenIssuer implements AccessTokenIssuerPort {

    public static final String ISSUER = "kkobak-med-server";

    private final JwtEncoder jwtEncoder;
    private final AuthProperties authProperties;
    private final Clock clock;

    @Override
    public AccessToken issue(Long userId) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(authProperties.getAccessTokenValidity());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new AccessToken(tokenValue, expiresAt);
    }
}
