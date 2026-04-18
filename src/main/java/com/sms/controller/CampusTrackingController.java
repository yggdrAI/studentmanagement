package com.sms.controller;

import com.sms.model.StudentLocation;
import com.sms.service.CampusTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campus")
@PreAuthorize("hasAnyRole('TEACHER','ADMIN','STUDENT')")
public class CampusTrackingController {

    private final CampusTrackingService campusTrackingService;

    public CampusTrackingController(CampusTrackingService campusTrackingService) {
        this.campusTrackingService = campusTrackingService;
    }

    @GetMapping("/live")
    public ResponseEntity<List<StudentLocation>> getLiveLocations(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "200") int limit) {
        return ResponseEntity.ok(campusTrackingService.getRecentLocations(subjectId, sessionId, status, limit));
    }

    @GetMapping("/live-map")
    public ResponseEntity<List<Map<String, Object>>> getLiveMapLocations(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "200") int limit,
            Authentication authentication) {
        boolean studentView = hasRole(authentication, "ROLE_STUDENT")
                && !hasRole(authentication, "ROLE_ADMIN")
                && !hasRole(authentication, "ROLE_TEACHER");

        List<Map<String, Object>> rows = campusTrackingService
                .getRecentLocations(subjectId, sessionId, status, limit)
                .stream()
                .map(location -> toMapRow(location, studentView))
                .toList();
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "200") int limit) {
        List<StudentLocation> locations = campusTrackingService.getRecentLocations(subjectId, sessionId, status, limit);
        long verified = locations.stream().filter(location -> Boolean.TRUE.equals(location.getLocationVerified())).count();
        long suspicious = locations.stream().filter(location -> Boolean.TRUE.equals(location.getSuspicious())).count();

        Map<String, Object> response = new HashMap<>();
        response.put("total", locations.size());
        response.put("verified", verified);
        response.put("suspicious", suspicious);
        response.put("subjectId", subjectId);
        response.put("sessionId", sessionId);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toMapRow(StudentLocation location, boolean studentView) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", location.getId());
        row.put("studentId", studentView ? anonymize(location.getStudentId()) : location.getStudentId());
        row.put("subjectId", location.getSubjectId());
        row.put("sessionId", location.getSessionId());
        row.put("attendanceId", location.getAttendanceId());
        row.put("latitude", location.getLatitude());
        row.put("longitude", location.getLongitude());
        row.put("locationVerified", location.getLocationVerified());
        row.put("suspicious", location.getSuspicious());
        row.put("locationConfidence", location.getLocationConfidence());
        row.put("recordedAt", location.getRecordedAt());
        return row;
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.equalsIgnoreCase(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private String anonymize(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return "Student #0000";
        }
        int hash = Math.abs(studentId.hashCode() % 10000);
        return String.format("Student #%04d", hash);
    }
}