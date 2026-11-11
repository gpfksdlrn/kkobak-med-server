package com.kkobak.server.domain.user;

import java.util.List;
import java.util.Optional;

// 사용자를 저장/조회하는 약속. 도메인이 "이런 기능이 필요하다"고 선언만 하고, 구현은 infrastructure 가 한다.
public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleSub(String googleSub);

    List<User> findAllByIds(List<Long> ids);
}
