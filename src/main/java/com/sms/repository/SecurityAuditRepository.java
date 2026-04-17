package com.sms.repository;

import com.sms.model.SecurityAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for SecurityAudit
 */
@Repository
public interface SecurityAuditRepository extends JpaRepository<SecurityAudit, Long> {
    
    /**
     * Get security violations for a student
     */
    List<SecurityAudit> findByStudentIdOrderByCreatedAtDesc(String studentId);

    /**
     * Get violations by severity
     */
    @Query("SELECT s FROM SecurityAudit s WHERE s.severityLevel = :severity ORDER BY s.createdAt DESC")
    List<SecurityAudit> findBySeverity(@Param("severity") String severity);

    /**
     * Get blocked violations
     */
    @Query("SELECT s FROM SecurityAudit s WHERE s.isBlocked = true ORDER BY s.createdAt DESC")
    List<SecurityAudit> findBlockedViolations();

    /**
     * Count violations for student in time period
     */
    @Query("SELECT COUNT(s) FROM SecurityAudit s WHERE s.studentId = :studentId AND s.createdAt >= :since")
    int countRecentViolations(@Param("studentId") String studentId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(s) FROM SecurityAudit s WHERE s.studentId = :studentId AND s.createdAt >= :since AND s.violationType = :violationType")
    long countRecentViolationsByType(@Param("studentId") String studentId,
                                     @Param("since") LocalDateTime since,
                                     @Param("violationType") String violationType);

    /**
     * Get violations for a specific type
     */
    List<SecurityAudit> findByViolationType(String violationType);
}
