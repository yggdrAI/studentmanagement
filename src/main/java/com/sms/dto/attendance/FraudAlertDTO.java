package com.sms.dto.attendance;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WebSocket alert payload for suspicious attendance activity.
 */
public class FraudAlertDTO {
    private Long fraudLogId;
    private String studentId;
    private Long subjectId;
    private Long attendanceId;
    private String decision;
    private double fraudScore;
    private List<String> reasons;
    private Double latitude;
    private Double longitude;
    private LocalDateTime recordedAt;

    public Long getFraudLogId() { return fraudLogId; }
    public void setFraudLogId(Long fraudLogId) { this.fraudLogId = fraudLogId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public Long getAttendanceId() { return attendanceId; }
    public void setAttendanceId(Long attendanceId) { this.attendanceId = attendanceId; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public double getFraudScore() { return fraudScore; }
    public void setFraudScore(double fraudScore) { this.fraudScore = fraudScore; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}