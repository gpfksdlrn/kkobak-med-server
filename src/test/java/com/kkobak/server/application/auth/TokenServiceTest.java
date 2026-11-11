package com.kkobak.server.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.kkobak.server.domain.auth.RefreshToken;
import com.kkobak.server.domain.auth.RefreshTokenRepository;
import com.kkobak.server.global.config.AuthProperties;
import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

class TokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");
    private static final Long USER_ID = 7L;

    private RefreshTokenRepository refreshTokenRepository;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        AccessTokenIssuerPort fakeIssuer = new AccessTokenIssuerPort() {
            @Override
            public AccessToken issue(Long userId) {
                return new AccessToken("access-for-" + userId, NOW.plus(Duration.ofMinutes(30)));
            }
        };
        AuthProperties properties = new AuthProperties("client-id", "secret", Duration.ofMinutes(30), Duration.ofDays(14),
                true, "Lax", Arrays.asList("http://localhost:3000"));
        tokenService = new TokenService(refreshTokenRepository, fakeIssuer, properties, new SecureRandom(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // 잡는 버그: DB 에 원문을 저장하는 것, 만료 시각을 잘못 계산하는 것
    @Test
    void 토큰을_발급하면_리프레시_토큰은_해시로만_저장되고_14일_뒤_만료된다() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenResult result = tokenService.issueTokens(USER_ID);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(RefreshToken.hash(result.getRefreshToken()));
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(result.getRefreshToken());
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(Instant.parse("2026-11-15T00:00:00Z"));
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(result.getAccessToken()).isEqualTo("access-for-7");
    }

    // 잡는 버그: 리프레시 토큰이 매번 같은 값(추측 가능)으로 만들어지는 것
    @Test
    void 발급할_때마다_다른_리프레시_토큰이_나온다() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String first = tokenService.issueTokens(USER_ID).getRefreshToken();
        String second = tokenService.issueTokens(USER_ID).getRefreshToken();

        assertThat(first).isNotEqualTo(second);
        assertThat(first.length()).isGreaterThanOrEqualTo(43);
    }

    // 잡는 버그: 재발급에 쓴 토큰을 폐기하지 않아 같은 토큰으로 계속 재발급되는 것
    @Test
    void 유효한_리프레시_토큰을_쓰면_주인을_돌려주고_그_토큰은_폐기된다() {
        RefreshToken stored = new RefreshToken(USER_ID, RefreshToken.hash("raw"), NOW.plus(Duration.ofDays(1)), NOW);
        when(refreshTokenRepository.findByTokenHash(RefreshToken.hash("raw"))).thenReturn(Optional.of(stored));

        Long userId = tokenService.consumeRefreshToken("raw");

        assertThat(userId).isEqualTo(USER_ID);
        assertThat(stored.isRevoked()).isTrue();
    }

    // 잡는 버그: 폐기된 토큰의 재사용(탈취 의심)을 그냥 거절만 하고, 훔친 사람이 이미 받은 새 토큰은 살려두는 것
    @Test
    void 폐기된_토큰이_다시_오면_그_사용자의_토큰을_전부_폐기하고_거절한다() {
        RefreshToken revoked = new RefreshToken(USER_ID, RefreshToken.hash("raw"), NOW.plus(Duration.ofDays(1)), NOW);
        revoked.revoke(NOW);
        when(refreshTokenRepository.findByTokenHash(RefreshToken.hash("raw"))).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> tokenService.consumeRefreshToken("raw"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokenRepository).revokeAllByUserId(USER_ID, NOW);
    }

    // 잡는 버그: 만료된 토큰으로 재발급되는 것 (만료는 탈취가 아니므로 전부 폐기까지는 하지 않는다)
    @Test
    void 만료된_토큰은_거절하지만_전부_폐기는_하지_않는다() {
        RefreshToken expired = new RefreshToken(USER_ID, RefreshToken.hash("raw"), NOW.minusSeconds(1), NOW.minus(Duration.ofDays(14)));
        when(refreshTokenRepository.findByTokenHash(RefreshToken.hash("raw"))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> tokenService.consumeRefreshToken("raw"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong(), any(Instant.class));
    }

    // 잡는 버그: 모르는 토큰(위조/오타)으로 재발급되는 것
    @Test
    void 저장되지_않은_토큰은_거절한다() {
        when(refreshTokenRepository.findByTokenHash(any(String.class))).thenReturn(Optional.<RefreshToken>empty());

        assertThatThrownBy(() -> tokenService.consumeRefreshToken("unknown"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
