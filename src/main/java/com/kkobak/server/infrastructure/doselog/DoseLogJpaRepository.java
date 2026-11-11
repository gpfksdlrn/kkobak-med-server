package com.kkobak.server.infrastructure.doselog;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kkobak.server.domain.doselog.DoseLog;

interface DoseLogJpaRepository extends JpaRepository<DoseLog, Long> {

    Optional<DoseLog> findByScheduleIdAndScheduledDate(Long scheduleId, LocalDate scheduledDate);

    // 복용 기록 -> 복용 시간 -> 약 -> 주인(user) 순으로 거슬러 올라가 그 주인(userId)의 것만 고른다
    @Query("select d from DoseLog d where d.scheduledDate = :date and d.scheduleId in "
            + "(select s.id from Schedule s where s.medication.userId = :userId) order by d.id asc")
    List<DoseLog> findAllByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    // PostgreSQL 의 upsert. (schedule_id, scheduled_date) 가 이미 있으면 INSERT 대신 taken_at, checked_by 만 UPDATE 한다.
    // DB 가 한 문장 안에서 판단하므로 동시에 여러 요청이 와도 충돌(UNIQUE 위반)이 나지 않는다.
    // clearAutomatically: JPA 를 거치지 않고 DB 를 바꿨으니, 메모리에 남은 옛 값(캐시)을 비운다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = "INSERT INTO dose_logs (schedule_id, scheduled_date, taken_at, checked_by, created_at) "
            + "VALUES (:scheduleId, :scheduledDate, :takenAt, :checkedBy, :takenAt) "
            + "ON CONFLICT (schedule_id, scheduled_date) "
            + "DO UPDATE SET taken_at = EXCLUDED.taken_at, checked_by = EXCLUDED.checked_by")
    int upsertTaken(@Param("scheduleId") Long scheduleId, @Param("scheduledDate") LocalDate scheduledDate,
                    @Param("takenAt") Instant takenAt, @Param("checkedBy") Long checkedBy);
}
