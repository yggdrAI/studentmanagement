package com.sms.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.ClassSession;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    List<ClassSession> findTop10ByStudentIdAndStartsAtAfterOrderByStartsAtAsc(String studentId, LocalDateTime now);
}
