package com.kkobak.server.application.care;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kkobak.server.application.user.UserResult;
import com.kkobak.server.application.user.UserService;
import com.kkobak.server.domain.care.CareRelation;
import com.kkobak.server.domain.user.User;

import lombok.RequiredArgsConstructor;

// 가족 연결 유스케이스의 입구. 연결(CareRelationService)과 회원(UserService)을 조합한다.
@Component
@RequiredArgsConstructor
public class CareRelationFacade {

    private final CareRelationService careRelationService;
    private final UserService userService;

    // 이메일로 가족 초대. 가족이 먼저 Google 로그인으로 가입해 있어야 한다.
    @Transactional
    public CareRelationResult invite(Long patientId, String caregiverEmail) {
        User caregiver = userService.getUserByEmail(caregiverEmail);
        CareRelation careRelation = careRelationService.invite(patientId, caregiver.getId());
        return toResults(Collections.singletonList(careRelation)).get(0);
    }

    @Transactional(readOnly = true)
    public List<CareRelationResult> getMyRelations(Long userId) {
        return toResults(careRelationService.getMyRelations(userId));
    }

    @Transactional
    public CareRelationResult accept(Long actorId, Long careRelationId) {
        CareRelation careRelation = careRelationService.accept(actorId, careRelationId);
        return toResults(Collections.singletonList(careRelation)).get(0);
    }

    @Transactional
    public void remove(Long actorId, Long careRelationId) {
        careRelationService.remove(actorId, careRelationId);
    }

    // 연결마다 환자/가족의 이름·이메일을 붙인다. 사람 정보는 한 번에 모아서 조회한다 (연결마다 따로 조회하지 않게)
    private List<CareRelationResult> toResults(List<CareRelation> careRelations) {
        List<Long> userIds = new ArrayList<>();
        for (CareRelation careRelation : careRelations) {
            userIds.add(careRelation.getPatientId());
            userIds.add(careRelation.getCaregiverId());
        }
        Map<Long, User> usersById = userService.getUsersByIds(userIds);

        List<CareRelationResult> results = new ArrayList<>();
        for (CareRelation careRelation : careRelations) {
            results.add(new CareRelationResult(careRelation.getId(),
                    UserResult.from(usersById.get(careRelation.getPatientId())),
                    UserResult.from(usersById.get(careRelation.getCaregiverId())),
                    careRelation.getStatus(), careRelation.getCreatedAt(), careRelation.getAcceptedAt()));
        }
        return results;
    }
}
