package com.sms.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    @GetMapping("/student/dashboard")
    public String studentDashboard() {
        return "student-dashboard";
    }

    @GetMapping("/student/timetable")
    public String studentTimetable() {
        return "student-timetable";
    }

    @GetMapping("/student/profile")
    public String studentProfile(Model model) {
        model.addAttribute("profileMode", "STUDENT");
        model.addAttribute("targetStudentId", "");
        return "student-profile";
    }
}