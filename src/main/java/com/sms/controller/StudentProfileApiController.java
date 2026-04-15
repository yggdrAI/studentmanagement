package com.sms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sms.dto.student.StudentProfileDTO;
import com.sms.service.StudentService;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentProfileApiController {

    private final StudentService studentService;

    public StudentProfileApiController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentProfileDTO> getProfile(Authentication auth) {
        return ResponseEntity.ok(studentService.getStudentProfileByUsername(auth.getName()));
    }
}
