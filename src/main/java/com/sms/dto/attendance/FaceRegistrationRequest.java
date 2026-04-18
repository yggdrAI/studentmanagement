package com.sms.dto.attendance;

import java.util.List;

/**
 * Request to register a student's face embedding.
 */
public class FaceRegistrationRequest {
    private List<Double> faceEmbedding;
    private Boolean livenessVerified;
    private String livenessPrompt;
    private Boolean blinkDetected;
    private Boolean headMovementDetected;
    private Integer frameCount;

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