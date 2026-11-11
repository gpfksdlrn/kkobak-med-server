package com.kkobak.server.infrastructure.auth;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.kkobak.server.application.auth.GoogleAccount;
import com.kkobak.server.application.auth.GoogleTokenVerifierPort;
import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

/**
 * Google ID 토큰 검증. 확인하는 것:
 * 1) 서명: Google 공개키(JWKS)로 서명됐는가 + 만료되지 않았는가 (JwtDecoder 가 확인)
 * 2) 발급자(iss): Google 인가
 * 3) 대상(aud): 우리 앱(클라이언트 ID)에게 발급된 토큰인가 — 다른 앱용 Google 토큰을 가져와 로그인하는 것을 막는다
 * 4) 이메일 인증(email_verified)이 됐는가
 * 빈 등록은 GoogleAuthConfig 에서 한다 (테스트에서 다른 JwtDecoder 를 끼울 수 있게 생성자로 받는다).
 */
public class GoogleIdTokenVerifierAdapter implements GoogleTokenVerifierPort {

    public static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final List<String> GOOGLE_ISSUERS = Arrays.asList("https://accounts.google.com", "accounts.google.com");

    private final JwtDecoder googleJwtDecoder;
    private final String googleClientId;

    public GoogleIdTokenVerifierAdapter(JwtDecoder googleJwtDecoder, String googleClientId) {
        this.googleJwtDecoder = googleJwtDecoder;
        this.googleClientId = googleClientId;
    }

    // 실제 Google 공개키로 검증하는 디코더. 공개키는 처음 검증할 때 받아오고 캐시한다.
    public static JwtDecoder createGoogleJwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
    }

    @Override
    public GoogleAccount verify(String idToken) {
        Jwt jwt;
        try {
            jwt = googleJwtDecoder.decode(idToken);
        } catch (JwtException exception) {
            throw new BusinessException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }

        String issuer = jwt.getClaimAsString("iss");
        List<String> audience = jwt.getAudience();
        Boolean isEmailVerified = jwt.getClaimAsBoolean("email_verified");
        String email = jwt.getClaimAsString("email");
        String googleSub = jwt.getSubject();

        boolean isValid = GOOGLE_ISSUERS.contains(issuer)
                && audience != null && audience.contains(googleClientId)
                && Boolean.TRUE.equals(isEmailVerified)
                && email != null && googleSub != null;
        if (!isValid) {
            throw new BusinessException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }

        // 이름을 공개하지 않은 계정도 있어서, 없으면 이메일 앞부분을 이름으로 쓴다
        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = email.substring(0, email.indexOf('@'));
        }
        return new GoogleAccount(googleSub, email, name);
    }
}
