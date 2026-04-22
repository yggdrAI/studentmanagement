package com.sms.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sms.dto.auth.ChangePasswordRequest;
import com.sms.dto.dashboard.CreateSubjectRequest;
import com.sms.dto.dashboard.CreateTaskRequest;
import com.sms.dto.dashboard.ScheduleClassRequest;
import com.sms.dto.dashboard.StudentProgressViewDto;
import com.sms.dto.publication.PublicationResponse;
import com.sms.model.Course;
import com.sms.model.TaskItem;
import com.sms.model.Teacher;
import com.sms.model.TeacherProfile;
import com.sms.repository.TeacherProfileRepository;
import com.sms.service.AcademicPublicationService;
import com.sms.service.CredentialService;
import com.sms.service.DashboardService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherApiController {

    private final DashboardService dashboardService;
    private final CredentialService credentialService;
    private final TeacherProfileRepository teacherProfileRepository;
    private final AcademicPublicationService academicPublicationService;

    public TeacherApiController(DashboardService dashboardService,
                                CredentialService credentialService,
                                TeacherProfileRepository teacherProfileRepository,
                                AcademicPublicationService academicPublicationService) {
        this.dashboardService = dashboardService;
        this.credentialService = credentialService;
        this.teacherProfileRepository = teacherProfileRepository;
        this.academicPublicationService = academicPublicationService;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Map<String, Object>> getTeacherProfile(Authentication authentication) {
        Teacher teacher = dashboardService.resolveTeacherByUsername(authentication.getName());
        TeacherProfile profile = teacherProfileRepository.findByTeacherId(teacher.getId()).orElse(null);
        return ResponseEntity.ok(Map.of(
                "id", teacher.getId(),
                "fullName", teacher.getName() == null ? "" : teacher.getName(),
                "email", teacher.getEmail() == null ? "" : teacher.getEmail(),
                "employeeId", teacher.getEmployeeId() == null ? "" : teacher.getEmployeeId(),
                "department", teacher.getDepartment() == null ? "" : teacher.getDepartment(),
                "designation", teacher.getDesignation() == null ? "" : teacher.getDesignation(),
                "phone", teacher.getPhone() == null ? "" : teacher.getPhone(),
                "profileImage", profile != null && profile.getProfileImage() != null ? profile.getProfileImage() : "",
                "profilePhotoUrl", profile != null && profile.getProfilePhotoUrl() != null ? profile.getProfilePhotoUrl() : ""
        ));
    }

    @GetMapping("/publications")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<PublicationResponse>> getTeacherPublications(Authentication authentication) {
        return ResponseEntity.ok(academicPublicationService.getTeacherPublications(authentication.getName()));
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

    @PutMapping("/profile/password")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> changeOwnPassword(@Valid @RequestBody ChangePasswordRequest request,
                                               Authentication authentication) {
        credentialService.changePassword(
                authentication.getName(),
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        return ResponseEntity.ok().build();
    }
}
