package com.kkobak.server.domain.doselog;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DoseLogRepository {

    DoseLog save(DoseLog doseLog);

    /**
     * 복용 체크 (upsert): 그날 기록이 없으면 만들고, 있으면 복용 시각과 체크한 사람을 바꾼다. 바뀐 뒤의 기록을 돌려준다.
     * "있나 확인 → 없으면 저장"을 두 번에 나눠 하면 동시에 들어온 요청끼리 부딪히므로, 한 번에 처리해야 한다.
     */
    DoseLog upsertTaken(Long scheduleId, LocalDate scheduledDate, Instant takenAt, Long checkedBy);

    Optional<DoseLog> findByScheduleIdAndScheduledDate(Long scheduleId, LocalDate scheduledDate);

    // 이 주인(userId)의 약들의 복용 시간에 대한, 특정 날짜의 복용 기록 전부
    List<DoseLog> findAllByUserIdAndDate(Long userId, LocalDate date);
}
