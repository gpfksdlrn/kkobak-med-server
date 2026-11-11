package com.kkobak.server.infrastructure.medication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kkobak.server.domain.medication.Medication;

interface MedicationJpaRepository extends JpaRepository<Medication, Long> {

    // 목록을 그릴 때 약마다 복용 시간을 따로 조회하지 않도록(N+1) 한 번에 가져온다
    @Query("select distinct m from Medication m left join fetch m.schedules "
            + "where m.userId = :userId order by m.createdAt desc, m.id desc")
    List<Medication> findAllWithSchedulesByUserId(@Param("userId") Long userId);

    // left join fetch: 약을 가져올 때 복용 시간까지 한 번의 쿼리로 같이 가져온다 (N+1 방지)
    @Query("select distinct m from Medication m left join fetch m.schedules "
            + "where m.userId = :userId and m.startDate <= :date "
            + "and (m.endDate is null or m.endDate >= :date) "
            + "order by m.createdAt desc, m.id desc")
    List<Medication> findActive(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("select m from Medication m join m.schedules s where s.id = :scheduleId")
    Optional<Medication> findByScheduleId(@Param("scheduleId") Long scheduleId);
}
