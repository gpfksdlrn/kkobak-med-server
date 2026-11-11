package com.kkobak.server.application.medication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.kkobak.server.domain.medication.MealTiming;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 약 등록/수정에 필요한 값 묶음. HTTP(presentation)와 상관없는 application 계층의 입력 모양이다.
@Getter
@RequiredArgsConstructor
public class MedicationCommand {

    private final String name;
    private final String dosageText;
    private final MealTiming mealTiming;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<LocalTime> times;
}
