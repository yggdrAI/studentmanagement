package com.sms.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sms.dto.publication.PublicationCreateRequest;
import com.sms.dto.publication.PublicationResponse;
import com.sms.service.AcademicPublicationService;

@RestController
@RequestMapping("/api/admin/publications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPublicationController {

    private final AcademicPublicationService academicPublicationService;

    public AdminPublicationController(AcademicPublicationService academicPublicationService) {
        this.academicPublicationService = academicPublicationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> publish(@RequestBody(required = false) PublicationCreateRequest request,
                                                       Authentication authentication) {
        List<PublicationResponse> publications = academicPublicationService.publish(
            authentication == null ? "ADMIN" : authentication.getName(),
            request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "count", publications.size(),
            "items", publications
        ));
    }

    @GetMapping
    public ResponseEntity<List<PublicationResponse>> listAdminPublications() {
        return ResponseEntity.ok(academicPublicationService.getAdminPublications());
    }
}
