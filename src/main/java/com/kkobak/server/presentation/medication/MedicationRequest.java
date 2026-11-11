package com.kkobak.server.presentation.medication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.kkobak.server.application.medication.MedicationCommand;
import com.kkobak.server.domain.medication.MealTiming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 약 등록/수정 요청 본문 (JSON). 형식 검증은 여기서, 업무 규칙(기간 등) 검증은 도메인에서 한다.
@Getter
@Setter
@NoArgsConstructor
public class MedicationRequest {

    @NotBlank(message = "약 이름을 입력해주세요.")
    @Size(max = 100, message = "약 이름은 100자 이하로 입력해주세요.")
    private String name;

    @Size(max = 100, message = "복용량은 100자 이하로 입력해주세요.")
    private String dosageText;

    @NotNull(message = "복용 방법(식전/식후 등)을 선택해주세요.")
    private MealTiming mealTiming;

    @NotNull(message = "복용 시작일을 선택해주세요.")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotEmpty(message = "복용 시간을 하나 이상 추가해주세요.")
    private List<@NotNull(message = "복용 시간을 입력해주세요.") LocalTime> times;

    public MedicationCommand toCommand() {
        // 복용량은 비워서 보내면 "" 대신 null 로 저장한다
        String trimmedDosageText = null;
        if (dosageText != null && !dosageText.trim().isEmpty()) {
            trimmedDosageText = dosageText.trim();
        }
        return new MedicationCommand(name.trim(), trimmedDosageText, mealTiming, startDate, endDate, times);
    }
}
