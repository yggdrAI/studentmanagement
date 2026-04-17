package com.sms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Live campus presence snapshot for map tracking.
 */
@Entity
@Table(name = "student_location", indexes = {
    @Index(name = "idx_location_subject_time", columnList = "subject_id,recorded_at"),
    @Index(name = "idx_location_session_time", columnList = "session_id,recorded_at"),
    @Index(name = "idx_location_student_time", columnList = "student_id,recorded_at")
})
public class StudentLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "attendance_id")
    private Long attendanceId;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "location_verified", nullable = false)
    private Boolean locationVerified = false;

    @Column(name = "suspicious", nullable = false)
    private Boolean suspicious = false;

    @Column(name = "face_similarity")
    private Double faceSimilarity;

    @Column(name = "location_confidence")
    private Integer locationConfidence;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Long getAttendanceId() { return attendanceId; }
    public void setAttendanceId(Long attendanceId) { this.attendanceId = attendanceId; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Boolean getLocationVerified() { return locationVerified; }
    public void setLocationVerified(Boolean locationVerified) { this.locationVerified = locationVerified; }
    public Boolean getSuspicious() { return suspicious; }
    public void setSuspicious(Boolean suspicious) { this.suspicious = suspicious; }
    public Double getFaceSimilarity() { return faceSimilarity; }
    public void setFaceSimilarity(Double faceSimilarity) { this.faceSimilarity = faceSimilarity; }
    public Integer getLocationConfidence() { return locationConfidence; }
    public void setLocationConfidence(Integer locationConfidence) { this.locationConfidence = locationConfidence; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}