package com.kkobak.server.domain.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

// Spring 도 DB 도 없는 순수 도메인 규칙 테스트
class MedicationTest {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

    private Medication createMedication() {
        return new Medication(1L, "타이레놀", "1정", MealTiming.AFTER_MEAL,
                LocalDate.of(2026, 11, 1), null, NOW);
    }

    private List<LocalTime> timesOf(Medication medication) {
        List<LocalTime> times = new ArrayList<>();
        for (Schedule schedule : medication.getSchedules()) {
            times.add(schedule.getTimeOfDay());
        }
        return times;
    }

    @Test
    void 종료일이_시작일보다_빠르면_약을_만들_수_없다() {
        assertThatThrownBy(() -> new Medication(1L, "약", null, MealTiming.NONE,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1), NOW))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_MEDICATION_PERIOD);
    }

    @Test
    void 종료일이_없는_약은_만들_수_있다() {
        Medication medication = createMedication();

        assertThat(medication.getEndDate()).isNull();
    }

    @Test
    void 복용_시간을_처음_지정하면_모두_추가된다() {
        Medication medication = createMedication();

        medication.replaceScheduleTimes(Arrays.asList(LocalTime.of(8, 0), LocalTime.of(20, 0)), NOW);

        assertThat(timesOf(medication)).containsExactlyInAnyOrder(LocalTime.of(8, 0), LocalTime.of(20, 0));
    }

    @Test
    void 복용_시간을_바꿔도_안_바뀐_시간의_스케줄_객체는_그대로_유지된다() {
        Medication medication = createMedication();
        medication.replaceScheduleTimes(Arrays.asList(LocalTime.of(8, 0), LocalTime.of(20, 0)), NOW);
        Schedule morningSchedule = medication.getSchedules().get(0);

        medication.replaceScheduleTimes(Arrays.asList(LocalTime.of(8, 0), LocalTime.of(12, 30)), NOW);

        assertThat(timesOf(medication)).containsExactlyInAnyOrder(LocalTime.of(8, 0), LocalTime.of(12, 30));
        // 같은 객체(=같은 DB 행)여야 그 스케줄의 복용 기록이 지워지지 않는다
        assertThat(medication.getSchedules()).contains(morningSchedule);
    }

    @Test
    void 중복된_시간과_초_단위_차이는_같은_시간으로_본다() {
        Medication medication = createMedication();

        medication.replaceScheduleTimes(
                Arrays.asList(LocalTime.of(8, 0), LocalTime.of(8, 0, 0), LocalTime.of(8, 0, 30)), NOW);

        assertThat(timesOf(medication)).containsExactly(LocalTime.of(8, 0));
    }

    @Test
    void 빈_목록으로_바꾸면_모든_복용_시간이_삭제된다() {
        Medication medication = createMedication();
        medication.replaceScheduleTimes(Arrays.asList(LocalTime.of(8, 0)), NOW);

        medication.replaceScheduleTimes(new ArrayList<LocalTime>(), NOW);

        assertThat(medication.getSchedules()).isEmpty();
    }

    @Test
    void 약_정보를_수정하면_값과_수정시각이_바뀐다() {
        Medication medication = createMedication();
        Instant later = Instant.parse("2026-11-02T00:00:00Z");

        medication.update("이부프로펜", "2정", MealTiming.BEFORE_MEAL,
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 30), later);

        assertThat(medication.getName()).isEqualTo("이부프로펜");
        assertThat(medication.getMealTiming()).isEqualTo(MealTiming.BEFORE_MEAL);
        assertThat(medication.getUpdatedAt()).isEqualTo(later);
        assertThat(medication.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void 복용_기간_안의_날짜면_복용_중이다() {
        Medication medication = new Medication(1L, "약", null, MealTiming.NONE,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), NOW);

        assertThat(medication.isActiveOn(LocalDate.of(2026, 11, 1))).isTrue();
        assertThat(medication.isActiveOn(LocalDate.of(2026, 11, 15))).isTrue();
        assertThat(medication.isActiveOn(LocalDate.of(2026, 11, 30))).isTrue();
    }

    @Test
    void 시작일_전이나_종료일_후는_복용_중이_아니다() {
        Medication medication = new Medication(1L, "약", null, MealTiming.NONE,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), NOW);

        assertThat(medication.isActiveOn(LocalDate.of(2026, 10, 31))).isFalse();
        assertThat(medication.isActiveOn(LocalDate.of(2026, 12, 1))).isFalse();
    }

    @Test
    void 종료일이_없으면_시작일_이후는_모두_복용_중이다() {
        Medication medication = createMedication();

        assertThat(medication.isActiveOn(LocalDate.of(2030, 1, 1))).isTrue();
        assertThat(medication.isActiveOn(LocalDate.of(2026, 10, 31))).isFalse();
    }

    // 경계값: 종료일이 시작일과 같은 하루짜리 약은 허용 (isBefore 이지 isBeforeOrEqual 이 아니다)
    @Test
    void 종료일이_시작일과_같은_하루짜리_약은_만들_수_있고_그날만_복용_중이다() {
        LocalDate day = LocalDate.of(2026, 11, 5);
        Medication medication = new Medication(1L, "하루약", null, MealTiming.NONE, day, day, NOW);

        assertThat(medication.isActiveOn(day.minusDays(1))).isFalse();
        assertThat(medication.isActiveOn(day)).isTrue();
        assertThat(medication.isActiveOn(day.plusDays(1))).isFalse();
    }

    @Test
    void 수정할_때_종료일이_시작일보다_빠르면_예외가_나고_기존_값은_바뀌지_않는다() {
        Medication medication = new Medication(1L, "타이레놀", "1정", MealTiming.AFTER_MEAL,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), NOW);

        assertThatThrownBy(() -> medication.update("바뀐이름", "2정", MealTiming.NONE,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1), NOW))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_MEDICATION_PERIOD);

        assertThat(medication.getName()).isEqualTo("타이레놀");
        assertThat(medication.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(medication.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 30));
    }
}
