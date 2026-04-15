package com.sms.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for rendering attendance-related pages
 */
@Controller
public class AttendanceViewController {

    /**
     * Display student attendance scanner page
     * GET /attendance/scanner
     */
    @GetMapping("/attendance/scanner")
    @PreAuthorize("hasRole('STUDENT')")
    public String attendanceScanner() {
        return "attendance-scanner";
    }

    /**
     * Display teacher attendance management page
     * GET /attendance/teacher
     */
    @GetMapping("/attendance/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public String teacherAttendance() {
        return "teacher-attendance";
    }

    /**
     * Display attendance reports
     * GET /attendance/reports
     */
    @GetMapping("/attendance/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public String attendanceReports() {
        return "attendance-reports";
    }
}
