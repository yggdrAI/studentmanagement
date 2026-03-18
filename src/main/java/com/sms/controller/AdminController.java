package com.sms.controller;

import com.sms.model.Student;
import com.sms.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AdminController {

    private final StudentService studentService;

    public AdminController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
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

        model.addAttribute("students", students);
        model.addAttribute("newStudent", new Student());
        return "admin-dashboard";
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
