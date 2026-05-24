package com.sms.service;

import com.sms.dto.ai.AiIngestionAnalyzeResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;

@Service
public class SmartIngestionAnalysisService {

    private static final int MAX_HEADERS = 40;

    public AiIngestionAnalyzeResponse analyze(List<MultipartFile> files, String targetEntity) {
        List<MultipartFile> safeFiles = files == null ? List.of() : files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        List<AiIngestionAnalyzeResponse.FileAnalysis> fileAnalyses = new ArrayList<>();
        Set<String> headers = new LinkedHashSet<>();
        List<AiIngestionAnalyzeResponse.DataQualityWarning> warnings = new ArrayList<>();

        for (MultipartFile file : safeFiles) {
            FileProbe probe = probe(file);
            headers.addAll(probe.headers());
            fileAnalyses.add(new AiIngestionAnalyzeResponse.FileAnalysis(
                    safeName(file.getOriginalFilename()),
                    file.getContentType(),
                    detectDocumentType(file, probe.headers()),
                    probe.headers(),
                    probe.confidence()));
            warnings.addAll(probe.warnings());
        }

        String normalizedTarget = normalizeTarget(targetEntity, headers);
        List<AiIngestionAnalyzeResponse.FieldMapping> mappings = headers.stream()
                .map(header -> mapHeader(header, normalizedTarget))
                .filter(mapping -> mapping.confidence() >= 0.5)
                .toList();

        warnings.addAll(mappingWarnings(headers, mappings));

        return new AiIngestionAnalyzeResponse(
                normalizedTarget,
                safeFiles.size(),
                fileAnalyses,
                mappings,
                warnings,
                buildNextActions(safeFiles, mappings, warnings));
    }

    private FileProbe probe(MultipartFile file) {
        String name = safeName(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".csv") || "text/csv".equalsIgnoreCase(file.getContentType())) {
                return probeCsv(file);
            }
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                return probeSpreadsheet(file);
            }
            if (name.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(file.getContentType())) {
                return probePdf(file);
            }
            if (name.endsWith(".zip")) {
                return probeZip(file);
            }
            if (isImage(file)) {
                return new FileProbe(List.of(), 0.7, List.of(warning(
                        "INFO",
                        "OCR_REQUIRED",
                        "Image upload detected. Send this file to the OCR worker before committing records.",
                        Map.of("fileName", safeName(file.getOriginalFilename())))));
            }
        } catch (Exception ex) {
            return new FileProbe(List.of(), 0.35, List.of(warning(
                    "WARN",
                    "EXTRACTION_FAILED",
                    "Could not extract preview headers from this file; manual review is required.",
                    Map.of("fileName", safeName(file.getOriginalFilename()), "reason", ex.getMessage()))));
        }

        return new FileProbe(List.of(), 0.45, List.of(warning(
                "WARN",
                "UNKNOWN_FILE_TYPE",
                "File type is not recognized by the smart ingestion preview.",
                Map.of("fileName", safeName(file.getOriginalFilename())))));
    }

    private FileProbe probeCsv(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return new FileProbe(List.of(), 0.4, List.of(warning("WARN", "EMPTY_CSV", "CSV file has no header row.", Map.of())));
            }
            return new FileProbe(splitHeaders(headerLine), 0.92, List.of());
        }
    }

    private FileProbe probeSpreadsheet(MultipartFile file) throws Exception {
        try (var input = file.getInputStream(); var workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                return new FileProbe(List.of(), 0.4, List.of(warning("WARN", "EMPTY_WORKBOOK", "Workbook has no sheets.", Map.of())));
            }
            Row row = sheet.getRow(sheet.getFirstRowNum());
            if (row == null) {
                return new FileProbe(List.of(), 0.4, List.of(warning("WARN", "EMPTY_SHEET", "First sheet has no header row.", Map.of())));
            }
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < Math.min(row.getLastCellNum(), MAX_HEADERS); i++) {
                String value = row.getCell(i) == null ? "" : row.getCell(i).toString().trim();
                if (!value.isBlank()) {
                    headers.add(value);
                }
            }
            return new FileProbe(headers, 0.94, List.of());
        }
    }

    private FileProbe probePdf(MultipartFile file) throws Exception {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(2, document.getNumberOfPages()));
            String text = stripper.getText(document);
            List<String> headers = inferHeadersFromText(text);
            List<AiIngestionAnalyzeResponse.DataQualityWarning> warnings = new ArrayList<>();
            if (headers.isEmpty()) {
                warnings.add(warning("INFO", "PDF_REQUIRES_AI_PARSE", "PDF text was extracted but headers need AI parsing or manual review.", Map.of()));
            }
            return new FileProbe(headers, headers.isEmpty() ? 0.55 : 0.76, warnings);
        }
    }

    private FileProbe probeZip(MultipartFile file) throws Exception {
        List<String> entries = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null && entries.size() < 25) {
                if (!entry.isDirectory()) {
                    entries.add(entry.getName());
                }
            }
        }
        return new FileProbe(entries, 0.72, List.of(warning(
                "INFO",
                "ZIP_BUNDLE_DETECTED",
                "ZIP bundle detected. Each file should be analyzed in the async ingestion worker.",
                Map.of("entryCountPreview", entries.size()))));
    }

    private String detectDocumentType(MultipartFile file, List<String> headers) {
        String name = safeName(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        String joined = String.join(" ", headers).toLowerCase(Locale.ROOT);
        if (name.endsWith(".zip")) return "MIXED_ZIP_BUNDLE";
        if (isImage(file)) return "SCANNED_IMAGE";
        if (name.endsWith(".pdf")) return "PDF_DOCUMENT";
        if (containsAny(joined, "attendance", "present", "absent")) return "ATTENDANCE";
        if (containsAny(joined, "marks", "score", "grade", "physics", "math", "chemistry")) return "MARKSHEET";
        if (containsAny(joined, "period", "teacher", "room", "weekday", "slot")) return "TIMETABLE";
        if (containsAny(joined, "fee", "paid", "due", "receipt")) return "FEE";
        if (containsAny(joined, "roll", "student", "name", "enrollment")) return "STUDENT_PROFILE";
        return "UNKNOWN_ACADEMIC_DOCUMENT";
    }

    private AiIngestionAnalyzeResponse.FieldMapping mapHeader(String header, String targetEntity) {
        String normalized = normalize(header);
        if (containsAny(normalized, "stu name", "student name", "full name", "name")) {
            return mapping(header, "studentName", targetEntity, 0.96, "Name-like header detected");
        }
        if (containsAny(normalized, "roll", "roll no", "roll number")) {
            return mapping(header, "rollNumber", targetEntity, 0.95, "Roll number synonym detected");
        }
        if (containsAny(normalized, "enrollment", "admission no", "registration")) {
            return mapping(header, "enrollmentNumber", targetEntity, 0.93, "Enrollment identifier synonym detected");
        }
        if (containsAny(normalized, "phy", "physics")) {
            return mapping(header, "physicsMarks", "marks", 0.91, "Subject shorthand resolved to Physics");
        }
        if (containsAny(normalized, "chem", "chemistry")) {
            return mapping(header, "chemistryMarks", "marks", 0.91, "Subject shorthand resolved to Chemistry");
        }
        if (containsAny(normalized, "math", "mathematics")) {
            return mapping(header, "mathematicsMarks", "marks", 0.91, "Subject shorthand resolved to Mathematics");
        }
        if (containsAny(normalized, "marks", "score", "grade")) {
            return mapping(header, "score", "marks", 0.84, "Assessment score field detected");
        }
        if (containsAny(normalized, "attendance", "present", "absent", "status")) {
            return mapping(header, "attendanceStatus", "attendance", 0.86, "Attendance status field detected");
        }
        if (containsAny(normalized, "subject", "course")) {
            return mapping(header, "subject", targetEntity, 0.8, "Subject/course field detected");
        }
        if (containsAny(normalized, "teacher", "faculty")) {
            return mapping(header, "teacherName", "teacher", 0.84, "Teacher/faculty field detected");
        }
        if (containsAny(normalized, "date", "day")) {
            return mapping(header, "date", targetEntity, 0.78, "Date-like field detected");
        }
        return mapping(header, "unmapped", targetEntity, 0.2, "No confident mapping found");
    }

    private List<AiIngestionAnalyzeResponse.DataQualityWarning> mappingWarnings(
            Set<String> headers,
            List<AiIngestionAnalyzeResponse.FieldMapping> mappings) {
        List<AiIngestionAnalyzeResponse.DataQualityWarning> warnings = new ArrayList<>();
        Set<String> canonical = new LinkedHashSet<>();
        for (AiIngestionAnalyzeResponse.FieldMapping mapping : mappings) {
            if (!"unmapped".equals(mapping.canonicalField()) && !canonical.add(mapping.canonicalField())) {
                warnings.add(warning(
                        "WARN",
                        "DUPLICATE_CANONICAL_FIELD",
                        "Multiple source columns map to the same canonical field.",
                        Map.of("canonicalField", mapping.canonicalField())));
            }
        }
        boolean hasStudentIdentity = canonical.contains("rollNumber") || canonical.contains("enrollmentNumber");
        if (!headers.isEmpty() && !hasStudentIdentity) {
            warnings.add(warning(
                    "ERROR",
                    "MISSING_STUDENT_IDENTIFIER",
                    "No roll number or enrollment number column was detected.",
                    Map.of("expectedAnyOf", List.of("rollNumber", "enrollmentNumber"))));
        }
        return warnings;
    }

    private List<String> buildNextActions(List<MultipartFile> files,
                                          List<AiIngestionAnalyzeResponse.FieldMapping> mappings,
                                          List<AiIngestionAnalyzeResponse.DataQualityWarning> warnings) {
        if (files == null || files.isEmpty()) {
            return List.of("Upload at least one academic source file.");
        }
        if (warnings.stream().anyMatch(w -> "ERROR".equals(w.severity()))) {
            return List.of("Review missing required fields", "Adjust mappings manually", "Re-run analysis");
        }
        if (mappings.isEmpty()) {
            return List.of("Send files to OCR/AI parser", "Review extracted fields", "Confirm import preview");
        }
        return List.of("Review suggested mappings", "Run duplicate and conflict validation", "Create preview import job");
    }

    private List<String> splitHeaders(String line) {
        List<String> result = new ArrayList<>();
        for (String item : line.split(",|\\t|;")) {
            String header = item.replace("\"", "").trim();
            if (!header.isBlank() && result.size() < MAX_HEADERS) {
                result.add(header);
            }
        }
        return result;
    }

    private List<String> inferHeadersFromText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String firstLine = text.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse("");
        return splitHeaders(firstLine.replaceAll("\\s{2,}", ","));
    }

    private String normalizeTarget(String targetEntity, Set<String> headers) {
        if (targetEntity != null && !targetEntity.isBlank()) {
            return targetEntity.trim().toLowerCase(Locale.ROOT);
        }
        String joined = String.join(" ", headers).toLowerCase(Locale.ROOT);
        if (containsAny(joined, "attendance", "present", "absent")) return "attendance";
        if (containsAny(joined, "marks", "score", "grade", "phy", "chem", "math")) return "marks";
        if (containsAny(joined, "teacher", "period", "room")) return "timetable";
        return "student";
    }

    private AiIngestionAnalyzeResponse.FieldMapping mapping(String sourceField,
                                                            String canonicalField,
                                                            String targetEntity,
                                                            double confidence,
                                                            String reason) {
        return new AiIngestionAnalyzeResponse.FieldMapping(sourceField, canonicalField, targetEntity, confidence, reason);
    }

    private AiIngestionAnalyzeResponse.DataQualityWarning warning(String severity,
                                                                  String code,
                                                                  String message,
                                                                  Map<String, Object> context) {
        return new AiIngestionAnalyzeResponse.DataQualityWarning(severity, code, message, context);
    }

    private boolean isImage(MultipartFile file) {
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String name = safeName(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        return type.startsWith("image/") || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp");
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String safeName(String fileName) {
        return fileName == null || fileName.isBlank() ? "upload" : fileName;
    }

    private record FileProbe(
            List<String> headers,
            double confidence,
            List<AiIngestionAnalyzeResponse.DataQualityWarning> warnings) {
    }
}
