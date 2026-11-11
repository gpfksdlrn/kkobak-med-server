package com.kkobak.server.presentation.medication;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.kkobak.server.application.medication.MedicationResult;
import com.kkobak.server.domain.medication.MealTiming;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 약 응답 본문. 복용 시간은 id 와 함께, 시간은 항상 "HH:mm" 문자열로 내려준다 (초 단위 "08:00:00" 이 섞이지 않게).
@Getter
@RequiredArgsConstructor
public class MedicationResponse {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final Long id;
    private final String name;
    private final String dosageText;
    private final MealTiming mealTiming;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<ScheduleResponse> schedules;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static MedicationResponse from(MedicationResult result) {
        List<ScheduleResponse> schedules = new ArrayList<>();
        for (MedicationResult.ScheduleResult schedule : result.getSchedules()) {
            schedules.add(new ScheduleResponse(schedule.getId(), schedule.getTimeOfDay().format(TIME_FORMAT)));
        }
        return new MedicationResponse(result.getId(), result.getName(), result.getDosageText(),
                result.getMealTiming(), result.getStartDate(), result.getEndDate(), schedules,
                result.getCreatedAt(), result.getUpdatedAt());
    }

    public static List<MedicationResponse> fromList(List<MedicationResult> results) {
        List<MedicationResponse> responses = new ArrayList<>();
        for (MedicationResult result : results) {
            responses.add(from(result));
        }
        return responses;
    }

    @Getter
    @RequiredArgsConstructor
    public static class ScheduleResponse {

        private final Long id;
        private final String time;
    }
}
