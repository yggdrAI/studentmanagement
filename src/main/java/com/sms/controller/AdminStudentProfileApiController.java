package com.sms.controller;

import com.sms.dto.profile.AdminUpdateStudentProfileRequest;
import com.sms.dto.profile.StudentProfileResponseDTO;
import com.sms.service.StudentProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/admin/student")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStudentProfileApiController {

    private final StudentProfileService studentProfileService;

    public AdminStudentProfileApiController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @GetMapping("/{studentId}/profile")
    public ResponseEntity<StudentProfileResponseDTO> getStudentProfile(@PathVariable String studentId) {
        return ResponseEntity.ok(studentProfileService.getProfileForAdmin(studentId));
    }

    @PutMapping("/{studentId}/profile")
    public ResponseEntity<StudentProfileResponseDTO> updateStudentProfile(@PathVariable String studentId,
                                                                          @RequestBody AdminUpdateStudentProfileRequest request,
                                                                          Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin authentication is required");
        }
        return ResponseEntity.ok(studentProfileService.updateByAdmin(studentId, request, auth.getName()));
    }
}
