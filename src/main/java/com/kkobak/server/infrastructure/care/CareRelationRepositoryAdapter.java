package com.kkobak.server.infrastructure.care;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.kkobak.server.domain.care.CareRelation;
import com.kkobak.server.domain.care.CareRelationRepository;
import com.kkobak.server.domain.care.CareRelationStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CareRelationRepositoryAdapter implements CareRelationRepository {

    private final CareRelationJpaRepository careRelationJpaRepository;

    @Override
    public CareRelation save(CareRelation careRelation) {
        return careRelationJpaRepository.save(careRelation);
    }

    @Override
    public Optional<CareRelation> findById(Long id) {
        return careRelationJpaRepository.findById(id);
    }

    @Override
    public boolean existsByPatientIdAndCaregiverId(Long patientId, Long caregiverId) {
        return careRelationJpaRepository.existsByPatientIdAndCaregiverId(patientId, caregiverId);
    }

    @Override
    public List<CareRelation> findAllByUserId(Long userId) {
        return careRelationJpaRepository.findAllByUserId(userId);
    }

    @Override
    public List<Long> findAcceptedPatientIdsByCaregiverId(Long caregiverId) {
        return careRelationJpaRepository.findPatientIdsByCaregiverIdAndStatus(caregiverId, CareRelationStatus.ACCEPTED);
    }

    @Override
    public void delete(CareRelation careRelation) {
        careRelationJpaRepository.delete(careRelation);
    }
}
