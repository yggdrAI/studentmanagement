package com.sms.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.DietLog;

@Repository
public interface DietLogRepository extends JpaRepository<DietLog, Long> {

    List<DietLog> findByStudentIdAndDate(String studentId, LocalDate date);

    List<DietLog> findByStudentIdAndDateBetween(String studentId, LocalDate startDate, LocalDate endDate);

    void deleteByStudentIdAndDate(String studentId, LocalDate date);

    void deleteByStudentId(String studentId);
}
