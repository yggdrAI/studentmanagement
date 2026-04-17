package com.sms.dto.campus;

import java.time.LocalDateTime;

/**
 * Live student location message for websocket updates.
 */
public class LocationDTO {
    private String studentId;
    private Long subjectId;
    private String sessionId;
    private Double lat;
    private Double lng;
    private Double accuracy;
    private Boolean flag;
    private String status;
    private String reason;
    private LocalDateTime recordedAt;

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public Double getAccuracy() { return accuracy; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
    public Boolean getFlag() { return flag; }
    public void setFlag(Boolean flag) { this.flag = flag; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}