package com.sms.service;

import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ImageUploadService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * Store a base64-encoded image as a data URI so it can be persisted in MySQL.
     */
    public String uploadBase64Image(String base64Data, String studentId) {
        if (!StringUtils.hasText(base64Data)) {
            return null;
        }

        // Extract base64 content (remove data URI prefix if present)
        String base64Content = extractBase64Content(base64Data);

        // Decode base64 to bytes
        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid base64 image data", e);
        }

        // Validate image size
        if (imageBytes.length > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image size exceeds maximum allowed size of " + (MAX_IMAGE_SIZE / 1024 / 1024) + "MB");
        }

        // Detect image type from magic bytes
        String imageType = detectImageType(imageBytes);
        if (imageType == null) {
            throw new IllegalArgumentException("Invalid image format. Only JPEG, PNG, GIF, and WebP are allowed");
        }

        return "data:" + imageType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * Extract base64 content from data URI (e.g., "data:image/jpeg;base64,/9j/...")
     */
    private String extractBase64Content(String base64Data) {
        if (base64Data.startsWith("data:")) {
            int commaIndex = base64Data.indexOf(",");
            if (commaIndex > 0) {
                return base64Data.substring(commaIndex + 1);
            }
        }
        return base64Data;
    }

    /**
     * Detect image type from file magic bytes
     */
    private String detectImageType(byte[] fileBytes) {
        if (fileBytes.length < 4) {
            return null;
        }

        // Check for JPEG (FF D8 FF)
        if (fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8 && fileBytes[2] == (byte) 0xFF) {
            return "image/jpeg";
        }

        // Check for PNG (89 50 4E 47)
        if (fileBytes[0] == (byte) 0x89 && fileBytes[1] == 0x50 && fileBytes[2] == 0x4E && fileBytes[3] == 0x47) {
            return "image/png";
        }

        // Check for GIF (47 49 46 38)
        if (fileBytes[0] == 0x47 && fileBytes[1] == 0x49 && fileBytes[2] == 0x46 && fileBytes[3] == 0x38) {
            return "image/gif";
        }

        // Check for WebP (RIFF ... WEBP)
        if (fileBytes[0] == 0x52 && fileBytes[1] == 0x49 && fileBytes[2] == 0x46 && fileBytes[3] == 0x46) {
            if (fileBytes.length >= 12 && 
                fileBytes[8] == 0x57 && fileBytes[9] == 0x45 && 
                fileBytes[10] == 0x42 && fileBytes[11] == 0x50) {
                return "image/webp";
            }
        }

        return null;
    }

    public void deleteImage(String imageUrl) {
        // Images are stored in the database now, so there is no filesystem cleanup.
    }
}
