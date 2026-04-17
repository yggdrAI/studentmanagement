package com.sms.controller;

import com.sms.model.StudentLocation;
import com.sms.service.CampusTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campus")
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
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

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String sessionId) {
        List<StudentLocation> locations = campusTrackingService.getRecentLocations(subjectId, sessionId, null, 200);
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
}