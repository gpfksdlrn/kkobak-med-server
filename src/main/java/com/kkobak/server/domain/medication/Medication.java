package com.kkobak.server.domain.medication;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "medications")
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 다른 도메인(User)은 객체가 아니라 id 로만 가리킨다. 도메인끼리 서로 엉키지 않게 하려는 것.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "dosage_text")
    private String dosageText;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_timing", nullable = false)
    private MealTiming mealTiming;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "medication", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timeOfDay ASC")
    private List<Schedule> schedules = new ArrayList<>();

    public Medication(Long userId, String name, String dosageText, MealTiming mealTiming,
                      LocalDate startDate, LocalDate endDate, Instant now) {
        validatePeriod(startDate, endDate);
        this.userId = userId;
        this.name = name;
        this.dosageText = dosageText;
        this.mealTiming = mealTiming;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = now;
        this.updatedAt = now;
    }

    // 종료일이 시작일보다 빠르면 안 된다. 생성/수정 모두에서 쓰므로 public static 으로 둔다.
    public static void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_MEDICATION_PERIOD);
        }
    }

    public void update(String name, String dosageText, MealTiming mealTiming,
                       LocalDate startDate, LocalDate endDate, Instant now) {
        validatePeriod(startDate, endDate);
        this.name = name;
        this.dosageText = dosageText;
        this.mealTiming = mealTiming;
        this.startDate = startDate;
        this.endDate = endDate;
        this.updatedAt = now;
    }

    // 그 날짜가 복용 기간(시작일 ~ 종료일) 안인가? 종료일이 없으면 시작일 이후는 모두 복용 기간이다.
    public boolean isActiveOn(LocalDate date) {
        if (date.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    public List<Schedule> getSchedules() {
        return Collections.unmodifiableList(schedules);
    }

    /**
     * 복용 시간 목록을 새 목록으로 맞춘다. 전부 지우고 다시 만들지 않고, 달라진 시간만 반영한다.
     * 이유: 스케줄을 지우면 그 스케줄의 복용 기록(dose_logs)도 같이 지워지기 때문에,
     * 안 바뀐 시간의 복용 기록이 사라지면 안 된다.
     * "08:00" 과 "08:00:00" 은 같은 시간으로 본다. (분 단위로 맞춘다)
     */
    public void replaceScheduleTimes(List<LocalTime> times, Instant now) {
        List<LocalTime> wantedTimes = new ArrayList<>();
        for (LocalTime time : times) {
            LocalTime minuteTime = time.truncatedTo(ChronoUnit.MINUTES);
            if (!wantedTimes.contains(minuteTime)) {
                wantedTimes.add(minuteTime);
            }
        }

        // 1) 새 목록에 없는 시간은 삭제
        Iterator<Schedule> iterator = schedules.iterator();
        while (iterator.hasNext()) {
            Schedule schedule = iterator.next();
            if (!wantedTimes.contains(schedule.getTimeOfDay())) {
                iterator.remove();
            }
        }

        // 2) 아직 없는 시간만 추가
        for (LocalTime wantedTime : wantedTimes) {
            boolean isExisting = false;
            for (Schedule schedule : schedules) {
                if (schedule.getTimeOfDay().equals(wantedTime)) {
                    isExisting = true;
                }
            }
            if (!isExisting) {
                schedules.add(new Schedule(this, wantedTime, now));
            }
        }

        this.updatedAt = now;
    }
}
