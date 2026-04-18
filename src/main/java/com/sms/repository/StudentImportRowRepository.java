package com.sms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.StudentImportJob;
import com.sms.model.StudentImportRow;

@Repository
public interface StudentImportRowRepository extends JpaRepository<StudentImportRow, Long> {
    List<StudentImportRow> findByJobOrderByRowIndexAsc(StudentImportJob job);
    List<StudentImportRow> findByJobIdOrderByRowIndexAsc(Long jobId);
}
