package com.kkobak.server.infrastructure.doselog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.kkobak.server.TestcontainersConfiguration;
import com.kkobak.server.domain.doselog.DoseLog;
import com.kkobak.server.domain.doselog.DoseLogRepository;
import com.kkobak.server.domain.medication.MealTiming;
import com.kkobak.server.domain.medication.Medication;
import com.kkobak.server.domain.medication.MedicationRepository;
import com.kkobak.server.infrastructure.medication.MedicationRepositoryAdapter;
import com.kkobak.server.domain.user.User;
import com.kkobak.server.domain.user.UserRepository;
import com.kkobak.server.infrastructure.user.UserRepositoryAdapter;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, DoseLogRepositoryAdapter.class,
        MedicationRepositoryAdapter.class, UserRepositoryAdapter.class})
class DoseLogRepositoryAdapterTest {

    private static final Instant USER_CREATED_AT = Instant.parse("2026-11-01T00:00:00Z");

    private static final Instant NOW = Instant.parse("2026-11-01T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    @Autowired
    private DoseLogRepository doseLogRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private Long saveScheduleOf(String email, String sub) {
        Long userId = userRepository.save(new User(email, "사용자", sub, USER_CREATED_AT)).getId();
        Medication medication = new Medication(userId, "약", null, MealTiming.NONE, TODAY, null, NOW);
        medication.replaceScheduleTimes(Arrays.asList(LocalTime.of(8, 0)), NOW);
        return medicationRepository.save(medication).getSchedules().get(0).getId();
    }

    @Test
    void 같은_복용_시간의_같은_날짜는_두_번_기록할_수_없다() {
        Long scheduleId = saveScheduleOf("a@example.com", "sub-a");
        doseLogRepository.save(new DoseLog(scheduleId, TODAY, NOW));

        assertThatThrownBy(() -> doseLogRepository.save(new DoseLog(scheduleId, TODAY, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // 잡는 버그: upsert SQL 이 새 기록을 만들지 못하거나, 복용 시각을 저장하지 않는 것
    @Test
    void 기록이_없으면_upsert가_새로_만들고_복용_시각을_저장한다() {
        Long scheduleId = saveScheduleOf("b@example.com", "sub-b");

        DoseLog checked = doseLogRepository.upsertTaken(scheduleId, TODAY, NOW, null);

        assertThat(checked.getId()).isNotNull();
        assertThat(checked.getTakenAt()).isEqualTo(NOW);
        assertThat(checked.getCreatedAt()).isEqualTo(NOW);
        assertThat(countRows(scheduleId)).isEqualTo(1);
    }

    // 잡는 버그: 두 번째 체크가 새 행을 만들거나(중복), 처음 만든 시각(created_at)까지 덮어쓰는 것
    @Test
    void 기록이_이미_있으면_upsert가_복용_시각만_바꾸고_행은_하나로_유지된다() {
        Long scheduleId = saveScheduleOf("c@example.com", "sub-c");
        Instant later = Instant.parse("2026-11-01T09:30:00Z");
        DoseLog first = doseLogRepository.upsertTaken(scheduleId, TODAY, NOW, null);

        DoseLog second = doseLogRepository.upsertTaken(scheduleId, TODAY, later, null);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getTakenAt()).isEqualTo(later);
        assertThat(second.getCreatedAt()).isEqualTo(NOW);
        assertThat(countRows(scheduleId)).isEqualTo(1);
    }

    // 잡는 버그: 체크 안 된 채(taken_at 없음) 미리 있던 기록을 upsert 가 못 채우는 것
    @Test
    void 복용_시각이_비어있는_기록도_upsert로_채워진다() {
        Long scheduleId = saveScheduleOf("d@example.com", "sub-d");
        doseLogRepository.save(new DoseLog(scheduleId, TODAY, NOW));
        entityManager.flush();

        DoseLog checked = doseLogRepository.upsertTaken(scheduleId, TODAY, NOW, null);

        assertThat(checked.isTaken()).isTrue();
        assertThat(countRows(scheduleId)).isEqualTo(1);
    }

    private long countRows(Long scheduleId) {
        Number count = (Number) entityManager
                .createNativeQuery("select count(*) from dose_logs where schedule_id = :scheduleId")
                .setParameter("scheduleId", scheduleId)
                .getSingleResult();
        return count.longValue();
    }

    @Test
    void 사용자와_날짜로_내_복용_기록만_조회된다() {
        Long myScheduleId = saveScheduleOf("me@example.com", "sub-me");
        Long otherScheduleId = saveScheduleOf("you@example.com", "sub-you");
        doseLogRepository.save(new DoseLog(myScheduleId, TODAY, NOW));
        doseLogRepository.save(new DoseLog(myScheduleId, TODAY.minusDays(1), NOW));
        doseLogRepository.save(new DoseLog(otherScheduleId, TODAY, NOW));
        Long myUserId = userRepository.findByEmail("me@example.com").orElseThrow().getId();

        List<DoseLog> myLogs = doseLogRepository.findAllByUserIdAndDate(myUserId, TODAY);

        assertThat(myLogs).hasSize(1);
        assertThat(myLogs.get(0).getScheduleId()).isEqualTo(myScheduleId);
    }

    // 위의 "저장하고 다시 조회" 테스트는 영속성 컨텍스트 캐시에서 읽을 수 있다. 여기서는 캐시를 비워서 DB 에서 읽는다.
    @Test
    void 영속성_컨텍스트를_비우고_DB에서_다시_읽어도_복용_시각과_날짜가_그대로다() {
        Long scheduleId = saveScheduleOf("round@example.com", "sub-round");
        Instant takenAt = Instant.parse("2026-11-01T14:08:11.353Z");
        doseLogRepository.upsertTaken(scheduleId, TODAY, takenAt, null);
        entityManager.flush();
        entityManager.clear();

        DoseLog found = doseLogRepository.findByScheduleIdAndScheduledDate(scheduleId, TODAY).orElseThrow();

        assertThat(found.getTakenAt()).isEqualTo(takenAt);
        assertThat(found.getScheduledDate()).isEqualTo(TODAY);
    }

    // 같은 복용 시간이라도 날짜가 다르면 별개 기록이다 (UNIQUE 는 (복용 시간, 날짜) 쌍)
    @Test
    void 같은_복용_시간이라도_날짜가_다르면_각각_기록할_수_있다() {
        Long scheduleId = saveScheduleOf("days@example.com", "sub-days");

        doseLogRepository.save(new DoseLog(scheduleId, TODAY, NOW));
        doseLogRepository.save(new DoseLog(scheduleId, TODAY.plusDays(1), NOW));
        entityManager.flush();

        assertThat(doseLogRepository.findByScheduleIdAndScheduledDate(scheduleId, TODAY)).isPresent();
        assertThat(doseLogRepository.findByScheduleIdAndScheduledDate(scheduleId, TODAY.plusDays(1))).isPresent();
    }

    // 잡는 버그: 체크한 사람이 저장되지 않거나, 다른 사람(가족)이 다시 체크했을 때 갱신되지 않는 것
    @Test
    void 체크한_사람이_저장되고_다른_사람이_다시_체크하면_그_사람으로_바뀐다() {
        Long scheduleId = saveScheduleOf("checker-patient@example.com", "sub-checker-patient");
        Long patientId = userRepository.findByEmail("checker-patient@example.com").orElseThrow().getId();
        Long caregiverId = userRepository.save(new User("checker-family@example.com", "가족", "sub-checker-family", USER_CREATED_AT)).getId();

        DoseLog byPatient = doseLogRepository.upsertTaken(scheduleId, TODAY, NOW, patientId);
        DoseLog byCaregiver = doseLogRepository.upsertTaken(scheduleId, TODAY, NOW.plusSeconds(60), caregiverId);

        assertThat(byPatient.getCheckedBy()).isEqualTo(patientId);
        assertThat(byCaregiver.getCheckedBy()).isEqualTo(caregiverId);
        assertThat(byCaregiver.getId()).isEqualTo(byPatient.getId());
    }
}
