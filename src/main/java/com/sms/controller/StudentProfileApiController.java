package com.sms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sms.dto.auth.ChangePasswordRequest;
import com.sms.dto.publication.PublicationResponse;
import com.sms.dto.profile.StudentDemographicConsentRequest;
import com.sms.dto.profile.StudentProfileResponseDTO;
import com.sms.dto.profile.StudentSelfUpdateProfileRequest;
import com.sms.service.AcademicPublicationService;
import com.sms.service.CredentialService;
import com.sms.service.StudentProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentProfileApiController {

    private final StudentProfileService studentProfileService;
    private final CredentialService credentialService;
    private final AcademicPublicationService academicPublicationService;

    public StudentProfileApiController(StudentProfileService studentProfileService,
                                       CredentialService credentialService,
                                       AcademicPublicationService academicPublicationService) {
        this.studentProfileService = studentProfileService;
        this.credentialService = credentialService;
        this.academicPublicationService = academicPublicationService;
    }

    @GetMapping("/profile")
    public ResponseEntity<StudentProfileResponseDTO> getProfile(Authentication auth) {
        return ResponseEntity.ok(studentProfileService.getProfileForStudent(auth.getName()));
    }

    @GetMapping("/publications")
    public ResponseEntity<java.util.List<PublicationResponse>> getPublications(Authentication auth) {
        return ResponseEntity.ok(academicPublicationService.getStudentPublications(auth.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<StudentProfileResponseDTO> updateOwnProfile(@RequestBody StudentSelfUpdateProfileRequest request,
                                                                      Authentication auth) {
        return ResponseEntity.ok(studentProfileService.updateByStudent(auth.getName(), request));
    }

    @PutMapping("/profile/demographics")
    public ResponseEntity<StudentProfileResponseDTO> submitDemographics(@Valid @RequestBody StudentDemographicConsentRequest request,
                                                                         Authentication auth) {
        return ResponseEntity.ok(studentProfileService.submitDemographicConsent(auth.getName(), request));
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
