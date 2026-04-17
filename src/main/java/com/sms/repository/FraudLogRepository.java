package com.sms.repository;

import com.sms.model.FraudLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FraudLogRepository extends JpaRepository<FraudLog, Long> {
    List<FraudLog> findTop100ByOrderByCreatedAtDesc();
    List<FraudLog> findTop100ByDecisionOrderByCreatedAtDesc(String decision);

    @Query("SELECT COUNT(f) FROM FraudLog f WHERE f.decision = :decision AND f.createdAt >= :since")
    long countByDecisionSince(@Param("decision") String decision, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(f) FROM FraudLog f WHERE f.studentId = :studentId AND f.createdAt >= :since AND f.decision = :decision")
    long countByStudentAndDecisionSince(@Param("studentId") String studentId,
                                        @Param("decision") String decision,
                                        @Param("since") LocalDateTime since);
}