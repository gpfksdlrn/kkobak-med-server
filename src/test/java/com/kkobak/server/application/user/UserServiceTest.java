package com.kkobak.server.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kkobak.server.application.auth.GoogleAccount;
import com.kkobak.server.application.auth.GoogleTokenVerifierPort;
import com.kkobak.server.domain.user.User;
import com.kkobak.server.domain.user.UserRepository;
import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

class UserServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        GoogleTokenVerifierPort fakeVerifier = new GoogleTokenVerifierPort() {
            @Override
            public GoogleAccount verify(String idToken) {
                if (!idToken.equals("valid-id-token")) {
                    throw new BusinessException(ErrorCode.INVALID_GOOGLE_TOKEN);
                }
                return new GoogleAccount("google-sub-1", "hong@gmail.com", "홍길동");
            }
        };
        userService = new UserService(userRepository, fakeVerifier, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // 잡는 버그: 이미 가입한 사람이 로그인할 때마다 회원이 새로 생기는 것
    @Test
    void 이미_가입한_Google_계정이면_기존_회원을_돌려주고_새로_저장하지_않는다() {
        User existing = new User("hong@gmail.com", "홍길동", "google-sub-1", NOW);
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.of(existing));

        User user = userService.findOrCreateByGoogleIdToken("valid-id-token");

        assertThat(user).isSameAs(existing);
        verify(userRepository, never()).save(any(User.class));
    }

    // 잡는 버그: 처음 온 사람을 가입시키지 않거나, Google 정보/가입 시각을 잘못 넣는 것
    @Test
    void 처음_온_Google_계정이면_그_정보로_회원을_만든다() {
        when(userRepository.findByGoogleSub("google-sub-1")).thenReturn(Optional.<User>empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.findOrCreateByGoogleIdToken("valid-id-token");

        assertThat(user.getGoogleSub()).isEqualTo("google-sub-1");
        assertThat(user.getEmail()).isEqualTo("hong@gmail.com");
        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.getCreatedAt()).isEqualTo(NOW);
    }

    // 잡는 버그: 검증에 실패한 토큰으로도 회원이 만들어지는 것
    @Test
    void Google_토큰_검증에_실패하면_회원을_찾거나_만들지_않는다() {
        assertThatThrownBy(() -> userService.findOrCreateByGoogleIdToken("forged"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_GOOGLE_TOKEN);
        verify(userRepository, never()).save(any(User.class));
    }
}
