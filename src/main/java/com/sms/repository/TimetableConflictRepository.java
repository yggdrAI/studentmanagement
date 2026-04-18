package com.sms.repository;

import com.sms.model.TimetableConflict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableConflictRepository extends JpaRepository<TimetableConflict, Long> {
    
    List<TimetableConflict> findByTimetableId(Long timetableId);
    
    List<TimetableConflict> findByTimetableIdAndStatus(Long timetableId, TimetableConflict.ConflictStatus status);
    
    @Query("SELECT c FROM TimetableConflict c WHERE c.timetable.id = :timetableId AND c.severity = :severity")
    List<TimetableConflict> findBySeverity(@Param("timetableId") Long timetableId, 
                                           @Param("severity") TimetableConflict.Severity severity);
    
    long countByTimetableIdAndStatus(Long timetableId, TimetableConflict.ConflictStatus status);
}
