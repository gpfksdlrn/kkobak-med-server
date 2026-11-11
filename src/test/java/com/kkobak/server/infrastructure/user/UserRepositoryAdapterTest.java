package com.kkobak.server.infrastructure.user;

import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.kkobak.server.TestcontainersConfiguration;
import com.kkobak.server.domain.user.User;
import com.kkobak.server.domain.user.UserRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, UserRepositoryAdapter.class})
class UserRepositoryAdapterTest {

    private static final Instant USER_CREATED_AT = Instant.parse("2026-11-01T00:00:00Z");

    // 테스트도 구현체(Adapter)가 아니라 도메인의 약속(UserRepository)을 통해 사용한다.
    @Autowired
    private UserRepository userRepository;

    @Test
    void 사용자를_저장하면_id가_생기고_이메일로_조회할_수_있다() {
        User savedUser = userRepository.save(new User("hong@example.com", "홍길동", "google-sub-1", USER_CREATED_AT));

        assertThat(savedUser.getId()).isNotNull();
        User foundUser = userRepository.findByEmail("hong@example.com").orElseThrow();
        assertThat(foundUser.getName()).isEqualTo("홍길동");
        assertThat(foundUser.getCreatedAt()).isNotNull();
    }

    @Test
    void 구글_고유_ID로_조회할_수_있다() {
        userRepository.save(new User("kim@example.com", "김철수", "google-sub-9", USER_CREATED_AT));

        assertThat(userRepository.findByGoogleSub("google-sub-9")).isPresent();
        assertThat(userRepository.findByGoogleSub("없는-값")).isEmpty();
    }

    @Test
    void 같은_이메일로_두_번_저장하면_예외가_발생한다() {
        userRepository.save(new User("same@example.com", "첫번째", "google-sub-2", USER_CREATED_AT));
        userRepository.save(new User("other@example.com", "다른사람", "google-sub-3", USER_CREATED_AT));

        // 같은 이메일을 가진 사용자를 하나 더 저장하면 UNIQUE 제약 때문에 DB 가 거부한다
        assertThatThrownBy(() -> userRepository.save(new User("same@example.com", "두번째", "google-sub-4", USER_CREATED_AT)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
