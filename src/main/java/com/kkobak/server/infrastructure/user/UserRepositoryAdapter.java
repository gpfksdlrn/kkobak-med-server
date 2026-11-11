package com.kkobak.server.infrastructure.user;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.kkobak.server.domain.user.User;
import com.kkobak.server.domain.user.UserRepository;

import lombok.RequiredArgsConstructor;

// 도메인의 UserRepository 약속을 JPA 로 실제 구현한다.
@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findByGoogleSub(String googleSub) {
        return userJpaRepository.findByGoogleSub(googleSub);
    }

    @Override
    public List<User> findAllByIds(List<Long> ids) {
        return userJpaRepository.findAllById(ids);
    }
}
