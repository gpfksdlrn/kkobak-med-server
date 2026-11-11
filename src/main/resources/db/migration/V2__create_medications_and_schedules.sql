-- 약 (사용자가 복용 중인 약 한 종류)
CREATE TABLE medications (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    dosage_text  VARCHAR(100),                          -- 예: "1정", "5ml"
    meal_timing  VARCHAR(20)  NOT NULL DEFAULT 'NONE',  -- 식전/식후/공복/상관없음
    start_date   DATE         NOT NULL,
    end_date     DATE,                                  -- NULL 이면 종료일 없음
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_medications_meal_timing
        CHECK (meal_timing IN ('BEFORE_MEAL', 'AFTER_MEAL', 'EMPTY_STOMACH', 'NONE')),
    CONSTRAINT ck_medications_period
        CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_medications_user_id ON medications (user_id);

-- 복용 시간 (한 약에 여러 개 가능)
CREATE TABLE schedules (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    medication_id  BIGINT      NOT NULL REFERENCES medications (id) ON DELETE CASCADE,
    time_of_day    TIME        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_schedules_medication_time UNIQUE (medication_id, time_of_day)
);
