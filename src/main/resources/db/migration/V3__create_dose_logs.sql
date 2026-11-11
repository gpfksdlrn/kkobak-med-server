-- 복용 기록. 상태(예정/완료/놓침)는 저장하지 않고 taken_at 으로 계산한다.
-- 행이 없거나 taken_at 이 NULL 이면 아직 안 먹은 것.
CREATE TABLE dose_logs (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    schedule_id     BIGINT      NOT NULL REFERENCES schedules (id) ON DELETE CASCADE,
    scheduled_date  DATE        NOT NULL,
    taken_at        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- 같은 시간표를 같은 날 두 번 기록할 수 없다 (복용 체크는 이 키로 upsert)
    CONSTRAINT uk_dose_logs_schedule_date UNIQUE (schedule_id, scheduled_date)
);

CREATE INDEX idx_dose_logs_scheduled_date ON dose_logs (scheduled_date);
