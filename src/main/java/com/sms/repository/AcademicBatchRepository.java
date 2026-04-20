package com.sms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.AcademicBatch;

@Repository
public interface AcademicBatchRepository extends JpaRepository<AcademicBatch, Long> {
    Optional<AcademicBatch> findByBatchNumber(Integer batchNumber);

    Optional<AcademicBatch> findByAcademicClass_ClassNumberAndBatchNumber(Integer classNumber, Integer batchNumber);

    List<AcademicBatch> findByAcademicClass_ClassNumberOrderByBatchNumberAsc(Integer classNumber);

    List<AcademicBatch> findAllByOrderByBatchNumberAsc();
}