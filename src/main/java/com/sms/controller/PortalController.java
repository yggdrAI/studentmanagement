package com.sms.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PortalController {

    @Value("${app.mapbox.access-token:}")
    private String mapboxAccessToken;

    @GetMapping("/attendance")
    public String attendance() {
        return "attendance";
    }

    @GetMapping("/cafeteria")
    public String cafeteria() {
        return "cafeteria";
    }

    @GetMapping("/teachers")
    public String teachers() {
        return "teachers";
    }

    @GetMapping("/campus-map")
    public String campusMap(Model model, Authentication authentication) {
        model.addAttribute("mapboxAccessToken", mapboxAccessToken);
        boolean studentView = false;
        String viewerRole = "GUEST";

        if (authentication != null && authentication.getAuthorities() != null) {
            studentView = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_STUDENT".equalsIgnoreCase(authority.getAuthority()))
                    && authentication.getAuthorities().stream()
                    .noneMatch(authority -> "ROLE_ADMIN".equalsIgnoreCase(authority.getAuthority())
                            || "ROLE_TEACHER".equalsIgnoreCase(authority.getAuthority()));

            viewerRole = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .findFirst()
                    .orElse("GUEST");
        }

        model.addAttribute("campusStudentView", studentView);
        model.addAttribute("campusViewerRole", viewerRole);
        return "campus-map";
    }

    @GetMapping("/students")
    public String students() {
        return "redirect:/admin/students";
    }

    // Default routes for generic items just map back to dashboard or their intended views
    @GetMapping({"/courses", "/exam-schedules", "/reports", "/holidays", "/services", "/enrollment"})
    public String genericPages() {
        return "admin-dashboard"; // Fallback for unsupported tabs in this demo
    }
}
