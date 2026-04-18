package com.sms.repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sms.model.ScheduleEntry;

@Repository
public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Long> {
    
    List<ScheduleEntry> findByTimetableId(Long timetableId);
    
    List<ScheduleEntry> findByTimetableIdAndDayOfWeek(Long timetableId, DayOfWeek dayOfWeek);

       List<ScheduleEntry> findByTimetableIdAndScheduleDate(Long timetableId, LocalDate scheduleDate);
    
    @Query("SELECT s FROM ScheduleEntry s WHERE s.timetable.id = :timetableId AND s.scheduleDate = :date")
    List<ScheduleEntry> findByTimetableAndDate(@Param("timetableId") Long timetableId, @Param("date") LocalDate date);
    
    @Query("SELECT s FROM ScheduleEntry s WHERE s.facultyId = :facultyId AND s.scheduleDate = :date " +
           "AND NOT (s.endTime <= :startTime OR s.startTime >= :endTime)")
    List<ScheduleEntry> findFacultyConflicts(@Param("facultyId") String facultyId, @Param("date") LocalDate date, 
                                             @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);
    
    @Query("SELECT s FROM ScheduleEntry s WHERE s.roomId = :roomId AND s.scheduleDate = :date " +
           "AND NOT (s.endTime <= :startTime OR s.startTime >= :endTime)")
    List<ScheduleEntry> findRoomConflicts(@Param("roomId") String roomId, @Param("date") LocalDate date,
                                          @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

    @Query("SELECT COUNT(s) FROM ScheduleEntry s WHERE (s.scheduleDate = :today OR (s.scheduleDate IS NULL AND s.dayOfWeek = :dayOfWeek))")
    long countClassesForDay(@Param("today") LocalDate today, @Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("SELECT s.dayOfWeek, COUNT(s) FROM ScheduleEntry s GROUP BY s.dayOfWeek")
    List<Object[]> classesPerWeekday();
    
    List<ScheduleEntry> findBySubjectCode(String subjectCode);
    
    List<ScheduleEntry> findByFacultyId(String facultyId);
}
