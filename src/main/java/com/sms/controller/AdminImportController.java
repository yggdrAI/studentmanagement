package com.sms.controller;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sms.dto.imports.StudentImportConfirmRequest;
import com.sms.dto.imports.StudentImportRowUpdateRequest;
import com.sms.model.StudentImportJob;
import com.sms.service.StudentImportService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/import")
@PreAuthorize("hasRole('ADMIN')")
public class AdminImportController {

    private final StudentImportService studentImportService;
    private final ObjectMapper objectMapper;

    public AdminImportController(StudentImportService studentImportService, ObjectMapper objectMapper) {
        this.studentImportService = studentImportService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/students", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadStudents(
        @RequestParam(value = "file", required = false) MultipartFile file,
        @RequestParam(value = "files", required = false) List<MultipartFile> files,
        @RequestParam(name = "duplicateStrategy", defaultValue = "SKIP") String duplicateStrategy,
        @RequestParam(name = "rollbackOnFailure", defaultValue = "true") Boolean rollbackOnFailure,
        @RequestParam(name = "mappingJson", required = false) String mappingJson,
        Authentication authentication) {

        List<MultipartFile> uploadFiles = new java.util.ArrayList<>();
        if (files != null) {
            uploadFiles.addAll(files);
        }
        if (file != null) {
            uploadFiles.add(file);
        }

        Map<String, Object> preview = studentImportService.uploadAndPreview(
            uploadFiles,
            authentication.getName(),
            duplicateStrategy,
            rollbackOnFailure,
            parseColumnMapping(mappingJson)
        );
        return ResponseEntity.ok(preview);
    }

    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> preview(@RequestParam("jobId") Long jobId, Authentication authentication) {
        return ResponseEntity.ok(studentImportService.getPreview(jobId, authentication.getName()));
    }

    @PutMapping("/rows/{rowId}")
    public ResponseEntity<Map<String, Object>> updateRow(@RequestParam("jobId") Long jobId,
                                                         @PathVariable Long rowId,
                                                         @RequestBody StudentImportRowUpdateRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(studentImportService.updateRow(jobId, rowId, request, authentication.getName()));
    }

    @DeleteMapping("/rows/{rowId}")
    public ResponseEntity<Map<String, Object>> deleteRow(@RequestParam("jobId") Long jobId,
                                                          @PathVariable Long rowId,
                                                          Authentication authentication) {
        return ResponseEntity.ok(studentImportService.deleteRow(jobId, rowId, authentication.getName()));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirm(@Valid @RequestBody StudentImportConfirmRequest request,
                                                       Authentication authentication) {
        Map<String, Object> response = studentImportService.confirmImport(
            request.getJobId(),
            authentication.getName(),
            request.getDuplicateStrategy(),
            request.getRollbackOnFailure()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<Map<String, Object>>> logs() {
        return ResponseEntity.ok(studentImportService.listLogs());
    }

    @PostMapping("/rollback-last")
    public ResponseEntity<Map<String, Object>> rollback(Authentication authentication) {
        return ResponseEntity.ok(studentImportService.rollbackLastImport(authentication.getName()));
    }

    @GetMapping("/sample-template")
    public ResponseEntity<ByteArrayResource> sampleTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Students");
            Row header = sheet.createRow(0);
            List<String> headers = List.of(
                "Full Name",
                "Enrollment Number",
                "Roll Number",
                "Email",
                "Phone",
                "Program",
                "Course",
                "Semester",
                "Department",
                "School",
                "Section",
                "Class",
                "House",
                "Joining Year",
                "Leaving Year",
                "Date of Birth",
                "Gender",
                "Address",
                "Blood Group",
                "Guardian Name"
            );
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
            }
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("Aarav Sharma");
            sample.createCell(1).setCellValue("BU2026001");
            sample.createCell(2).setCellValue("22BCS045");
            sample.createCell(3).setCellValue("aarav.sharma@bennett.edu.in");
            sample.createCell(4).setCellValue("9876543210");
            sample.createCell(5).setCellValue("B.Tech CSE");
            sample.createCell(6).setCellValue("B.Tech CSE");
            sample.createCell(7).setCellValue("Semester 1");
            sample.createCell(8).setCellValue("Computer Science");
            sample.createCell(9).setCellValue("School of Engineering");
            sample.createCell(10).setCellValue("CSE-A");
            sample.createCell(11).setCellValue("Cedar");
            sample.createCell(12).setCellValue("2022");
            sample.createCell(13).setCellValue("2026");
            sample.createCell(14).setCellValue("2005-08-15");
            sample.createCell(15).setCellValue("Male");
            sample.createCell(16).setCellValue("Greater Noida, UP");
            sample.createCell(17).setCellValue("O+");
            sample.createCell(18).setCellValue("Rajesh Sharma");
            workbook.write(outputStream);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student-import-template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(outputStream.toByteArray()));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate sample template", ex);
        }
    }

    private Map<String, String> parseColumnMapping(String mappingJson) {
        if (mappingJson == null || mappingJson.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            Map<String, String> parsed = objectMapper.readValue(mappingJson, new TypeReference<Map<String, String>>() {});
            return parsed == null ? Collections.emptyMap() : parsed;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid column mapping payload");
        }
    }
}
