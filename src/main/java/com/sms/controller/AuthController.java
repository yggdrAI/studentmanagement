package com.sms.controller;

import org.springframework.security.core.Authentication;
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

        String role = authentication.getAuthorities()
                                    .iterator()
                                    .next()
                                    .getAuthority();

        if (role.equals("ROLE_ADMIN")) {
            return "redirect:/admin/dashboard";
        }

        if (role.equals("ROLE_TEACHER")) {
            return "redirect:/teacher/dashboard";
        }

        return "redirect:/student/dashboard";
    }
}