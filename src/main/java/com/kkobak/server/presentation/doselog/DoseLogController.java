package com.kkobak.server.presentation.doselog;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kkobak.server.application.doselog.DoseLogFacade;
import com.kkobak.server.application.doselog.DoseLogResult;
import com.kkobak.server.global.response.ApiResponse;
import com.kkobak.server.global.security.LoginUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 복용 기록 API.
 * 로그인한 사용자 id 는 Access Token 에서 꺼낸다 (@LoginUserId).
 */
@RestController
@RequestMapping("/api/dose-logs")
@RequiredArgsConstructor
public class DoseLogController implements DoseLogApiSpec {

    private final DoseLogFacade doseLogFacade;

    // 특정 날짜의 복용 기록. patientId 가 없으면 내 기록, 있으면 돌보는 환자의 기록 (예: /api/dose-logs?date=2026-11-01)
    @Override
    @GetMapping
    public ApiResponse<List<DoseLogResponse>> getDoseLogs(
            @LoginUserId Long userId,
            @RequestParam(required = false) Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DoseLogResult> results = doseLogFacade.getDoseLogs(userId, patientId, date);
        return ApiResponse.success(DoseLogResponse.fromList(results));
    }

    // 복용 체크. PUT 은 "몇 번을 보내도 결과가 같은" 요청에 쓴다 (기록은 하나만 남음)
    @Override
    @PutMapping
    public ApiResponse<DoseLogResponse> checkDose(@LoginUserId Long userId,
                                                  @Valid @RequestBody DoseLogCheckRequest request) {
        DoseLogResult result = doseLogFacade.checkDose(userId, request.getScheduleId(), request.getScheduledDate());
        return ApiResponse.success(DoseLogResponse.from(result));
    }
}
