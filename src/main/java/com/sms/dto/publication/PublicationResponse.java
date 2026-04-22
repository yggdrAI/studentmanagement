package com.sms.dto.publication;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sms.model.PublicationAudience;
import com.sms.model.PublicationCategory;
import com.sms.model.PublicationScope;

public class PublicationResponse {

    private Long id;
    private PublicationCategory category;
    private PublicationAudience audience;
    private PublicationScope scope;
    private String title;
    private String summary;
    private String studentId;
    private String classGroup;
    private String batchGroup;
    private String course;
    private String semester;
    private Boolean published;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private Map<String, Object> payload = new LinkedHashMap<>();

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

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getClassGroup() {
        return classGroup;
    }

    public void setClassGroup(String classGroup) {
        this.classGroup = classGroup;
    }

    public String getBatchGroup() {
        return batchGroup;
    }

    public void setBatchGroup(String batchGroup) {
        this.batchGroup = batchGroup;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload == null ? new LinkedHashMap<>() : payload;
    }
}
