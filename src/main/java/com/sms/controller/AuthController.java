package com.sms.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping({"/", "/."})
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("studentLoginMode", false);
        return "login";
    }

    @GetMapping("/student-login")
    public String studentLoginPage(Model model) {
        model.addAttribute("studentLoginMode", true);
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = hasAuthority(authentication, "ROLE_ADMIN");
        boolean isTeacher = hasAuthority(authentication, "ROLE_TEACHER");
        boolean isStudent = hasAuthority(authentication, "ROLE_STUDENT");

        if (isAdmin) {
            return "redirect:/admin/dashboard";
        }

        if (isTeacher) {
            return "redirect:/teacher/dashboard";
        }

        if (isStudent) {
            return "redirect:/student/dashboard";
        }

        return "redirect:/student/dashboard";
    }

    private boolean hasAuthority(Authentication authentication, String expectedAuthority) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(expectedAuthority::equals);
    }
}