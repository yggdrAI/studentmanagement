package com.sms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "timetable_conflict", indexes = {
    @Index(name = "idx_conflict_timetable", columnList = "timetable_id"),
    @Index(name = "idx_conflict_status", columnList = "status")
})
public class TimetableConflict {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false)
    private Timetable timetable;
    
    @Column(name = "schedule_entry_id_1")
    private Long scheduleEntryId1;
    
    @Column(name = "schedule_entry_id_2")
    private Long scheduleEntryId2;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "conflict_type", nullable = false)
    private ConflictType conflictType;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "resource_1", length = 255)
    private String resource1;
    
    @Column(name = "resource_2", length = 255)
    private String resource2;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity = Severity.MEDIUM;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConflictStatus status = ConflictStatus.PENDING;
    
    @Column(name = "resolution_suggestion", columnDefinition = "TEXT")
    private String resolutionSuggestion;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Timetable getTimetable() { return timetable; }
    public void setTimetable(Timetable timetable) { this.timetable = timetable; }
    
    public Long getScheduleEntryId1() { return scheduleEntryId1; }
    public void setScheduleEntryId1(Long scheduleEntryId1) { this.scheduleEntryId1 = scheduleEntryId1; }
    
    public Long getScheduleEntryId2() { return scheduleEntryId2; }
    public void setScheduleEntryId2(Long scheduleEntryId2) { this.scheduleEntryId2 = scheduleEntryId2; }
    
    public ConflictType getConflictType() { return conflictType; }
    public void setConflictType(ConflictType conflictType) { this.conflictType = conflictType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getResource1() { return resource1; }
    public void setResource1(String resource1) { this.resource1 = resource1; }
    
    public String getResource2() { return resource2; }
    public void setResource2(String resource2) { this.resource2 = resource2; }
    
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    
    public ConflictStatus getStatus() { return status; }
    public void setStatus(ConflictStatus status) { this.status = status; }
    
    public String getResolutionSuggestion() { return resolutionSuggestion; }
    public void setResolutionSuggestion(String resolutionSuggestion) { this.resolutionSuggestion = resolutionSuggestion; }
    
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    
    public enum ConflictType {
        FACULTY_CLASH, ROOM_CLASH, STUDENT_OVERLAP, TIME_OVERLAP, RESOURCE_OVERLAP
    }
    
    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public enum ConflictStatus {
        PENDING, REVIEWED, RESOLVED, IGNORED, ESCALATED
    }
}
