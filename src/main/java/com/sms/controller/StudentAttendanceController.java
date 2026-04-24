package com.sms.controller;

import com.sms.dto.attendance.MarkAttendanceRequest;
import com.sms.dto.attendance.MarkAttendanceResponse;
import com.sms.dto.attendance.FaceRegistrationRequest;
import com.sms.model.CampusLocation;
import com.sms.service.AttendanceQRTokenService;
import com.sms.service.AttendanceService;
import com.sms.service.AntiCheatingService;
import com.sms.service.CampusTrackingService;
import com.sms.service.FraudDetectionService;
import com.sms.service.FaceVerificationService;
import com.sms.service.GeolocationService;
import com.sms.model.Attendance;
import com.sms.model.Student;
import com.sms.repository.CampusLocationRepository;
import com.sms.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
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

    @Autowired
    private CampusLocationRepository campusLocationRepository;

    @Autowired
    private GeolocationService geolocationService;

    @Autowired
    private AntiCheatingService antiCheatingService;

    @Autowired
    private FaceVerificationService faceVerificationService;

    @Autowired
    private CampusTrackingService campusTrackingService;

    @Autowired
    private FraudDetectionService fraudDetectionService;

    @Autowired
    private StudentRepository studentRepository;

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
            @RequestBody MarkAttendanceRequest markRequest,
            Authentication auth,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        try {
            String studentId = resolveAuthenticatedStudentId(auth);
            Long tenantId = resolveTenantId(httpRequest);

            // ✅ STEP 1: Validate QR token
            AttendanceQRTokenService.AttendanceTokenClaims claims;
            try {
                claims = qrTokenService.validateAttendanceToken(markRequest.getQrToken());
            } catch (Exception e) {
                return ResponseEntity.ok(new MarkAttendanceResponse(
                        false,
                        "Invalid QR token: " + e.getMessage(),
                        "INVALID"));
            }

            // ✅ STEP 2: Check if token is expired
            Long detectedAt = markRequest.getQrDetectedAtEpochMs();
            if (qrTokenService.isTokenExpired(claims.getExpiresAt())
                    && !wasScannedBeforeExpiry(detectedAt, claims.getExpiresAt())) {
                return ResponseEntity.ok(new MarkAttendanceResponse(
                        false,
                        "QR code has expired",
                        "EXPIRED"));
            }

            // ✅ STEP 3: Hash token for duplicate checking
            String tokenHash = qrTokenService.hashToken(markRequest.getQrToken());

            Double latitude = markRequest.getLatitude();
            Double longitude = markRequest.getLongitude();
            if (latitude == null || longitude == null) {
                return ResponseEntity.ok(new MarkAttendanceResponse(
                        false,
                        "Location access is required to mark attendance",
                        "LOCATION_REQUIRED"));
            }

            if (detectedAt == null || Math.abs(System.currentTimeMillis() - detectedAt) > 30_000L) {
                return ResponseEntity.ok(new MarkAttendanceResponse(
                        false,
                        "QR scan is stale. Please scan the latest QR code and retry.",
                        "QR_STALE"));
            }

            // ✅ STEP 4: Get request info for audit
            String clientIp = extractClientIp(httpRequest);
            String clientDeviceId = markRequest.getDeviceId() != null ? markRequest.getDeviceId() : "UNKNOWN";
            String deviceInfo = (userAgent != null ? userAgent : "UNKNOWN") + " | clientDeviceId=" + clientDeviceId;
            String deviceFingerprint = antiCheatingService.generateDeviceFingerprint(deviceInfo, clientIp);
            boolean faceVerificationRequired = !Boolean.FALSE.equals(claims.getFaceVerificationRequired());

            // ✅ STEP 4.2: Verify proximity to teacher-issued QR location (100-200m policy)
            Double teacherLatitude = claims.getTeacherLatitude();
            Double teacherLongitude = claims.getTeacherLongitude();
            if (teacherLatitude != null && teacherLongitude != null) {
                int maxDistanceMeters = claims.getMaxDistanceMeters() == null
                        ? 150
                        : Math.max(100, Math.min(200, claims.getMaxDistanceMeters()));
                double teacherDistanceMeters = geolocationService.calculateDistanceMeters(
                        latitude, longitude, teacherLatitude, teacherLongitude);
                if (teacherDistanceMeters > maxDistanceMeters) {
                    antiCheatingService.logViolation(
                            studentId,
                            "TEACHER_DISTANCE_EXCEEDED",
                            String.format("Student is %.0fm away from teacher session, limit=%dm",
                                    teacherDistanceMeters, maxDistanceMeters),
                            deviceFingerprint,
                            clientIp,
                            latitude,
                            longitude,
                            teacherLatitude,
                            teacherLongitude,
                            "CRITICAL");
                    return ResponseEntity.ok(new MarkAttendanceResponse(
                            false,
                            String.format("You are %.0f m away from the teacher session. Move within %d m.",
                                    teacherDistanceMeters, maxDistanceMeters),
                            "TEACHER_DISTANCE_INVALID"));
                }
            }

            // ✅ STEP 4.5: Face verification before any geolocation decision
            FaceVerificationService.FaceVerificationResult faceResult = null;
            if (faceVerificationRequired) {
                try {
                    faceResult = faceVerificationService.verifyFace(
                            studentId,
                            tenantId,
                            markRequest.getFaceEmbedding(),
                            markRequest.getLivenessVerified(),
                            markRequest.getLivenessPrompt(),
                            markRequest.getBlinkDetected(),
                            markRequest.getHeadMovementDetected(),
                            markRequest.getFrameCount(),
                            markRequest.getMotionParallaxScore(),
                            markRequest.getBrightnessVariance(),
                            markRequest.getFrameEmbeddings(),
                            markRequest.getFrameSnapshots());
                } catch (RuntimeException faceError) {
                    antiCheatingService.logViolation(
                            studentId,
                            "FACE_MISMATCH",
                            faceError.getMessage(),
                            deviceFingerprint,
                            clientIp,
                            latitude,
                            longitude,
                            null,
                            null,
                            "HIGH");
                    return ResponseEntity.ok(new MarkAttendanceResponse(
                            false,
                            faceError.getMessage(),
                            "FACE_MISMATCH"));
                }
            }

            // ✅ STEP 5: Check if student is blocked
            if (antiCheatingService.isStudentBlocked(studentId)) {
                antiCheatingService.logViolation(
                        studentId,
                        "STUDENT_BLOCKED",
                        "Student has accumulated too many recent violations",
                        deviceFingerprint,
                        clientIp,
                        latitude,
                        longitude,
                        null,
                        null,
                        "CRITICAL");
                return ResponseEntity.ok(new MarkAttendanceResponse(
                        false,
                        "Your account is temporarily blocked due to repeated suspicious activity",
                        "BLOCKED"));
            }

            // ✅ STEP 6: Rate-limit rapid attempts
            if (antiCheatingService.detectRapidAttempts(studentId)) {
                antiCheatingService.logViolation(
                        studentId,
                        "RAPID_ATTEMPTS",
                        "Attendance attempts exceeded the allowed rate",
                        deviceFingerprint,
                        clientIp,
                        latitude,
                        longitude,
                        null,
                        null,
                        "HIGH");
                return ResponseEntity.ok(new MarkAttendanceResponse(
                        false,
                        "Please wait a moment before trying again",
                        "RATE_LIMITED"));
            }

            // ✅ STEP 7: Validate campus geofence
            List<CampusLocation> activeLocations = campusLocationRepository.findAllActive();
            if (activeLocations.isEmpty()) {
                return ResponseEntity.ok(new MarkAttendanceResponse(
                        false,
                        "Campus attendance zones are not configured yet",
                        "CONFIG_ERROR"));
            }

            CampusLocation closestLocation = geolocationService.findClosestLocation(latitude, longitude,
                    activeLocations);
            if (closestLocation == null) {
                return ResponseEntity.ok(new MarkAttendanceResponse(
                        false,
                        "Unable to validate location against campus zones",
                        "CONFIG_ERROR"));
            }

            boolean locationVerified = geolocationService.isInsideGeofence(latitude, longitude, closestLocation);
            if (!locationVerified) {
                double distanceMeters = geolocationService.calculateDistanceMeters(
                        latitude,
                        longitude,
                        closestLocation.getLatitude(),
                        closestLocation.getLongitude());

                antiCheatingService.logViolation(
                        studentId,
                        "LOCATION_OUTSIDE_GEOFENCE",
                        String.format("Student was %.0f meters outside %s", distanceMeters, closestLocation.getName()),
                        deviceFingerprint,
                        clientIp,
                        latitude,
                        longitude,
                        closestLocation.getLatitude(),
                        closestLocation.getLongitude(),
                        "HIGH");

                return ResponseEntity.ok(new MarkAttendanceResponse(
                        false,
                        String.format("You are outside the allowed attendance area for %s", closestLocation.getName()),
                        "LOCATION_INVALID"));
            }

            // ✅ STEP 8: Check VPN/proxy mismatch
            AntiCheatingService.VPNDetectionResult vpnResult = antiCheatingService.detectVPN(
                    latitude,
                    longitude,
                    clientIp,
                    geolocationService);

            if (vpnResult.isVPNDetected) {
                antiCheatingService.logViolation(
                        studentId,
                        "VPN_DETECTED",
                        vpnResult.reason,
                        deviceFingerprint,
                        clientIp,
                        latitude,
                        longitude,
                        closestLocation.getLatitude(),
                        closestLocation.getLongitude(),
                        "CRITICAL");

                return ResponseEntity.ok(new MarkAttendanceResponse(
                        false,
                        "VPN or proxy use was detected. Please use your direct device connection.",
                        "VPN_BLOCKED"));
            }

            // ✅ STEP 9: Detect impossible movement / spoofing
            AntiCheatingService.ImpossibleMovementResult movementResult = antiCheatingService.detectImpossibleMovement(
                    deviceFingerprint,
                    latitude,
                    longitude,
                    System.currentTimeMillis());

            if (movementResult.isImpossible) {
                antiCheatingService.logViolation(
                        studentId,
                        "IMPOSSIBLE_MOVEMENT",
                        movementResult.reason,
                        deviceFingerprint,
                        clientIp,
                        latitude,
                        longitude,
                        closestLocation.getLatitude(),
                        closestLocation.getLongitude(),
                        "CRITICAL");

                return ResponseEntity.ok(new MarkAttendanceResponse(
                        false,
                        "Suspicious movement pattern detected. Attendance was not marked.",
                        "IMPOSSIBLE_MOVEMENT"));
            }

            int confidenceScore = geolocationService.getConfidenceScore(latitude, longitude, closestLocation);

            boolean deviceSharingDetected = antiCheatingService.detectDeviceSharing(deviceFingerprint, studentId);

            FraudDetectionService.FraudAssessment fraudAssessment = fraudDetectionService.assess(
                    studentId,
                    claims.getSubjectId(),
                    claims.getSessionId(),
                    deviceFingerprint,
                    clientIp,
                    clientDeviceId,
                    latitude,
                    longitude,
                    markRequest.getAccuracy(),
                    closestLocation,
                    confidenceScore,
                    markRequest.getQrDetectedAtEpochMs(),
                    !faceVerificationRequired || faceResult != null,
                    faceResult != null ? faceResult.getSimilarity() : 1.0,
                    locationVerified,
                    deviceSharingDetected,
                    vpnResult,
                    movementResult);

            if (fraudAssessment.isRejected()) {
                fraudDetectionService.recordDecision(
                        studentId,
                        claims.getSubjectId(),
                        null,
                        fraudAssessment.getFraudScore(),
                        fraudAssessment.getDecision(),
                        fraudAssessment.getReasons(),
                        clientDeviceId,
                        clientIp,
                        latitude,
                        longitude,
                        true);

                return ResponseEntity.ok(new MarkAttendanceResponse(
                        false,
                        "Fraud score too high: " + String.join(" | ", fraudAssessment.getReasons()),
                        "REJECTED"));
            }

            String attendanceStatus = fraudAssessment.isSuspicious() ? "PRESENT" : "PRESENT";
            String markingType = fraudAssessment.isSuspicious() ? "QR_FACE_GEO_AI_SUSPICIOUS"
                    : "QR_FACE_GEO_AI_APPROVED";

            // ✅ STEP 10: Mark attendance with all validations
            try {
                Attendance attendance = attendanceService.markAttendance(
                        studentId,
                        claims.getSubjectId(),
                        claims.getTeacherId(),
                        attendanceStatus,
                        markingType,
                        deviceInfo,
                        clientIp,
                        latitude,
                        longitude,
                        true,
                        deviceFingerprint,
                        closestLocation.getId(),
                        tokenHash,
                        tenantId);

                fraudDetectionService.recordDecision(
                        studentId,
                        claims.getSubjectId(),
                        attendance.getId(),
                        fraudAssessment.getFraudScore(),
                        fraudAssessment.getDecision(),
                        fraudAssessment.getReasons(),
                        clientDeviceId,
                        clientIp,
                        latitude,
                        longitude,
                        true);

                campusTrackingService.recordLocation(
                        studentId,
                        claims.getSubjectId(),
                        claims.getSessionId(),
                        attendance.getId(),
                        latitude,
                        longitude,
                        !faceVerificationRequired || faceResult != null,
                        false,
                        faceResult != null ? faceResult.getSimilarity() : 1.0,
                        confidenceScore);

                MarkAttendanceResponse response = new MarkAttendanceResponse(
                        true,
                        fraudAssessment.isSuspicious()
                                ? "Attendance marked with suspicious activity flagged"
                                : "Attendance marked successfully and all checks passed",
                        fraudAssessment.isSuspicious() ? "SUSPICIOUS" : "MARKED",
                        attendance.getId().toString());
                response.setConfidenceScore(confidenceScore);
                response.setFaceVerified(!faceVerificationRequired || faceResult != null);
                response.setFaceSimilarity(faceResult != null ? faceResult.getSimilarity() : 1.0);
                response.setLocationVerified(locationVerified);
                response.setFraudScore(fraudAssessment.getFraudScore());
                response.setDecision(fraudAssessment.getDecision());
                response.setRiskLevel(fraudAssessment.getRiskLevel());

                return ResponseEntity.ok(response);
            } catch (RuntimeException e) {
                String status = e.getMessage().contains("already marked") ? "ALREADY_MARKED" : "ERROR";
                return ResponseEntity.ok(new MarkAttendanceResponse(
                        false,
                        e.getMessage(),
                        status));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(new MarkAttendanceResponse(
                    false,
                    "Server error: " + e.getMessage(),
                    "ERROR"));
        }
    }

    @PostMapping("/register-face")
    public ResponseEntity<Map<String, Object>> registerFace(
            @RequestBody FaceRegistrationRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {
        try {
            String studentId = resolveAuthenticatedStudentId(auth);
            Long tenantId = resolveTenantId(httpRequest);
            FaceVerificationService.FaceVerificationResult result = faceVerificationService.registerFace(
                    studentId,
                    tenantId,
                    request.getFaceEmbedding(),
                    request.getLivenessVerified(),
                    request.getLivenessPrompt(),
                    request.getBlinkDetected(),
                    request.getHeadMovementDetected(),
                    request.getFrameCount(),
                    request.getMotionParallaxScore(),
                    request.getBrightnessVariance(),
                    request.getFrameEmbeddings());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result.getMessage());
            response.put("studentId", studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/face-status")
    public ResponseEntity<Map<String, Object>> faceStatus(Authentication auth, HttpServletRequest request) {
        String studentId = resolveAuthenticatedStudentId(auth);
        Long tenantId = resolveTenantId(request);
        boolean registered = faceVerificationService.hasRegisteredFace(studentId, tenantId);
        Map<String, Object> response = new HashMap<>();
        response.put("studentId", studentId);
        response.put("tenantId", tenantId);
        response.put("registered", registered);
        response.put("message", registered ? "Face registered" : "Face not registered");
        return ResponseEntity.ok(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private Long resolveTenantId(HttpServletRequest request) {
        Object tenantAttr = request.getAttribute("tenantId");
        if (tenantAttr instanceof Number number) {
            return number.longValue();
        }

        String tenantHeader = request.getHeader("X-Tenant-Id");
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            try {
                return Long.parseLong(tenantHeader);
            } catch (NumberFormatException ignored) {
                return 1L;
            }
        }
        return 1L;
    }

    private boolean wasScannedBeforeExpiry(Long detectedAt, long expiresAtEpochMs) {
        if (detectedAt == null) {
            return false;
        }

        long graceWindowMs = 30_000L;
        return detectedAt <= expiresAtEpochMs && (System.currentTimeMillis() - detectedAt) <= graceWindowMs;
    }

    private String resolveAuthenticatedStudentId(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated student is required");
        }

        String username = auth.getName().trim();
        Optional<Student> student = studentRepository.findByUserUsername(username);
        return student.map(Student::getId).orElse(username);
    }

    /**
     * Get student's attendance record for a subject
     * 
     * GET /api/student/attendance/my-records?subjectId=1
     */
    @GetMapping("/my-records")
    public ResponseEntity<Map<String, Object>> getMyAttendance(
            @RequestParam Long subjectId,
            Authentication auth,
            HttpServletRequest request) {
        try {
            String studentId = resolveAuthenticatedStudentId(auth);
            Long tenantId = resolveTenantId(request);

            List<Attendance> records = attendanceService.getStudentAttendance(studentId, subjectId, tenantId);
            Double percentage = attendanceService.calculateAttendancePercentage(studentId, subjectId, tenantId);

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
            Authentication auth,
            HttpServletRequest request) {
        try {
            String studentId = resolveAuthenticatedStudentId(auth);
            Long tenantId = resolveTenantId(request);
            LocalDate today = LocalDate.now();

            boolean alreadyMarked = false;
            String status = "PENDING";

            var existing = attendanceService.getAttendanceForDate(subjectId, today, tenantId)
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

    @PostMapping("/geofence/check")
    public ResponseEntity<Map<String, Object>> checkGeofence(@RequestBody Map<String, Double> request) {
        try {
            Double latitude = request != null ? request.get("latitude") : null;
            Double longitude = request != null ? request.get("longitude") : null;

            if (latitude == null || longitude == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "latitude and longitude are required"));
            }

            List<CampusLocation> activeLocations = campusLocationRepository.findAllActive();
            if (activeLocations.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "No active campus locations configured",
                        "inside", false));
            }

            CampusLocation closestLocation = geolocationService.findClosestLocation(latitude, longitude,
                    activeLocations);
            if (closestLocation == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Unable to resolve closest campus location",
                        "inside", false));
            }

            double distanceMeters = geolocationService.calculateDistanceMeters(
                    latitude,
                    longitude,
                    closestLocation.getLatitude(),
                    closestLocation.getLongitude());
            boolean inside = geolocationService.isInsideGeofence(latitude, longitude, closestLocation);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("inside", inside);
            response.put("distanceMeters", Math.round(distanceMeters));
            response.put("radiusMeters", closestLocation.getRadiusMeters());
            response.put("closestLocation", closestLocation.getName());
            response.put("message", inside
                    ? "Inside campus attendance zone"
                    : "Outside campus attendance zone");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getAttendanceMetrics(@RequestParam Long subjectId,
            Authentication auth,
            HttpServletRequest request) {
        try {
            String studentId = resolveAuthenticatedStudentId(auth);
            Long tenantId = resolveTenantId(request);
            AttendanceService.WeightedAttendanceMetrics metrics = attendanceService
                    .getWeightedAttendanceMetrics(studentId, subjectId, tenantId);

            Map<String, Object> response = new HashMap<>();
            response.put("subjectId", subjectId);
            response.put("studentId", studentId);
            response.put("weightedAttendance", metrics.getWeightedPercentage());
            response.put("presentCount", metrics.getPresentCount());
            response.put("lateCount", metrics.getLateCount());
            response.put("absentCount", metrics.getAbsentCount());
            response.put("suspiciousCount", metrics.getSuspiciousCount());
            response.put("faceVerificationEnforced", metrics.isFaceVerificationEnforced());
            response.put("engine", "weighted-v2");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
            AttendanceQRTokenService.AttendanceTokenClaims claims = qrTokenService.validateAttendanceToken(token);

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
            response.put("maxDistanceMeters", claims.getMaxDistanceMeters());
            response.put("faceVerificationRequired", !Boolean.FALSE.equals(claims.getFaceVerificationRequired()));
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
