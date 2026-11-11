package com.kkobak.server.global.config;

import java.security.SecureRandom;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 난수도 시각처럼 주입받는다. 리프레시 토큰처럼 추측되면 안 되는 값은 SecureRandom 으로 만든다.
@Configuration
public class RandomConfig {

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
