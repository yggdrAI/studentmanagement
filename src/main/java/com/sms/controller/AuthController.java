package com.sms.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage() {
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