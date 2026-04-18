package com.sms.dto.attendance;

import java.util.List;

/**
 * Request to mark attendance by scanning QR code
 */
public class MarkAttendanceRequest {
    private String qrToken; // JWT token scanned from QR
    private String deviceId; // For device tracking
    private String userAgent; // Browser info
    private Double latitude; // Geographic location (optional)
    private Double longitude;
    private Double accuracy;
    private Long qrDetectedAtEpochMs;
    private List<Double> faceEmbedding;
    private Boolean livenessVerified;
    private String livenessPrompt;
    private Boolean blinkDetected;
    private Boolean headMovementDetected;
    private Integer frameCount;

    public MarkAttendanceRequest() {}

    public MarkAttendanceRequest(String qrToken, String deviceId, String userAgent) {
        this.qrToken = qrToken;
        this.deviceId = deviceId;
        this.userAgent = userAgent;
    }

    public MarkAttendanceRequest(String qrToken, String deviceId, String userAgent, Double latitude, Double longitude, Double accuracy) {
        this.qrToken = qrToken;
        this.deviceId = deviceId;
        this.userAgent = userAgent;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Long getQrDetectedAtEpochMs() {
        return qrDetectedAtEpochMs;
    }

    public void setQrDetectedAtEpochMs(Long qrDetectedAtEpochMs) {
        this.qrDetectedAtEpochMs = qrDetectedAtEpochMs;
    }

    public List<Double> getFaceEmbedding() {
        return faceEmbedding;
    }

    public void setFaceEmbedding(List<Double> faceEmbedding) {
        this.faceEmbedding = faceEmbedding;
    }

    public Boolean getLivenessVerified() {
        return livenessVerified;
    }

    public void setLivenessVerified(Boolean livenessVerified) {
        this.livenessVerified = livenessVerified;
    }

    public String getLivenessPrompt() {
        return livenessPrompt;
    }

    public void setLivenessPrompt(String livenessPrompt) {
        this.livenessPrompt = livenessPrompt;
    }

    public Boolean getBlinkDetected() {
        return blinkDetected;
    }

    public void setBlinkDetected(Boolean blinkDetected) {
        this.blinkDetected = blinkDetected;
    }

    public Boolean getHeadMovementDetected() {
        return headMovementDetected;
    }

    public void setHeadMovementDetected(Boolean headMovementDetected) {
        this.headMovementDetected = headMovementDetected;
    }

    public Integer getFrameCount() {
        return frameCount;
    }

    public void setFrameCount(Integer frameCount) {
        this.frameCount = frameCount;
    }
}
