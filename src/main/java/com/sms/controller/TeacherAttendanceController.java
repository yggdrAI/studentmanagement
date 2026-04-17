package com.sms.controller;

import com.sms.dto.attendance.*;
import com.sms.model.Course;
import com.sms.model.Teacher;
import com.sms.service.AttendanceQRTokenService;
import com.sms.service.AttendanceService;
import com.sms.service.DashboardService;
import com.sms.model.Attendance;
import com.sms.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * API endpoints for teacher attendance management
 * Endpoints for generating QR codes and manual attendance
 */
@RestController
@RequestMapping("/api/teacher/attendance")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherAttendanceController {

    @Autowired
    private AttendanceQRTokenService qrTokenService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private CourseRepository courseRepository;

    private Teacher resolveTeacher(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new IllegalArgumentException("Unauthenticated teacher request");
        }
        return dashboardService.resolveTeacherByUsername(auth.getName());
    }

    private Course resolveTeacherCourse(Authentication auth, Long subjectId) {
        Teacher teacher = resolveTeacher(auth);
        Course course = courseRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
        if (course.getTeacher() == null || !course.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Subject does not belong to authenticated teacher");
        }
        return course;
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<Map<String, Object>>> getTeacherSubjects(Authentication auth) {
        Teacher teacher = resolveTeacher(auth);
        List<Map<String, Object>> payload = new ArrayList<>();
        for (Course course : courseRepository.findByTeacherId(teacher.getId())) {
            Map<String, Object> item = new HashMap<>();
            item.put("subjectId", course.getId());
            item.put("subjectCode", course.getCode());
            item.put("subjectName", course.getCourseName());
            payload.add(item);
        }
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/subject/{subjectId}/students")
    public ResponseEntity<List<Map<String, Object>>> getSubjectStudents(@PathVariable Long subjectId,
                                                                        Authentication auth) {
        resolveTeacherCourse(auth, subjectId);
        List<Map<String, Object>> payload = new ArrayList<>();
        dashboardService.getSubjectProgress(auth.getName(), subjectId).forEach(student -> {
            Map<String, Object> item = new HashMap<>();
            item.put("studentId", student.getStudentId());
            item.put("studentName", student.getStudentName());
            item.put("progressPercent", student.getProgressPercent());
            payload.add(item);
        });
        return ResponseEntity.ok(payload);
    }

    /**
     * Generate QR code for attendance
     * Teacher initiates this to start attendance marking for a class
     * 
     * POST /api/teacher/attendance/generate-qr
     */
    @PostMapping("/generate-qr")
    public ResponseEntity<AttendanceQRResponse> generateAttendanceQR(
            @RequestBody GenerateAttendanceQRRequest request,
            Authentication auth) {
        try {
            Course course = resolveTeacherCourse(auth, request.getSubjectId());
            Long teacherId = course.getTeacher().getId();
            String subjectName = course.getCourseName();
            
            // Generate JWT token
            String qrToken = qrTokenService.generateAttendanceToken(
                request.getSubjectId(),
                teacherId,
                request.getExpiryMinutes()
            );

            // Generate QR code image
            String qrImageBase64 = qrTokenService.generateQRCodeBase64(qrToken);
            
            // Calculate expiry
            long expiryMs = request.getExpiryMinutes() * 60 * 1000;
            long expiresAt = System.currentTimeMillis() + expiryMs;
            Integer expirySeconds = request.getExpiryMinutes() * 60;
            
            String sessionId = UUID.randomUUID().toString();

            AttendanceQRResponse response = new AttendanceQRResponse(
                qrToken,
                qrImageBase64,
                request.getSubjectId(),
                subjectName,
                expiresAt,
                expirySeconds,
                sessionId
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mark attendance manually for multiple students
     * 
     * POST /api/teacher/attendance/manual
     */
    @PostMapping("/manual")
    public ResponseEntity<Map<String, Object>> markManualAttendance(
            @RequestBody ManualAttendanceRequest request,
            Authentication auth) {
        try {
            Course course = resolveTeacherCourse(auth, request.getSubjectId());
            Long teacherId = course.getTeacher().getId();
            
            LocalDate attendanceDate = LocalDate.parse(
                request.getAttendanceDate(),
                DateTimeFormatter.ISO_LOCAL_DATE
            );

            // Convert request records to service records
            List<AttendanceService.ManualAttendanceRecord> records = new ArrayList<>();
            for (var record : request.getAttendanceRecords()) {
                records.add(new AttendanceService.ManualAttendanceRecord(
                    record.getStudentId(),
                    record.getStatus()
                ));
            }

            // Mark attendance
            attendanceService.markManualAttendance(
                request.getSubjectId(),
                teacherId,
                attendanceDate,
                records
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Attendance marked successfully");
            response.put("count", records.size());
            response.put("date", attendanceDate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get real-time attendance count for current session
     * Shows how many students have scanned
     * 
     * GET /api/teacher/attendance/session-stats?subjectId=1&date=2026-04-16
     */
    @GetMapping("/session-stats")
    public ResponseEntity<Map<String, Object>> getSessionStats(
            @RequestParam Long subjectId,
            @RequestParam(required = false) String date,
            Authentication auth) {
        try {
            resolveTeacherCourse(auth, subjectId);
            LocalDate attendanceDate = date != null ? 
                LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE) : 
                LocalDate.now();

            List<Attendance> records = attendanceService.getAttendanceForDate(subjectId, attendanceDate);
            AttendanceService.AttendanceStats stats = 
                attendanceService.getAttendanceStats(subjectId, attendanceDate);

            long locationVerifiedCount = records.stream()
                .filter(record -> Boolean.TRUE.equals(record.getLocationVerified()))
                .count();

            Map<String, Object> response = new HashMap<>();
            response.put("date", attendanceDate);
            response.put("subjectId", subjectId);
            response.put("presentCount", stats.getPresent());
            response.put("absentCount", stats.getAbsent());
            response.put("lateCount", stats.getLate());
            response.put("totalExpected", stats.getTotal());
            response.put("percentage", String.format("%.2f%%", stats.getPercentage()));
            response.put("locationVerifiedCount", locationVerifiedCount);
            response.put("locationVerificationRate", String.format("%.2f%%", (locationVerifiedCount * 100.0) / Math.max(records.size(), 1)));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get class attendance records for a date
     * 
     * GET /api/teacher/attendance/records?subjectId=1&date=2026-04-16
     */
    @GetMapping("/records")
    public ResponseEntity<List<Map<String, Object>>> getAttendanceRecords(
            @RequestParam Long subjectId,
            @RequestParam(required = false) String date,
            Authentication auth) {
        try {
            resolveTeacherCourse(auth, subjectId);
            LocalDate attendanceDate = date != null ? 
                LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE) : 
                LocalDate.now();

            List<Attendance> records = attendanceService.getAttendanceForDate(subjectId, attendanceDate);

            List<Map<String, Object>> response = new ArrayList<>();
            for (Attendance record : records) {
                Map<String, Object> item = new HashMap<>();
                item.put("studentId", record.getStudentId());
                item.put("status", record.getStatus());
                item.put("markedTime", record.getMarkedTime());
                item.put("markingType", record.getMarkingType());
                response.add(item);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Verify QR token validity
     * Can be called to check if QR is still valid
     * 
     * GET /api/teacher/attendance/verify-qr?token=...
     */
    @GetMapping("/verify-qr")
    public ResponseEntity<Map<String, Object>> verifyQR(@RequestParam String token) {
        try {
            AttendanceQRTokenService.AttendanceTokenClaims claims = 
                qrTokenService.validateAttendanceToken(token);

            if (qrTokenService.isTokenExpired(claims.getExpiresAt())) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("message", "QR has expired");
                return ResponseEntity.ok(response);
            }

            long remainingSeconds = qrTokenService.getRemainingValidity(claims.getExpiresAt());

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("subjectId", claims.getSubjectId());
            response.put("teacherId", claims.getTeacherId());
            response.put("sessionId", claims.getSessionId());
            response.put("remainingSeconds", remainingSeconds);
            response.put("expiresAt", new Date(claims.getExpiresAt()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
