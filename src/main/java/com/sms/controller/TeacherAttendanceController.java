package com.sms.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sms.dto.attendance.AttendanceQRResponse;
import com.sms.dto.attendance.GenerateAttendanceQRRequest;
import com.sms.dto.attendance.ManualAttendanceRequest;
import com.sms.model.Attendance;
import com.sms.model.Course;
import com.sms.model.Enrollment;
import com.sms.model.Teacher;
import com.sms.repository.CourseRepository;
import com.sms.repository.EnrollmentRepository;
import com.sms.repository.StudentRepository;
import com.sms.service.AttendanceQRTokenService;
import com.sms.service.AttendanceService;
import com.sms.service.DashboardService;

import jakarta.servlet.http.HttpServletRequest;

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

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

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

    private Long resolveTenantId(HttpServletRequest request) {
        Object tenantAttr = request.getAttribute("tenantId");
        if (tenantAttr instanceof Number number) {
            return number.longValue();
        }

        String tenantHeader = request.getHeader("X-Tenant-Id");
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            try {
                return Long.valueOf(tenantHeader);
            } catch (NumberFormatException ignored) {
                return 1L;
            }
        }
        return 1L;
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<Map<String, Object>>> getTeacherSubjects(Authentication auth) {
        Teacher teacher = resolveTeacher(auth);
        List<Course> teacherCourses = courseRepository.findByTeacherId(teacher.getId());
        if (teacherCourses.stream().noneMatch(course -> course.getCourseName() != null
                && course.getCourseName().equalsIgnoreCase("Java"))) {
            Course javaCourse = createDemoJavaCourse(teacher);
            teacherCourses = new ArrayList<>(teacherCourses);
            teacherCourses.add(javaCourse);
        }

        List<Map<String, Object>> payload = new ArrayList<>();
        for (Course course : teacherCourses) {
            Map<String, Object> item = new HashMap<>();
            item.put("subjectId", course.getId());
            item.put("subjectCode", course.getCode());
            item.put("subjectName", course.getCourseName());
            payload.add(item);
        }
        return ResponseEntity.ok(payload);
    }

    private Course createDemoJavaCourse(Teacher teacher) {
        Course javaCourse = new Course();
        javaCourse.setCourseName("Java");
        javaCourse.setCode(resolveUniqueJavaCode(teacher));
        javaCourse.setCredits(3);
        javaCourse.setTeacher(teacher);
        javaCourse = courseRepository.save(javaCourse);

        for (var student : studentRepository.findAll()) {
            if (student.getId() == null || student.getId().isBlank()) {
                continue;
            }
            if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), javaCourse.getId())) {
                continue;
            }
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(javaCourse);
            enrollment.setMarks(0.0);
            enrollmentRepository.save(enrollment);
        }

        return javaCourse;
    }

    private String resolveUniqueJavaCode(Teacher teacher) {
        String base = "JAVA";
        if (courseRepository.findByCode(base).isEmpty()) {
            return base;
        }

        String teacherCode = base + "-" + teacher.getId();
        if (courseRepository.findByCode(teacherCode).isEmpty()) {
            return teacherCode;
        }

        for (int suffix = 2; suffix <= 50; suffix++) {
            String candidate = teacherCode + "-" + suffix;
            if (courseRepository.findByCode(candidate).isEmpty()) {
                return candidate;
            }
        }

        return teacherCode + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
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
            int expirySeconds = resolveExpirySeconds(request);
            Integer maxDistanceMeters = request.getMaxDistanceMeters() == null
                ? 150
                : Math.max(100, Math.min(200, request.getMaxDistanceMeters()));

            // Always allow QR generation, skip location requirement
            Double teacherLat = request.getTeacherLatitude();
            Double teacherLng = request.getTeacherLongitude();
            if (teacherLat == null || teacherLng == null) {
            teacherLat = null;
            teacherLng = null;
            }

            String qrToken = qrTokenService.generateAttendanceToken(
                request.getSubjectId(),
                teacherId,
                expirySeconds,
                teacherLat,
                teacherLng,
                maxDistanceMeters);

            String qrImageBase64 = qrTokenService.generateQRCodeBase64(qrToken);
            long expiryMs = (long) expirySeconds * 1000L;
            long expiresAt = System.currentTimeMillis() + expiryMs;
            String sessionId = UUID.randomUUID().toString();

            AttendanceQRResponse response = new AttendanceQRResponse(
                qrToken,
                qrImageBase64,
                request.getSubjectId(),
                subjectName,
                expiresAt,
                expirySeconds,
                sessionId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private int resolveExpirySeconds(GenerateAttendanceQRRequest request) {
        boolean dynamicMode = false;
        if (request != null) {
            dynamicMode = Boolean.TRUE.equals(request.getDynamicQr())
                    || (request.getQrMode() != null && "dynamic".equalsIgnoreCase(request.getQrMode()));
        }
        if (dynamicMode) {
            return 10;
        }
        if (request != null && request.getExpirySeconds() != null && request.getExpirySeconds() > 0) {
            return Math.max(8, Math.min(7200, request.getExpirySeconds()));
        }
        if (request != null && request.getExpiryMinutes() != null && request.getExpiryMinutes() > 0) {
            int fromMinutes = request.getExpiryMinutes() * 60;
            return Math.max(8, Math.min(7200, fromMinutes));
        }
        return 10;
    }

    /**
     * Mark attendance manually for multiple students
     * 
     * POST /api/teacher/attendance/manual
     */
    @PostMapping("/manual")
    public ResponseEntity<Map<String, Object>> markManualAttendance(
            @RequestBody ManualAttendanceRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {
        try {
            Course course = resolveTeacherCourse(auth, request.getSubjectId());
            Long teacherId = course.getTeacher().getId();
            Long tenantId = resolveTenantId(httpRequest);

            LocalDate attendanceDate = LocalDate.parse(
                    request.getAttendanceDate(),
                    DateTimeFormatter.ISO_LOCAL_DATE);

            // Convert request records to service records
            List<AttendanceService.ManualAttendanceRecord> records = new ArrayList<>();
            for (var record : request.getAttendanceRecords()) {
                records.add(new AttendanceService.ManualAttendanceRecord(
                        record.getStudentId(),
                        record.getStatus()));
            }

            // Mark attendance
            attendanceService.markManualAttendance(
                    request.getSubjectId(),
                    teacherId,
                    attendanceDate,
                    records,
                    tenantId);

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
            Authentication auth,
            HttpServletRequest httpRequest) {
        try {
            resolveTeacherCourse(auth, subjectId);
            Long tenantId = resolveTenantId(httpRequest);
            LocalDate attendanceDate = date != null ? LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
                    : LocalDate.now();

            List<Attendance> records = attendanceService.getAttendanceForDate(subjectId, attendanceDate, tenantId);
            AttendanceService.AttendanceStats stats = attendanceService.getAttendanceStats(subjectId, attendanceDate,
                    tenantId);

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
            response.put("locationVerificationRate",
                    String.format("%.2f%%", (locationVerifiedCount * 100.0) / Math.max(records.size(), 1)));

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
            Authentication auth,
            HttpServletRequest httpRequest) {
        try {
            resolveTeacherCourse(auth, subjectId);
            Long tenantId = resolveTenantId(httpRequest);
            LocalDate attendanceDate = date != null ? LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
                    : LocalDate.now();

            List<Attendance> records = attendanceService.getAttendanceForDate(subjectId, attendanceDate, tenantId);

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
            AttendanceQRTokenService.AttendanceTokenClaims claims = qrTokenService.validateAttendanceToken(token);

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