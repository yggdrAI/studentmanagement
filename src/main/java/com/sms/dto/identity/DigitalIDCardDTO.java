package com.sms.dto.identity;

import java.time.LocalDate;

/**
 * Digital ID Card data transfer object
 * Contains all fields needed for rendering a government-style ID card
 */
public record DigitalIDCardDTO(
        String studentId,
        String fullName,
        String email,
        String phone,
        String course,
        String department,
        String semester,
        String rollNumber,
        String enrollmentYear,
        String profileImageUrl,
        LocalDate dateOfBirth,
        String institutionName,
        String validUntil,
        String qrCodeBase64,
        String status
) {}
