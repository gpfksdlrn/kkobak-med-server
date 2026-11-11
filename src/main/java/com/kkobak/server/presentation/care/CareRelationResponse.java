package com.kkobak.server.presentation.care;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.kkobak.server.application.care.CareRelationResult;
import com.kkobak.server.domain.care.CareRelationStatus;
import com.kkobak.server.presentation.user.UserResponse;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CareRelationResponse {

    private final Long id;
    private final UserResponse patient;     // 돌봄을 받는 사람 (약의 주인)
    private final UserResponse caregiver;   // 돌보는 가족
    private final CareRelationStatus status;
    private final Instant createdAt;
    private final Instant acceptedAt;

    public static CareRelationResponse from(CareRelationResult result) {
        return new CareRelationResponse(result.getId(), UserResponse.from(result.getPatient()),
                UserResponse.from(result.getCaregiver()), result.getStatus(), result.getCreatedAt(), result.getAcceptedAt());
    }

    public static List<CareRelationResponse> fromList(List<CareRelationResult> results) {
        List<CareRelationResponse> responses = new ArrayList<>();
        for (CareRelationResult result : results) {
            responses.add(from(result));
        }
        return responses;
    }
}
