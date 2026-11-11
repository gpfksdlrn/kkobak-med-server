package com.kkobak.server.application.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kkobak.server.domain.care.CareRelation;
import com.kkobak.server.domain.care.CareRelationRepository;
import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

class CareRelationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T00:00:00Z");
    private static final Long PATIENT_ID = 1L;
    private static final Long CAREGIVER_ID = 2L;
    private static final Long STRANGER_ID = 3L;

    private CareRelationRepository careRelationRepository;
    private CareRelationService careRelationService;

    @BeforeEach
    void setUp() {
        careRelationRepository = mock(CareRelationRepository.class);
        careRelationService = new CareRelationService(careRelationRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // 잡는 버그: 같은 가족을 두 번 초대해 중복 연결이 생기거나, DB 제약 에러(500/엉뚱한 409 문구)로 새는 것
    @Test
    void 이미_초대한_가족을_다시_초대하면_이미_있다는_예외가_발생한다() {
        when(careRelationRepository.existsByPatientIdAndCaregiverId(PATIENT_ID, CAREGIVER_ID)).thenReturn(true);

        assertThatThrownBy(() -> careRelationService.invite(PATIENT_ID, CAREGIVER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CARE_RELATION_ALREADY_EXISTS);
        verify(careRelationRepository, never()).save(any(CareRelation.class));
    }

    // 잡는 버그: 연결과 상관없는 제3자가 남의 연결을 지워 가족 연결을 끊는 것
    @Test
    void 당사자가_아니면_연결을_삭제할_수_없다() {
        CareRelation relation = new CareRelation(PATIENT_ID, CAREGIVER_ID, NOW);
        when(careRelationRepository.findById(10L)).thenReturn(Optional.of(relation));

        assertThatThrownBy(() -> careRelationService.remove(STRANGER_ID, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CARE_RELATION_NOT_FOUND);
        verify(careRelationRepository, never()).delete(any(CareRelation.class));
    }

    @Test
    void 환자와_가족은_둘_다_연결을_삭제할_수_있다() {
        CareRelation relation = new CareRelation(PATIENT_ID, CAREGIVER_ID, NOW);
        when(careRelationRepository.findById(10L)).thenReturn(Optional.of(relation));

        careRelationService.remove(PATIENT_ID, 10L);
        careRelationService.remove(CAREGIVER_ID, 10L);

        verify(careRelationRepository, org.mockito.Mockito.times(2)).delete(relation);
    }

    // 잡는 버그: 제3자가 남의 초대를 수락해서 가족 권한을 가로채는 것
    @Test
    void 제3자는_남의_초대를_수락할_수_없다() {
        CareRelation relation = new CareRelation(PATIENT_ID, CAREGIVER_ID, NOW);
        when(careRelationRepository.findById(10L)).thenReturn(Optional.of(relation));

        assertThatThrownBy(() -> careRelationService.accept(STRANGER_ID, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CARE_RELATION_NOT_FOUND);
        assertThat(relation.isAccepted()).isFalse();
    }
}
