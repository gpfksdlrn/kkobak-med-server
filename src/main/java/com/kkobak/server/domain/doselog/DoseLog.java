package com.kkobak.server.domain.doselog;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 한 복용 시간(schedule)의 하루치 복용 기록. 상태(완료/놓침)는 저장하지 않고 takenAt 으로 계산한다.
// 복용 체크(기록 생성/갱신)는 동시 요청 충돌을 막기 위해 DB upsert(DoseLogRepository.upsertTaken)로 한다.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "dose_logs")
public class DoseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "taken_at")
    private Instant takenAt;

    // 복용 체크를 한 사람 (본인 또는 가족). 가족 기능 이전에 만든 기록이면 비어 있다.
    // 회원 탈퇴 기능은 아직 없다. 생겨서 체크한 사람이 지워지면 이것도 비게 된다 (DB 가 ON DELETE SET NULL).
    @Column(name = "checked_by")
    private Long checkedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DoseLog(Long scheduleId, LocalDate scheduledDate, Instant now) {
        this.scheduleId = scheduleId;
        this.scheduledDate = scheduledDate;
        this.createdAt = now;
    }

    public boolean isTaken() {
        return takenAt != null;
    }
}
