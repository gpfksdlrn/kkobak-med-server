package com.kkobak.server.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.kkobak.server.TestcontainersConfiguration;
import com.kkobak.server.domain.medication.MealTiming;
import com.kkobak.server.domain.medication.Medication;
import com.kkobak.server.domain.medication.MedicationRepository;
import com.kkobak.server.domain.user.User;
import com.kkobak.server.domain.user.UserRepository;
import com.kkobak.server.infrastructure.medication.MedicationRepositoryAdapter;
import com.kkobak.server.infrastructure.user.UserRepositoryAdapter;

// 회귀(버그 노트 1번): hibernate.jdbc.time_zone 설정이 복용 시간(벽시계 시간)을 변환해 08:00 이 23:00 으로 저장되었다.
// 저장 -> 다시 읽기만 하면 저장/읽기 양쪽이 같은 변환을 해서 값이 멀쩡해 보인다.
// 그래서 Hibernate 를 거치지 않고 JDBC 로 DB 에 실제 저장된 값을 직접 읽는다.
// 서버 시간대가 UTC 인 환경에서는 변환이 티가 나지 않으므로, JVM 시간대를 서울로 고정해서 실행한다.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, MedicationRepositoryAdapter.class, UserRepositoryAdapter.class})
class TimeOfDayStorageTest {

    private static final Instant USER_CREATED_AT = Instant.parse("2026-11-01T00:00:00Z");

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

    private static TimeZone originalTimeZone;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void 시간대를_서울로_고정한다() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    @AfterAll
    static void 시간대를_원래대로_돌린다() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    void 복용_시간은_DB에_입력한_벽시계_시간_그대로_저장된다() {
        Long userId = userRepository.save(new User("tz@example.com", "시간대", "sub-tz", USER_CREATED_AT)).getId();
        Medication medication = new Medication(userId, "타이레놀", null, MealTiming.NONE,
                LocalDate.of(2026, 11, 1), null, NOW);
        medication.replaceScheduleTimes(Arrays.asList(LocalTime.of(8, 0), LocalTime.of(20, 30)), NOW);
        medicationRepository.save(medication);
        medicationRepository.flush();

        // 손으로 정한 기대값: 08:00, 20:30 이 그대로 있어야 한다 (23:00, 11:30 이 되면 안 된다)
        assertThat(jdbcTemplate.queryForList(
                "select cast(time_of_day as varchar) from schedules order by time_of_day", String.class))
                .containsExactly("08:00:00", "20:30:00");
    }

    @Test
    void 날짜와_복용_시작일도_DB에_입력한_날짜_그대로_저장된다() {
        Long userId = userRepository.save(new User("tz2@example.com", "시간대2", "sub-tz2", USER_CREATED_AT)).getId();
        Medication medication = new Medication(userId, "타이레놀", null, MealTiming.NONE,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), NOW);
        medication.replaceScheduleTimes(Arrays.asList(LocalTime.of(0, 30)), NOW);
        medicationRepository.save(medication);
        medicationRepository.flush();

        assertThat(jdbcTemplate.queryForObject("select cast(start_date as varchar) from medications", String.class))
                .isEqualTo("2026-11-01");
        assertThat(jdbcTemplate.queryForObject("select cast(end_date as varchar) from medications", String.class))
                .isEqualTo("2026-11-30");
    }
}
