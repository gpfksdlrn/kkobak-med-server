package com.kkobak.server.application.doselog;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.kkobak.server.domain.doselog.DoseLog;
import com.kkobak.server.domain.doselog.DoseLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DoseLogService {

    private final DoseLogRepository doseLogRepository;
    private final Clock clock;

    /**
     * 복용 체크. 그날 기록이 없으면 만들고, 있으면 복용 시각만 갱신한다 (upsert).
     * 같은 요청을 여러 번, 동시에 보내도 기록은 하나만 남고 모두 성공한다.
     */
    // checkedBy: 체크한 사람 (본인 또는 가족)
    public DoseLog check(Long scheduleId, LocalDate scheduledDate, Long checkedBy) {
        Instant now = clock.instant();
        return doseLogRepository.upsertTaken(scheduleId, scheduledDate, now, checkedBy);
    }

    // ownerId: 약 주인 (본인 또는 돌보는 환자)
    public List<DoseLog> getDoseLogs(Long ownerId, LocalDate date) {
        return doseLogRepository.findAllByUserIdAndDate(ownerId, date);
    }
}
