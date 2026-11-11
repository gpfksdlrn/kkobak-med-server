-- 가족 연결: 환자(patient)가 가족(caregiver)을 초대하고, 가족이 수락하면 ACCEPTED.
-- ACCEPTED 인 가족은 환자의 약/복용 기록을 조회하고 수정할 수 있다.
CREATE TABLE care_relations (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    caregiver_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at   TIMESTAMPTZ,

    CONSTRAINT uk_care_relations_patient_caregiver UNIQUE (patient_id, caregiver_id),
    CONSTRAINT ck_care_relations_status CHECK (status IN ('PENDING', 'ACCEPTED')),
    CONSTRAINT ck_care_relations_not_self CHECK (patient_id <> caregiver_id)
);

CREATE INDEX idx_care_relations_caregiver_id ON care_relations (caregiver_id);

-- 복용 체크를 누가 했는지 (본인 또는 가족). 이전 기록은 비어 있다.
-- 체크한 사람이 탈퇴해도 복용 기록은 남기고 체크한 사람만 비운다.
ALTER TABLE dose_logs ADD COLUMN checked_by BIGINT REFERENCES users (id) ON DELETE SET NULL;
