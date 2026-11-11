package com.kkobak.server.domain.medication;

import java.time.Instant;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 복용 시간 하나 (예: 08:00). 약(Medication)의 일부라서 Medication 을 통해서만 만든다.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "schedules")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medication_id")
    private Medication medication;

    @Column(name = "time_of_day", nullable = false)
    private LocalTime timeOfDay;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // 같은 패키지(Medication)에서만 만들 수 있게 package-private
    Schedule(Medication medication, LocalTime timeOfDay, Instant now) {
        this.medication = medication;
        this.timeOfDay = timeOfDay;
        this.createdAt = now;
    }
}
