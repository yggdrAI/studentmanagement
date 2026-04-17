package com.sms.dto.campus;

import java.time.LocalDateTime;

/**
 * Live campus tracking payload for teacher/admin map views.
 */
public class CampusLocationUpdateDTO {
    private Long locationId;
    private Long attendanceId;
    private String studentId;
    private Long subjectId;
    private String sessionId;
    private Double latitude;
    private Double longitude;
    private Boolean locationVerified;
    private Boolean suspicious;
    private Double faceSimilarity;
    private Integer locationConfidence;
    private LocalDateTime recordedAt;

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Long getAttendanceId() { return attendanceId; }
    public void setAttendanceId(Long attendanceId) { this.attendanceId = attendanceId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
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