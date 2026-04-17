package com.sms.dto.attendance;

import java.util.List;

/**
 * Request to register a student's face embedding.
 */
public class FaceRegistrationRequest {
    private List<Double> faceEmbedding;
    private Boolean livenessVerified;
    private String livenessPrompt;

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
}