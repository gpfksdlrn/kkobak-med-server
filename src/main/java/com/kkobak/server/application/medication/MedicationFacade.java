package com.kkobak.server.application.medication;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kkobak.server.application.care.CareRelationService;
import com.kkobak.server.domain.care.AccessScope;

import lombok.RequiredArgsConstructor;

/**
 * 약 관련 유스케이스의 입구. Controller 는 Facade 만 부른다.
 * - 트랜잭션 경계: 메서드 하나 = 트랜잭션 하나
 * - 권한: 가족 연결(CareRelationService)에서 "이 사람이 다룰 수 있는 범위(AccessScope)"를 받아 약 Service 에 넘긴다
 * - 결과 변환: Entity 를 트랜잭션 안에서 MedicationResult 로 바꿔서 내보낸다
 * patientId: 가족이 돌보는 환자의 약을 다룰 때만 넣는다. 없으면 내 약.
 */
@Component
@RequiredArgsConstructor
public class MedicationFacade {

    private final MedicationService medicationService;
    private final CareRelationService careRelationService;

    @Transactional
    public MedicationResult createMedication(Long actorId, Long patientId, MedicationCommand command) {
        AccessScope scope = careRelationService.getAccessScope(actorId);
        return MedicationResult.from(medicationService.create(scope, patientId, command));
    }

    @Transactional(readOnly = true)
    public MedicationResult getMedication(Long actorId, Long medicationId) {
        AccessScope scope = careRelationService.getAccessScope(actorId);
        return MedicationResult.from(medicationService.getAccessibleMedication(scope, medicationId));
    }

    @Transactional(readOnly = true)
    public List<MedicationResult> getMedications(Long actorId, Long patientId) {
        AccessScope scope = careRelationService.getAccessScope(actorId);
        return MedicationResult.fromList(medicationService.getMedications(scope, patientId));
    }

    @Transactional(readOnly = true)
    public List<MedicationResult> getActiveMedications(Long actorId, Long patientId, LocalDate date) {
        AccessScope scope = careRelationService.getAccessScope(actorId);
        return MedicationResult.fromList(medicationService.getActiveMedications(scope, patientId, date));
    }

    @Transactional
    public MedicationResult updateMedication(Long actorId, Long medicationId, MedicationCommand command) {
        AccessScope scope = careRelationService.getAccessScope(actorId);
        return MedicationResult.from(medicationService.update(scope, medicationId, command));
    }

    @Transactional
    public void deleteMedication(Long actorId, Long medicationId) {
        AccessScope scope = careRelationService.getAccessScope(actorId);
        medicationService.delete(scope, medicationId);
    }
}
