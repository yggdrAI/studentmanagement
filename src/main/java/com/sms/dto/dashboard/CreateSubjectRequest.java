package com.sms.dto.dashboard;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CreateSubjectRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String courseName;

    @Min(1)
    private Integer credits = 3;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Integer getCredits() {
        return credits;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }
}
