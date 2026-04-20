package com.sms.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.sms.model.User;
import com.sms.repository.UserRepository;

@Controller
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
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

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
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
            boolean firstLogin = userRepository.findByUsername(authentication.getName())
                    .map(User::getIsFirstLogin)
                    .orElse(false);
            if (firstLogin) {
                return "redirect:/student/profile?forcePasswordChange=1";
            }
            return "redirect:/student/dashboard";
        }

        return "redirect:/login";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean hasAuthority(Authentication authentication, String expectedAuthority) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(expectedAuthority::equals);
    }
}