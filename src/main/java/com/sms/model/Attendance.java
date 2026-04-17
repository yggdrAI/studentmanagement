package com.sms.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Attendance record for students
 * Tracks attendance with security audit information
 */
@Entity
@Table(name = "attendance", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_attendance_unique_daily", 
            columnNames = {"student_id", "subject_id", "attendance_date"})
    },
    indexes = {
        @Index(name = "idx_student_subject_date", columnList = "student_id,subject_id,attendance_date"),
        @Index(name = "idx_subject_date", columnList = "subject_id,attendance_date"),
        @Index(name = "idx_student_date", columnList = "student_id,attendance_date"),
        @Index(name = "idx_subject_marking_type", columnList = "subject_id,marking_type"),
        @Index(name = "idx_attendance_date", columnList = "attendance_date")
    })
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "marked_time")
    private LocalTime markedTime;

    @Column(name = "status", nullable = false)
    private String status; // PRESENT, ABSENT, LATE

    @Column(name = "marking_type", nullable = false)
    private String markingType; // QR_SCANNED, MANUAL, BIOMETRIC

    @Column(name = "device_info")
    private String deviceInfo; // For audit logging

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "qr_token_used")
    private String qrTokenUsed; // Hash of JWT token for audit

    @Column(name = "student_latitude")
    private Double studentLatitude; // GPS location where attendance was marked

    @Column(name = "student_longitude")
    private Double studentLongitude;

    @Column(name = "location_verified", nullable = false)
    private Boolean locationVerified = false; // Whether location check passed

    @Column(name = "device_id")
    private String deviceId; // Device fingerprint

    @Column(name = "campus_location_id")
    private Long campusLocationId; // Which campus location

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        if (markedTime == null) {
            markedTime = LocalTime.now();
        }
    }

    // Constructors
    public Attendance() {}

    public Attendance(String studentId, Long subjectId, Long teacherId, 
                     LocalDate attendanceDate, String status, String markingType) {
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.teacherId = teacherId;
        this.attendanceDate = attendanceDate;
        this.status = status;
        this.markingType = markingType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public LocalTime getMarkedTime() {
        return markedTime;
    }

    public void setMarkedTime(LocalTime markedTime) {
        this.markedTime = markedTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMarkingType() {
        return markingType;
    }

    public void setMarkingType(String markingType) {
        this.markingType = markingType;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getQrTokenUsed() {
        return qrTokenUsed;
    }

    public void setQrTokenUsed(String qrTokenUsed) {
        this.qrTokenUsed = qrTokenUsed;
    }

    public Double getStudentLatitude() {
        return studentLatitude;
    }

    public void setStudentLatitude(Double studentLatitude) {
        this.studentLatitude = studentLatitude;
    }

    public Double getStudentLongitude() {
        return studentLongitude;
    }

    public void setStudentLongitude(Double studentLongitude) {
        this.studentLongitude = studentLongitude;
    }

    public Boolean getLocationVerified() {
        return locationVerified;
    }

    public void setLocationVerified(Boolean locationVerified) {
        this.locationVerified = locationVerified;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getCampusLocationId() {
        return campusLocationId;
    }

    public void setCampusLocationId(Long campusLocationId) {
        this.campusLocationId = campusLocationId;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
