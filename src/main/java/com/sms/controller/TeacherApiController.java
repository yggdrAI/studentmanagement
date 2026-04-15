package com.sms.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sms.dto.dashboard.CreateSubjectRequest;
import com.sms.dto.dashboard.CreateTaskRequest;
import com.sms.dto.dashboard.ScheduleClassRequest;
import com.sms.dto.dashboard.StudentProgressViewDto;
import com.sms.model.Course;
import com.sms.model.TaskItem;
import com.sms.service.DashboardService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherApiController {

    private final DashboardService dashboardService;

    public TeacherApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PostMapping("/subject")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> createSubject(@Valid @RequestBody CreateSubjectRequest request,
                                                             Authentication authentication) {
        Course course = dashboardService.createSubject(authentication.getName(), request);
        return ResponseEntity.ok(Map.of(
                "subjectId", course.getId(),
                "code", course.getCode(),
                "name", course.getCourseName()
        ));
    }

    @PostMapping("/task")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> createTask(@Valid @RequestBody CreateTaskRequest request,
                                                          Authentication authentication) {
        TaskItem taskItem = dashboardService.createTask(authentication.getName(), request);
        return ResponseEntity.ok(Map.of(
                "taskId", taskItem.getId(),
                "title", taskItem.getTitle(),
                "subjectId", taskItem.getCourse().getId()
        ));
    }

    @GetMapping("/subject/{id}/students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<StudentProgressViewDto>> getSubjectStudents(@PathVariable Long id,
                                                                            Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getSubjectProgress(authentication.getName(), id));
    }

    @PostMapping("/class")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> scheduleClass(@Valid @RequestBody ScheduleClassRequest request,
                                                              Authentication authentication) {
        int affectedStudents = dashboardService.scheduleClass(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("scheduledForStudents", affectedStudents));
    }
}
