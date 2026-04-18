package com.sms.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "timetable", indexes = {
    @Index(name = "idx_timetable_course_sem", columnList = "course_id,semester"),
    @Index(name = "idx_timetable_academic_year", columnList = "academic_year"),
    @Index(name = "idx_timetable_effective", columnList = "effective_from")
})
public class Timetable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "timetable_code", unique = true, nullable = false, length = 64)
    private String timetableCode;
    
    @Column(name = "course_id", nullable = false, length = 64)
    private String courseId;
    
    @Column(name = "course_name", nullable = false, length = 255)
    private String courseName;
    
    @Column(name = "semester", nullable = false)
    private Integer semester;
    
    @Column(name = "section", length = 32)
    private String section;
    
    @Column(name = "academic_year", nullable = false, length = 16)
    private String academicYear;
    
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TimetableStatus status = TimetableStatus.DRAFT;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "timetable_id")
    private List<ScheduleEntry> scheduleEntries = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 128)
    private String createdBy;
    
    @Column(name = "updated_by", length = 128)
    private String updatedBy;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTimetableCode() { return timetableCode; }
    public void setTimetableCode(String timetableCode) { this.timetableCode = timetableCode; }
    
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
    
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    
    public TimetableStatus getStatus() { return status; }
    public void setStatus(TimetableStatus status) { this.status = status; }
    
    public List<ScheduleEntry> getScheduleEntries() { return scheduleEntries; }
    public void setScheduleEntries(List<ScheduleEntry> scheduleEntries) { this.scheduleEntries = scheduleEntries; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    
    public enum TimetableStatus {
        DRAFT, PUBLISHED, ARCHIVED, CANCELLED
    }
}
