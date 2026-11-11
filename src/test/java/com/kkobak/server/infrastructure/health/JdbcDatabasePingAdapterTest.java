package com.kkobak.server.infrastructure.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.kkobak.server.TestcontainersConfiguration;

// 실제 Postgres(Testcontainers)에 붙어서 select 1 이 성공하는지 확인한다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JdbcDatabasePingAdapterTest {

    @Autowired
    private JdbcDatabasePingAdapter jdbcDatabasePingAdapter;

    @Test
    void 실제_데이터베이스에_연결되면_true를_반환한다() {
        assertThat(jdbcDatabasePingAdapter.isDatabaseReachable()).isTrue();
    }
}
