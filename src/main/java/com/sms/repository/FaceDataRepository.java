package com.sms.repository;

import com.sms.model.FaceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FaceDataRepository extends JpaRepository<FaceData, Long> {
    Optional<FaceData> findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);
}