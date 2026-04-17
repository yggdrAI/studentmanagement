package com.sms.repository;

import com.sms.model.AnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, Long> {
    Optional<AnalyticsSnapshot> findTopByScopeRoleAndScopeKeyOrderBySnapshotDateDescCreatedAtDesc(String scopeRole, String scopeKey);

    List<AnalyticsSnapshot> findTop10BySnapshotDateOrderByCreatedAtDesc(LocalDate snapshotDate);
}
