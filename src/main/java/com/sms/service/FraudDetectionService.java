package com.sms.service;

import com.sms.dto.attendance.FraudAlertDTO;
import com.sms.model.Attendance;
import com.sms.model.CampusLocation;
import com.sms.model.FraudLog;
import com.sms.model.StudentLocation;
import com.sms.repository.AttendanceRepository;
import com.sms.repository.FraudLogRepository;
import com.sms.repository.SecurityAuditRepository;
import com.sms.repository.StudentLocationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class FraudDetectionService {

    private static final double LOCATION_THRESHOLD_METERS = 200.0;
    private static final double REJECT_THRESHOLD = 60.0;
    private static final double SUSPICIOUS_THRESHOLD = 30.0;

    private final AttendanceRepository attendanceRepository;
    private final FraudLogRepository fraudLogRepository;
    private final StudentLocationRepository studentLocationRepository;
    private final SecurityAuditRepository securityAuditRepository;
    private final GeolocationService geolocationService;
    private final AntiCheatingService antiCheatingService;
    private final SimpMessagingTemplate messagingTemplate;

    public FraudDetectionService(AttendanceRepository attendanceRepository,
                                 FraudLogRepository fraudLogRepository,
                                 StudentLocationRepository studentLocationRepository,
                                 SecurityAuditRepository securityAuditRepository,
                                 GeolocationService geolocationService,
                                 AntiCheatingService antiCheatingService,
                                 SimpMessagingTemplate messagingTemplate) {
        this.attendanceRepository = attendanceRepository;
        this.fraudLogRepository = fraudLogRepository;
        this.studentLocationRepository = studentLocationRepository;
        this.securityAuditRepository = securityAuditRepository;
        this.geolocationService = geolocationService;
        this.antiCheatingService = antiCheatingService;
        this.messagingTemplate = messagingTemplate;
    }

    public FraudAssessment assess(String studentId,
                                  Long subjectId,
                                  String sessionId,
                                  String deviceFingerprint,
                                  String clientIp,
                                  String deviceId,
                                  Double latitude,
                                  Double longitude,
                                  Double accuracy,
                                  CampusLocation closestLocation,
                                  Integer locationConfidence,
                                  Long qrDetectedAtEpochMs,
                                  boolean faceVerified,
                                  double faceSimilarity,
                                  boolean locationVerified,
                                  boolean deviceSharingDetected,
                                  AntiCheatingService.VPNDetectionResult vpnResult,
                                  AntiCheatingService.ImpossibleMovementResult movementResult) {

        List<String> reasons = new ArrayList<>();
        double fraudScore = 0.0;

        if (closestLocation != null && latitude != null && longitude != null) {
            double distanceMeters = geolocationService.calculateDistanceMeters(
                latitude,
                longitude,
                closestLocation.getLatitude(),
                closestLocation.getLongitude()
            );
            if (distanceMeters > LOCATION_THRESHOLD_METERS) {
                fraudScore += 40.0;
                reasons.add(String.format(Locale.US, "Location mismatch: %.0fm from campus zone", distanceMeters));
            }
        }

        if (vpnResult != null && vpnResult.isVPNDetected) {
            fraudScore += 25.0;
            reasons.add("IP/GPS mismatch or VPN suspected");
        }

        if (movementResult != null && movementResult.isImpossible) {
            fraudScore += 20.0;
            reasons.add(movementResult.reason);
        }

        if (deviceSharingDetected) {
            fraudScore += 30.0;
            reasons.add("Device sharing suspected");
        }

        if (qrDetectedAtEpochMs != null) {
            long scanDelayMs = Math.max(0L, System.currentTimeMillis() - qrDetectedAtEpochMs);
            if (scanDelayMs < 2000L) {
                fraudScore += 15.0;
                reasons.add("QR scan completed too quickly");
            }
        }

        if (deviceChanged(studentId, subjectId, deviceId)) {
            fraudScore += 20.0;
            reasons.add("Device changed since last attendance");
        }

        int clusterCount = detectClusterCount(subjectId, sessionId, latitude, longitude);
        if (clusterCount >= 5) {
            fraudScore += 30.0;
            reasons.add(String.format(Locale.US, "Cluster detected: %d students near same coordinates", clusterCount));
        }

        long faceFails = countRecentFaceFailures(studentId);
        if (faceFails > 2) {
            fraudScore += 35.0;
            reasons.add("Repeated face mismatch attempts");
        }

        if (!faceVerified) {
            fraudScore += 35.0;
            reasons.add("Face verification failed");
        }

        if (accuracy != null && accuracy > 150.0) {
            fraudScore += 10.0;
            reasons.add("Low GPS accuracy");
        }

        String decision = fraudScore >= REJECT_THRESHOLD ? "REJECTED" : fraudScore >= SUSPICIOUS_THRESHOLD ? "SUSPICIOUS" : "APPROVED";
        return new FraudAssessment(fraudScore, decision, reasons, locationVerified, faceSimilarity, confidenceLabel(fraudScore));
    }

    @Transactional
    public FraudLog recordDecision(String studentId,
                                   Long subjectId,
                                   Long attendanceId,
                                   double fraudScore,
                                   String decision,
                                   List<String> reasons,
                                   String deviceId,
                                   String ipAddress,
                                   Double latitude,
                                   Double longitude,
                                   boolean notify) {
        FraudLog log = new FraudLog();
        log.setStudentId(studentId);
        log.setSubjectId(subjectId);
        log.setAttendanceId(attendanceId);
        log.setFraudScore(fraudScore);
        log.setDecision(decision);
        log.setReason(String.join("; ", reasons));
        log.setDeviceId(deviceId);
        log.setIpAddress(ipAddress);
        log.setLatitude(latitude);
        log.setLongitude(longitude);

        FraudLog saved = fraudLogRepository.save(log);

        if (notify && ("SUSPICIOUS".equals(decision) || "REJECTED".equals(decision))) {
            FraudAlertDTO alert = new FraudAlertDTO();
            alert.setFraudLogId(saved.getId());
            alert.setStudentId(studentId);
            alert.setSubjectId(subjectId);
            alert.setAttendanceId(attendanceId);
            alert.setDecision(decision);
            alert.setFraudScore(fraudScore);
            alert.setReasons(reasons);
            alert.setLatitude(latitude);
            alert.setLongitude(longitude);
            alert.setRecordedAt(saved.getCreatedAt());
            messagingTemplate.convertAndSend("/topic/alerts", alert);
        }

        return saved;
    }

    public FraudSummary getSummary() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long suspicious = fraudLogRepository.countByDecisionSince("SUSPICIOUS", since);
        long rejected = fraudLogRepository.countByDecisionSince("REJECTED", since);
        long approved = fraudLogRepository.countByDecisionSince("APPROVED", since);
        return new FraudSummary(approved, suspicious, rejected);
    }

    private boolean deviceChanged(String studentId, Long subjectId, String deviceId) {
        if (studentId == null || subjectId == null || deviceId == null || deviceId.isBlank()) {
            return false;
        }

        Optional<Attendance> lastAttendance = attendanceRepository.findByStudentIdAndSubjectIdOrderByAttendanceDateDesc(studentId, subjectId)
            .stream()
            .findFirst();

        return lastAttendance.map(attendance -> attendance.getDeviceId() != null && !attendance.getDeviceId().equals(deviceId)).orElse(false);
    }

    private int detectClusterCount(Long subjectId, String sessionId, Double latitude, Double longitude) {
        if (subjectId == null || latitude == null || longitude == null) {
            return 0;
        }

        List<StudentLocation> recentLocations = sessionId != null && !sessionId.isBlank()
            ? studentLocationRepository.findTop100BySessionIdOrderByRecordedAtDesc(sessionId)
            : studentLocationRepository.findTop200BySubjectIdAndRecordedAtAfterOrderByRecordedAtDesc(subjectId, LocalDateTime.now().minusMinutes(15));

        double latBucket = round4(latitude);
        double lonBucket = round4(longitude);
        int count = 0;

        for (StudentLocation location : recentLocations) {
            if (location.getLatitude() == null || location.getLongitude() == null) {
                continue;
            }
            if (round4(location.getLatitude()) == latBucket && round4(location.getLongitude()) == lonBucket) {
                count++;
            }
        }

        return count;
    }

    private long countRecentFaceFailures(String studentId) {
        return securityAuditRepository.countRecentViolationsByType(studentId, LocalDateTime.now().minusHours(24), "FACE_MISMATCH");
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private String confidenceLabel(double fraudScore) {
        if (fraudScore >= REJECT_THRESHOLD) {
            return "HIGH";
        }
        if (fraudScore >= SUSPICIOUS_THRESHOLD) {
            return "MEDIUM";
        }
        return "LOW";
    }

    public static class FraudAssessment {
        private final double fraudScore;
        private final String decision;
        private final List<String> reasons;
        private final boolean locationVerified;
        private final double faceSimilarity;
        private final String riskLevel;

        public FraudAssessment(double fraudScore, String decision, List<String> reasons, boolean locationVerified, double faceSimilarity, String riskLevel) {
            this.fraudScore = fraudScore;
            this.decision = decision;
            this.reasons = reasons;
            this.locationVerified = locationVerified;
            this.faceSimilarity = faceSimilarity;
            this.riskLevel = riskLevel;
        }

        public double getFraudScore() { return fraudScore; }
        public String getDecision() { return decision; }
        public List<String> getReasons() { return reasons; }
        public boolean isLocationVerified() { return locationVerified; }
        public double getFaceSimilarity() { return faceSimilarity; }
        public String getRiskLevel() { return riskLevel; }
        public boolean isSuspicious() { return "SUSPICIOUS".equals(decision); }
        public boolean isRejected() { return "REJECTED".equals(decision); }
    }

    public static class FraudSummary {
        private final long approved;
        private final long suspicious;
        private final long rejected;

        public FraudSummary(long approved, long suspicious, long rejected) {
            this.approved = approved;
            this.suspicious = suspicious;
            this.rejected = rejected;
        }

        public long getApproved() { return approved; }
        public long getSuspicious() { return suspicious; }
        public long getRejected() { return rejected; }
    }
}