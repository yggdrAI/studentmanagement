package com.sms.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sms.service.AdminDashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardApiController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardApiController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return adminDashboardService.buildSummary();
    }

    @GetMapping("/database-health")
    public Map<String, Object> databaseHealth() {
        return adminDashboardService.buildDatabaseHealth();
    }

    @GetMapping("/analytics")
    public Map<String, Object> analytics() {
        return adminDashboardService.buildAnalytics();
    }

    @GetMapping("/alerts")
    public Map<String, Object> alerts() {
        return adminDashboardService.buildAlerts();
    }

    @GetMapping("/recent-activity")
    public List<Map<String, ?>> recentActivity(@RequestParam(name = "limit", defaultValue = "12") int limit) {
        return List.of();
    }
}
