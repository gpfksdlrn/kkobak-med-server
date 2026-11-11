package com.kkobak.server.application.medication;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.kkobak.server.domain.medication.MealTiming;
import com.kkobak.server.domain.medication.Medication;
import com.kkobak.server.domain.medication.Schedule;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Facade 가 돌려주는 결과. Entity 를 트랜잭션 밖(Controller)으로 내보내지 않으려고 값만 복사해 담는다.
@Getter
@RequiredArgsConstructor
public class MedicationResult {

    private final Long id;
    private final String name;
    private final String dosageText;
    private final MealTiming mealTiming;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<ScheduleResult> schedules;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static MedicationResult from(Medication medication) {
        List<ScheduleResult> schedules = new ArrayList<>();
        for (Schedule schedule : medication.getSchedules()) {
            schedules.add(new ScheduleResult(schedule.getId(), schedule.getTimeOfDay()));
        }
        // 복용 시간 순으로 정렬 (새로 추가된 시간은 목록 끝에 붙어 있을 수 있음)
        Collections.sort(schedules, new Comparator<ScheduleResult>() {
            @Override
            public int compare(ScheduleResult first, ScheduleResult second) {
                return first.getTimeOfDay().compareTo(second.getTimeOfDay());
            }
        });

        return new MedicationResult(medication.getId(), medication.getName(), medication.getDosageText(),
                medication.getMealTiming(), medication.getStartDate(), medication.getEndDate(), schedules,
                medication.getCreatedAt(), medication.getUpdatedAt());
    }

    public static List<MedicationResult> fromList(List<Medication> medications) {
        List<MedicationResult> results = new ArrayList<>();
        for (Medication medication : medications) {
            results.add(from(medication));
        }
        return results;
    }

    // 복용 시간 하나. 복용 체크(PUT /api/dose-logs)에 이 id 를 쓴다.
    @Getter
    @RequiredArgsConstructor
    public static class ScheduleResult {

        private final Long id;
        private final LocalTime timeOfDay;
    }
}
