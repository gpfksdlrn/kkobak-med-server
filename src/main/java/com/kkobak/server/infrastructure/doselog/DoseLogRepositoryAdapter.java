package com.kkobak.server.infrastructure.doselog;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.kkobak.server.domain.doselog.DoseLog;
import com.kkobak.server.domain.doselog.DoseLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DoseLogRepositoryAdapter implements DoseLogRepository {

    private final DoseLogJpaRepository doseLogJpaRepository;

    @Override
    public DoseLog save(DoseLog doseLog) {
        return doseLogJpaRepository.save(doseLog);
    }

    @Override
    public DoseLog upsertTaken(Long scheduleId, LocalDate scheduledDate, Instant takenAt, Long checkedBy) {
        doseLogJpaRepository.upsertTaken(scheduleId, scheduledDate, takenAt, checkedBy);
        // 바로 위에서 만들었거나 고친 행이므로 반드시 있다
        return doseLogJpaRepository.findByScheduleIdAndScheduledDate(scheduleId, scheduledDate)
                .orElseThrow(() -> new IllegalStateException("upsert 직후 복용 기록을 찾지 못했습니다."));
    }

    @Override
    public Optional<DoseLog> findByScheduleIdAndScheduledDate(Long scheduleId, LocalDate scheduledDate) {
        return doseLogJpaRepository.findByScheduleIdAndScheduledDate(scheduleId, scheduledDate);
    }

    @Override
    public List<DoseLog> findAllByUserIdAndDate(Long userId, LocalDate date) {
        return doseLogJpaRepository.findAllByUserIdAndDate(userId, date);
    }
}
