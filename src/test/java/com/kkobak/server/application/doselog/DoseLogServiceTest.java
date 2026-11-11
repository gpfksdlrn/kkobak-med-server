package com.kkobak.server.application.doselog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.kkobak.server.domain.doselog.DoseLog;
import com.kkobak.server.domain.doselog.DoseLogRepository;

// "있으면 갱신, 없으면 생성"은 DB(upsert)가 판단한다 → DoseLogRepositoryAdapterTest 에서 실제 DB 로 검증.
// Service 의 책임은 "복용 시각을 주입된 Clock 으로 정하는 것"이라 그것만 본다.
class DoseLogServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T08:05:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);
    private static final Long CHECKER_ID = 9L;

    // 잡는 버그: 복용 시각을 Clock 이 아니라 서버 시계(Instant.now())나 엉뚱한 값으로 기록하는 것, 체크한 사람을 빠뜨리는 것
    @Test
    void 복용_체크는_주입된_Clock의_현재_시각과_체크한_사람으로_기록한다() {
        DoseLogRepository doseLogRepository = mock(DoseLogRepository.class);
        DoseLog upserted = new DoseLog(1L, TODAY, NOW);
        when(doseLogRepository.upsertTaken(1L, TODAY, NOW, CHECKER_ID)).thenReturn(upserted);
        DoseLogService doseLogService = new DoseLogService(doseLogRepository, Clock.fixed(NOW, ZoneOffset.UTC));

        DoseLog checked = doseLogService.check(1L, TODAY, CHECKER_ID);

        verify(doseLogRepository).upsertTaken(1L, TODAY, NOW, CHECKER_ID);
        assertThat(checked).isSameAs(upserted);
    }
}
