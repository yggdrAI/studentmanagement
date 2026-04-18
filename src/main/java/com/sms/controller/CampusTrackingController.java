package com.sms.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sms.model.StudentLocation;
import com.sms.service.BehaviorAIService;
import com.sms.service.CampusTrackingService;

@RestController
@RequestMapping({"/api/teacher/campus", "/api/admin/campus"})
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class CampusTrackingController {

    private final CampusTrackingService campusTrackingService;
    private final BehaviorAIService behaviorAIService;

    public CampusTrackingController(CampusTrackingService campusTrackingService,
                                    BehaviorAIService behaviorAIService) {
        this.campusTrackingService = campusTrackingService;
        this.behaviorAIService = behaviorAIService;
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
            @RequestParam(defaultValue = "200") int limit) {

        List<Map<String, Object>> rows = campusTrackingService
                .getRecentLocations(subjectId, sessionId, status, limit)
                .stream()
            .map(location -> toMapRow(location, false))
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

    @GetMapping("/behavior-analysis")
    public ResponseEntity<Map<String, Object>> getBehaviorAnalysis(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "120") int limit) {
        List<StudentLocation> locations = campusTrackingService.getRecentLocations(subjectId, sessionId, null, limit);
        return ResponseEntity.ok(behaviorAIService.analyzeLocations(locations));
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

    private String anonymize(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return "Student #0000";
        }
        int hash = Math.abs(studentId.hashCode() % 10000);
        return String.format("Student #%04d", hash);
    }
}