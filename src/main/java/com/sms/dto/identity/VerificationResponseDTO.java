package com.sms.dto.identity;

import java.time.Instant;

/**
 * Response after QR code verification
 * Contains student details and verification status
 */
public record VerificationResponseDTO(
        boolean verified,
        String status,  // ACTIVE, EXPIRED, INVALID, TAMPERED
        String studentId,
        String fullName,
        String course,
        String department,
        String enrollmentYear,
        Instant verifiedAt,
        Long secondsUntilExpiry,
        String message
) {}
