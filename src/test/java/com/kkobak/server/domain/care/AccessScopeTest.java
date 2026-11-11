package com.kkobak.server.domain.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.kkobak.server.global.exception.BusinessException;
import com.kkobak.server.global.exception.ErrorCode;

class AccessScopeTest {

    private static final Long ME = 2L;
    private static final Long MY_PATIENT = 1L;
    private static final Long STRANGER = 3L;

    private final AccessScope caregiverScope = new AccessScope(ME, Arrays.asList(MY_PATIENT));

    // 잡는 버그: 권한 판단에서 본인이나 돌보는 환자가 빠지거나, 제3자가 포함되는 것
    @Test
    void 본인과_돌보는_환자의_데이터만_다룰_수_있다() {
        assertThat(caregiverScope.canAccess(ME)).isTrue();
        assertThat(caregiverScope.canAccess(MY_PATIENT)).isTrue();
        assertThat(caregiverScope.canAccess(STRANGER)).isFalse();
    }

    // 잡는 버그: patientId 를 안 넣었을 때 내 데이터가 아니라 엉뚱한 사람으로 처리되는 것 (기존 API 가 깨짐)
    @Test
    void patientId가_없거나_나_자신이면_대상은_나다() {
        assertThat(caregiverScope.resolveTargetUserId(null)).isEqualTo(ME);
        assertThat(caregiverScope.resolveTargetUserId(ME)).isEqualTo(ME);
    }

    @Test
    void 돌보는_환자의_patientId면_대상은_그_환자다() {
        assertThat(caregiverScope.resolveTargetUserId(MY_PATIENT)).isEqualTo(MY_PATIENT);
    }

    // 잡는 버그: 아무 사용자 id 나 patientId 로 넣으면 그 사람의 데이터를 볼 수 있는 것
    @Test
    void 돌보지_않는_사람의_patientId면_연결된_환자를_찾을_수_없다() {
        assertThatThrownBy(() -> caregiverScope.resolveTargetUserId(STRANGER))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PATIENT_NOT_FOUND);
    }

    @Test
    void 돌보는_환자가_없으면_본인_데이터만_다룰_수_있다() {
        AccessScope ownerOnly = new AccessScope(ME, new ArrayList<Long>());

        assertThat(ownerOnly.canAccess(ME)).isTrue();
        assertThat(ownerOnly.canAccess(MY_PATIENT)).isFalse();
    }
}
