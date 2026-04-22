package com.sms.dto.publication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sms.model.PublicationAudience;
import com.sms.model.PublicationCategory;
import com.sms.model.PublicationScope;

public class PublicationCreateRequest {

    private PublicationCategory category = PublicationCategory.NOTICE;
    private PublicationAudience audience = PublicationAudience.BOTH;
    private PublicationScope scope = PublicationScope.GLOBAL;
    private String title;
    private String summary;
    private String studentId;
    private List<String> studentIds = new ArrayList<>();
    private String classGroup;
    private List<String> classGroups = new ArrayList<>();
    private String batchGroup;
    private List<String> batchGroups = new ArrayList<>();
    private String course;
    private String semester;
    private Boolean published = true;
    private Map<String, Object> payload = new LinkedHashMap<>();

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

    public List<String> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<String> studentIds) {
        this.studentIds = studentIds == null ? new ArrayList<>() : studentIds;
    }

    public String getClassGroup() {
        return classGroup;
    }

    public void setClassGroup(String classGroup) {
        this.classGroup = classGroup;
    }

    public List<String> getClassGroups() {
        return classGroups;
    }

    public void setClassGroups(List<String> classGroups) {
        this.classGroups = classGroups == null ? new ArrayList<>() : classGroups;
    }

    public String getBatchGroup() {
        return batchGroup;
    }

    public void setBatchGroup(String batchGroup) {
        this.batchGroup = batchGroup;
    }

    public List<String> getBatchGroups() {
        return batchGroups;
    }

    public void setBatchGroups(List<String> batchGroups) {
        this.batchGroups = batchGroups == null ? new ArrayList<>() : batchGroups;
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

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload == null ? new LinkedHashMap<>() : payload;
    }
}
