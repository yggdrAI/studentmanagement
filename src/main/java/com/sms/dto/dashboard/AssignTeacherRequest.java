package com.sms.dto.dashboard;

import jakarta.validation.constraints.NotNull;

public class AssignTeacherRequest {

    @NotNull
    private Long courseId;

    @NotNull
    private Long teacherId;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }
}
