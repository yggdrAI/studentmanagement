package com.sms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Security Audit Log
 * Tracks suspicious activities and cheating attempts
 */
@Entity
@Table(name = "security_audit", indexes = {
    @Index(name = "idx_audit_student_date", columnList = "student_id,created_at"),
    @Index(name = "idx_audit_severity", columnList = "severity_level")
})
public class SecurityAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "severity_level", nullable = false)
    private String severityLevel; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "violation_type", nullable = false)
    private String violationType; // LOCATION_MISMATCH, VPN_DETECTED, DEVICE_MISMATCH, etc.

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "student_lat")
    private Double studentLatitude;

    @Column(name = "student_lng")
    private Double studentLongitude;

    @Column(name = "expected_lat")
    private Double expectedLatitude;

    @Column(name = "expected_lng")
    private Double expectedLongitude;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(String severityLevel) { this.severityLevel = severityLevel; }

    public String getViolationType() { return violationType; }
    public void setViolationType(String violationType) { this.violationType = violationType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Double getStudentLatitude() { return studentLatitude; }
    public void setStudentLatitude(Double studentLatitude) { this.studentLatitude = studentLatitude; }

    public Double getStudentLongitude() { return studentLongitude; }
    public void setStudentLongitude(Double studentLongitude) { this.studentLongitude = studentLongitude; }

    public Double getExpectedLatitude() { return expectedLatitude; }
    public void setExpectedLatitude(Double expectedLatitude) { this.expectedLatitude = expectedLatitude; }

    public Double getExpectedLongitude() { return expectedLongitude; }
    public void setExpectedLongitude(Double expectedLongitude) { this.expectedLongitude = expectedLongitude; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Boolean getIsBlocked() { return isBlocked; }
    public void setIsBlocked(Boolean isBlocked) { this.isBlocked = isBlocked; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
