package com.sms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.StudentImportJob;

@Repository
public interface StudentImportJobRepository extends JpaRepository<StudentImportJob, Long> {
    List<StudentImportJob> findTop50ByOrderByUploadedAtDesc();
    Optional<StudentImportJob> findByIdAndUploadedBy(Long id, String uploadedBy);
}
