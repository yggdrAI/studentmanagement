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
import java.time.Instant;
import java.util.Date;

/**
 * Secure QR Code generation and token management for digital ID verification.
 * Uses JWT-signed tokens to prevent tampering and ensure authenticity.
 */
@Service
public class QRService {

    @Value("${jwt.secret:mySecureSecretKeyForJWTTokenGenerationAndValidation2024}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")  // 24 hours default
    private long jwtExpirationMs;

    /**
     * Generate a secure JWT token containing student ID and verification metadata
     * This token is embedded in the QR code
     */
    public String generateSecureToken(String studentId, String studentName, String course) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        
        Instant now = Instant.now();
        Instant expiryTime = now.plusMillis(jwtExpirationMs);

        return Jwts.builder()
                .subject(studentId)
                .claim("name", studentName)
                .claim("course", course)
                .claim("type", "STUDENT_ID")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Verify and decode JWT token from QR scan
     */
    public IDTokenClaims verifyToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            
            var claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new IDTokenClaims(
                    claims.getSubject(),
                    (String) claims.get("name"),
                    (String) claims.get("course"),
                    claims.getIssuedAt(),
                    claims.getExpiration()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or tampered token: " + e.getMessage());
        }
    }

    /**
     * Generate QR code image from JWT token
     * QR contains only the signed token (no raw student data)
     */
    public byte[] generateQRCode(String token) throws Exception {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(token, BarcodeFormat.QR_CODE, 250, 250);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage(), e);
        }
    }

    /**
     * Record representing parsed JWT claims from QR token
     */
    public record IDTokenClaims(
            String studentId,
            String name,
            String course,
            Date issuedAt,
            Date expiresAt
    ) {
        public boolean isExpired() {
            return expiresAt.before(new Date());
        }

        public long getSecondsUntilExpiry() {
            return (expiresAt.getTime() - System.currentTimeMillis()) / 1000;
        }
    }
}
