package com.kkobak.server.infrastructure.care;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kkobak.server.domain.care.CareRelation;
import com.kkobak.server.domain.care.CareRelationStatus;

interface CareRelationJpaRepository extends JpaRepository<CareRelation, Long> {

    boolean existsByPatientIdAndCaregiverId(Long patientId, Long caregiverId);

    @Query("select r from CareRelation r where r.patientId = :userId or r.caregiverId = :userId "
            + "order by r.createdAt desc, r.id desc")
    List<CareRelation> findAllByUserId(@Param("userId") Long userId);

    @Query("select r.patientId from CareRelation r where r.caregiverId = :caregiverId and r.status = :status")
    List<Long> findPatientIdsByCaregiverIdAndStatus(@Param("caregiverId") Long caregiverId,
                                                    @Param("status") CareRelationStatus status);
}
