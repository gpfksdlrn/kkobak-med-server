package com.kkobak.server.infrastructure.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kkobak.server.global.config.AuthProperties;

@Configuration
public class GoogleAuthConfig {

    @Bean
    public GoogleIdTokenVerifierAdapter googleIdTokenVerifierAdapter(AuthProperties authProperties) {
        return new GoogleIdTokenVerifierAdapter(GoogleIdTokenVerifierAdapter.createGoogleJwtDecoder(),
                authProperties.getGoogleClientId());
    }
}
