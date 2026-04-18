package com.sms.controller;

import java.util.Objects;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.sms.dto.dashboard.DashboardDTO;
import com.sms.dto.dashboard.TimetableUpdateRequest;
import com.sms.dto.dashboard.UpcomingClassDto;
import com.sms.model.Student;
import com.sms.service.DashboardService;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class DashboardController {

    private final DashboardService dashboardService;
    private final SimpMessagingTemplate messagingTemplate;

    public DashboardController(DashboardService dashboardService,
                               SimpMessagingTemplate messagingTemplate) {
        this.dashboardService = dashboardService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<DashboardDTO> getDashboard(Authentication authentication) {
        Student student = dashboardService.resolveStudentByUsername(authentication.getName());
        return ResponseEntity.ok(dashboardService.buildDashboard(student.getId()));
    }

    @GetMapping("/timetable")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<UpcomingClassDto>> getTimetable(Authentication authentication) {
        Student student = dashboardService.resolveStudentByUsername(authentication.getName());
        return ResponseEntity.ok(dashboardService.getUpcomingClasses(student.getId()));
    }

    @PatchMapping("/timetable/session/{sessionId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> updateTimetableSession(@PathVariable Long sessionId,
                                                    @RequestBody TimetableUpdateRequest request,
                                                    Authentication authentication) {
        Student student = dashboardService.resolveStudentByUsername(authentication.getName());

        LocalDateTime startsAt = parseDateTime(request.getStartsAt());
        LocalDateTime endsAt = parseDateTime(request.getEndsAt());

        try {
            return ResponseEntity.ok(
                    dashboardService.rescheduleStudentSession(student.getId(), sessionId, startsAt, endsAt)
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/task/{taskId}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<DashboardDTO> completeTask(@PathVariable Long taskId,
                                                     Authentication authentication) {
        Student student = dashboardService.resolveStudentByUsername(authentication.getName());
        dashboardService.markTaskCompleted(student.getId(), taskId);
        DashboardDTO updatedDashboard = dashboardService.buildDashboard(student.getId());
        messagingTemplate.convertAndSendToUser(
                Objects.requireNonNull(authentication.getName(), "Authenticated username is required"),
                "/queue/dashboard",
                Objects.requireNonNull(updatedDashboard, "Updated dashboard payload is required")
        );
        return ResponseEntity.ok(updatedDashboard);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Date-time value is required");
        }

        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(value);
        }
    }
}
