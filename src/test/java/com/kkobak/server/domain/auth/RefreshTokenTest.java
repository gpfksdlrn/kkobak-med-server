package com.kkobak.server.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-15T00:00:00Z");

    // 잡는 버그: 해시가 아니라 원문이 저장되거나, 같은 원문인데 해시가 매번 달라 조회가 안 되는 것
    @Test
    void 해시는_원문과_다르고_같은_원문이면_항상_같은_64자_해시가_나온다() {
        String hash = RefreshToken.hash("raw-token-value");

        assertThat(hash).isNotEqualTo("raw-token-value").hasSize(64).matches("[0-9a-f]{64}");
        assertThat(RefreshToken.hash("raw-token-value")).isEqualTo(hash);
        assertThat(RefreshToken.hash("raw-token-valuE")).isNotEqualTo(hash);
    }

    // 잡는 버그: 만료 시각 경계를 잘못 비교해서 만료된 토큰을 받아주는 것
    @Test
    void 만료_시각_직전까지만_쓸_수_있고_만료_시각부터는_못_쓴다() {
        RefreshToken token = new RefreshToken(1L, "hash", EXPIRES_AT, NOW);

        assertThat(token.isUsable(EXPIRES_AT.minusSeconds(1))).isTrue();
        assertThat(token.isUsable(EXPIRES_AT)).isFalse();
        assertThat(token.isUsable(EXPIRES_AT.plusSeconds(1))).isFalse();
    }

    // 잡는 버그: 폐기된 토큰을 만료 전이라는 이유로 다시 받아주는 것
    @Test
    void 폐기된_토큰은_만료_전이어도_못_쓴다() {
        RefreshToken token = new RefreshToken(1L, "hash", EXPIRES_AT, NOW);

        token.revoke(NOW);

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.isUsable(NOW.plusSeconds(60))).isFalse();
    }

    // 잡는 버그: 이미 폐기된 토큰을 다시 폐기할 때 폐기 시각이 덮어써져 "언제 처음 폐기됐는지"가 사라지는 것
    @Test
    void 다시_폐기해도_처음_폐기한_시각이_유지된다() {
        RefreshToken token = new RefreshToken(1L, "hash", EXPIRES_AT, NOW);
        Instant firstRevokedAt = NOW.plusSeconds(10);

        token.revoke(firstRevokedAt);
        token.revoke(NOW.plusSeconds(99));

        assertThat(token.getRevokedAt()).isEqualTo(firstRevokedAt);
    }
}
