package com.kkobak.server.application.auth;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kkobak.server.application.user.UserResult;
import com.kkobak.server.application.user.UserService;
import com.kkobak.server.domain.user.User;
import com.kkobak.server.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

// 로그인/재발급/로그아웃 유스케이스의 입구. 회원(UserService)과 토큰(TokenService)을 조합한다.
@Component
@RequiredArgsConstructor
public class AuthFacade {

    private final UserService userService;
    private final TokenService tokenService;

    // Google ID 토큰으로 로그인. 처음 온 사용자는 회원으로 만든다.
    @Transactional
    public LoginResult loginWithGoogle(String idToken) {
        User user = userService.findOrCreateByGoogleIdToken(idToken);
        TokenResult tokens = tokenService.issueTokens(user.getId());
        return new LoginResult(tokens, UserResult.from(user));
    }

    /**
     * 리프레시 토큰으로 새 토큰 한 쌍 발급 (쓴 리프레시 토큰은 폐기 = 교체).
     * noRollbackFor: 폐기된 토큰이 재사용되면 "전부 폐기"를 한 뒤 예외를 던진다.
     * 기본 규칙대로 예외에 롤백하면 그 "전부 폐기"까지 취소되므로, 이 메서드는 업무 예외로는 롤백하지 않는다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResult refresh(String rawRefreshToken) {
        Long userId = tokenService.consumeRefreshToken(rawRefreshToken);
        return tokenService.issueTokens(userId);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        tokenService.revokeRefreshToken(rawRefreshToken);
    }
}
