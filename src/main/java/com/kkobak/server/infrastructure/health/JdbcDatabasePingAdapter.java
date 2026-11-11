package com.kkobak.server.infrastructure.health;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.kkobak.server.application.health.DatabasePingPort;

import lombok.RequiredArgsConstructor;

// DatabasePingPort 의 실제 구현. 가장 가벼운 쿼리(select 1)로 DB 연결을 확인한다.
@Component
@RequiredArgsConstructor
public class JdbcDatabasePingAdapter implements DatabasePingPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean isDatabaseReachable() {
        try {
            Integer queryResult = jdbcTemplate.queryForObject("select 1", Integer.class);
            return queryResult != null && queryResult == 1;
        } catch (DataAccessException exception) {
            return false;
        }
    }
}
