package com.kkobak.server.infrastructure.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.kkobak.server.TestcontainersConfiguration;
import com.kkobak.server.domain.care.CareRelation;
import com.kkobak.server.domain.care.CareRelationRepository;
import com.kkobak.server.domain.user.User;
import com.kkobak.server.domain.user.UserRepository;
import com.kkobak.server.infrastructure.user.UserRepositoryAdapter;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, CareRelationRepositoryAdapter.class, UserRepositoryAdapter.class})
class CareRelationRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-11-02T00:00:00Z");

    @Autowired
    private CareRelationRepository careRelationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long saveUser(String key) {
        return userRepository.save(new User(key + "@example.com", key, "sub-" + key, NOW)).getId();
    }

    // 잡는 버그: 수락 전(PENDING) 초대만 있어도 환자 데이터 접근 권한이 생기는 것,
    //           방향을 거꾸로 봐서 "내가 환자인 연결"의 상대를 내가 돌보는 환자로 착각하는 것
    @Test
    void 내가_가족으로_수락된_환자만_돌보는_환자로_조회된다() {
        Long me = saveUser("me");
        Long acceptedPatient = saveUser("accepted-patient");
        Long pendingPatient = saveUser("pending-patient");
        Long myCaregiver = saveUser("my-caregiver");

        CareRelation accepted = careRelationRepository.save(new CareRelation(acceptedPatient, me, NOW));
        accepted.accept(me, NOW);
        careRelationRepository.save(new CareRelation(pendingPatient, me, NOW));
        CareRelation iAmPatient = careRelationRepository.save(new CareRelation(me, myCaregiver, NOW));
        iAmPatient.accept(myCaregiver, NOW);
        entityManager.flush();
        entityManager.clear();

        List<Long> myPatients = careRelationRepository.findAcceptedPatientIdsByCaregiverId(me);

        assertThat(myPatients).containsExactly(acceptedPatient);
    }

    @Test
    void 내_연결_목록에는_내가_환자인_연결과_가족인_연결이_모두_나온다() {
        Long me = saveUser("list-me");
        Long patient = saveUser("list-patient");
        Long caregiver = saveUser("list-caregiver");
        Long stranger = saveUser("list-stranger");
        careRelationRepository.save(new CareRelation(patient, me, NOW));
        careRelationRepository.save(new CareRelation(me, caregiver, NOW.plusSeconds(1)));
        careRelationRepository.save(new CareRelation(stranger, caregiver, NOW));
        entityManager.flush();
        entityManager.clear();

        List<CareRelation> relations = careRelationRepository.findAllByUserId(me);

        assertThat(relations).hasSize(2);
        assertThat(relations.get(0).getCaregiverId()).as("최근 순").isEqualTo(caregiver);
    }

    // 잡는 버그: 같은 환자-가족 쌍이 두 번 저장되는 것 (코드 검사를 우회해도 DB 가 막는지)
    @Test
    void 같은_환자와_가족_쌍은_DB가_두_번_저장하지_못하게_막는다() {
        Long patient = saveUser("dup-patient");
        Long caregiver = saveUser("dup-caregiver");
        careRelationRepository.save(new CareRelation(patient, caregiver, NOW));
        entityManager.flush();

        assertThatThrownBy(() -> {
            careRelationRepository.save(new CareRelation(patient, caregiver, NOW));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    // 잡는 버그: 엔티티 검사를 거치지 않는 경로(직접 SQL 등)로 자기 자신과의 연결이 저장되는 것
    @Test
    void 자기_자신과의_연결은_DB가_막는다() {
        Long user = saveUser("self");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into care_relations (patient_id, caregiver_id, status) values (?, ?, 'PENDING')", user, user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
