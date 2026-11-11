package com.kkobak.server.application.doselog;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kkobak.server.application.care.CareRelationService;
import com.kkobak.server.application.medication.MedicationService;
import com.kkobak.server.domain.care.AccessScope;

import lombok.RequiredArgsConstructor;

/**
 * 복용 기록 유스케이스의 입구.
 * 복용 체크는 세 도메인이 엮인다: (1) 가족 연결로 다룰 수 있는 범위 확인 → (2) 그 복용 시간이 범위 안의 약이고 복용 기간인지(약)
 * → (3) 기록(복용 기록, 체크한 사람 포함). 이 순서를 Facade 가 한곳에서 책임지고, 하나의 트랜잭션으로 묶는다.
 */
@Component
@RequiredArgsConstructor
public class DoseLogFacade {

    private final CareRelationService careRelationService;
    private final MedicationService medicationService;
    private final DoseLogService doseLogService;

    @Transactional
    public DoseLogResult checkDose(Long actorId, Long scheduleId, LocalDate scheduledDate) {
        AccessScope scope = careRelationService.getAccessScope(actorId);
        medicationService.getAccessibleActiveMedicationByScheduleId(scope, scheduleId, scheduledDate);
        return DoseLogResult.from(doseLogService.check(scheduleId, scheduledDate, actorId));
    }

    // patientId: 가족이 돌보는 환자의 기록을 볼 때만 넣는다. 없으면 내 기록.
    @Transactional(readOnly = true)
    public List<DoseLogResult> getDoseLogs(Long actorId, Long patientId, LocalDate date) {
        AccessScope scope = careRelationService.getAccessScope(actorId);
        return DoseLogResult.fromList(doseLogService.getDoseLogs(scope.resolveTargetUserId(patientId), date));
    }
}
