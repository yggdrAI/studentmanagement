package com.sms.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    @GetMapping("/teacher/dashboard")
    public String teacherDashboard() {
        return "teacher-dashboard";
    }
}