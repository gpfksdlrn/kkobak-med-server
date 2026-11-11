package com.kkobak.server.application.medication;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.kkobak.server.domain.care.AccessScope;
import com.kkobak.server.domain.medication.Medication;
import com.kkobak.server.domain.medication.MedicationRepository;
import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 약 도메인의 업무 로직. 트랜잭션은 이 Service 를 부르는 Facade 가 연다.
 * 권한: AccessScope(본인 + 수락된 가족으로서 돌보는 환자)에 들어가는 사람의 약만 다룰 수 있다.
 * 다룰 수 없는 약은 없는 약과 똑같이 응답한다 (남의 약이 존재한다는 사실 자체가 새지 않게).
 */
@Service
@RequiredArgsConstructor
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final Clock clock;

    // patientId 가 없으면 내 약, 있으면 돌보는 환자의 약으로 등록한다
    public Medication create(AccessScope scope, Long patientId, MedicationCommand command) {
        Long ownerId = scope.resolveTargetUserId(patientId);
        Instant now = clock.instant();
        Medication medication = new Medication(ownerId, command.getName(), command.getDosageText(),
                command.getMealTiming(), command.getStartDate(), command.getEndDate(), now);
        medication.replaceScheduleTimes(command.getTimes(), now);
        return medicationRepository.save(medication);
    }

    public Medication getAccessibleMedication(AccessScope scope, Long medicationId) {
        Optional<Medication> foundMedication = medicationRepository.findById(medicationId);
        if (!foundMedication.isPresent() || !scope.canAccess(foundMedication.get().getUserId())) {
            throw new BusinessException(ErrorCode.MEDICATION_NOT_FOUND);
        }
        return foundMedication.get();
    }

    // 복용 시간 id 로 약을 찾는다. 없거나 다룰 수 없으면 똑같이 "복용 시간을 찾을 수 없음".
    public Medication getAccessibleMedicationByScheduleId(AccessScope scope, Long scheduleId) {
        Optional<Medication> foundMedication = medicationRepository.findByScheduleId(scheduleId);
        if (!foundMedication.isPresent() || !scope.canAccess(foundMedication.get().getUserId())) {
            throw new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND);
        }
        return foundMedication.get();
    }

    // 복용 체크용: 다룰 수 있는 약이면서, 그 날짜가 복용 기간이어야 한다.
    // 권한 확인을 먼저 한다. 남의 약인데 "기간이 아님"(400)이 먼저 나가면 남의 약의 복용 기간이 새기 때문.
    public Medication getAccessibleActiveMedicationByScheduleId(AccessScope scope, Long scheduleId, LocalDate date) {
        Medication medication = getAccessibleMedicationByScheduleId(scope, scheduleId);
        if (!medication.isActiveOn(date)) {
            throw new BusinessException(ErrorCode.DOSE_DATE_OUT_OF_PERIOD);
        }
        return medication;
    }

    public List<Medication> getMedications(AccessScope scope, Long patientId) {
        return medicationRepository.findAllByUserId(scope.resolveTargetUserId(patientId));
    }

    public List<Medication> getActiveMedications(AccessScope scope, Long patientId, LocalDate date) {
        return medicationRepository.findActiveByUserIdAndDate(scope.resolveTargetUserId(patientId), date);
    }

    // 변경 내용은 트랜잭션이 끝날 때 JPA 가 알아서 UPDATE 한다 (변경 감지). 그래서 save 를 다시 부르지 않는다.
    public Medication update(AccessScope scope, Long medicationId, MedicationCommand command) {
        Medication medication = getAccessibleMedication(scope, medicationId);
        Instant now = clock.instant();
        medication.update(command.getName(), command.getDosageText(), command.getMealTiming(),
                command.getStartDate(), command.getEndDate(), now);
        medication.replaceScheduleTimes(command.getTimes(), now);
        // 새로 추가된 복용 시간은 DB 에 들어가야 id 가 생긴다. 응답에 id 를 담으려고 바로 반영한다.
        medicationRepository.flush();
        return medication;
    }

    public void delete(AccessScope scope, Long medicationId) {
        medicationRepository.delete(getAccessibleMedication(scope, medicationId));
    }
}
