package com.kkobak.server.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.kkobak.server.application.auth.GoogleAccount;
import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

// 진짜 Google 대신 테스트용 RSA 키로 "Google 처럼" 서명한 토큰을 만들어서, 검증 규칙을 하나씩 확인한다.
class GoogleIdTokenVerifierAdapterTest {

    private static final String CLIENT_ID = "kkobak-client-id.apps.googleusercontent.com";
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    private static KeyPair googleKeyPair;
    private static KeyPair attackerKeyPair;
    private static GoogleIdTokenVerifierAdapter verifier;

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        googleKeyPair = generator.generateKeyPair();
        attackerKeyPair = generator.generateKeyPair();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) googleKeyPair.getPublic()).build();
        verifier = new GoogleIdTokenVerifierAdapter(decoder, CLIENT_ID);
    }

    private JWTClaimsSet.Builder validClaims() {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(GOOGLE_ISSUER)
                .audience(CLIENT_ID)
                .subject("google-sub-123")
                .claim("email", "hong@gmail.com")
                .claim("email_verified", true)
                .claim("name", "홍길동")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)));
    }

    private String sign(JWTClaimsSet claims, KeyPair keyPair) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
        return jwt.serialize();
    }

    private void assertRejected(String idToken) {
        assertThatThrownBy(() -> verifier.verify(idToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_GOOGLE_TOKEN);
    }

    @Test
    void 올바른_Google_토큰이면_사용자_정보를_꺼낸다() throws Exception {
        GoogleAccount account = verifier.verify(sign(validClaims().build(), googleKeyPair));

        assertThat(account.getGoogleSub()).isEqualTo("google-sub-123");
        assertThat(account.getEmail()).isEqualTo("hong@gmail.com");
        assertThat(account.getName()).isEqualTo("홍길동");
    }

    // 잡는 버그: 다른 앱용으로 발급된 Google 토큰을 가져와도 우리 앱에 로그인되는 것
    @Test
    void 다른_앱의_클라이언트_ID로_발급된_토큰은_거절한다() throws Exception {
        assertRejected(sign(validClaims().audience("other-app.apps.googleusercontent.com").build(), googleKeyPair));
    }

    // 잡는 버그: Google 이 아닌 곳이 발급한 토큰을 받아주는 것
    @Test
    void 발급자가_Google이_아니면_거절한다() throws Exception {
        assertRejected(sign(validClaims().issuer("https://evil.example.com").build(), googleKeyPair));
    }

    // 잡는 버그: Google 공개키가 아닌 다른 키로 서명한(위조한) 토큰을 받아주는 것
    @Test
    void Google_키가_아닌_다른_키로_서명한_토큰은_거절한다() throws Exception {
        assertRejected(sign(validClaims().build(), attackerKeyPair));
    }

    // 잡는 버그: 만료된 토큰을 다시 써서 로그인되는 것
    @Test
    void 만료된_토큰은_거절한다() throws Exception {
        Instant past = Instant.now().minusSeconds(7200);
        JWTClaimsSet expired = validClaims()
                .issueTime(Date.from(past))
                .expirationTime(Date.from(past.plusSeconds(600)))
                .build();
        assertRejected(sign(expired, googleKeyPair));
    }

    // 잡는 버그: 소유가 확인되지 않은 이메일로 회원이 만들어지는 것
    @Test
    void 이메일_인증이_안_된_계정은_거절한다() throws Exception {
        assertRejected(sign(validClaims().claim("email_verified", false).build(), googleKeyPair));
    }

    @Test
    void 토큰_형식이_아니면_거절한다() {
        assertRejected("not-a-jwt");
    }

    // 잡는 버그: 이름 없는 계정이 로그인할 때 이름이 null 이라 회원 저장(NOT NULL)에서 터지는 것
    @Test
    void 이름이_없는_계정은_이메일_앞부분을_이름으로_쓴다() throws Exception {
        JWTClaimsSet withoutName = validClaims().claim("name", null).build();

        GoogleAccount account = verifier.verify(sign(withoutName, googleKeyPair));

        assertThat(account.getName()).isEqualTo("hong");
    }
}
