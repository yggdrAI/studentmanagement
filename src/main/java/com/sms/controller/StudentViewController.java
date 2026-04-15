package com.sms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for rendering student view pages
 */
@Controller
@RequestMapping("/student")
public class StudentViewController {

    /**
     * Display the Digital ID Card page
     * GET /student/digital-id
     */
    @GetMapping("/digital-id")
    public String digitalIdCard() {
        return "student-id-card";
    }
}
