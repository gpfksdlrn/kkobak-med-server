package com.kkobak.server.domain.medication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicationRepository {

    Medication save(Medication medication);

    Optional<Medication> findById(Long id);

    // 이 주인(userId)의 약 전체, 최근 등록순
    List<Medication> findAllByUserId(Long userId);

    // 특정 날짜에 복용 중인(시작일 <= 날짜 <= 종료일) 약과 그 복용 시간들
    List<Medication> findActiveByUserIdAndDate(Long userId, LocalDate date);

    // 복용 시간(schedule) id 로 그 시간이 속한 약을 찾는다. 복용 시간은 약을 통해서만 다룬다.
    Optional<Medication> findByScheduleId(Long scheduleId);

    void delete(Medication medication);

    // 지금까지의 변경을 DB 에 즉시 반영한다. 수정 중 새로 추가된 복용 시간의 id 를 바로 받기 위해 쓴다.
    void flush();
}
