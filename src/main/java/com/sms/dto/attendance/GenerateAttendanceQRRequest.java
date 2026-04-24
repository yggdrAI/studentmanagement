package com.sms.dto.attendance;

import java.time.LocalDate;

/**
 * Request to generate QR code for attendance
 */
public class GenerateAttendanceQRRequest {
    private Long subjectId;
    private String subjectName;
    private Integer expiryMinutes; // How long QR should be valid (default 5)
    private Integer expirySeconds; // Preferred expiry in seconds for dynamic QR (default 10)
    private Double teacherLatitude;
    private Double teacherLongitude;
    private Integer maxDistanceMeters;
    private Boolean dynamicQr;
    private String qrMode;
    private Boolean faceVerificationRequired;

    public GenerateAttendanceQRRequest() {
    }

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

    public Integer getExpirySeconds() {
        return expirySeconds;
    }

    public void setExpirySeconds(Integer expirySeconds) {
        this.expirySeconds = expirySeconds;
    }

    public Double getTeacherLatitude() {
        return teacherLatitude;
    }

    public void setTeacherLatitude(Double teacherLatitude) {
        this.teacherLatitude = teacherLatitude;
    }

    public Double getTeacherLongitude() {
        return teacherLongitude;
    }

    public void setTeacherLongitude(Double teacherLongitude) {
        this.teacherLongitude = teacherLongitude;
    }

    public Integer getMaxDistanceMeters() {
        return maxDistanceMeters;
    }

    public void setMaxDistanceMeters(Integer maxDistanceMeters) {
        this.maxDistanceMeters = maxDistanceMeters;
    }

    public Boolean getDynamicQr() {
        return dynamicQr;
    }

    public void setDynamicQr(Boolean dynamicQr) {
        this.dynamicQr = dynamicQr;
    }

    public String getQrMode() {
        return qrMode;
    }

    public void setQrMode(String qrMode) {
        this.qrMode = qrMode;
    }

    public Boolean getFaceVerificationRequired() {
        return faceVerificationRequired;
    }

    public void setFaceVerificationRequired(Boolean faceVerificationRequired) {
        this.faceVerificationRequired = faceVerificationRequired;
    }
}