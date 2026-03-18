package com.sms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PortalController {

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

    // Default routes for generic items just map back to dashboard or their intended views
    @GetMapping({"/students", "/courses", "/exam-schedules", "/reports", "/holidays", "/services", "/enrollment"})
    public String genericPages() {
        return "admin-dashboard"; // Fallback for unsupported tabs in this demo
    }
}
