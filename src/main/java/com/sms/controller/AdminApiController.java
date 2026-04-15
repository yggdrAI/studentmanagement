package com.sms.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sms.dto.dashboard.AssignTeacherRequest;
import com.sms.dto.dashboard.EnrollStudentRequest;
import com.sms.model.Course;
import com.sms.model.Enrollment;
import com.sms.service.DashboardService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    private final DashboardService dashboardService;

    public AdminApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PostMapping("/enroll")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> enrollStudent(@Valid @RequestBody EnrollStudentRequest request) {
        Enrollment enrollment;
        try {
            enrollment = dashboardService.enrollStudent(request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }

        return ResponseEntity.ok(Map.of(
                "enrollmentId", enrollment.getId(),
                "studentId", enrollment.getStudent().getId(),
                "subjectId", enrollment.getCourse().getId()
        ));
    }

    @PostMapping("/assign-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> assignTeacher(@Valid @RequestBody AssignTeacherRequest request) {
        Course course = dashboardService.assignTeacher(request);
        return ResponseEntity.ok(Map.of(
                "subjectId", course.getId(),
                "teacherId", course.getTeacher().getId()
        ));
    }
}
