package com.kkobak.server.presentation.doselog;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DoseLogCheckRequest {

    @NotNull(message = "복용 시간을 선택해주세요.")
    private Long scheduleId;

    @NotNull(message = "복용 날짜를 입력해주세요.")
    private LocalDate scheduledDate;
}
