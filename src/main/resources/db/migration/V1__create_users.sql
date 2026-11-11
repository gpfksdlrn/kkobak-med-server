-- 로그인한 사용자 (Google 계정 기준)
CREATE TABLE users (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    google_sub  VARCHAR(255) NOT NULL,   -- Google이 주는 사용자 고유 ID
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_users_email      UNIQUE (email),
    CONSTRAINT uk_users_google_sub UNIQUE (google_sub)
);
