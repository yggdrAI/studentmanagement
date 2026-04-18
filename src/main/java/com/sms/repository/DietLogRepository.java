package com.sms.repository;

import com.sms.model.DietLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DietLogRepository extends JpaRepository<DietLog, Long> {

    List<DietLog> findByStudentIdAndDate(String studentId, LocalDate date);

    List<DietLog> findByStudentIdAndDateBetween(String studentId, LocalDate startDate, LocalDate endDate);

    void deleteByStudentIdAndDate(String studentId, LocalDate date);
}
