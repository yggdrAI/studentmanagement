package com.sms.controller;

import com.sms.dto.identity.DigitalIDCardDTO;
import com.sms.dto.identity.VerificationResponseDTO;
import com.sms.model.Student;
import com.sms.service.QRService;
import com.sms.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;

/**
 * API endpoints for secure digital ID card with QR verification
 * Handles:
 * - QR code generation and delivery
 * - QR token verification
 * - ID card data retrieval with embedded QR
 */
@RestController
@RequestMapping("/api/student/id")
@PreAuthorize("hasRole('STUDENT')")
public class DigitalIDApi {

    @Autowired
    private QRService qrService;

    @Autowired
    private StudentService studentService;

    /**
     * Fetch authenticated student's digital ID card with embedded QR code
     * GET /api/student/id/card
     */
    @GetMapping("/card")
    public ResponseEntity<DigitalIDCardDTO> getDigitalIDCard(Authentication auth) {
        String username = auth.getName();
        Student student = studentService.getStudentByUsername(username);

        // Generate secure QR token for this student
        String qrToken = qrService.generateSecureToken(
                student.getId(),
                student.getName(),
                student.getCourse()
        );

        // Generate QR code image
        try {
            byte[] qrImage = qrService.generateQRCode(qrToken);
            String qrBase64 = Base64.getEncoder().encodeToString(qrImage);

            // Calculate valid until date (2 years from enrollment)
            LocalDate validUntil = LocalDate.now().plusYears(2);

            DigitalIDCardDTO card = new DigitalIDCardDTO(
                    student.getId(),
                    student.getName(),
                    student.getEmail() != null ? student.getEmail() : "student@bennett.edu",
                    student.getPhone() != null ? student.getPhone() : "+91-XXXXXXXXXX",
                    student.getCourse() != null ? student.getCourse() : "B.Tech",
                    student.getDepartment() != null ? student.getDepartment() : "CSE",
                    student.getSemester() != null ? student.getSemester() : "4",
                    student.getRollNumber() != null ? student.getRollNumber() : "001",
                    student.getEnrollmentYear() != null ? student.getEnrollmentYear() : "2022",
                    student.getProfileImageUrl() != null ? student.getProfileImageUrl() : "/images/default-avatar.png",
                    student.getDob(),
                    "Bennett University",
                    "31-May-" + validUntil.getYear(),
                    "data:image/png;base64," + qrBase64,
                    "ACTIVE"
            );

            return ResponseEntity.ok(card);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Verify QR token and return student verification details
     * POST /api/student/id/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<VerificationResponseDTO> verifyID(@RequestBody String token) {
        try {
            // Decode and verify JWT token from QR
            QRService.IDTokenClaims claims = qrService.verifyToken(token);

            // Check if token is expired
            if (claims.isExpired()) {
                return ResponseEntity.ok(new VerificationResponseDTO(
                        false,
                        "EXPIRED",
                        claims.studentId(),
                        claims.name(),
                        null,
                        null,
                        null,
                        Instant.now(),
                        0L,
                        "Digital ID has expired. Please renew from student portal."
                ));
            }

            // Fetch student details for verification
            Student student = studentService.getStudentById(claims.studentId());

            // Cross-verify token claims with database
            if (!student.getName().equals(claims.name())) {
                return ResponseEntity.ok(new VerificationResponseDTO(
                        false,
                        "TAMPERED",
                        claims.studentId(),
                        "UNKNOWN",
                        null,
                        null,
                        null,
                        Instant.now(),
                        0L,
                        "⚠️ SECURITY ALERT: ID card data has been tampered with!"
                ));
            }

            // Return successful verification
            return ResponseEntity.ok(new VerificationResponseDTO(
                    true,
                    "ACTIVE",
                    student.getId(),
                    student.getName(),
                    student.getCourse(),
                    student.getDepartment(),
                    student.getEnrollmentYear(),
                    Instant.now(),
                    claims.getSecondsUntilExpiry(),
                    "✅ Verified Student - Digital Identity Confirmed"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(new VerificationResponseDTO(
                    false,
                    "INVALID",
                    "UNKNOWN",
                    "UNKNOWN",
                    null,
                    null,
                    null,
                    Instant.now(),
                    0L,
                    "❌ Invalid or forged digital ID. Verification failed."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new VerificationResponseDTO(
                            false,
                            "ERROR",
                            "UNKNOWN",
                            "UNKNOWN",
                            null,
                            null,
                            null,
                            Instant.now(),
                            0L,
                            "Server error: " + e.getMessage()
                    ));
        }
    }

    /**
     * Serve QR code image directly as PNG
     * GET /api/student/id/qr
     */
    @GetMapping("/qr")
    public ResponseEntity<byte[]> getQRCode(Authentication auth) {
        try {
            String username = auth.getName();
            Student student = studentService.getStudentByUsername(username);

            // Generate secure token
            String qrToken = qrService.generateSecureToken(
                    student.getId(),
                    student.getName(),
                    student.getCourse()
            );

            // Generate QR image
            byte[] qrImage = qrService.generateQRCode(qrToken);

            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"student_id_qr.png\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrImage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
