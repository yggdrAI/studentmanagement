package com.sms.dto.attendance;

/**
 * Request to mark attendance by scanning QR code
 */
public class MarkAttendanceRequest {
    private String qrToken; // JWT token scanned from QR
    private String deviceId; // For device tracking
    private String userAgent; // Browser info
    private Double latitude; // Geographic location (optional)
    private Double longitude;

    public MarkAttendanceRequest() {}

    public MarkAttendanceRequest(String qrToken, String deviceId, String userAgent) {
        this.qrToken = qrToken;
        this.deviceId = deviceId;
        this.userAgent = userAgent;
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
}
