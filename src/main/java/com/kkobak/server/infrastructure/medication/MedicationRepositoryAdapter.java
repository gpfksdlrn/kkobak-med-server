package com.kkobak.server.infrastructure.medication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.kkobak.server.domain.medication.Medication;
import com.kkobak.server.domain.medication.MedicationRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicationRepositoryAdapter implements MedicationRepository {

    private final MedicationJpaRepository medicationJpaRepository;

    @Override
    public Medication save(Medication medication) {
        return medicationJpaRepository.save(medication);
    }

    @Override
    public Optional<Medication> findById(Long id) {
        return medicationJpaRepository.findById(id);
    }

    @Override
    public List<Medication> findAllByUserId(Long userId) {
        return medicationJpaRepository.findAllWithSchedulesByUserId(userId);
    }

    @Override
    public List<Medication> findActiveByUserIdAndDate(Long userId, LocalDate date) {
        return medicationJpaRepository.findActive(userId, date);
    }

    @Override
    public Optional<Medication> findByScheduleId(Long scheduleId) {
        return medicationJpaRepository.findByScheduleId(scheduleId);
    }

    @Override
    public void delete(Medication medication) {
        medicationJpaRepository.delete(medication);
    }

    @Override
    public void flush() {
        medicationJpaRepository.flush();
    }
}
