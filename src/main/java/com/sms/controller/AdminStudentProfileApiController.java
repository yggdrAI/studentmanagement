package com.sms.controller;

import java.util.Map;

import com.sms.dto.profile.AdminUpdateStudentProfileRequest;
import com.sms.dto.profile.StudentProfileResponseDTO;
import com.sms.model.Student;
import com.sms.model.StudentProfile;
import com.sms.repository.StudentProfileRepository;
import com.sms.service.ImageUploadService;
import com.sms.service.StudentProfileService;
import com.sms.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/student")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStudentProfileApiController {

    private final StudentProfileService studentProfileService;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentService studentService;
    private final ImageUploadService imageUploadService;

    public AdminStudentProfileApiController(StudentProfileService studentProfileService,
                                            StudentProfileRepository studentProfileRepository,
                                            StudentService studentService,
                                            ImageUploadService imageUploadService) {
        this.studentProfileService = studentProfileService;
        this.studentProfileRepository = studentProfileRepository;
        this.studentService = studentService;
        this.imageUploadService = imageUploadService;
    }

    @GetMapping("/{studentId}/profile")
    public ResponseEntity<StudentProfileResponseDTO> getStudentProfile(@PathVariable String studentId) {
        return ResponseEntity.ok(studentProfileService.getProfileForAdmin(studentId));
    }

    @PutMapping("/{studentId}/profile")
    public ResponseEntity<StudentProfileResponseDTO> updateStudentProfile(@PathVariable String studentId,
                                                                          @RequestBody AdminUpdateStudentProfileRequest request,
                                                                          Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin authentication is required");
        }
        return ResponseEntity.ok(studentProfileService.updateByAdmin(studentId, request, auth.getName()));
    }

    /**
     * Upload a profile photo for a student via base64 JSON payload.
     * The frontend compresses the image client-side and sends it as a data URI.
     * We store the processed image in both the StudentProfile entity (profileImage
     * and profilePhotoUrl) and on the Student entity (profileImageUrl) for maximum
     * compatibility across all views.
     */
    @PostMapping("/{studentId}/photo")
    public ResponseEntity<Map<String, Object>> uploadStudentPhoto(
            @PathVariable String studentId,
            @RequestBody Map<String, String> payload) {

        String photoBase64 = payload.get("photoBase64");
        if (photoBase64 == null || photoBase64.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "photoBase64 is required");
        }

        // Validate student exists
        Student student = studentService.findById(studentId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));

        // Process the base64 image (validates, normalises, detects MIME type)
        String processedDataUri;
        try {
            processedDataUri = imageUploadService.uploadBase64Image(photoBase64, studentId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        if (processedDataUri == null || processedDataUri.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to process image data");
        }

        // Persist on StudentProfile (create if missing)
        StudentProfile profile = studentProfileRepository.findById(studentId).orElse(null);
        if (profile == null) {
            profile = new StudentProfile();
            profile.setStudentId(studentId);
        }
        profile.setProfileImage(processedDataUri);
        profile.setProfilePhotoUrl(normalizePhotoUrl(processedDataUri, profile.getProfilePhotoUrl()));
        studentProfileRepository.save(profile);

        // Also persist on the Student entity for backward compatibility. StudentService
        // mirrors this field back onto StudentProfile during save, so keep the original
        // data URI here and only guard the VARCHAR profilePhotoUrl field above.
        student.setProfileImageUrl(processedDataUri);
        studentService.save(student);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Profile photo uploaded successfully",
                "studentId", studentId));
    }

    private String normalizePhotoUrl(String profileImage, String existingPhotoUrl) {
        if (profileImage == null || profileImage.isBlank()) {
            return existingPhotoUrl;
        }

        String normalized = profileImage.trim();
        if (normalized.startsWith("data:image") || normalized.length() > 2048) {
            return existingPhotoUrl;
        }

        return normalized;
    }
}
