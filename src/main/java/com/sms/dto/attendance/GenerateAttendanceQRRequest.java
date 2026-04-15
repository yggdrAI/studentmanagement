package com.sms.dto.attendance;

import java.time.LocalDate;

/**
 * Request to generate QR code for attendance
 */
public class GenerateAttendanceQRRequest {
    private Long subjectId;
    private String subjectName;
    private Integer expiryMinutes; // How long QR should be valid (default 5)

    public GenerateAttendanceQRRequest() {}

    public GenerateAttendanceQRRequest(Long subjectId, String subjectName, Integer expiryMinutes) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.expiryMinutes = expiryMinutes != null ? expiryMinutes : 5;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Integer getExpiryMinutes() {
        return expiryMinutes != null ? expiryMinutes : 5;
    }

    public void setExpiryMinutes(Integer expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }
}
