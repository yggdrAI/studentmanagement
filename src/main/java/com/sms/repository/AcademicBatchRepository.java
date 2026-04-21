package com.sms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.AcademicBatch;
import com.sms.model.AcademicClass;

@Repository
public interface AcademicBatchRepository extends JpaRepository<AcademicBatch, Long> {
    Optional<AcademicBatch> findByBatchNumber(Integer batchNumber);

    Optional<AcademicBatch> findByAcademicProgram_IdAndLocalBatchNumber(Long programId, Integer localBatchNumber);

    Optional<AcademicBatch> findByAcademicClass_ClassNumberAndBatchNumber(Integer classNumber, Integer batchNumber);

    Optional<AcademicBatch> findByAcademicClassAndLocalBatchNumber(AcademicClass clazz, Integer localBatchNumber);

    List<AcademicBatch> findByAcademicClass_ClassNumberOrderByBatchNumberAsc(Integer classNumber);

    List<AcademicBatch> findByAcademicClass_IdOrderByLocalBatchNumberAsc(Long classId);

    List<AcademicBatch> findAllByOrderByBatchNumberAsc();
}