package com.kkobak.server.presentation.doselog;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.kkobak.server.application.doselog.DoseLogResult;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DoseLogResponse {

    private final Long id;
    private final Long scheduleId;
    private final LocalDate scheduledDate;
    private final Instant takenAt;
    private final Long checkedBy;   // 체크한 사람의 사용자 id (본인 또는 가족)

    public static DoseLogResponse from(DoseLogResult result) {
        return new DoseLogResponse(result.getId(), result.getScheduleId(), result.getScheduledDate(),
                result.getTakenAt(), result.getCheckedBy());
    }

    public static List<DoseLogResponse> fromList(List<DoseLogResult> results) {
        List<DoseLogResponse> responses = new ArrayList<>();
        for (DoseLogResult result : results) {
            responses.add(from(result));
        }
        return responses;
    }
}
