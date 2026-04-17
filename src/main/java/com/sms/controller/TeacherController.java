package com.sms.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    @Value("${app.mapbox.access-token:}")
    private String mapboxAccessToken;

    @GetMapping("/teacher/dashboard")
    public String teacherDashboard(Model model) {
        model.addAttribute("mapboxAccessToken", mapboxAccessToken);
        return "teacher-dashboard";
    }
}