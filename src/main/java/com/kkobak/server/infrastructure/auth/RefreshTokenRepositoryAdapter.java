package com.kkobak.server.infrastructure.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.kkobak.server.domain.auth.RefreshToken;
import com.kkobak.server.domain.auth.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return refreshTokenJpaRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash);
    }

    @Override
    public void revokeAllByUserId(Long userId, Instant now) {
        refreshTokenJpaRepository.revokeAllByUserId(userId, now);
    }
}
