package com.sms.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "timetable_holiday", indexes = {
    @Index(name = "idx_holiday_timetable", columnList = "timetable_id"),
    @Index(name = "idx_holiday_date", columnList = "holiday_date")
})
public class TimetableHoliday {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false)
    private Timetable timetable;
    
    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_type", nullable = false)
    private HolidayType holidayType;
    
    @Column(name = "reason", length = 255)
    private String reason;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
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
    
    public LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }
    
    public HolidayType getHolidayType() { return holidayType; }
    public void setHolidayType(HolidayType holidayType) { this.holidayType = holidayType; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    
    public enum HolidayType {
        NATIONAL_HOLIDAY,
        INSTITUTIONAL_HOLIDAY,
        EXAM_DAY,
        NO_CLASSES,
        SPECIAL_EVENT,
        MAINTENANCE
    }
}
