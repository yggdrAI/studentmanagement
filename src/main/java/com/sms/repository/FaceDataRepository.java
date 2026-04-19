package com.sms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.FaceData;

@Repository
public interface FaceDataRepository extends JpaRepository<FaceData, Long> {
    Optional<FaceData> findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);
    Optional<FaceData> findByStudentIdAndTenantId(String studentId, Long tenantId);
    boolean existsByStudentIdAndTenantId(String studentId, Long tenantId);
    void deleteByStudentId(String studentId);
}