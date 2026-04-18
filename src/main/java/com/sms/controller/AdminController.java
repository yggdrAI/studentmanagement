package com.sms.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sms.dto.profile.StudentProfileResponseDTO;
import com.sms.model.Student;
import com.sms.service.DatabaseMigrationService;
import com.sms.service.DatabaseStatusService;
import com.sms.service.StudentProfileService;
import com.sms.service.StudentService;

import jakarta.validation.Valid;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final StudentService studentService;
    private final StudentProfileService studentProfileService;
    private final DatabaseStatusService databaseStatusService;
    private final DatabaseMigrationService databaseMigrationService;

    public AdminController(StudentService studentService,
                           StudentProfileService studentProfileService,
                           DatabaseStatusService databaseStatusService,
                           DatabaseMigrationService databaseMigrationService) {
        this.studentService = studentService;
        this.studentProfileService = studentProfileService;
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
        return "admin-dashboard";
    }

    // ── Student Management ─────────────────────────────────────────────── //

    @GetMapping("/admin/students")
    public String listStudents(@RequestParam(name = "sort", required = false) String sort,
                               Model model) {

        List<Student> students;
        if ("name".equalsIgnoreCase(sort)) {
            students = studentService.getAllStudentsSortedByName();
        } else if ("id".equalsIgnoreCase(sort)) {
            students = studentService.getAllStudentsSortedById();
        } else if ("marks".equalsIgnoreCase(sort)) {
            students = studentService.getAllStudentsSortedByMarks();
        } else {
            students = studentService.getAllStudents();
        }

        Map<String, StudentProfileResponseDTO> profileMap = new HashMap<>();
        for (Student student : students) {
            try {
                profileMap.put(student.getId(), studentProfileService.getProfileForAdmin(student.getId()));
            } catch (IllegalArgumentException ignored) {
                // Keep list rendering resilient even if a specific profile entry fails.
            }
        }

        model.addAttribute("students", students);
        model.addAttribute("profileMap", profileMap);
        model.addAttribute("averageMap", studentService.getAverageMarksMap(students));
        model.addAttribute("newStudent", new Student());
        return "admin-students-react";
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
        return "admin-students-react";
    }

    @PostMapping("/admin/students")
    public String createStudent(@Valid @ModelAttribute("newStudent") Student student) {
        // Courses can be added later via a dedicated screen; here we just persist ID + name
        studentService.save(student);
        return "redirect:/admin/students";
    }

    @GetMapping("/admin/students/delete/{id}")
    public String deleteStudent(@PathVariable("id") String id) {
        studentService.deleteById(id);
        return "redirect:/admin/students";
    }
}
