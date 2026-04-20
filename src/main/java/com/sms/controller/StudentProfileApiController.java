package com.sms.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sms.dto.auth.ChangePasswordRequest;
import com.sms.dto.profile.StudentProfileResponseDTO;
import com.sms.service.CredentialService;
import com.sms.service.StudentProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentProfileApiController {

    private final StudentProfileService studentProfileService;
    private final CredentialService credentialService;

    public StudentProfileApiController(StudentProfileService studentProfileService,
                                       CredentialService credentialService) {
        this.studentProfileService = studentProfileService;
        this.credentialService = credentialService;
    }

    @GetMapping("/profile")
    public ResponseEntity<StudentProfileResponseDTO> getProfile(Authentication auth) {
        return ResponseEntity.ok(studentProfileService.getProfileForStudent(auth.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<StudentProfileResponseDTO> updateOwnProfile() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Profile editing is restricted to admins");
    }

    @PutMapping("/profile/password")
    public ResponseEntity<?> changeOwnPassword(@Valid @RequestBody ChangePasswordRequest request,
                                               Authentication auth) {
        credentialService.changePassword(
                auth.getName(),
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        return ResponseEntity.ok().build();
    }
}
