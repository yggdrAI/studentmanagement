package com.sms.repository;

import com.sms.model.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    
    Optional<Timetable> findByTimetableCode(String timetableCode);
    
    List<Timetable> findByCourseIdAndSemesterAndAcademicYear(String courseId, Integer semester, String academicYear);
    
    List<Timetable> findByAcademicYearAndStatus(String academicYear, Timetable.TimetableStatus status);
    
    @Query("SELECT t FROM Timetable t WHERE t.courseId = :courseId AND t.semester = :semester AND t.status = 'PUBLISHED'")
    List<Timetable> findPublishedTimetables(@Param("courseId") String courseId, @Param("semester") Integer semester);
    
    @Query("SELECT t FROM Timetable t WHERE t.effectiveFrom <= :date AND (t.effectiveTo IS NULL OR t.effectiveTo >= :date) AND t.status = 'PUBLISHED'")
    List<Timetable> findActiveTimetablesForDate(@Param("date") LocalDate date);
    
    @Query("SELECT COUNT(t) FROM Timetable t WHERE t.courseId = :courseId AND t.semester = :semester AND t.academicYear = :academicYear")
    long countByCourseAndSemesterAndYear(@Param("courseId") String courseId, @Param("semester") Integer semester, @Param("academicYear") String academicYear);
}
