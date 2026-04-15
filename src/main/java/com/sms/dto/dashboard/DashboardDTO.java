package com.sms.dto.dashboard;

import java.util.List;

public class DashboardDTO {

    private String studentId;
    private String studentName;
    private Integer totalCredits;
    private Integer totalTasks;
    private Integer completedTasks;
    private Double overallProgress;
    private List<SubjectDTO> subjects;
    private List<TaskDto> tasks;
    private List<ClassDTO> upcomingClasses;

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Integer getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(Integer totalCredits) {
        this.totalCredits = totalCredits;
    }

    public Integer getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(Integer totalTasks) {
        this.totalTasks = totalTasks;
    }

    public Integer getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(Integer completedTasks) {
        this.completedTasks = completedTasks;
    }

    public Double getOverallProgress() {
        return overallProgress;
    }

    public void setOverallProgress(Double overallProgress) {
        this.overallProgress = overallProgress;
    }

    public List<SubjectDTO> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<SubjectDTO> subjects) {
        this.subjects = subjects;
    }

    public List<TaskDto> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDto> tasks) {
        this.tasks = tasks;
    }

    public List<ClassDTO> getUpcomingClasses() {
        return upcomingClasses;
    }

    public void setUpcomingClasses(List<ClassDTO> upcomingClasses) {
        this.upcomingClasses = upcomingClasses;
    }
}
