package com.sms.service;

import com.sms.model.SecurityAudit;
import com.sms.repository.SecurityAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Anti-Cheating Service
 * Implements device fingerprinting, VPN detection, and behavior tracking
 */
@Service
public class AntiCheatingService {

    @Autowired
    private SecurityAuditRepository securityAuditRepository;

    @Autowired
    private IpGeolocationService ipGeolocationService;

    // Track recent locations per device (in-memory cache)
    private Map<String, List<LocationTimestamp>> deviceLocationHistory = Collections.synchronizedMap(new HashMap<>());

    // Track attendance attempts per time window
    private Map<String, List<Long>> attendanceAttempts = Collections.synchronizedMap(new HashMap<>());

    // Track device sharing: deviceFingerprint -> (studentId -> lastSeenMs)
    private final Map<String, Map<String, Long>> deviceStudentLastSeen = Collections.synchronizedMap(new HashMap<>());

    private static final int LOCATION_HISTORY_SIZE = 10;
    private static final double IMPOSSIBLE_SPEED_KMH = 100.0; // Detect teleporting
    private static final long IMPOSSIBLE_TIME_SECS = 60; // 1 minute between distant locations
    private static final int VIOLATION_THRESHOLD = 3; // Block after 3 violations
    private static final long DEVICE_SHARING_WINDOW_MS = 10 * 60_000L; // 10 minutes

    /**
     * Generate device fingerprint from user agent and IP
     * 
     * Device ID = SHA256(userAgent + staticSalt)
     */
    public String generateDeviceFingerprint(String userAgent, String ipAddress) {
        try {
            String combined = (userAgent != null ? userAgent : "UNKNOWN") + "|" + 
                            (ipAddress != null ? ipAddress : "UNKNOWN") + "|BENNETT_SMS_SALT";
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            String fingerprint = "device_" + hexString.toString().substring(0, 16);
            System.out.println("[ANTI-CHEAT] Generated device fingerprint: " + fingerprint);
            return fingerprint;
        } catch (Exception e) {
            System.err.println("[ANTI-CHEAT] Error generating fingerprint: " + e.getMessage());
            return "device_unknown";
        }
    }

    /**
     * Detect VPN/Proxy by checking IP geolocation vs GPS location mismatch
     * 
     * High mismatch = likely VPN/proxy
     */
    public VPNDetectionResult detectVPN(double studentLat, double studentLon, String ipAddress, GeolocationService geoService) {
        VPNDetectionResult result = new VPNDetectionResult();

        double[] ipCoords = ipGeolocationService != null ? ipGeolocationService.resolve(ipAddress) : null;
        if (ipCoords == null) {
            result.isVPNDetected = false;
            result.suspicionScore = 0;
            result.reason = ipGeolocationService != null && ipGeolocationService.isEnabled()
                    ? "IP geolocation unavailable"
                    : "IP geolocation disabled";
            return result;
        }

        double distanceKm = geoService.calculateDistanceKm(studentLat, studentLon, ipCoords[0], ipCoords[1]);

        // If IP location is wildly different from GPS, likely VPN/proxy.
        // We keep this generous (>= 200km) to avoid false positives from mobile carrier routing.
        if (distanceKm > 200.0) {
            result.isVPNDetected = true;
            result.suspicionScore = 100;
            result.reason = String.format("GPS vs IP location mismatch: %.0fkm", distanceKm);
            System.out.println("[ANTI-CHEAT] VPN DETECTED: " + result.reason);
        } else {
            result.isVPNDetected = false;
            result.suspicionScore = (int) Math.min(100, distanceKm);
            result.reason = "IP/GPS distance within tolerance";
        }

        return result;
    }

    /**
     * Detect impossible movement (teleporting)
     */
    public ImpossibleMovementResult detectImpossibleMovement(String deviceId, double lat, double lon, long timestamp) {
        ImpossibleMovementResult result = new ImpossibleMovementResult();
        result.isImpossible = false;
        result.reason = "Normal movement";

        List<LocationTimestamp> history = deviceLocationHistory.getOrDefault(deviceId, new ArrayList<>());
        
        if (history.size() > 0) {
            LocationTimestamp lastLocation = history.get(history.size() - 1);
            
            // Calculate distance and time
            GeolocationService geoService = new GeolocationService();
            double distanceKm = geoService.calculateDistanceKm(lastLocation.lat, lastLocation.lon, lat, lon);
            long timeDiffSecs = (timestamp - lastLocation.timestamp) / 1000;
            
            if (timeDiffSecs > 0) {
                double speedKmh = (distanceKm / timeDiffSecs) * 3600;
                
                if (speedKmh > IMPOSSIBLE_SPEED_KMH && timeDiffSecs < IMPOSSIBLE_TIME_SECS) {
                    result.isImpossible = true;
                    result.reason = String.format("Impossible speed: %.0f km/h (%.1f km in %d secs)", 
                        speedKmh, distanceKm, timeDiffSecs);
                    System.out.println("[ANTI-CHEAT] IMPOSSIBLE MOVEMENT: " + result.reason);
                }
            }
        }

        // Update history
        history.add(new LocationTimestamp(lat, lon, timestamp));
        if (history.size() > LOCATION_HISTORY_SIZE) {
            history.remove(0);
        }
        deviceLocationHistory.put(deviceId, history);

        return result;
    }

    /**
     * Detect rapid-fire attendance attempts (suspicious pattern)
     */
    public boolean detectRapidAttempts(String studentId) {
        long now = System.currentTimeMillis();
        long oneMinuteAgo = now - 60_000;

        List<Long> attempts = attendanceAttempts.getOrDefault(studentId, new ArrayList<>());
        attempts.removeIf(timestamp -> timestamp < oneMinuteAgo);

        if (attempts.size() > 2) {
            System.out.println("[ANTI-CHEAT] RAPID ATTEMPTS: " + attempts.size() + " attempts in 1 minute");
            attendanceAttempts.put(studentId, attempts);
            return true;
        }

        attempts.add(now);
        attendanceAttempts.put(studentId, attempts);
        return false;
    }

    /**
     * Detect device sharing: same device fingerprint used by multiple studentIds in a short window.
     */
    public boolean detectDeviceSharing(String deviceFingerprint, String studentId) {
        if (deviceFingerprint == null || deviceFingerprint.isBlank() || studentId == null || studentId.isBlank()) {
            return false;
        }
        long now = System.currentTimeMillis();
        Map<String, Long> lastSeen = deviceStudentLastSeen.computeIfAbsent(deviceFingerprint, k -> new HashMap<>());
        lastSeen.entrySet().removeIf(entry -> now - entry.getValue() > DEVICE_SHARING_WINDOW_MS);
        lastSeen.put(studentId, now);
        return lastSeen.size() > 1;
    }

    /**
     * Check cumulative violations - block if too many
     */
    public boolean isStudentBlocked(String studentId) {
        LocalDateTime since24HoursAgo = LocalDateTime.now().minusHours(24);
        int recentViolations = securityAuditRepository.countRecentViolations(studentId, since24HoursAgo);
        
        boolean isBlocked = recentViolations >= VIOLATION_THRESHOLD;
        
        if (isBlocked) {
            System.out.println("[ANTI-CHEAT] STUDENT BLOCKED: " + studentId + " has " + recentViolations + " violations");
        }
        
        return isBlocked;
    }

    /**
     * Log security violation
     */
    @Transactional
    public SecurityAudit logViolation(String studentId, String violationType, String description,
                                     String deviceId, String ipAddress,
                                     Double studentLat, Double studentLon,
                                     Double expectedLat, Double expectedLon,
                                     String severityLevel) {
        
        SecurityAudit audit = new SecurityAudit();
        audit.setStudentId(studentId);
        audit.setViolationType(violationType);
        audit.setDescription(description);
        audit.setDeviceId(deviceId);
        audit.setIpAddress(ipAddress);
        audit.setStudentLatitude(studentLat);
        audit.setStudentLongitude(studentLon);
        audit.setExpectedLatitude(expectedLat);
        audit.setExpectedLongitude(expectedLon);
        audit.setSeverityLevel(severityLevel);

        if (studentLat != null && expectedLat != null) {
            GeolocationService geoService = new GeolocationService();
            double distance = geoService.calculateDistanceKm(studentLat, studentLon, expectedLat, expectedLon);
            audit.setDistanceKm(distance);
        }

        // Auto-block on CRITICAL violations
        if ("CRITICAL".equals(severityLevel)) {
            audit.setIsBlocked(true);
        }

        return securityAuditRepository.save(audit);
    }

    /**
     * Helper class for tracking location history
     */
    private static class LocationTimestamp {
        double lat;
        double lon;
        long timestamp;

        LocationTimestamp(double lat, double lon, long timestamp) {
            this.lat = lat;
            this.lon = lon;
            this.timestamp = timestamp;
        }
    }

    /**
     * VPN Detection Result
     */
    public static class VPNDetectionResult {
        public boolean isVPNDetected;
        public int suspicionScore; // 0-100
        public String reason;
    }

    /**
     * Impossible Movement Result
     */
    public static class ImpossibleMovementResult {
        public boolean isImpossible;
        public String reason;
    }
}
