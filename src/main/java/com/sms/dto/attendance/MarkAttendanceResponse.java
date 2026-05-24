package com.sms.dto.attendance;

import java.time.LocalTime;
import java.util.List;

/**
 * Response after marking attendance
 */
public class MarkAttendanceResponse {
    private Boolean success;
    private String message;
    private String status; // MARKED, ALREADY_MARKED, EXPIRED, INVALID
    private LocalTime markedAt;
    private String attendanceId;
    private Integer confidenceScore;
    private Boolean faceVerified;
    private Double faceSimilarity;
    private Boolean locationVerified;
    private Double fraudScore;
    private String decision;
    private String riskLevel;
    private Double finalTrustScore;
    private Double faceTrustScore;
    private Double livenessTrustScore;
    private Double qrTrustScore;
    private Double locationTrustScore;
    private Double deviceTrustScore;
    private Double behavioralTrustScore;
    private List<String> verificationReasons;

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

    public MarkAttendanceResponse(Boolean success, String message, String status, String attendanceId, Integer confidenceScore) {
        this.success = success;
        this.message = message;
        this.status = status;
        this.attendanceId = attendanceId;
        this.confidenceScore = confidenceScore;
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

    public Integer getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Integer confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Boolean getFaceVerified() {
        return faceVerified;
    }

    public void setFaceVerified(Boolean faceVerified) {
        this.faceVerified = faceVerified;
    }

    public Double getFaceSimilarity() {
        return faceSimilarity;
    }

    public void setFaceSimilarity(Double faceSimilarity) {
        this.faceSimilarity = faceSimilarity;
    }

    public Boolean getLocationVerified() {
        return locationVerified;
    }

    public void setLocationVerified(Boolean locationVerified) {
        this.locationVerified = locationVerified;
    }

    public Double getFraudScore() {
        return fraudScore;
    }

    public void setFraudScore(Double fraudScore) {
        this.fraudScore = fraudScore;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Double getFinalTrustScore() {
        return finalTrustScore;
    }

    public void setFinalTrustScore(Double finalTrustScore) {
        this.finalTrustScore = finalTrustScore;
    }

    public Double getFaceTrustScore() {
        return faceTrustScore;
    }

    public void setFaceTrustScore(Double faceTrustScore) {
        this.faceTrustScore = faceTrustScore;
    }

    public Double getLivenessTrustScore() {
        return livenessTrustScore;
    }

    public void setLivenessTrustScore(Double livenessTrustScore) {
        this.livenessTrustScore = livenessTrustScore;
    }

    public Double getQrTrustScore() {
        return qrTrustScore;
    }

    public void setQrTrustScore(Double qrTrustScore) {
        this.qrTrustScore = qrTrustScore;
    }

    public Double getLocationTrustScore() {
        return locationTrustScore;
    }

    public void setLocationTrustScore(Double locationTrustScore) {
        this.locationTrustScore = locationTrustScore;
    }

    public Double getDeviceTrustScore() {
        return deviceTrustScore;
    }

    public void setDeviceTrustScore(Double deviceTrustScore) {
        this.deviceTrustScore = deviceTrustScore;
    }

    public Double getBehavioralTrustScore() {
        return behavioralTrustScore;
    }

    public void setBehavioralTrustScore(Double behavioralTrustScore) {
        this.behavioralTrustScore = behavioralTrustScore;
    }

    public List<String> getVerificationReasons() {
        return verificationReasons;
    }

    public void setVerificationReasons(List<String> verificationReasons) {
        this.verificationReasons = verificationReasons;
    }
}
