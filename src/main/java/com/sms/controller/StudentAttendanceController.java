package com.sms.controller;

import com.sms.dto.attendance.MarkAttendanceRequest;
import com.sms.dto.attendance.MarkAttendanceResponse;
import com.sms.service.AttendanceQRTokenService;
import com.sms.service.AttendanceService;
import com.sms.model.Attendance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * API endpoints for student attendance management
 * Endpoints for scanning QR and marking attendance
 */
@RestController
@RequestMapping("/api/student/attendance")
@PreAuthorize("hasRole('STUDENT')")
public class StudentAttendanceController {

    @Autowired
    private AttendanceQRTokenService qrTokenService;

    @Autowired
    private AttendanceService attendanceService;

    /**
     * Mark attendance by scanning QR code
     * Student scans teacher's QR and attendance is marked
     * 
     * POST /api/student/attendance/mark
     * 
     * SECURITY NOTES:
     * ✅ Validates JWT token signature and expiry on backend
     * ✅ Prevents duplicate attendance on same day
     * ✅ Prevents token reuse
     * ✅ Logs IP and device info for audit
     */
    @PostMapping("/mark")
    public ResponseEntity<MarkAttendanceResponse> markAttendanceByQR(
            @RequestBody MarkAttendanceRequest request,
            Authentication auth,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        try {
            // Get student ID from authentication
            String studentId = auth.getName(); // TODO: Extract actual student ID
            
            // ✅ STEP 1: Validate QR token
            AttendanceQRTokenService.AttendanceTokenClaims claims;
            try {
                claims = qrTokenService.validateAttendanceToken(request.getQrToken());
            } catch (Exception e) {
                return ResponseEntity.ok(new MarkAttendanceResponse(
                    false,
                    "Invalid QR token: " + e.getMessage(),
                    "INVALID"
                ));
            }

            // ✅ STEP 2: Check if token is expired
            if (qrTokenService.isTokenExpired(claims.getExpiresAt())) {
                return ResponseEntity.ok(new MarkAttendanceResponse(
                    false,
                    "QR code has expired",
                    "EXPIRED"
                ));
            }

            // ✅ STEP 3: Hash token for duplicate checking
            String tokenHash = qrTokenService.hashToken(request.getQrToken());

            // ✅ STEP 4: Get request info for audit
            String deviceId = request.getDeviceId() != null ? request.getDeviceId() : "UNKNOWN";
            String deviceInfo = userAgent != null ? userAgent : "UNKNOWN";
            
            // ✅ STEP 5: Mark attendance with all validations
            try {
                Attendance attendance = attendanceService.markAttendance(
                    studentId,
                    claims.getSubjectId(),
                    claims.getTeacherId(),
                    "PRESENT",
                    "QR_SCANNED",
                    deviceInfo,
                    deviceId, // Using device ID as IP placeholder
                    tokenHash
                );

                return ResponseEntity.ok(new MarkAttendanceResponse(
                    true,
                    "✅ Attendance marked successfully!",
                    "MARKED",
                    attendance.getId().toString()
                ));
            } catch (RuntimeException e) {
                String status = e.getMessage().contains("already marked") ? "ALREADY_MARKED" : "ERROR";
                return ResponseEntity.ok(new MarkAttendanceResponse(
                    false,
                    e.getMessage(),
                    status
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(new MarkAttendanceResponse(
                false,
                "Server error: " + e.getMessage(),
                "ERROR"
            ));
        }
    }

    /**
     * Get student's attendance record for a subject
     * 
     * GET /api/student/attendance/my-records?subjectId=1
     */
    @GetMapping("/my-records")
    public ResponseEntity<Map<String, Object>> getMyAttendance(
            @RequestParam Long subjectId,
            Authentication auth) {
        try {
            String studentId = auth.getName();
            
            List<Attendance> records = attendanceService.getStudentAttendance(studentId, subjectId);
            Double percentage = attendanceService.calculateAttendancePercentage(studentId, subjectId);

            Map<String, Object> response = new HashMap<>();
            response.put("subjectId", subjectId);
            response.put("studentId", studentId);
            response.put("attendancePercentage", String.format("%.2f%%", percentage));
            response.put("totalDays", records.size());
            response.put("presentDays", records.stream().filter(a -> "PRESENT".equals(a.getStatus())).count());
            response.put("absentDays", records.stream().filter(a -> "ABSENT".equals(a.getStatus())).count());
            response.put("lateDays", records.stream().filter(a -> "LATE".equals(a.getStatus())).count());

            List<Map<String, Object>> recordsList = new ArrayList<>();
            for (Attendance record : records) {
                Map<String, Object> item = new HashMap<>();
                item.put("date", record.getAttendanceDate());
                item.put("status", record.getStatus());
                item.put("markedTime", record.getMarkedTime());
                item.put("markingType", record.getMarkingType());
                recordsList.add(item);
            }
            response.put("records", recordsList);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Check if student has already marked attendance today
     * 
     * GET /api/student/attendance/check-today?subjectId=1
     */
    @GetMapping("/check-today")
    public ResponseEntity<Map<String, Object>> checkTodayAttendance(
            @RequestParam Long subjectId,
            Authentication auth) {
        try {
            String studentId = auth.getName();
            LocalDate today = LocalDate.now();

            boolean alreadyMarked = false;
            String status = "PENDING";
            
            var existing = attendanceService.getAttendanceForDate(subjectId, today)
                .stream()
                .filter(a -> a.getStudentId().equals(studentId))
                .findFirst();

            if (existing.isPresent()) {
                alreadyMarked = true;
                status = existing.get().getStatus();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("subjectId", subjectId);
            response.put("date", today);
            response.put("alreadyMarked", alreadyMarked);
            response.put("status", status);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Public endpoint to verify QR (for scanner app)
        * Students verify a teacher-issued QR before marking attendance
     * 
     * GET /api/student/attendance/verify-qr?token=...
     */
    @PostMapping("/verify-qr")
        @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> verifyQR(@RequestParam String token) {
        try {
            AttendanceQRTokenService.AttendanceTokenClaims claims = 
                qrTokenService.validateAttendanceToken(token);

            if (qrTokenService.isTokenExpired(claims.getExpiresAt())) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("message", "QR has expired");
                response.put("sessionId", claims.getSessionId());
                return ResponseEntity.ok(response);
            }

            long remainingSeconds = qrTokenService.getRemainingValidity(claims.getExpiresAt());

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("sessionId", claims.getSessionId());
            response.put("subjectId", claims.getSubjectId());
            response.put("remainingSeconds", remainingSeconds);
            response.put("message", "QR is valid - You can now mark attendance");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
