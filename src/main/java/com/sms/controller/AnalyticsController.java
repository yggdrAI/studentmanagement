package com.sms.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AnalyticsController {

    @GetMapping("/ai-insights")
    public String aiInsightsEntry(Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if ("ROLE_ADMIN".equals(role)) {
            return "redirect:/admin/ai-insights";
        }
        if ("ROLE_TEACHER".equals(role)) {
            return "redirect:/teacher/ai-insights";
        }
        return "redirect:/student/ai-insights";
    }

    @GetMapping("/admin/ai-insights")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminInsights(Model model) {
        model.addAttribute("analyticsRole", "ADMIN");
        return "ai-insights";
    }

    @GetMapping("/teacher/ai-insights")
    @PreAuthorize("hasRole('TEACHER')")
    public String teacherInsights(Model model) {
        model.addAttribute("analyticsRole", "TEACHER");
        return "ai-insights";
    }

    @GetMapping("/student/ai-insights")
    @PreAuthorize("hasRole('STUDENT')")
    public String studentInsights(Model model) {
        model.addAttribute("analyticsRole", "STUDENT");
        return "ai-insights";
    }
}
