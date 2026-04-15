package com.sms.dto.dashboard;

import java.util.List;

public class DashboardResponse {

    private String studentId;
    private String studentName;
    private Integer totalCredits;
    private Double overallProgress;
    private Double taskCompletionRate;
    private List<CourseProgressDto> courses;
    private List<TaskDto> tasks;
    private List<UpcomingClassDto> upcomingClasses;

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

    public Double getOverallProgress() {
        return overallProgress;
    }

    public void setOverallProgress(Double overallProgress) {
        this.overallProgress = overallProgress;
    }

    public Double getTaskCompletionRate() {
        return taskCompletionRate;
    }

    public void setTaskCompletionRate(Double taskCompletionRate) {
        this.taskCompletionRate = taskCompletionRate;
    }

    public List<CourseProgressDto> getCourses() {
        return courses;
    }

    public void setCourses(List<CourseProgressDto> courses) {
        this.courses = courses;
    }

    public List<TaskDto> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDto> tasks) {
        this.tasks = tasks;
    }

    public List<UpcomingClassDto> getUpcomingClasses() {
        return upcomingClasses;
    }

    public void setUpcomingClasses(List<UpcomingClassDto> upcomingClasses) {
        this.upcomingClasses = upcomingClasses;
    }
}
