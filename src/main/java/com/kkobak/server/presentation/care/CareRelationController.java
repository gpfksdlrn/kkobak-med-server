package com.kkobak.server.presentation.care;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kkobak.server.application.care.CareRelationFacade;
import com.kkobak.server.global.response.ApiResponse;
import com.kkobak.server.global.security.LoginUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 가족 연결 API: 초대 → (가족이) 수락 → 연결. 연결된 가족은 환자의 약/복용 기록을 다룰 수 있다.
@RestController
@RequestMapping("/api/care-relations")
@RequiredArgsConstructor
public class CareRelationController implements CareRelationApiSpec {

    private final CareRelationFacade careRelationFacade;

    @Override
    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CareRelationResponse> invite(@LoginUserId Long userId,
                                                    @Valid @RequestBody CareInvitationRequest request) {
        return ApiResponse.success(CareRelationResponse.from(careRelationFacade.invite(userId, request.getEmail().trim())));
    }

    @Override
    @GetMapping
    public ApiResponse<List<CareRelationResponse>> getMyRelations(@LoginUserId Long userId) {
        return ApiResponse.success(CareRelationResponse.fromList(careRelationFacade.getMyRelations(userId)));
    }

    @Override
    @PostMapping("/{careRelationId}/accept")
    public ApiResponse<CareRelationResponse> accept(@LoginUserId Long userId, @PathVariable Long careRelationId) {
        return ApiResponse.success(CareRelationResponse.from(careRelationFacade.accept(userId, careRelationId)));
    }

    @Override
    @DeleteMapping("/{careRelationId}")
    public ApiResponse<Void> remove(@LoginUserId Long userId, @PathVariable Long careRelationId) {
        careRelationFacade.remove(userId, careRelationId);
        return ApiResponse.success();
    }
}
