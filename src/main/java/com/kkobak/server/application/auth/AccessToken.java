package com.kkobak.server.application.auth;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AccessToken {

    private final String value;
    private final Instant expiresAt;
}
