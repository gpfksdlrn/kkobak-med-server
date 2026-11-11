package com.kkobak.server.application.user;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.kkobak.server.application.auth.GoogleAccount;
import com.kkobak.server.application.auth.GoogleTokenVerifierPort;
import com.kkobak.server.domain.user.User;
import com.kkobak.server.domain.user.UserRepository;
import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final GoogleTokenVerifierPort googleTokenVerifierPort;
    private final Clock clock;

    // Google ID 토큰을 검증하고, 그 Google 계정의 회원을 찾는다. 없으면 새로 만든다 (회원가입 폼 없음).
    public User findOrCreateByGoogleIdToken(String idToken) {
        GoogleAccount account = googleTokenVerifierPort.verify(idToken);

        Optional<User> foundUser = userRepository.findByGoogleSub(account.getGoogleSub());
        if (foundUser.isPresent()) {
            return foundUser.get();
        }
        return userRepository.save(new User(account.getEmail(), account.getName(), account.getGoogleSub(), clock.instant()));
    }

    public User getUser(Long userId) {
        Optional<User> foundUser = userRepository.findById(userId);
        if (!foundUser.isPresent()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return foundUser.get();
    }

    public User getUserByEmail(String email) {
        Optional<User> foundUser = userRepository.findByEmail(email);
        if (!foundUser.isPresent()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return foundUser.get();
    }

    // id → 회원. 같은 id 가 여러 번 들어와도 한 번만 조회된다.
    public Map<Long, User> getUsersByIds(List<Long> userIds) {
        Map<Long, User> usersById = new HashMap<>();
        for (User user : userRepository.findAllByIds(userIds)) {
            usersById.put(user.getId(), user);
        }
        return usersById;
    }
}
