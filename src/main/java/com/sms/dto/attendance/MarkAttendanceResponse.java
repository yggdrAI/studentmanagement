package com.sms.dto.attendance;

import java.time.LocalTime;

/**
 * Response after marking attendance
 */
public class MarkAttendanceResponse {
    private Boolean success;
    private String message;
    private String status; // MARKED, ALREADY_MARKED, EXPIRED, INVALID
    private LocalTime markedAt;
    private String attendanceId;

    public MarkAttendanceResponse(Boolean success, String message, String status) {
        this.success = success;
        this.message = message;
        this.status = status;
        this.markedAt = LocalTime.now();
    }

    public MarkAttendanceResponse(Boolean success, String message, String status, String attendanceId) {
        this.success = success;
        this.message = message;
        this.status = status;
        this.attendanceId = attendanceId;
        this.markedAt = LocalTime.now();
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalTime markedAt) {
        this.markedAt = markedAt;
    }

    public String getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(String attendanceId) {
        this.attendanceId = attendanceId;
    }
}
