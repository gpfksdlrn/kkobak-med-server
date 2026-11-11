package com.kkobak.server;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

// 테스트가 시작될 때 일회용 Postgres 컨테이너를 띄우고, 접속 정보를 자동으로 연결한다.
// 개발용 DB(localhost:5432)와 완전히 분리되어, 테스트가 내 데이터를 건드리지 않는다.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18");
    }
}
