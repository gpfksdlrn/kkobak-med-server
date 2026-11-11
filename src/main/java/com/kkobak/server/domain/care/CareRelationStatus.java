package com.kkobak.server.domain.care;

public enum CareRelationStatus {
    PENDING,    // 초대만 한 상태 (가족이 아직 수락 안 함) — 아무 권한 없음
    ACCEPTED    // 가족이 수락함 — 환자의 약/복용 기록을 조회·수정할 수 있음
}
