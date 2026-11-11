-- 리프레시 토큰. 원문은 저장하지 않고 SHA-256 해시만 저장한다 (DB 가 유출돼도 토큰을 쓸 수 없게).
CREATE TABLE refresh_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,                -- 폐기(로그아웃, 재발급으로 교체) 시각. NULL 이면 아직 유효
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
