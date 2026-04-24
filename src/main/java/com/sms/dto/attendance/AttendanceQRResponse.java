package com.sms.dto.attendance;

/**
 * Response containing QR code and metadata
 */
public class AttendanceQRResponse {
    private String qrToken;
    private String qrImage; // Base64-encoded QR PNG
    private Long subjectId;
    private String subjectName;
    private Long expiresAt; // Timestamp when QR expires
    private Integer expirySeconds;
    private String sessionId; // For tracking this attendance session
    private Boolean faceVerificationRequired;

    public AttendanceQRResponse() {}

    public AttendanceQRResponse(String qrToken, String qrImage, Long subjectId, 
                               String subjectName, Long expiresAt, Integer expirySeconds, String sessionId) {
        this.qrToken = qrToken;
        this.qrImage = qrImage;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.expiresAt = expiresAt;
        this.expirySeconds = expirySeconds;
        this.sessionId = sessionId;
    }

    public AttendanceQRResponse(String qrToken, String qrImage, Long subjectId,
                               String subjectName, Long expiresAt, Integer expirySeconds, String sessionId,
                               Boolean faceVerificationRequired) {
        this(qrToken, qrImage, subjectId, subjectName, expiresAt, expirySeconds, sessionId);
        this.faceVerificationRequired = faceVerificationRequired;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public String getQrImage() {
        return qrImage;
    }

    public void setQrImage(String qrImage) {
        this.qrImage = qrImage;
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

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getExpirySeconds() {
        return expirySeconds;
    }

    public void setExpirySeconds(Integer expirySeconds) {
        this.expirySeconds = expirySeconds;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Boolean getFaceVerificationRequired() {
        return faceVerificationRequired;
    }

    public void setFaceVerificationRequired(Boolean faceVerificationRequired) {
        this.faceVerificationRequired = faceVerificationRequired;
    }
}
