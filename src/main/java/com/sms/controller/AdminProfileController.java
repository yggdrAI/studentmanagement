package com.sms.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminProfileController {

    @GetMapping("/admin/profile")
    public String adminProfile(Model model) {
        model.addAttribute("adminName", "The Creator, Bhavya Jain");
        model.addAttribute("adminEmail", "bhavya.jain@bennett.edu.in");
        model.addAttribute("adminRole", "ADMIN");
        model.addAttribute("adminDob", "15-03-2007");
        model.addAttribute("adminGender", "Female");
        model.addAttribute("adminDepartment", "School of Computer Science & Tech");
        model.addAttribute("adminDesignation", "System Creator");
        return "admin-profile";
    }
}
