package com.sms.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.Map;

/**
 * Service for generating and validating attendance QR tokens
 * Uses JWT for secure, expiring tokens
 */
@Service
public class AttendanceQRTokenService {

    @Value("${jwt.secret:mySecureSecretKeyForJWTTokenGenerationAndValidation2024}")
    private String jwtSecret;

    private static final int DEFAULT_EXPIRY_MINUTES = 5;
    private static final int MAX_EXPIRY_MINUTES = 30;
    private static final int DEFAULT_EXPIRY_SECONDS = 10;
    private static final int MIN_EXPIRY_SECONDS = 8;
    private static final int MAX_EXPIRY_SECONDS = 7200;

    /**
     * Generate secure JWT token for attendance QR
     * Token contains: SubjectID | TeacherID | Timestamp | SessionID
     */
    public String generateAttendanceToken(Long subjectId, Long teacherId, Integer expiryMinutes) {
        int exMins = (expiryMinutes == null || expiryMinutes <= 0) ? DEFAULT_EXPIRY_MINUTES : expiryMinutes;
        if (exMins > MAX_EXPIRY_MINUTES)
            exMins = MAX_EXPIRY_MINUTES;
        int expirySeconds = exMins * 60;
        return generateAttendanceToken(subjectId, teacherId, expirySeconds, null, null, null);
    }

    public String generateAttendanceToken(Long subjectId,
            Long teacherId,
            Integer expirySeconds,
            Double teacherLatitude,
            Double teacherLongitude,
            Integer maxDistanceMeters) {
        return generateAttendanceToken(subjectId, teacherId, expirySeconds, teacherLatitude, teacherLongitude,
                maxDistanceMeters, true);
    }

    public String generateAttendanceToken(Long subjectId,
            Long teacherId,
            Integer expirySeconds,
            Double teacherLatitude,
            Double teacherLongitude,
            Integer maxDistanceMeters,
            Boolean faceVerificationRequired) {
        // Validate expiry
        int exSecs = (expirySeconds == null || expirySeconds <= 0) ? DEFAULT_EXPIRY_SECONDS : expirySeconds;
        exSecs = Math.max(MIN_EXPIRY_SECONDS, Math.min(MAX_EXPIRY_SECONDS, exSecs));
        Integer boundedDistance = maxDistanceMeters == null ? 150 : Math.max(100, Math.min(200, maxDistanceMeters));

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiryTime = now.plusSeconds(exSecs);

        var builder = Jwts.builder()
                .subject("ATTENDANCE")
                .claim("subjectId", subjectId)
                .claim("teacherId", teacherId)
                .claim("sessionId", sessionId)
                .claim("type", "ATTENDANCE_QR")
                .claim("maxDistanceMeters", boundedDistance)
                .claim("faceVerificationRequired", !Boolean.FALSE.equals(faceVerificationRequired))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryTime));

        if (teacherLatitude != null && teacherLongitude != null) {
            builder.claims(Map.of(
                    "teacherLatitude", teacherLatitude,
                    "teacherLongitude", teacherLongitude));
        }

        return builder
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate attendance token and extract claims
     */
    public AttendanceTokenClaims validateAttendanceToken(String token) throws Exception {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

            var claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Verify token type
            if (!"ATTENDANCE_QR".equals(claims.get("type"))) {
                throw new IllegalArgumentException("Invalid token type");
            }

            return new AttendanceTokenClaims(
                    ((Number) claims.get("subjectId")).longValue(),
                    ((Number) claims.get("teacherId")).longValue(),
                    (String) claims.get("sessionId"),
                    claims.getIssuedAt().getTime(),
                    claims.getExpiration().getTime(),
                    getNullableDouble(claims.get("teacherLatitude")),
                    getNullableDouble(claims.get("teacherLongitude")),
                    claims.get("maxDistanceMeters") instanceof Number n ? n.intValue() : null,
                    claims.get("faceVerificationRequired") instanceof Boolean b ? b : true);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or tampered token: " + e.getMessage(), e);
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(long expirationTime) {
        return System.currentTimeMillis() > expirationTime;
    }

    /**
     * Get remaining validity in seconds
     */
    public long getRemainingValidity(long expirationTime) {
        long remaining = (expirationTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    /**
     * Generate QR code image from token
     */
    public byte[] generateQRCode(String token) throws Exception {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(token, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage(), e);
        }
    }

    /**
     * Generate QR code as Base64 data URI
     */
    public String generateQRCodeBase64(String token) throws Exception {
        byte[] qrImage = generateQRCode(token);
        String base64 = Base64.getEncoder().encodeToString(qrImage);
        return "data:image/png;base64," + base64;
    }

    /**
     * Generate hash of token for duplicate checking
     */
    public String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((token == null ? "" : token).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash attendance token", ex);
        }
    }

    /**
     * DTO for attendance token claims
     */
    public static class AttendanceTokenClaims {
        private final Long subjectId;
        private final Long teacherId;
        private final String sessionId;
        private final long issuedAt;
        private final long expiresAt;
        private final Double teacherLatitude;
        private final Double teacherLongitude;
        private final Integer maxDistanceMeters;
        private final Boolean faceVerificationRequired;

        public AttendanceTokenClaims(Long subjectId,
                Long teacherId,
                String sessionId,
                long issuedAt,
                long expiresAt,
                Double teacherLatitude,
                Double teacherLongitude,
                Integer maxDistanceMeters,
                Boolean faceVerificationRequired) {
            this.subjectId = subjectId;
            this.teacherId = teacherId;
            this.sessionId = sessionId;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
            this.teacherLatitude = teacherLatitude;
            this.teacherLongitude = teacherLongitude;
            this.maxDistanceMeters = maxDistanceMeters;
            this.faceVerificationRequired = faceVerificationRequired;
        }

        public Long getSubjectId() {
            return subjectId;
        }

        public Long getTeacherId() {
            return teacherId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public long getIssuedAt() {
            return issuedAt;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public Double getTeacherLatitude() {
            return teacherLatitude;
        }

        public Double getTeacherLongitude() {
            return teacherLongitude;
        }

        public Integer getMaxDistanceMeters() {
            return maxDistanceMeters;
        }

        public Boolean getFaceVerificationRequired() {
            return faceVerificationRequired;
        }
    }

    private Double getNullableDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
}
