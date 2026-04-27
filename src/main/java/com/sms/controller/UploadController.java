package com.sms.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sms.service.ImportService;

@RestController
@RequestMapping("/api/admin/upload")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    @Autowired
    private ImportService importService;

    @PostMapping("/{type}")
    public ResponseEntity<?> uploadCsv(
            @PathVariable String type,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String batchId) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded");
        }

        try {
            importService.importFile(type.toLowerCase(), file, classId, batchId);
            return ResponseEntity.ok("File accepted for processing");
        } catch (IOException ex) {
            log.error("Failed to import file", ex);
            return ResponseEntity.status(500).body("Import failed: " + ex.getMessage());
        }
    }
}
