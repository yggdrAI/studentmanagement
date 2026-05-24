package com.sms.service;

import com.sms.dto.attendance.MarkAttendanceRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HybridAttendanceTrustService {

    private final double approvalThreshold;

    public HybridAttendanceTrustService(
            @Value("${app.attendance.trust.approval-threshold:82}") double approvalThreshold) {
        this.approvalThreshold = approvalThreshold;
    }

    public TrustDecision evaluate(TrustInput input) {
        List<String> reasons = new ArrayList<>();

        double qrScore = scoreQr(input.qrDetectedAtEpochMs(), input.tokenExpiresAt(), reasons);
        double faceScore = scoreFace(input.faceVerificationRequired(), input.faceVerified(), input.faceSimilarity(), reasons);
        double livenessScore = scoreLiveness(input.request(), input.faceVerificationRequired(), reasons);
        double locationScore = scoreLocation(input.locationVerified(), input.locationConfidence(), input.request().getAccuracy(), reasons);
        double deviceScore = scoreDevice(input.request(), input.deviceSharingDetected(), input.vpnDetected(), input.impossibleMovement(), reasons);
        double behavioralScore = scoreBehavior(input.fraudScore(), input.fraudRejected(), input.fraudSuspicious(), reasons);

        double finalScore = round(
                (qrScore * 0.16)
                + (faceScore * 0.24)
                + (livenessScore * 0.18)
                + (locationScore * 0.18)
                + (deviceScore * 0.12)
                + (behavioralScore * 0.12));

        boolean approved = finalScore >= approvalThreshold && !input.fraudRejected();
        String decision = approved ? (input.fraudSuspicious() ? "SUSPICIOUS" : "APPROVED") : "REJECTED";
        if (!approved) {
            reasons.add("Combined biometric trust score below approval threshold");
        }

        return new TrustDecision(
                approved,
                decision,
                finalScore,
                round(faceScore),
                round(livenessScore),
                round(qrScore),
                round(locationScore),
                round(deviceScore),
                round(behavioralScore),
                approvalThreshold,
                reasons);
    }

    private double scoreQr(Long qrDetectedAtEpochMs, long tokenExpiresAt, List<String> reasons) {
        if (qrDetectedAtEpochMs == null) {
            reasons.add("QR scan timestamp missing");
            return 55.0;
        }
        long now = System.currentTimeMillis();
        long scanAgeMs = Math.max(0L, now - qrDetectedAtEpochMs);
        if (qrDetectedAtEpochMs > tokenExpiresAt) {
            reasons.add("QR was detected after token expiry");
            return 0.0;
        }
        if (scanAgeMs <= 12_000L) {
            return 100.0;
        }
        if (scanAgeMs <= 30_000L) {
            reasons.add("QR scan is valid but stale");
            return 78.0;
        }
        reasons.add("QR scan age is too old");
        return 35.0;
    }

    private double scoreFace(boolean required, boolean verified, double similarity, List<String> reasons) {
        if (!required) {
            return 92.0;
        }
        if (!verified) {
            reasons.add("Face verification did not pass");
            return 0.0;
        }
        double score = clamp(similarity * 100.0);
        if (score < 92.0) {
            reasons.add("Face similarity is close to the minimum threshold");
        }
        return score;
    }

    private double scoreLiveness(MarkAttendanceRequest request, boolean faceRequired, List<String> reasons) {
        if (!faceRequired) {
            return 88.0;
        }
        if (!Boolean.TRUE.equals(request.getLivenessVerified())) {
            reasons.add("Liveness was not verified");
            return 0.0;
        }

        double score = 72.0;
        if (Boolean.TRUE.equals(request.getBlinkDetected())) {
            score += 8.0;
        } else if (request.getBlinkDetected() != null) {
            reasons.add("Blink signal missing");
        }
        if (Boolean.TRUE.equals(request.getHeadMovementDetected())) {
            score += 8.0;
        } else if (request.getHeadMovementDetected() != null) {
            reasons.add("Head movement signal missing");
        }
        if (request.getFrameCount() != null && request.getFrameCount() >= 5) {
            score += 5.0;
        }
        if (request.getMotionParallaxScore() != null) {
            score += Math.min(4.0, Math.max(0.0, request.getMotionParallaxScore() * 4.0));
        }
        if (request.getBrightnessVariance() != null && request.getBrightnessVariance() > 6.0) {
            score += 3.0;
        }
        return clamp(score);
    }

    private double scoreLocation(boolean locationVerified, Integer locationConfidence, Double accuracy, List<String> reasons) {
        if (!locationVerified) {
            reasons.add("Location geofence was not verified");
            return 0.0;
        }
        double score = locationConfidence == null ? 84.0 : clamp(locationConfidence);
        if (accuracy != null && accuracy > 80.0) {
            score -= 12.0;
            reasons.add("GPS accuracy is weak");
        } else if (accuracy != null && accuracy <= 30.0) {
            score += 5.0;
        }
        return clamp(score);
    }

    private double scoreDevice(MarkAttendanceRequest request,
                               boolean deviceSharingDetected,
                               boolean vpnDetected,
                               boolean impossibleMovement,
                               List<String> reasons) {
        double score = 100.0;
        if (deviceSharingDetected) {
            score -= 35.0;
            reasons.add("Device sharing detected");
        }
        if (vpnDetected || Boolean.TRUE.equals(request.getVpnDetectedByClient())) {
            score -= 35.0;
            reasons.add("VPN or proxy signal detected");
        }
        if (impossibleMovement) {
            score -= 45.0;
            reasons.add("Impossible movement signal detected");
        }
        if (Boolean.TRUE.equals(request.getMockLocationDetected())) {
            score -= 40.0;
            reasons.add("Mock location provider detected");
        }
        if (Boolean.TRUE.equals(request.getEmulatorDetected())) {
            score -= 20.0;
            reasons.add("Emulator signal detected");
        }
        if (Boolean.TRUE.equals(request.getRootedOrJailbroken())) {
            score -= 15.0;
            reasons.add("Rooted or jailbroken device signal detected");
        }
        if (request.getWifiBssidHash() != null && !request.getWifiBssidHash().isBlank()) {
            score += 3.0;
        }
        if (request.getBluetoothBeaconIds() != null && !request.getBluetoothBeaconIds().isEmpty()) {
            score += 3.0;
        }
        return clamp(score);
    }

    private double scoreBehavior(double fraudScore, boolean rejected, boolean suspicious, List<String> reasons) {
        if (rejected) {
            reasons.add("Fraud engine rejected the scan attempt");
            return 0.0;
        }
        double score = clamp(100.0 - fraudScore);
        if (suspicious) {
            reasons.add("Fraud engine marked the scan as suspicious");
        }
        return score;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record TrustInput(
            MarkAttendanceRequest request,
            boolean faceVerificationRequired,
            boolean faceVerified,
            double faceSimilarity,
            boolean locationVerified,
            Integer locationConfidence,
            boolean deviceSharingDetected,
            boolean vpnDetected,
            boolean impossibleMovement,
            double fraudScore,
            boolean fraudSuspicious,
            boolean fraudRejected,
            long tokenExpiresAt,
            Long qrDetectedAtEpochMs) {
    }

    public record TrustDecision(
            boolean approved,
            String decision,
            double finalScore,
            double faceScore,
            double livenessScore,
            double qrScore,
            double locationScore,
            double deviceScore,
            double behavioralScore,
            double approvalThreshold,
            List<String> reasons) {
    }
}
