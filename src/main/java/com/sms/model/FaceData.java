package com.sms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Stores face embeddings only, never raw face images.
 */
@Entity
@Table(name = "face_data", indexes = {
    @Index(name = "idx_face_student_id", columnList = "student_id", unique = true)
})
public class FaceData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, unique = true)
    private String studentId;

    @Lob
    @Column(name = "embedding_vector", nullable = false)
    private byte[] embeddingVector;

    @Column(name = "embedding_dimension", nullable = false)
    private Integer embeddingDimension = 128;

    @Column(name = "face_model", nullable = false)
    private String faceModel = "face-api.js";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "verified_at", nullable = false, updatable = false)
    private LocalDateTime verifiedAt;

    @PrePersist
    protected void onCreate() {
        if (verifiedAt == null) {
            verifiedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public byte[] getEmbeddingVector() { return embeddingVector; }
    public void setEmbeddingVector(byte[] embeddingVector) { this.embeddingVector = embeddingVector; }
    public Integer getEmbeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(Integer embeddingDimension) { this.embeddingDimension = embeddingDimension; }
    public String getFaceModel() { return faceModel; }
    public void setFaceModel(String faceModel) { this.faceModel = faceModel; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}