package com.sms.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminStudentProfilePageController {

    @GetMapping("/admin/students/{studentId}/profile")
    public String openStudentProfile(@PathVariable String studentId, Model model) {
        model.addAttribute("profileMode", "ADMIN");
        model.addAttribute("targetStudentId", studentId);
        return "student-profile";
    }

    @GetMapping("/students/{studentId}")
    public String openStudentProfileFromHierarchy(@PathVariable String studentId, Model model) {
        model.addAttribute("profileMode", "ADMIN");
        model.addAttribute("targetStudentId", studentId);
        return "student-profile";
    }
}
