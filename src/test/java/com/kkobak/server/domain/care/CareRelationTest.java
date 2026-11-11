package com.kkobak.server.domain.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

class CareRelationTest {

    private static final Instant NOW = Instant.parse("2026-11-02T00:00:00Z");
    private static final Long PATIENT_ID = 1L;
    private static final Long CAREGIVER_ID = 2L;

    // 잡는 버그: 자기 자신을 가족으로 초대할 수 있는 것
    @Test
    void 자기_자신은_초대할_수_없다() {
        assertThatThrownBy(() -> new CareRelation(PATIENT_ID, PATIENT_ID, NOW))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_INVITE_SELF);
    }

    // 잡는 버그: 초대만 했는데 바로 권한이 생기는 것
    @Test
    void 초대만_한_연결은_수락_전_상태다() {
        CareRelation relation = new CareRelation(PATIENT_ID, CAREGIVER_ID, NOW);

        assertThat(relation.getStatus()).isEqualTo(CareRelationStatus.PENDING);
        assertThat(relation.isAccepted()).isFalse();
        assertThat(relation.getAcceptedAt()).isNull();
    }

    @Test
    void 초대받은_가족이_수락하면_연결되고_수락_시각이_남는다() {
        CareRelation relation = new CareRelation(PATIENT_ID, CAREGIVER_ID, NOW);
        Instant acceptedAt = NOW.plusSeconds(300);

        relation.accept(CAREGIVER_ID, acceptedAt);

        assertThat(relation.isAccepted()).isTrue();
        assertThat(relation.getAcceptedAt()).isEqualTo(acceptedAt);
    }

    // 잡는 버그: 환자가 스스로 "수락"해서 상대 동의 없이 가족 연결을 만드는 것
    @Test
    void 환자_본인은_수락할_수_없다() {
        CareRelation relation = new CareRelation(PATIENT_ID, CAREGIVER_ID, NOW);

        assertThatThrownBy(() -> relation.accept(PATIENT_ID, NOW))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CARE_RELATION_NOT_FOUND);
        assertThat(relation.isAccepted()).isFalse();
    }

    // 잡는 버그: 이미 수락된 연결을 다시 수락할 때 처음 수락 시각이 덮어써지는 것
    @Test
    void 다시_수락해도_처음_수락_시각이_유지된다() {
        CareRelation relation = new CareRelation(PATIENT_ID, CAREGIVER_ID, NOW);
        Instant firstAcceptedAt = NOW.plusSeconds(10);
        relation.accept(CAREGIVER_ID, firstAcceptedAt);

        relation.accept(CAREGIVER_ID, NOW.plusSeconds(999));

        assertThat(relation.getAcceptedAt()).isEqualTo(firstAcceptedAt);
    }

    @Test
    void 당사자는_환자와_가족뿐이다() {
        CareRelation relation = new CareRelation(PATIENT_ID, CAREGIVER_ID, NOW);

        assertThat(relation.involves(PATIENT_ID)).isTrue();
        assertThat(relation.involves(CAREGIVER_ID)).isTrue();
        assertThat(relation.involves(3L)).isFalse();
    }
}
