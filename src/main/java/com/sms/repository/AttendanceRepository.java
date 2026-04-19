package com.sms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sms.model.Attendance;

/**
 * Repository for Attendance entity
 * Includes custom queries for attendance tracking and reporting
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /**
     * Check if student already has attendance for a specific subject on a date
     */
    @Query("SELECT COUNT(a) > 0 FROM Attendance a WHERE a.studentId = :studentId " +
           "AND a.subjectId = :subjectId AND a.attendanceDate = :date")
    boolean existsByStudentAndSubjectAndDate(
        @Param("studentId") String studentId,
        @Param("subjectId") Long subjectId,
        @Param("date") LocalDate date
    );

    @Query("SELECT COUNT(a) > 0 FROM Attendance a WHERE a.studentId = :studentId " +
           "AND a.subjectId = :subjectId AND a.attendanceDate = :date AND a.tenantId = :tenantId")
    boolean existsByStudentAndSubjectAndDateAndTenantId(
        @Param("studentId") String studentId,
        @Param("subjectId") Long subjectId,
        @Param("date") LocalDate date,
        @Param("tenantId") Long tenantId
    );

    /**
     * Get attendance record for a student on a specific date and subject
     */
    Optional<Attendance> findByStudentIdAndSubjectIdAndAttendanceDate(
        String studentId, Long subjectId, LocalDate date
    );

    /**
     * Get all attendance records for a student in a subject
     */
    List<Attendance> findByStudentIdAndSubjectIdOrderByAttendanceDateDesc(
        String studentId, Long subjectId
    );

    List<Attendance> findByStudentIdAndSubjectIdAndTenantIdOrderByAttendanceDateDesc(
        String studentId, Long subjectId, Long tenantId
    );

    /**
     * Get attendance records for a subject on a specific date
     */
    @Query("SELECT a FROM Attendance a WHERE a.subjectId = :subjectId " +
           "AND a.attendanceDate = :date ORDER BY a.markedTime ASC")
    List<Attendance> findBySubjectAndDate(
        @Param("subjectId") Long subjectId,
        @Param("date") LocalDate date
    );

    @Query("SELECT a FROM Attendance a WHERE a.subjectId = :subjectId " +
           "AND a.attendanceDate = :date AND a.tenantId = :tenantId ORDER BY a.markedTime ASC")
    List<Attendance> findBySubjectAndDateAndTenantId(
        @Param("subjectId") Long subjectId,
        @Param("date") LocalDate date,
        @Param("tenantId") Long tenantId
    );

    /**
     * Count students present for a subject on a date
     */
    @Query("SELECT COUNT(DISTINCT a.studentId) FROM Attendance a WHERE a.subjectId = :subjectId " +
           "AND a.attendanceDate = :date AND a.status = 'PRESENT'")
    Long countPresentBySubjectAndDate(
        @Param("subjectId") Long subjectId,
        @Param("date") LocalDate date
    );

    @Query("SELECT COUNT(DISTINCT a.studentId) FROM Attendance a WHERE a.subjectId = :subjectId " +
           "AND a.attendanceDate = :date AND a.status = 'PRESENT' AND a.tenantId = :tenantId")
    Long countPresentBySubjectAndDateAndTenantId(
        @Param("subjectId") Long subjectId,
        @Param("date") LocalDate date,
        @Param("tenantId") Long tenantId
    );

    /**
     * Get attendance for a date range
     */
    @Query("SELECT a FROM Attendance a WHERE a.studentId = :studentId " +
           "AND a.subjectId = :subjectId AND a.attendanceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.attendanceDate ASC")
    List<Attendance> findAttendanceRange(
        @Param("studentId") String studentId,
        @Param("subjectId") Long subjectId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT a FROM Attendance a WHERE a.studentId = :studentId " +
           "AND a.subjectId = :subjectId AND a.attendanceDate BETWEEN :startDate AND :endDate " +
           "AND a.tenantId = :tenantId ORDER BY a.attendanceDate ASC")
    List<Attendance> findAttendanceRangeByTenant(
        @Param("studentId") String studentId,
        @Param("subjectId") Long subjectId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("tenantId") Long tenantId
    );

    void deleteByStudentId(String studentId);

    /**
     * Get attendance statistics - present count
     */
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.studentId = :studentId " +
           "AND a.subjectId = :subjectId AND a.status = 'PRESENT'")
    Long countPresent(
        @Param("studentId") String studentId,
        @Param("subjectId") Long subjectId
    );

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.studentId = :studentId " +
           "AND a.subjectId = :subjectId AND a.status = 'PRESENT' AND a.tenantId = :tenantId")
    Long countPresentByTenant(
        @Param("studentId") String studentId,
        @Param("subjectId") Long subjectId,
        @Param("tenantId") Long tenantId
    );

    /**
     * Get all attendance for a teacher's subject
     */
    List<Attendance> findByTeacherIdAndSubjectIdOrderByAttendanceDateDesc(
        Long teacherId, Long subjectId
    );

    List<Attendance> findByTeacherIdAndSubjectIdAndTenantIdOrderByAttendanceDateDesc(
        Long teacherId, Long subjectId, Long tenantId
    );

    /**
     * Check duplicate using QR token (anti-cheating)
     */
    @Query("SELECT COUNT(a) > 0 FROM Attendance a WHERE a.qrTokenUsed = :tokenHash " +
           "AND a.studentId = :studentId AND a.attendanceDate = :date")
    boolean existsByTokenHashAndStudentAndDate(
        @Param("tokenHash") String tokenHash,
        @Param("studentId") String studentId,
        @Param("date") LocalDate date
    );

    @Query("SELECT COUNT(a) > 0 FROM Attendance a WHERE a.qrTokenUsed = :tokenHash " +
           "AND a.studentId = :studentId AND a.attendanceDate = :date AND a.tenantId = :tenantId")
    boolean existsByTokenHashAndStudentAndDateAndTenantId(
        @Param("tokenHash") String tokenHash,
        @Param("studentId") String studentId,
        @Param("date") LocalDate date,
        @Param("tenantId") Long tenantId
    );

        @Query("SELECT a.attendanceDate, " +
            "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END), " +
            "COUNT(a) " +
            "FROM Attendance a " +
            "WHERE a.attendanceDate BETWEEN :fromDate AND :toDate " +
            "GROUP BY a.attendanceDate ORDER BY a.attendanceDate")
        List<Object[]> attendanceTrend(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

        @Query("SELECT a.studentId, " +
            "SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END), " +
            "COUNT(a) " +
            "FROM Attendance a GROUP BY a.studentId HAVING COUNT(a) >= :minRecords")
        List<Object[]> studentAttendanceRates(@Param("minRecords") long minRecords);
}
