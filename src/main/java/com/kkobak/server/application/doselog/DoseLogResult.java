package com.kkobak.server.application.doselog;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.kkobak.server.domain.doselog.DoseLog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DoseLogResult {

    private final Long id;
    private final Long scheduleId;
    private final LocalDate scheduledDate;
    private final Instant takenAt;
    private final Long checkedBy;

    public static DoseLogResult from(DoseLog doseLog) {
        return new DoseLogResult(doseLog.getId(), doseLog.getScheduleId(), doseLog.getScheduledDate(),
                doseLog.getTakenAt(), doseLog.getCheckedBy());
    }

    public static List<DoseLogResult> fromList(List<DoseLog> doseLogs) {
        List<DoseLogResult> results = new ArrayList<>();
        for (DoseLog doseLog : doseLogs) {
            results.add(from(doseLog));
        }
        return results;
    }
}
