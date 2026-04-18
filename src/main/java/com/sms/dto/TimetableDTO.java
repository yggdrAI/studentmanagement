package com.sms.dto;

import com.sms.model.Timetable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TimetableDTO {
    
    private Long id;
    private String timetableCode;
    private String courseId;
    private String courseName;
    private Integer semester;
    private String section;
    private String academicYear;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Timetable.TimetableStatus status;
    private List<ScheduleEntryDTO> scheduleEntries;
    private List<TimetableHolidayDTO> holidays;
    private Integer conflictCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public TimetableDTO() {
    }

    public TimetableDTO(Long id, String timetableCode, String courseId, String courseName, Integer semester,
                        String section, String academicYear, LocalDate effectiveFrom, LocalDate effectiveTo,
                        Timetable.TimetableStatus status, List<ScheduleEntryDTO> scheduleEntries,
                        List<TimetableHolidayDTO> holidays, Integer conflictCount,
                        LocalDateTime createdAt, LocalDateTime updatedAt,
                        String createdBy, String updatedBy) {
        this.id = id;
        this.timetableCode = timetableCode;
        this.courseId = courseId;
        this.courseName = courseName;
        this.semester = semester;
        this.section = section;
        this.academicYear = academicYear;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.status = status;
        this.scheduleEntries = scheduleEntries;
        this.holidays = holidays;
        this.conflictCount = conflictCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTimetableCode() {
        return timetableCode;
    }

    public void setTimetableCode(String timetableCode) {
        this.timetableCode = timetableCode;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Timetable.TimetableStatus getStatus() {
        return status;
    }

    public void setStatus(Timetable.TimetableStatus status) {
        this.status = status;
    }

    public List<ScheduleEntryDTO> getScheduleEntries() {
        return scheduleEntries;
    }

    public void setScheduleEntries(List<ScheduleEntryDTO> scheduleEntries) {
        this.scheduleEntries = scheduleEntries;
    }

    public List<TimetableHolidayDTO> getHolidays() {
        return holidays;
    }

    public void setHolidays(List<TimetableHolidayDTO> holidays) {
        this.holidays = holidays;
    }

    public Integer getConflictCount() {
        return conflictCount;
    }

    public void setConflictCount(Integer conflictCount) {
        this.conflictCount = conflictCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
