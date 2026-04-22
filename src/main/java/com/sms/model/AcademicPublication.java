package com.sms.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "academic_publications")
public class AcademicPublication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PublicationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PublicationAudience audience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PublicationScope scope;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 1000)
    private String summary;

    @Lob
    @Column(name = "payload_json")
    private String payloadJson;

    @Column(name = "target_student_id", length = 64)
    private String targetStudentId;

    @Column(name = "target_class_group", length = 64)
    private String targetClassGroup;

    @Column(name = "target_batch_group", length = 64)
    private String targetBatchGroup;

    @Column(length = 128)
    private String course;

    @Column(length = 64)
    private String semester;

    @Column(nullable = false)
    private Boolean published = true;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (publishedAt == null) {
            publishedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PublicationCategory getCategory() {
        return category;
    }

    public void setCategory(PublicationCategory category) {
        this.category = category;
    }

    public PublicationAudience getAudience() {
        return audience;
    }

    public void setAudience(PublicationAudience audience) {
        this.audience = audience;
    }

    public PublicationScope getScope() {
        return scope;
    }

    public void setScope(PublicationScope scope) {
        this.scope = scope;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getTargetStudentId() {
        return targetStudentId;
    }

    public void setTargetStudentId(String targetStudentId) {
        this.targetStudentId = targetStudentId;
    }

    public String getTargetClassGroup() {
        return targetClassGroup;
    }

    public void setTargetClassGroup(String targetClassGroup) {
        this.targetClassGroup = targetClassGroup;
    }

    public String getTargetBatchGroup() {
        return targetBatchGroup;
    }

    public void setTargetBatchGroup(String targetBatchGroup) {
        this.targetBatchGroup = targetBatchGroup;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
