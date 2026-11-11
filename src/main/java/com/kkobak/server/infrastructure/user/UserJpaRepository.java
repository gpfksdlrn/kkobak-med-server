package com.kkobak.server.infrastructure.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kkobak.server.domain.user.User;

// Spring Data JPA 가 구현을 자동으로 만들어 주는 인터페이스. infrastructure 안에서만 쓴다.
interface UserJpaRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleSub(String googleSub);
}
