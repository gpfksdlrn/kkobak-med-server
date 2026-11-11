package com.kkobak.server.application.care;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.kkobak.server.domain.care.AccessScope;
import com.kkobak.server.domain.care.CareRelation;
import com.kkobak.server.domain.care.CareRelationRepository;
import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CareRelationService {

    private final CareRelationRepository careRelationRepository;
    private final Clock clock;

    // 환자(patientId)가 가족(caregiverId)을 초대한다. 같은 사람을 두 번 초대할 수 없다.
    public CareRelation invite(Long patientId, Long caregiverId) {
        if (careRelationRepository.existsByPatientIdAndCaregiverId(patientId, caregiverId)) {
            throw new BusinessException(ErrorCode.CARE_RELATION_ALREADY_EXISTS);
        }
        return careRelationRepository.save(new CareRelation(patientId, caregiverId, clock.instant()));
    }

    public CareRelation accept(Long actorId, Long careRelationId) {
        CareRelation careRelation = getRelationOfParticipant(actorId, careRelationId);
        careRelation.accept(actorId, clock.instant());
        return careRelation;
    }

    // 거절(초대받은 가족) / 초대 취소(환자) / 연결 끊기(양쪽) 모두 이 한 가지 동작이다
    public void remove(Long actorId, Long careRelationId) {
        careRelationRepository.delete(getRelationOfParticipant(actorId, careRelationId));
    }

    public List<CareRelation> getMyRelations(Long userId) {
        return careRelationRepository.findAllByUserId(userId);
    }

    // 이 사람이 다룰 수 있는 데이터 범위: 본인 + 수락된 가족으로서 돌보는 환자들
    public AccessScope getAccessScope(Long actorId) {
        return new AccessScope(actorId, careRelationRepository.findAcceptedPatientIdsByCaregiverId(actorId));
    }

    // 연결의 당사자가 아니면 없는 연결과 똑같이 응답한다 (남의 연결이 있다는 사실을 숨김)
    private CareRelation getRelationOfParticipant(Long actorId, Long careRelationId) {
        Optional<CareRelation> foundRelation = careRelationRepository.findById(careRelationId);
        if (!foundRelation.isPresent() || !foundRelation.get().involves(actorId)) {
            throw new BusinessException(ErrorCode.CARE_RELATION_NOT_FOUND);
        }
        return foundRelation.get();
    }
}
