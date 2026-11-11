package com.kkobak.server.domain.care;

import java.time.Instant;

import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 환자-가족 연결 한 건. 방향이 있다: patient 의 데이터를 caregiver 가 다룬다 (반대는 아님).
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "care_relations")
public class CareRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "caregiver_id", nullable = false)
    private Long caregiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CareRelationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    // 초대. 자기 자신은 초대할 수 없다.
    public CareRelation(Long patientId, Long caregiverId, Instant now) {
        if (patientId.equals(caregiverId)) {
            throw new BusinessException(ErrorCode.CANNOT_INVITE_SELF);
        }
        this.patientId = patientId;
        this.caregiverId = caregiverId;
        this.status = CareRelationStatus.PENDING;
        this.createdAt = now;
    }

    /**
     * 수락. 초대받은 가족만 할 수 있다.
     * 다른 사람(환자 본인 포함)이 수락하려 하면 "연결을 찾을 수 없음"으로 응답한다 (남의 초대가 있다는 사실을 숨김).
     * 이미 수락된 연결을 다시 수락해도 처음 수락 시각을 유지한다.
     */
    public void accept(Long actorId, Instant now) {
        if (!caregiverId.equals(actorId)) {
            throw new BusinessException(ErrorCode.CARE_RELATION_NOT_FOUND);
        }
        if (status == CareRelationStatus.ACCEPTED) {
            return;
        }
        this.status = CareRelationStatus.ACCEPTED;
        this.acceptedAt = now;
    }

    // 이 연결의 당사자(환자 또는 가족)인가
    public boolean involves(Long userId) {
        return patientId.equals(userId) || caregiverId.equals(userId);
    }

    public boolean isAccepted() {
        return status == CareRelationStatus.ACCEPTED;
    }
}
