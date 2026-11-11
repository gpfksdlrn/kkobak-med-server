package com.kkobak.server.application.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kkobak.server.domain.care.AccessScope;
import com.kkobak.server.domain.medication.MealTiming;
import com.kkobak.server.domain.medication.Medication;
import com.kkobak.server.domain.medication.MedicationRepository;
import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

// DB 없이 Repository 를 가짜(mock)로 바꿔서 Service 의 판단 로직만 검사한다.
class MedicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");
    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private MedicationRepository medicationRepository;
    private MedicationService medicationService;

    @BeforeEach
    void setUp() {
        medicationRepository = mock(MedicationRepository.class);
        medicationService = new MedicationService(medicationRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // 본인 데이터만 다룰 수 있는 범위
    private AccessScope scopeOf(Long actorId) {
        return new AccessScope(actorId, new ArrayList<Long>());
    }

    // patientId 의 수락된 가족(caregiverId)의 범위
    private AccessScope caregiverScopeOf(Long caregiverId, Long patientId) {
        return new AccessScope(caregiverId, Arrays.asList(patientId));
    }

    private MedicationCommand createCommand(String name) {
        return new MedicationCommand(name, "1정", MealTiming.AFTER_MEAL, LocalDate.of(2026, 11, 1), null,
                Arrays.asList(LocalTime.of(8, 0), LocalTime.of(20, 0)));
    }

    @Test
    void 약을_등록하면_주인과_복용_시간과_등록_시각이_채워진다() {
        when(medicationRepository.save(any(Medication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Medication created = medicationService.create(scopeOf(OWNER_ID), null, createCommand("타이레놀"));

        assertThat(created.getUserId()).isEqualTo(OWNER_ID);
        assertThat(created.getSchedules()).hasSize(2);
        assertThat(created.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void 내_약은_조회할_수_있다() {
        Medication myMedication = new Medication(OWNER_ID, "타이레놀", null, MealTiming.NONE, LocalDate.of(2026, 11, 1), null, NOW);
        when(medicationRepository.findById(10L)).thenReturn(Optional.of(myMedication));

        assertThat(medicationService.getAccessibleMedication(scopeOf(OWNER_ID), 10L)).isSameAs(myMedication);
    }

    @Test
    void 남의_약을_조회하면_약을_찾을_수_없다는_예외가_발생한다() {
        Medication othersMedication = new Medication(OTHER_USER_ID, "남의약", null, MealTiming.NONE, LocalDate.of(2026, 11, 1), null, NOW);
        when(medicationRepository.findById(10L)).thenReturn(Optional.of(othersMedication));

        assertThatThrownBy(() -> medicationService.getAccessibleMedication(scopeOf(OWNER_ID), 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEDICATION_NOT_FOUND);
    }

    @Test
    void 없는_약을_조회하면_약을_찾을_수_없다는_예외가_발생한다() {
        when(medicationRepository.findById(99L)).thenReturn(Optional.<Medication>empty());

        assertThatThrownBy(() -> medicationService.getAccessibleMedication(scopeOf(OWNER_ID), 99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEDICATION_NOT_FOUND);
    }

    @Test
    void 남의_약은_삭제할_수_없다() {
        Medication othersMedication = new Medication(OTHER_USER_ID, "남의약", null, MealTiming.NONE, LocalDate.of(2026, 11, 1), null, NOW);
        when(medicationRepository.findById(10L)).thenReturn(Optional.of(othersMedication));

        assertThatThrownBy(() -> medicationService.delete(scopeOf(OWNER_ID), 10L)).isInstanceOf(BusinessException.class);
        verify(medicationRepository, never()).delete(any(Medication.class));
    }

    private Medication novemberMedicationOf(Long userId) {
        return new Medication(userId, "타이레놀", null, MealTiming.NONE,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), NOW);
    }

    // 잡는 버그: 복용 기간 안의 날짜인데도 체크를 막는 것
    @Test
    void 복용_기간_안의_날짜면_내_약을_돌려준다() {
        Medication myMedication = novemberMedicationOf(OWNER_ID);
        when(medicationRepository.findByScheduleId(5L)).thenReturn(Optional.of(myMedication));

        Medication found = medicationService.getAccessibleActiveMedicationByScheduleId(scopeOf(OWNER_ID), 5L, LocalDate.of(2026, 11, 30));

        assertThat(found).isSameAs(myMedication);
    }

    // 잡는 버그: 복용 기간이 끝난 날짜에 체크가 통과하는 것
    @Test
    void 복용_기간이_아닌_날짜면_DOSE_DATE_OUT_OF_PERIOD_예외가_발생한다() {
        when(medicationRepository.findByScheduleId(5L)).thenReturn(Optional.of(novemberMedicationOf(OWNER_ID)));

        assertThatThrownBy(() -> medicationService.getAccessibleActiveMedicationByScheduleId(scopeOf(OWNER_ID), 5L, LocalDate.of(2026, 12, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DOSE_DATE_OUT_OF_PERIOD);
    }

    // 잡는 버그: 기간 검사를 소유자 검사보다 먼저 해서, 남의 약의 복용 기간이 400/404 차이로 새는 것
    @Test
    void 남의_약이면_기간_밖_날짜라도_기간_오류가_아니라_SCHEDULE_NOT_FOUND가_먼저_난다() {
        when(medicationRepository.findByScheduleId(5L)).thenReturn(Optional.of(novemberMedicationOf(OTHER_USER_ID)));

        assertThatThrownBy(() -> medicationService.getAccessibleActiveMedicationByScheduleId(scopeOf(OWNER_ID), 5L, LocalDate.of(2026, 12, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
    }

    private static final Long CAREGIVER_ID = 9L;

    // 잡는 버그: 수락된 가족이 환자의 약을 볼 수 없는 것 (가족 기능이 동작 안 함)
    @Test
    void 수락된_가족은_돌보는_환자의_약을_조회할_수_있다() {
        Medication patientMedication = new Medication(OWNER_ID, "환자약", null, MealTiming.NONE, LocalDate.of(2026, 11, 1), null, NOW);
        when(medicationRepository.findById(10L)).thenReturn(Optional.of(patientMedication));

        assertThat(medicationService.getAccessibleMedication(caregiverScopeOf(CAREGIVER_ID, OWNER_ID), 10L))
                .isSameAs(patientMedication);
    }

    // 잡는 버그: 가족이 돌보는 환자가 아닌 제3자의 약까지 볼 수 있는 것
    @Test
    void 가족이라도_돌보지_않는_사람의_약은_찾을_수_없다() {
        Medication othersMedication = new Medication(OTHER_USER_ID, "남의약", null, MealTiming.NONE, LocalDate.of(2026, 11, 1), null, NOW);
        when(medicationRepository.findById(10L)).thenReturn(Optional.of(othersMedication));

        assertThatThrownBy(() -> medicationService.getAccessibleMedication(caregiverScopeOf(CAREGIVER_ID, OWNER_ID), 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEDICATION_NOT_FOUND);
    }

    // 잡는 버그: 가족이 환자 대신 등록한 약이 가족 본인의 약으로 저장되는 것
    @Test
    void 가족이_patientId로_등록하면_약의_주인은_환자다() {
        when(medicationRepository.save(any(Medication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Medication created = medicationService.create(caregiverScopeOf(CAREGIVER_ID, OWNER_ID), OWNER_ID, createCommand("환자약"));

        assertThat(created.getUserId()).isEqualTo(OWNER_ID);
    }

    // 잡는 버그: 연결되지 않은 사람의 id 를 patientId 로 넣어 그 사람 이름으로 약을 등록할 수 있는 것
    @Test
    void 돌보지_않는_사람의_patientId로는_등록할_수_없다() {
        assertThatThrownBy(() -> medicationService.create(scopeOf(CAREGIVER_ID), OWNER_ID, createCommand("남의약")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PATIENT_NOT_FOUND);
        verify(medicationRepository, never()).save(any(Medication.class));
    }
}
