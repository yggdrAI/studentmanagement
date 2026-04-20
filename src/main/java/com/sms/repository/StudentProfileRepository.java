package com.sms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.StudentProfile;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, String> {
    Optional<StudentProfile> findByStudentId(String studentId);
    Optional<StudentProfile> findByEnrollmentNumberIgnoreCase(String enrollmentNumber);
    void deleteByStudentId(String studentId);
}
