package com.kkobak.server.infrastructure.medication;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.kkobak.server.TestcontainersConfiguration;
import com.kkobak.server.domain.doselog.DoseLog;
import com.kkobak.server.domain.doselog.DoseLogRepository;
import com.kkobak.server.infrastructure.doselog.DoseLogRepositoryAdapter;
import com.kkobak.server.domain.medication.MealTiming;
import com.kkobak.server.domain.medication.Medication;
import com.kkobak.server.domain.medication.MedicationRepository;
import com.kkobak.server.domain.user.User;
import com.kkobak.server.domain.user.UserRepository;
import com.kkobak.server.infrastructure.user.UserRepositoryAdapter;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, MedicationRepositoryAdapter.class,
        DoseLogRepositoryAdapter.class, UserRepositoryAdapter.class})
class MedicationRepositoryAdapterTest {

    private static final Instant USER_CREATED_AT = Instant.parse("2026-11-01T00:00:00Z");

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private DoseLogRepository doseLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private Long userId;

    @BeforeEach
    void 사용자를_만든다() {
        userId = userRepository.save(new User("owner@example.com", "주인", "sub-owner", USER_CREATED_AT)).getId();
    }

    // DB 에 보내고(flush) 영속성 컨텍스트 캐시를 비워서(clear), 다음 조회가 진짜 DB 에서 읽어오게 한다
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Medication saveMedication(String name, LocalDate startDate, LocalDate endDate, LocalTime... times) {
        Medication medication = new Medication(userId, name, "1정", MealTiming.NONE, startDate, endDate, NOW);
        medication.replaceScheduleTimes(Arrays.asList(times), NOW);
        return medicationRepository.save(medication);
    }

    @Test
    void 약을_저장하면_복용_시간도_함께_저장된다() {
        Medication saved = saveMedication("타이레놀", TODAY, null, LocalTime.of(8, 0), LocalTime.of(20, 0));
        flushAndClear();

        Medication found = medicationRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getSchedules()).hasSize(2);
        assertThat(found.getSchedules().get(0).getTimeOfDay()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void 해당_날짜에_복용_중인_약만_조회된다() {
        saveMedication("복용중", TODAY.minusDays(3), TODAY.plusDays(3), LocalTime.of(8, 0));
        saveMedication("종료일없음", TODAY.minusDays(3), null, LocalTime.of(9, 0));
        saveMedication("이미끝남", TODAY.minusDays(10), TODAY.minusDays(1), LocalTime.of(10, 0));
        saveMedication("아직시작전", TODAY.plusDays(1), null, LocalTime.of(11, 0));
        flushAndClear();

        List<Medication> activeMedications = medicationRepository.findActiveByUserIdAndDate(userId, TODAY);

        assertThat(activeMedications).extracting(Medication::getName)
                .containsExactlyInAnyOrder("복용중", "종료일없음");
        assertThat(activeMedications.get(0).getSchedules()).isNotEmpty();
    }

    @Test
    void 다른_사용자의_약은_조회되지_않는다() {
        Long otherUserId = userRepository.save(new User("other@example.com", "남", "sub-other", USER_CREATED_AT)).getId();
        Medication othersMedication = new Medication(otherUserId, "남의약", null, MealTiming.NONE, TODAY, null, NOW);
        medicationRepository.save(othersMedication);
        saveMedication("내약", TODAY, null, LocalTime.of(8, 0));
        flushAndClear();

        assertThat(medicationRepository.findAllByUserId(userId)).extracting(Medication::getName)
                .containsExactly("내약");
    }

    @Test
    void 약을_삭제하면_복용_시간과_복용_기록도_함께_삭제된다() {
        Medication saved = saveMedication("타이레놀", TODAY, null, LocalTime.of(8, 0));
        Long scheduleId = saved.getSchedules().get(0).getId();
        doseLogRepository.save(new DoseLog(scheduleId, TODAY, NOW));
        flushAndClear();

        medicationRepository.delete(medicationRepository.findById(saved.getId()).orElseThrow());
        flushAndClear();

        assertThat(medicationRepository.findById(saved.getId())).isEmpty();
        assertThat(doseLogRepository.findByScheduleIdAndScheduledDate(scheduleId, TODAY)).isEmpty();
    }

    @Test
    void 복용_시간을_수정해도_안_바뀐_시간의_복용_기록은_남는다() {
        Medication saved = saveMedication("타이레놀", TODAY, null, LocalTime.of(8, 0), LocalTime.of(20, 0));
        Long morningScheduleId = saved.getSchedules().get(0).getId();
        Long eveningScheduleId = saved.getSchedules().get(1).getId();
        doseLogRepository.save(new DoseLog(morningScheduleId, TODAY, NOW));
        doseLogRepository.save(new DoseLog(eveningScheduleId, TODAY, NOW));
        flushAndClear();

        // 20:00 을 빼고 12:30 을 추가한다. 08:00 은 그대로.
        Medication found = medicationRepository.findById(saved.getId()).orElseThrow();
        found.replaceScheduleTimes(Arrays.asList(LocalTime.of(8, 0), LocalTime.of(12, 30)), NOW);
        medicationRepository.save(found);
        flushAndClear();

        Medication reloaded = medicationRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getSchedules()).extracting(schedule -> schedule.getTimeOfDay())
                .containsExactly(LocalTime.of(8, 0), LocalTime.of(12, 30));
        assertThat(doseLogRepository.findByScheduleIdAndScheduledDate(morningScheduleId, TODAY)).isPresent();
        assertThat(doseLogRepository.findByScheduleIdAndScheduledDate(eveningScheduleId, TODAY)).isEmpty();
    }

    @Test
    void 복용_시간_id로_그_시간이_속한_약을_찾을_수_있다() {
        Medication saved = saveMedication("타이레놀", TODAY, null, LocalTime.of(8, 0), LocalTime.of(20, 0));
        Long eveningScheduleId = saved.getSchedules().get(1).getId();
        flushAndClear();

        Medication found = medicationRepository.findByScheduleId(eveningScheduleId).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(medicationRepository.findByScheduleId(-1L)).isEmpty();
    }

    // 날짜별 조회 쿼리에도 사용자 조건이 있어야 한다. 목록 조회 테스트(다른_사용자의_약은_조회되지_않는다)와는 다른 쿼리다.
    @Test
    void 다른_사용자의_약은_날짜별_복용_중_조회에서도_제외된다() {
        Long otherUserId = userRepository.save(new User("other-active@example.com", "남", "sub-other-active", USER_CREATED_AT)).getId();
        Medication othersMedication = new Medication(otherUserId, "남의약", null, MealTiming.NONE, TODAY, null, NOW);
        othersMedication.replaceScheduleTimes(Arrays.asList(LocalTime.of(9, 0)), NOW);
        medicationRepository.save(othersMedication);
        saveMedication("내약", TODAY, null, LocalTime.of(8, 0));
        flushAndClear();

        assertThat(medicationRepository.findActiveByUserIdAndDate(userId, TODAY)).extracting(Medication::getName)
                .containsExactly("내약");
    }

    // 경계값: 종료일 당일까지 복용 중이고 시작일 당일부터 복용 중이다 (쿼리의 <=, >= 방향 검증)
    @Test
    void 날짜별_복용_중_조회는_시작일_당일과_종료일_당일을_포함하고_전날과_다음날은_제외한다() {
        saveMedication("11월1일부터11월10일", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10), LocalTime.of(8, 0));
        flushAndClear();

        assertThat(medicationRepository.findActiveByUserIdAndDate(userId, LocalDate.of(2026, 10, 31))).isEmpty();
        assertThat(medicationRepository.findActiveByUserIdAndDate(userId, LocalDate.of(2026, 11, 1))).hasSize(1);
        assertThat(medicationRepository.findActiveByUserIdAndDate(userId, LocalDate.of(2026, 11, 10))).hasSize(1);
        assertThat(medicationRepository.findActiveByUserIdAndDate(userId, LocalDate.of(2026, 11, 11))).isEmpty();
    }

    // 약 목록은 복용 시간이 여러 개여도 약 한 건으로 한 번만 나와야 한다 (join fetch 중복 제거)
    @Test
    void 복용_시간이_여러_개인_약도_목록에는_한_번만_나온다() {
        saveMedication("하루세번", TODAY, null, LocalTime.of(8, 0), LocalTime.of(13, 0), LocalTime.of(20, 0));
        flushAndClear();

        assertThat(medicationRepository.findAllByUserId(userId)).hasSize(1);
        assertThat(medicationRepository.findActiveByUserIdAndDate(userId, TODAY)).hasSize(1);
    }

    // 잡는 버그: 날짜별 복용 중 목록에 정렬이 없어 새로고침마다 순서가 바뀌는 것.
    // 저장 순서(id 순)와 등록 시각 순서를 일부러 다르게 해서, "우연히 맞는 순서"로는 통과하지 못하게 한다.
    @Test
    void 날짜별_복용_중_약은_최근_등록순으로_정렬된다() {
        Instant nine = Instant.parse("2026-11-01T09:00:00Z");
        Instant ten = Instant.parse("2026-11-01T10:00:00Z");
        Instant eleven = Instant.parse("2026-11-01T11:00:00Z");
        saveMedicationCreatedAt("10시등록", ten);
        saveMedicationCreatedAt("9시등록", nine);
        saveMedicationCreatedAt("11시등록", eleven);
        flushAndClear();

        List<Medication> activeMedications = medicationRepository.findActiveByUserIdAndDate(userId, TODAY);

        assertThat(activeMedications).extracting(Medication::getName)
                .containsExactly("11시등록", "10시등록", "9시등록");
    }

    private void saveMedicationCreatedAt(String name, Instant createdAt) {
        Medication medication = new Medication(userId, name, null, MealTiming.NONE, TODAY, null, createdAt);
        medication.replaceScheduleTimes(Arrays.asList(LocalTime.of(8, 0)), createdAt);
        medicationRepository.save(medication);
    }
}
