package com.kkobak.server.presentation.medication;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kkobak.server.application.medication.MedicationFacade;
import com.kkobak.server.application.medication.MedicationResult;
import com.kkobak.server.global.response.ApiResponse;
import com.kkobak.server.global.security.LoginUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 약 CRUD API.
 * 로그인한 사용자 id 는 Access Token 에서 꺼낸다 (@LoginUserId).
 * patientId: 가족이 돌보는 환자의 약을 다룰 때만 넣는다 (없으면 내 약). id 로 다루는 API 는 권한을 약 주인으로 판단한다.
 */
@RestController
@RequestMapping("/api/medications")
@RequiredArgsConstructor
public class MedicationController implements MedicationApiSpec {

    private final MedicationFacade medicationFacade;

    // 약 목록 (최근 등록순). patientId 가 없으면 내 약, 있으면 돌보는 환자의 약
    @Override
    @GetMapping
    public ApiResponse<List<MedicationResponse>> getMedications(@LoginUserId Long userId,
                                                                @RequestParam(required = false) Long patientId) {
        List<MedicationResult> results = medicationFacade.getMedications(userId, patientId);
        return ApiResponse.success(MedicationResponse.fromList(results));
    }

    // 특정 날짜에 복용 중인 약 (예: /api/medications/active?date=2026-11-01)
    @Override
    @GetMapping("/active")
    public ApiResponse<List<MedicationResponse>> getActiveMedications(
            @LoginUserId Long userId,
            @RequestParam(required = false) Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<MedicationResult> results = medicationFacade.getActiveMedications(userId, patientId, date);
        return ApiResponse.success(MedicationResponse.fromList(results));
    }

    @Override
    @GetMapping("/{medicationId}")
    public ApiResponse<MedicationResponse> getMedication(@LoginUserId Long userId,
                                                         @PathVariable Long medicationId) {
        MedicationResult result = medicationFacade.getMedication(userId, medicationId);
        return ApiResponse.success(MedicationResponse.from(result));
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MedicationResponse> createMedication(@LoginUserId Long userId,
                                                            @RequestParam(required = false) Long patientId,
                                                            @Valid @RequestBody MedicationRequest request) {
        MedicationResult result = medicationFacade.createMedication(userId, patientId, request.toCommand());
        return ApiResponse.success(MedicationResponse.from(result));
    }

    @Override
    @PutMapping("/{medicationId}")
    public ApiResponse<MedicationResponse> updateMedication(@LoginUserId Long userId,
                                                            @PathVariable Long medicationId,
                                                            @Valid @RequestBody MedicationRequest request) {
        MedicationResult result = medicationFacade.updateMedication(userId, medicationId, request.toCommand());
        return ApiResponse.success(MedicationResponse.from(result));
    }

    @Override
    @DeleteMapping("/{medicationId}")
    public ApiResponse<Void> deleteMedication(@LoginUserId Long userId,
                                              @PathVariable Long medicationId) {
        medicationFacade.deleteMedication(userId, medicationId);
        return ApiResponse.success();
    }
}
