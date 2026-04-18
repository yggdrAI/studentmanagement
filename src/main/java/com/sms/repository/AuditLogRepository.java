package com.sms.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
    List<AuditLog> findTop100ByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<AuditLog> findByOrderByCreatedAtDesc(Pageable pageable);
}
