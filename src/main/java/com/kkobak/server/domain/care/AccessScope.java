package com.kkobak.server.domain.care;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

/**
 * "이 사람(actor)이 누구의 데이터를 다룰 수 있는가".
 * 본인 + 내가 가족으로 수락된 환자들. 약/복용 기록의 모든 권한 검사는 이 객체 하나로 한다.
 */
public class AccessScope {

    private final Long actorId;
    private final Set<Long> patientIds;

    public AccessScope(Long actorId, List<Long> acceptedPatientIds) {
        this.actorId = actorId;
        this.patientIds = Collections.unmodifiableSet(new HashSet<Long>(acceptedPatientIds));
    }

    // 이 주인(ownerId)의 데이터를 다룰 수 있는가: 본인이거나, 수락된 가족으로서 돌보는 환자
    public boolean canAccess(Long ownerId) {
        return actorId.equals(ownerId) || patientIds.contains(ownerId);
    }

    /**
     * 요청이 "누구의 데이터"를 대상으로 하는지 정한다.
     * patientId 가 없으면 본인, 있으면 그 환자. 돌보는 환자가 아니면 "연결된 환자를 찾을 수 없음".
     */
    public Long resolveTargetUserId(Long patientId) {
        if (patientId == null || patientId.equals(actorId)) {
            return actorId;
        }
        if (patientIds.contains(patientId)) {
            return patientId;
        }
        throw new BusinessException(ErrorCode.PATIENT_NOT_FOUND);
    }
}
