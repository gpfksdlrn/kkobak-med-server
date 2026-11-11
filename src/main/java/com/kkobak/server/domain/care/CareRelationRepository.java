package com.kkobak.server.domain.care;

import java.util.List;
import java.util.Optional;

public interface CareRelationRepository {

    CareRelation save(CareRelation careRelation);

    Optional<CareRelation> findById(Long id);

    boolean existsByPatientIdAndCaregiverId(Long patientId, Long caregiverId);

    // 내가 환자이거나 가족인 연결 전부 (최근 순)
    List<CareRelation> findAllByUserId(Long userId);

    // 내가 가족으로 "수락된" 환자들의 id
    List<Long> findAcceptedPatientIdsByCaregiverId(Long caregiverId);

    void delete(CareRelation careRelation);
}
