package com.sms.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    @GetMapping("/student/dashboard")
    public String studentDashboard() {
        return "student-dashboard";
    }

    @GetMapping("/student/profile")
    public String studentProfile() {
        return "student-profile";
    }
}