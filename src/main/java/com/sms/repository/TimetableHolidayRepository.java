package com.sms.repository;

import com.sms.model.TimetableHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimetableHolidayRepository extends JpaRepository<TimetableHoliday, Long> {
    
    List<TimetableHoliday> findByTimetableId(Long timetableId);
    
    @Query("SELECT h FROM TimetableHoliday h WHERE h.timetable.id = :timetableId AND h.holidayDate BETWEEN :startDate AND :endDate")
    List<TimetableHoliday> findHolidaysBetween(@Param("timetableId") Long timetableId, 
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
    
    boolean existsByTimetableIdAndHolidayDate(Long timetableId, LocalDate holidayDate);
}
