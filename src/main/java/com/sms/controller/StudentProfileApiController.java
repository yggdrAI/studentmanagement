package com.sms.controller;

import com.sms.dto.profile.StudentProfileResponseDTO;
import com.sms.service.StudentProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentProfileApiController {

    private final StudentProfileService studentProfileService;

    public StudentProfileApiController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @GetMapping("/profile")
    public ResponseEntity<StudentProfileResponseDTO> getProfile(Authentication auth) {
        return ResponseEntity.ok(studentProfileService.getProfileForStudent(auth.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<StudentProfileResponseDTO> updateOwnProfile() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Profile editing is restricted to admins");
    }
}
