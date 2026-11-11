package com.kkobak.server.domain.auth;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // 그 사용자의 아직 유효한 토큰을 전부 폐기한다 (토큰 탈취가 의심될 때)
    void revokeAllByUserId(Long userId, Instant now);
}
