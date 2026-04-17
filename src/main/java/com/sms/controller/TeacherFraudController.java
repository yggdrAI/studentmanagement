package com.sms.controller;

import com.sms.service.FraudDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/fraud")
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class TeacherFraudController {

    private final FraudDetectionService fraudDetectionService;

    public TeacherFraudController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        FraudDetectionService.FraudSummary summary = fraudDetectionService.getSummary();
        Map<String, Object> response = new HashMap<>();
        response.put("approved", summary.getApproved());
        response.put("suspicious", summary.getSuspicious());
        response.put("rejected", summary.getRejected());
        response.put("total", summary.getApproved() + summary.getSuspicious() + summary.getRejected());
        return ResponseEntity.ok(response);
    }
}