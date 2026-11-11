package com.kkobak.server.application.care;

import java.time.Instant;

import com.kkobak.server.application.user.UserResult;
import com.kkobak.server.domain.care.CareRelationStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CareRelationResult {

    private final Long id;
    private final UserResult patient;
    private final UserResult caregiver;
    private final CareRelationStatus status;
    private final Instant createdAt;
    private final Instant acceptedAt;
}
