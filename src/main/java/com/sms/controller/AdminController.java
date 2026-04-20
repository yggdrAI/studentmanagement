package com.sms.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.sms.service.DatabaseMigrationService;
import com.sms.service.DatabaseStatusService;
import com.sms.service.StudentService;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final StudentService studentService;
    private final DatabaseStatusService databaseStatusService;
    private final DatabaseMigrationService databaseMigrationService;

    public AdminController(StudentService studentService,
                           DatabaseStatusService databaseStatusService,
                           DatabaseMigrationService databaseMigrationService) {
        this.studentService = studentService;
        this.databaseStatusService = databaseStatusService;
        this.databaseMigrationService = databaseMigrationService;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Authentication authentication, Model model) {
        model.addAttribute("adminName", authentication != null ? authentication.getName() : "admin");
        model.addAttribute("studentCount", studentService.getAllStudents().size());
        model.addAttribute("databaseStatus", databaseStatusService.getSnapshot());
        model.addAttribute("databaseMigrationMessage", databaseMigrationService.getLastMigrationMessage());
        model.addAttribute("databaseMigrationSuccess", databaseMigrationService.isLastMigrationSuccess());
        model.addAttribute("assetVersion", System.currentTimeMillis());
        return "admin-dashboard";
    }

    // ── Student Management ─────────────────────────────────────────────── //

    @GetMapping("/admin/students")
    public String listStudents() {
        return "admin-students-hierarchy";
    }

    @GetMapping("/admin/students/manage")
    public String studentsManageWorkspace() {
        return "admin-students";
    }

    @GetMapping("/admin/timetables")
    public String manageTimetables() {
        return "admin-timetables";
    }

    @GetMapping("/admin/import/students")
    public String importStudents() {
        return "admin-student-import";
    }

    @GetMapping("/admin/students/react")
    public String studentsReactHierarchy() {
        return "admin-students-hierarchy";
    }

    @GetMapping("/admin/students/hierarchy")
    public String studentsHierarchy() {
        return "admin-students-hierarchy";
    }

}
