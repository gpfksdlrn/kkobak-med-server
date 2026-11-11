package com.kkobak.server.application.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.kkobak.server.domain.auth.RefreshToken;
import com.kkobak.server.domain.auth.RefreshTokenRepository;
import com.kkobak.server.global.config.AuthProperties;
import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 토큰 발급/재발급/폐기.
 * - Access Token: 짧게(30분) 쓰는 JWT. 서버는 저장하지 않고 서명만 확인한다.
 * - Refresh Token: 길게(14일) 쓰는 무작위 문자열. DB 에 해시를 저장해서 폐기할 수 있게 한다.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenIssuerPort accessTokenIssuerPort;
    private final AuthProperties authProperties;
    private final SecureRandom secureRandom;
    private final Clock clock;

    public TokenResult issueTokens(Long userId) {
        Instant now = clock.instant();

        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant refreshExpiresAt = now.plus(authProperties.getRefreshTokenValidity());
        refreshTokenRepository.save(new RefreshToken(userId, RefreshToken.hash(rawRefreshToken), refreshExpiresAt, now));

        AccessToken accessToken = accessTokenIssuerPort.issue(userId);
        return new TokenResult(accessToken.getValue(), authProperties.getAccessTokenValidity(),
                rawRefreshToken, authProperties.getRefreshTokenValidity());
    }

    /**
     * 재발급용으로 리프레시 토큰을 "사용"한다: 확인 후 폐기하고, 주인(userId)을 돌려준다. (한 번 쓴 토큰은 다시 못 쓴다 = 교체)
     * 이미 폐기된 토큰이 다시 들어오면 누군가 훔쳐서 쓰는 것으로 보고, 그 사용자의 토큰을 전부 폐기한다.
     */
    public Long consumeRefreshToken(String rawRefreshToken) {
        Instant now = clock.instant();
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByTokenHash(RefreshToken.hash(rawRefreshToken));
        if (!foundToken.isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken refreshToken = foundToken.get();
        if (refreshToken.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(refreshToken.getUserId(), now);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (!refreshToken.isUsable(now)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        refreshToken.revoke(now);
        return refreshToken.getUserId();
    }

    // 로그아웃. 없는 토큰이나 이미 폐기된 토큰이어도 조용히 넘어간다 (로그아웃은 몇 번 해도 결과가 같아야 한다)
    public void revokeRefreshToken(String rawRefreshToken) {
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByTokenHash(RefreshToken.hash(rawRefreshToken));
        if (foundToken.isPresent()) {
            foundToken.get().revoke(clock.instant());
        }
    }
}
