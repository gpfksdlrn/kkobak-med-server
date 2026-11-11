package com.kkobak.server.global.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 현재 시각을 직접 꺼내 쓰지 않고 Clock 을 주입받게 해서, 테스트에서 시간을 고정할 수 있게 한다.
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
