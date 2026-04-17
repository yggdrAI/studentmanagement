package com.sms.repository;

import com.sms.model.StudentLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudentLocationRepository extends JpaRepository<StudentLocation, Long> {
    List<StudentLocation> findTop200BySubjectIdOrderByRecordedAtDesc(Long subjectId);
    List<StudentLocation> findTop200BySubjectIdAndRecordedAtAfterOrderByRecordedAtDesc(Long subjectId, LocalDateTime recordedAt);
    List<StudentLocation> findTop100BySessionIdOrderByRecordedAtDesc(String sessionId);
    List<StudentLocation> findByStudentIdOrderByRecordedAtDesc(String studentId);
}