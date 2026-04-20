package com.sms.service;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.dto.imports.StudentImportRowUpdateRequest;
import com.sms.model.Course;
import com.sms.model.Enrollment;
import com.sms.model.Student;
import com.sms.model.StudentImportJob;
import com.sms.model.StudentImportRow;
import com.sms.model.StudentProfile;
import com.sms.repository.CourseRepository;
import com.sms.repository.EnrollmentRepository;
import com.sms.repository.StudentImportJobRepository;
import com.sms.repository.StudentImportRowRepository;
import com.sms.repository.StudentProfileRepository;
import com.sms.repository.StudentRepository;

@Service
public class StudentImportService {

    private static final int BATCH_SIZE = 30;
    private static final int CLASS_SIZE = 120;

    private static final List<String> FIELD_ORDER = List.of(
        "fullName",
        "enrollmentNumber",
        "rollNumber",
        "email",
        "personalEmail",
        "phone",
        "program",
        "course",
        "semester",
        "department",
        "school",
        "section",
        "className",
        "house",
        "foundationClassroom",
        "teamNumber",
        "memberNumber",
        "joiningYear",
        "leavingYear",
        "dateOfBirth",
        "gender",
        "address",
        "bloodGroup",
        "guardianName"
    );

    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
        Map.entry("fullName", "Full Name"),
        Map.entry("enrollmentNumber", "Enrollment Number"),
        Map.entry("rollNumber", "Roll Number"),
        Map.entry("email", "Email"),
        Map.entry("personalEmail", "Personal Email"),
        Map.entry("phone", "Phone"),
        Map.entry("program", "Program"),
        Map.entry("course", "Course"),
        Map.entry("semester", "Semester"),
        Map.entry("department", "Department"),
        Map.entry("school", "School"),
        Map.entry("section", "Section / Class"),
        Map.entry("className", "Class"),
        Map.entry("house", "House"),
        Map.entry("foundationClassroom", "Foundation Classroom"),
        Map.entry("teamNumber", "Team Number"),
        Map.entry("memberNumber", "Member Number"),
        Map.entry("joiningYear", "Joining Year"),
        Map.entry("leavingYear", "Leaving Year"),
        Map.entry("dateOfBirth", "Date of Birth"),
        Map.entry("gender", "Gender"),
        Map.entry("address", "Address"),
        Map.entry("bloodGroup", "Blood Group"),
        Map.entry("guardianName", "Guardian Name")
    );

    private static final Set<String> REQUIRED_FIELDS = Set.of("fullName", "enrollmentNumber");
    private static final Pattern HEADER_TOKEN_SPLIT = Pattern.compile("\\s+");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{7,15}$");
    private static final Pattern ENROLLMENT_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,64}$");
    private static final Pattern ENROLLMENT_SERIAL_PATTERN = Pattern.compile("(\\d+)");
    private static final Pattern ENROLLMENT_YEAR_4_PATTERN = Pattern.compile("(19\\d{2}|20\\d{2})");
    private static final Pattern ENROLLMENT_YEAR_2_PREFIX_PATTERN = Pattern.compile("^(\\d{2})[A-Z].*");
    private static final List<String> ENROLLMENT_VALUE_ALIASES = List.of(
        "Enrollment Number",
        "Enrolment Number",
        "Enrollment No",
        "Enrollment No.",
        "Enrolment No",
        "Enrolment No.",
        "Enrollment",
        "Registration Number",
        "Registration No",
        "Registration No.",
        "Admission Number",
        "Admission No",
        "Admission No.",
        "Student Id",
        "Student ID",
        "Roll Number",
        "Roll No",
        "Roll No.",
        "Roll"
    );
    private static final DateTimeFormatter[] DATE_PATTERNS = new DateTimeFormatter[] {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd/MM/uuuu"),
        DateTimeFormatter.ofPattern("dd-MM-uuuu"),
        DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH)
    };

    private final StudentImportJobRepository jobRepository;
    private final StudentImportRowRepository rowRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentService studentService;
    private final StudentFusionService studentFusionService;
    private final StudentProfileRepository studentProfileRepository;
    private final ImportArtifactService importArtifactService;
    private final AnalyticsRealtimeNotifier analyticsRealtimeNotifier;
    private final AnalyticsCacheService analyticsCacheService;
    private final ObjectMapper objectMapper;

    @Value("${app.import.students.max-file-size-mb:10}")
    private int maxFileSizeMb;

    public StudentImportService(StudentImportJobRepository jobRepository,
                                StudentImportRowRepository rowRepository,
                                StudentRepository studentRepository,
                                EnrollmentRepository enrollmentRepository,
                                CourseRepository courseRepository,
                                StudentService studentService,
                                StudentFusionService studentFusionService,
                                StudentProfileRepository studentProfileRepository,
                                ImportArtifactService importArtifactService,
                                AnalyticsRealtimeNotifier analyticsRealtimeNotifier,
                                AnalyticsCacheService analyticsCacheService,
                                ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.rowRepository = rowRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.studentService = studentService;
        this.studentFusionService = studentFusionService;
        this.studentProfileRepository = studentProfileRepository;
        this.importArtifactService = importArtifactService;
        this.analyticsRealtimeNotifier = analyticsRealtimeNotifier;
        this.analyticsCacheService = analyticsCacheService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> uploadAndPreview(MultipartFile file,
                                                String uploadedBy,
                                                String duplicateStrategy,
                                                Boolean rollbackOnFailure,
                                                Map<String, String> mappingOverride) {
        return uploadAndPreview(file == null ? List.of() : List.of(file), uploadedBy, duplicateStrategy, rollbackOnFailure, mappingOverride);
    }

    @Transactional
    public Map<String, Object> uploadAndPreview(List<MultipartFile> files,
                                                String uploadedBy,
                                                String duplicateStrategy,
                                                Boolean rollbackOnFailure,
                                                Map<String, String> mappingOverride) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one CSV or XLSX file is required");
        }

        List<MultipartFile> activeFiles = files.stream()
            .filter(file -> file != null && !file.isEmpty())
            .toList();

        if (activeFiles.isEmpty()) {
            throw new IllegalArgumentException("At least one CSV or XLSX file is required");
        }

        for (MultipartFile file : activeFiles) {
            validateUpload(file);
        }

        StudentImportJob job = new StudentImportJob();
        List<StudentImportRow> allRows = new ArrayList<>();
        List<String> sourceFiles = new ArrayList<>();
        List<Map<String, Object>> fileSummaries = new ArrayList<>();

        for (MultipartFile file : activeFiles) {
            String safeName = safeFileName(file.getOriginalFilename());
            byte[] sourceBytes;
            try {
                sourceBytes = file.getBytes();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }

            String sourceFilePath = importArtifactService.saveUploadedSource(safeName, sourceBytes);
            sourceFiles.add(safeName);

            ParsedImport parsed = parse(safeName, sourceBytes, mappingOverride == null ? Collections.emptyMap() : mappingOverride);
            List<StudentImportRow> rows = validateRows(job, parsed.rows(), parsed.headerIndex(), parsed.headers());
            for (StudentImportRow row : rows) {
                row.setSourceFileName(safeName);
            }

            allRows.addAll(rows);
            fileSummaries.add(Map.of(
                "fileName", safeName,
                "headers", parsed.headers(),
                "rowCount", rows.size(),
                "filePath", sourceFilePath,
                "warnings", parsed.warnings(),
                "missingRequiredFields", parsed.missingRequiredFields(),
                "suggestions", parsed.suggestions(),
                "mapping", parsed.mappingByField()
            ));
        }

        job.setFileName(sourceFiles.size() == 1 ? sourceFiles.get(0) : sourceFiles.size() + " files merged");
        job.setUploadedBy(uploadedBy);
        job.setDuplicateStrategy(normalizeDuplicateStrategy(duplicateStrategy));
        job.setRollbackOnFailure(Boolean.TRUE.equals(rollbackOnFailure));
        job.setStatus(StudentImportJob.Status.UPLOADED);
        job.setSourceFileCount(sourceFiles.size());
        job.setSourceFilesJson(compactForDb(writeJson(sourceFiles), 240));
        job = jobRepository.save(job);

        rowRepository.saveAll(allRows);

        StudentFusionService.FusionResult fusion = studentFusionService.analyze(allRows);
        job.setMergeLogJson(buildMergeLogSummaryForDb(fusion));
        job.setFusedStudentCount(fusion.clusterCount());
        job.setTotalRows(allRows.size());
        job.setValidRows((int) allRows.stream().filter(row -> "VALID".equals(row.getStatus())).count());
        job.setInvalidRows((int) allRows.stream().filter(row -> "INVALID".equals(row.getStatus())).count());
        job.setFailureCount(job.getInvalidRows());
        job.setStatus(StudentImportJob.Status.PREVIEW_READY);
        jobRepository.save(job);

        return buildPreviewPayload(job, allRows, null, fusion, fileSummaries);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPreview(Long jobId, String requester) {
        StudentImportJob job = getOwnedJob(jobId, requester);
        List<StudentImportRow> rows = rowRepository.findByJobOrderByRowIndexAsc(job);
        return buildPreviewPayload(job, rows, null, studentFusionService.analyze(rows), List.of());
    }

    @Transactional
    public Map<String, Object> updateRow(Long jobId, Long rowId, StudentImportRowUpdateRequest request, String requester) {
        StudentImportJob job = getOwnedJob(jobId, requester);
        StudentImportRow row = rowRepository.findById(rowId)
            .filter(existing -> existing.getJob().getId().equals(job.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Import row not found"));

        if (request.getFullName() != null) row.setFullName(clean(request.getFullName()));
        if (request.getEnrollmentNumber() != null) row.setEnrollmentNumber(clean(request.getEnrollmentNumber()));
        if (request.getEmail() != null) row.setEmail(clean(request.getEmail()));
        if (request.getPersonalEmail() != null) row.setPersonalEmail(clean(request.getPersonalEmail()));
        if (request.getPhone() != null) row.setPhone(clean(request.getPhone()));
        if (request.getCourse() != null) row.setCourse(clean(request.getCourse()));
        if (request.getSemester() != null) row.setSemester(clean(request.getSemester()));
        if (request.getDepartment() != null) row.setDepartment(clean(request.getDepartment()));
        if (request.getSection() != null) row.setSection(clean(request.getSection()));
        if (request.getHouse() != null) row.setHouse(clean(request.getHouse()));
        if (request.getFoundationClassroom() != null) row.setFoundationClassroom(clean(request.getFoundationClassroom()));
        if (request.getTeamNumber() != null) row.setTeamNumber(clean(request.getTeamNumber()));
        if (request.getMemberNumber() != null) row.setMemberNumber(clean(request.getMemberNumber()));
        if (request.getDateOfBirth() != null) row.setDateOfBirth(clean(request.getDateOfBirth()));
        if (request.getGender() != null) row.setGender(clean(request.getGender()));
        if (request.getAddress() != null) row.setAddress(clean(request.getAddress()));
        if (request.getBloodGroup() != null) row.setBloodGroup(clean(request.getBloodGroup()));
        if (request.getGuardianName() != null) row.setGuardianName(clean(request.getGuardianName()));

        sanitizeRow(row);
        rowRepository.save(row);
        List<StudentImportRow> rows = revalidateAllRows(job);
        return buildPreviewPayload(job, rows, null, studentFusionService.analyze(rows), List.of());
    }

    @Transactional
    public Map<String, Object> deleteRow(Long jobId, Long rowId, String requester) {
        StudentImportJob job = getOwnedJob(jobId, requester);
        StudentImportRow row = rowRepository.findById(rowId)
            .filter(existing -> existing.getJob().getId().equals(job.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Import row not found"));

        rowRepository.delete(row);
        List<StudentImportRow> rows = revalidateAllRows(job);
        return buildPreviewPayload(job, rows, null, studentFusionService.analyze(rows), List.of());
    }

    public Map<String, Object> confirmImport(Long jobId, String requester, String duplicateStrategy, Boolean rollbackOnFailure) {
        StudentImportJob job = getOwnedJob(jobId, requester);
        List<StudentImportRow> rows = rowRepository.findByJobOrderByRowIndexAsc(job);
        String strategy = normalizeDuplicateStrategy(duplicateStrategy != null ? duplicateStrategy : job.getDuplicateStrategy());
        boolean rollback = rollbackOnFailure == null ? job.isRollbackOnFailure() : rollbackOnFailure;

        List<StudentImportRow> validRows = rows.stream()
            .filter(row -> "VALID".equals(row.getStatus()))
            .toList();
        if (validRows.isEmpty()) {
            throw new IllegalArgumentException("No valid rows available to import");
        }

        StudentFusionService.FusionResult fusion = studentFusionService.analyze(validRows);
        List<Map<String, Object>> mergedStudents = fusion.mergedStudents();

        List<String> createdStudentIds = new ArrayList<>();
        int success = 0;
        int failure = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        String errorReportPath = null;

        for (Map<String, Object> mergedStudent : mergedStudents) {
            try {
                ImportResult result = importMergedStudent(mergedStudent, rows, strategy);
                if (result.skipped()) {
                    markRowsForCluster(rows, mergedStudent, "SKIPPED", result.message(), null);
                    skipped++;
                } else {
                    markRowsForCluster(rows, mergedStudent, "IMPORTED", null, result.studentId());
                    createdStudentIds.add(result.studentId());
                    success++;
                }
            } catch (Exception ex) {
                markRowsForCluster(rows, mergedStudent, "FAILED", safeMessage(ex.getMessage()), null);
                failure++;
                errors.add(buildErrorLineForCluster(mergedStudent, safeMessage(ex.getMessage())));
                if (rollback) {
                    rollbackCreatedStudents(createdStudentIds);
                    job.setStatus(StudentImportJob.Status.FAILED);
                    job.setSuccessCount(0);
                    job.setFailureCount(mergedStudents.size());
                    if (!errors.isEmpty()) {
                        errorReportPath = exportErrors(errors);
                        attachErrorReport(job, errorReportPath);
                    }
                    jobRepository.save(job);
                    rowRepository.saveAll(rows);
                    Map<String, Object> rolledBack = new LinkedHashMap<>();
                    rolledBack.put("jobId", job.getId());
                    rolledBack.put("status", "ROLLED_BACK");
                    rolledBack.put("successCount", 0);
                    rolledBack.put("failureCount", mergedStudents.size());
                    rolledBack.put("skippedCount", skipped);
                    rolledBack.put("message", "Import failed and transaction was rolled back");
                    rolledBack.put("errorReport", errorReportPath);
                    return rolledBack;
                }
            }
        }

        if (!errors.isEmpty()) {
            errorReportPath = exportErrors(errors);
            attachErrorReport(job, errorReportPath);
        }

    job.setStatus(failure == 0 ? StudentImportJob.Status.CONFIRMED : (success > 0 ? StudentImportJob.Status.CONFIRMED : StudentImportJob.Status.FAILED));
        job.setSuccessCount(success);
        job.setFailureCount(failure);
        job.setDuplicateStrategy(strategy);
    job.setFusedStudentCount(mergedStudents.size());
    job.setMergeLogJson(buildMergeLogSummaryForDb(fusion));
        jobRepository.save(job);
        rowRepository.saveAll(rows);
        analyticsRealtimeNotifier.notifyStudentBulkImport(job.getId(), success);
        analyticsCacheService.evictAnalyticsCaches();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobId", job.getId());
        response.put("status", job.getStatus().name());
        response.put("successCount", success);
        response.put("failureCount", failure);
        response.put("skippedCount", skipped);
        response.put("message", success + " students imported" + (skipped > 0 ? (", " + skipped + " skipped") : ""));
        response.put("errorReport", errorReportPath);
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listLogs() {
        return jobRepository.findTop50ByOrderByUploadedAtDesc().stream().map(job -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("jobId", job.getId());
            item.put("fileName", job.getFileName());
            item.put("uploadedBy", job.getUploadedBy());
            item.put("uploadedAt", job.getUploadedAt());
            item.put("totalRows", job.getTotalRows());
            item.put("successCount", job.getSuccessCount());
            item.put("failureCount", job.getFailureCount());
            item.put("status", job.getStatus().name());
            item.put("duplicateStrategy", job.getDuplicateStrategy());
            item.put("sourceFileCount", job.getSourceFileCount());
            item.put("fusedStudentCount", job.getFusedStudentCount());
            item.put("errorReportName", job.getLastErrorReportName());
            item.put("errorReportPath", job.getLastErrorReportPath());
            return item;
        }).toList();
    }

    @Transactional
    public Map<String, Object> rollbackLastImport(String requester) {
        StudentImportJob job = jobRepository.findTop50ByOrderByUploadedAtDesc().stream()
            .filter(candidate -> requester.equals(candidate.getUploadedBy()))
            .filter(candidate -> candidate.getStatus() == StudentImportJob.Status.CONFIRMED)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No import available to rollback"));

        List<StudentImportRow> rows = rowRepository.findByJobOrderByRowIndexAsc(job);
        int deleted = 0;
        for (StudentImportRow row : rows) {
            if (row.getCreatedStudentId() != null && studentRepository.existsById(row.getCreatedStudentId())) {
                studentService.deleteById(row.getCreatedStudentId());
                deleted++;
            }
        }

        job.setStatus(StudentImportJob.Status.ROLLED_BACK);
        jobRepository.save(job);
        return Map.of(
            "jobId", job.getId(),
            "rolledBack", deleted,
            "message", "Last import rolled back"
        );
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A CSV or XLSX file is required");
        }
        String name = safeFileName(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!(name.endsWith(".csv") || name.endsWith(".xlsx") || name.endsWith(".xls"))) {
            throw new IllegalArgumentException("Only CSV/XLSX/XLS files are supported");
        }
        long maxBytes = maxFileSizeMb <= 0 ? 10L * 1024L * 1024L : maxFileSizeMb * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File exceeds max size of " + maxFileSizeMb + "MB");
        }
    }

    private ParsedImport parse(String originalFileName, byte[] content, Map<String, String> mappingOverride) {
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            if (safeFileName(originalFileName).toLowerCase(Locale.ROOT).endsWith(".csv")) {
                return parseCsv(inputStream, mappingOverride);
            }
            return parseXlsx(inputStream, mappingOverride);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private ParsedImport parseCsv(InputStream inputStream, Map<String, String> mappingOverride) throws IOException {
        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        String[] lines = content.split("\\R");
        if (lines.length == 0) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        List<List<String>> rawRows = new ArrayList<>();
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            List<String> parsed = parseCsvLine(line);
            if (!isRowEmpty(parsed)) {
                rawRows.add(parsed);
            }
        }

        if (rawRows.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        int headerRowIndex = detectHeaderRowIndex(rawRows);
        List<String> headers = rawRows.get(headerRowIndex);
        HeaderResolution resolution = resolveHeaderMapping(headers, mappingOverride);
        if (resolution.fieldIndex() instanceof HeaderIndexMap indexMap) {
            indexMap.setHeaderRowIndex(headerRowIndex);
        }

        if (!resolution.missingRequiredFields().isEmpty()) {
            throw new IllegalArgumentException("Missing required columns after mapping: " + String.join(", ", resolution.missingRequiredFields()));
        }

        List<List<String>> rows = new ArrayList<>();
        for (int i = headerRowIndex + 1; i < rawRows.size(); i++) {
            if (isRowEmpty(rawRows.get(i))) {
                continue;
            }
            rows.add(rawRows.get(i));
        }

        return new ParsedImport(headers,
            rows,
            resolution.fieldIndex(),
            headerRowIndex,
            resolution.mappingByField(),
            resolution.availableHeaders(),
            resolution.missingRequiredFields(),
            resolution.suggestions(),
            headerRowIndex > 0 ? List.of("Header row auto-detected at row " + (headerRowIndex + 1)) : List.of());
    }

    private ParsedImport parseXlsx(InputStream inputStream, Map<String, String> mappingOverride) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            List<List<String>> rawRows = readWorkbookRows(sheet, formatter);
            if (rawRows.isEmpty()) {
                throw new IllegalArgumentException("XLSX file does not contain headers");
            }

            int headerRowIndex = detectHeaderRowIndex(rawRows);
            List<String> headers = rawRows.get(headerRowIndex);
            HeaderResolution resolution = resolveHeaderMapping(headers, mappingOverride);
            if (resolution.fieldIndex() instanceof HeaderIndexMap indexMap) {
                indexMap.setHeaderRowIndex(headerRowIndex);
            }
            if (!resolution.missingRequiredFields().isEmpty()) {
                throw new IllegalArgumentException("Missing required columns after mapping: " + String.join(", ", resolution.missingRequiredFields()));
            }

            List<List<String>> rows = new ArrayList<>();
            for (int i = headerRowIndex + 1; i < rawRows.size(); i++) {
                if (isRowEmpty(rawRows.get(i))) {
                    continue;
                }
                rows.add(rawRows.get(i));
            }

            return new ParsedImport(headers,
                rows,
                resolution.fieldIndex(),
                headerRowIndex,
                resolution.mappingByField(),
                resolution.availableHeaders(),
                resolution.missingRequiredFields(),
                resolution.suggestions(),
                headerRowIndex > 0 ? List.of("Header row auto-detected at row " + (headerRowIndex + 1)) : List.of());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private List<List<String>> readWorkbookRows(Sheet sheet, DataFormatter formatter) {
        int maxColumns = 0;
        for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                maxColumns = Math.max(maxColumns, Math.max(row.getLastCellNum(), 0));
            }
        }

        maxColumns = Math.max(maxColumns, 16);
        List<List<String>> rows = new ArrayList<>();
        for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            List<String> values = new ArrayList<>();
            for (int c = 0; c < maxColumns; c++) {
                if (row == null) {
                    values.add("");
                    continue;
                }
                Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                values.add(cell == null ? "" : formatter.formatCellValue(cell));
            }
            if (!isRowEmpty(values)) {
                rows.add(values);
            }
        }
        return rows;
    }

    private List<StudentImportRow> validateRows(StudentImportJob job,
                                                List<List<String>> rows,
                                                Map<String, Integer> headerIndex,
                                                List<String> headers) {
        List<StudentImportRow> items = new ArrayList<>();
        Set<String> seenEnrollments = new LinkedHashSet<>();

        int dataStartRow = 2;
        if (headerIndex instanceof HeaderIndexMap indexMap) {
            dataStartRow = indexMap.getHeaderRowIndex() + 2;
        }

        for (int i = 0; i < rows.size(); i++) {
            List<String> values = rows.get(i);
            StudentImportRow row = new StudentImportRow();
            row.setJob(job);
            row.setRowIndex(dataStartRow + i);
            row.setFullName(value(values, headerIndex, "fullName"));
            row.setEnrollmentNumber(value(values, headerIndex, "enrollmentNumber"));
            row.setRollNumber(value(values, headerIndex, "rollNumber"));
            row.setEmail(value(values, headerIndex, "email"));
            row.setPersonalEmail(value(values, headerIndex, "personalEmail"));
            row.setPhone(value(values, headerIndex, "phone"));
            row.setProgram(value(values, headerIndex, "program"));
            row.setCourse(value(values, headerIndex, "course"));
            row.setSemester(value(values, headerIndex, "semester"));
            row.setDepartment(value(values, headerIndex, "department"));
            row.setSchool(value(values, headerIndex, "school"));
            row.setSection(value(values, headerIndex, "section"));
            row.setClassName(value(values, headerIndex, "className"));
            row.setHouse(value(values, headerIndex, "house"));
            row.setFoundationClassroom(value(values, headerIndex, "foundationClassroom"));
            row.setTeamNumber(value(values, headerIndex, "teamNumber"));
            row.setMemberNumber(value(values, headerIndex, "memberNumber"));
            row.setJoiningYear(value(values, headerIndex, "joiningYear"));
            row.setLeavingYear(value(values, headerIndex, "leavingYear"));
            row.setDateOfBirth(value(values, headerIndex, "dateOfBirth"));
            row.setGender(value(values, headerIndex, "gender"));
            row.setAddress(value(values, headerIndex, "address"));
            row.setBloodGroup(value(values, headerIndex, "bloodGroup"));
            row.setGuardianName(value(values, headerIndex, "guardianName"));

            if (!hasText(row.getFullName())) {
                row.setFullName(composeFullName(values, headerIndex, headers));
            }

            if (!hasText(row.getEnrollmentNumber())) {
                row.setEnrollmentNumber(firstNonBlank(
                    row.getRollNumber(),
                    valueByAliases(values, headerIndex, headers, ENROLLMENT_VALUE_ALIASES)
                ));
            }

            sanitizeRow(row);
            row.setNormalizedEnrollment(normalize(row.getEnrollmentNumber()));
            validateRow(job, row, seenEnrollments);
            items.add(row);
        }
        return items;
    }

    private void validateRow(StudentImportJob job, StudentImportRow row, Set<String> seenEnrollments) {
        List<String> errors = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        if (!StringUtils.hasText(row.getFullName())) errors.add("Full Name is required");
        if (!StringUtils.hasText(row.getEnrollmentNumber())) errors.add("Enrollment Number is required");

        if (StringUtils.hasText(row.getEnrollmentNumber()) && !ENROLLMENT_PATTERN.matcher(row.getEnrollmentNumber().trim()).matches()) {
            errors.add("Enrollment Number contains invalid characters");
        }
        if (StringUtils.hasText(row.getEmail()) && !EMAIL_PATTERN.matcher(row.getEmail().trim()).matches()) {
            errors.add("Email is invalid");
        }
        if (StringUtils.hasText(row.getPersonalEmail()) && !EMAIL_PATTERN.matcher(row.getPersonalEmail().trim()).matches()) {
            errors.add("Personal Email is invalid");
        }
        if (StringUtils.hasText(row.getPhone()) && !PHONE_PATTERN.matcher(row.getPhone().trim()).matches()) {
            errors.add("Phone must contain 7-15 digits");
        }
        if (row.getNormalizedEnrollment() != null && !seenEnrollments.add(row.getNormalizedEnrollment())) {
            errors.add("Duplicate enrollment number in uploaded file");
        }

        if (StringUtils.hasText(row.getEnrollmentNumber()) && studentRepository.existsById(row.getEnrollmentNumber())) {
            String strategy = normalizeDuplicateStrategy(job.getDuplicateStrategy());
            if ("REJECT".equals(strategy)) {
                errors.add("Enrollment already exists in system");
            } else if ("SKIP".equals(strategy)) {
                notes.add("Existing enrollment will be skipped on confirm");
            } else {
                notes.add("Existing enrollment will be updated on confirm");
            }
        }

        if (errors.isEmpty()) {
            row.setStatus("VALID");
            row.setErrorMessage(notes.isEmpty() ? null : String.join("; ", notes));
        } else {
            row.setStatus("INVALID");
            if (!notes.isEmpty()) {
                errors.addAll(notes);
            }
            row.setErrorMessage(String.join("; ", errors));
        }
    }

    private List<StudentImportRow> revalidateAllRows(StudentImportJob job) {
        List<StudentImportRow> rows = rowRepository.findByJobOrderByRowIndexAsc(job);
        Set<String> seen = new LinkedHashSet<>();
        for (StudentImportRow row : rows) {
            sanitizeRow(row);
            row.setNormalizedEnrollment(normalize(row.getEnrollmentNumber()));
            validateRow(job, row, seen);
        }
        rowRepository.saveAll(rows);
        updateJobCounts(job, rows);
        jobRepository.save(job);
        return rows;
    }

    private ImportResult importRow(StudentImportRow row, String strategy) {
        String enrollmentNumber = cleanEnrollment(row.getEnrollmentNumber());
        if (!StringUtils.hasText(enrollmentNumber)) {
            throw new IllegalArgumentException("Enrollment Number is required");
        }

        Student existing = studentRepository.findById(enrollmentNumber).orElse(null);
        if (existing != null) {
            switch (strategy) {
                case "SKIP" -> {
                    return ImportResult.skipped("Existing enrollment skipped");
                }
                case "OVERWRITE", "UPDATE" -> {
                    Student mapped = mapStudent(existing, row);
                    Student saved = studentService.save(mapped);
                    upsertEnrollment(saved, row.getCourse());
                    return ImportResult.imported(saved.getId());
                }
                case "REJECT" -> {
                    return ImportResult.skipped("Enrollment rejected as duplicate");
                }
                default -> throw new IllegalArgumentException("Unsupported duplicate strategy: " + strategy);
            }
        }

        Student student = new Student(enrollmentNumber, clean(row.getFullName()));
        Student mapped = mapStudent(student, row);
        Student saved = studentService.save(mapped);
        upsertEnrollment(saved, row.getCourse());
        return ImportResult.imported(saved.getId());
    }

    private ImportResult importMergedStudent(Map<String, Object> mergedStudent, List<StudentImportRow> rows, String strategy) {
        String studentId = resolveStudentId(mergedStudent);
        if (!StringUtils.hasText(studentId)) {
            throw new IllegalArgumentException("Unable to resolve student identifier");
        }

        Student existing = studentRepository.findById(studentId).orElse(null);
        if (existing != null) {
            return switch (strategy) {
                case "SKIP" -> ImportResult.skipped("Existing student skipped");
                case "REJECT" -> ImportResult.skipped("Existing student rejected as duplicate");
                case "OVERWRITE", "UPDATE" -> persistMergedStudent(existing, mergedStudent, rows);
                default -> throw new IllegalArgumentException("Unsupported duplicate strategy: " + strategy);
            };
        }

        Student student = new Student(studentId, stringValue(mergedStudent.get("fullName"), studentId));
        return persistMergedStudent(student, mergedStudent, rows);
    }

    private ImportResult persistMergedStudent(Student student, Map<String, Object> mergedStudent, List<StudentImportRow> rows) {
        boolean preferExisting = student.getId() != null && studentRepository.existsById(student.getId());

        String mergedName = mergeValue(student.getName(), stringValue(mergedStudent.get("fullName"), null), preferExisting);
        student.setName(firstNonBlank(mergedName, student.getName(), stringValue(mergedStudent.get("fullName"), null)));
        student.setEmail(mergeValue(student.getEmail(), stringValue(mergedStudent.get("email"), null), preferExisting));
        student.setPhone(mergeValue(student.getPhone(), stringValue(mergedStudent.get("phone"), null), preferExisting));
        student.setCourse(mergeValue(student.getCourse(), firstNonBlank(stringValue(mergedStudent.get("course"), null), stringValue(mergedStudent.get("program"), null)), preferExisting));
        student.setSemester(mergeValue(student.getSemester(), stringValue(mergedStudent.get("semester"), null), preferExisting));
        student.setDepartment(mergeValue(student.getDepartment(), stringValue(mergedStudent.get("department"), null), preferExisting));
        student.setSection(mergeValue(student.getSection(), firstNonBlank(stringValue(mergedStudent.get("section"), null), stringValue(mergedStudent.get("className"), null)), preferExisting));
        student.setGender(StudentFieldDerivationUtils.inferGender(student.getName(), mergeValue(student.getGender(), stringValue(mergedStudent.get("gender"), null), preferExisting)));
        student.setAddress(mergeValue(student.getAddress(), stringValue(mergedStudent.get("address"), null), preferExisting));
        student.setEnrollmentYear(mergeValue(student.getEnrollmentYear(), firstNonBlank(stringValue(mergedStudent.get("joiningYear"), null), extractYear(student.getId())), preferExisting));
        if (hasText(stringValue(mergedStudent.get("dateOfBirth"), null))) {
            try {
                LocalDate parsedDob = parseDate(stringValue(mergedStudent.get("dateOfBirth"), null));
                if (!preferExisting || student.getDob() == null) {
                    student.setDob(parsedDob);
                }
            } catch (Exception ignored) {
                if (!preferExisting) {
                    student.setDob(null);
                }
            }
        }

        Student saved = studentService.save(student);
        upsertEnrollment(saved, firstNonBlank(saved.getCourse(), stringValue(mergedStudent.get("course"), null), stringValue(mergedStudent.get("program"), null)));
        updateStudentProfileMetadata(saved.getId(), mergedStudent, preferExisting);
        return ImportResult.imported(saved.getId());
    }

    private void updateStudentProfileMetadata(String studentId, Map<String, Object> mergedStudent, boolean preferExisting) {
        StudentProfile profile = studentProfileRepository.findByStudentId(studentId).orElseGet(StudentProfile::new);
        String universityEmail = firstNonBlank(profile.getUniversityEmail(), profile.getEmail(), deriveUniversityEmail(studentId));
        String personalEmail = mergeValue(
            profile.getPersonalEmail(),
            firstNonBlank(stringValue(mergedStudent.get("personalEmail"), null), stringValue(mergedStudent.get("email"), null)),
            preferExisting
        );

        profile.setStudentId(studentId);
        profile.setFullName(mergeValue(profile.getFullName(), stringValue(mergedStudent.get("fullName"), null), preferExisting));
        profile.setEnrollmentNumber(firstNonBlank(stringValue(mergedStudent.get("enrollmentNumber"), null), studentId));
        profile.setProfileImage(firstNonBlank(profile.getProfileImage(), null));
        if (!preferExisting || profile.getDob() == null) {
            profile.setDob(parseOptionalDate(stringValue(mergedStudent.get("dateOfBirth"), null), profile.getDob()));
        }
        profile.setGender(StudentFieldDerivationUtils.inferGender(profile.getFullName(), mergeValue(profile.getGender(), stringValue(mergedStudent.get("gender"), null), preferExisting)));
        profile.setPhone(mergeValue(profile.getPhone(), stringValue(mergedStudent.get("phone"), null), preferExisting));
        profile.setUniversityEmail(universityEmail);
        profile.setPersonalEmail(personalEmail);
        profile.setEmail(universityEmail);
        profile.setAddress(mergeValue(profile.getAddress(), stringValue(mergedStudent.get("address"), null), preferExisting));
        profile.setHouse(mergeValue(profile.getHouse(), stringValue(mergedStudent.get("house"), null), preferExisting));
        String incomingFoundationClassroom = firstNonBlank(
            stringValue(mergedStudent.get("foundationClassroom"), null),
            stringValue(mergedStudent.get("className"), null)
        );
        profile.setFoundationClassroom(normalizeFoundationClassroom(mergeValue(profile.getFoundationClassroom(), incomingFoundationClassroom, preferExisting), profile.getHouse()));
        profile.setTeamNumber(parseInteger(mergeValue(
            profile.getTeamNumber() == null ? null : String.valueOf(profile.getTeamNumber()),
            stringValue(mergedStudent.get("teamNumber"), null),
            preferExisting
        ), profile.getTeamNumber()));
        profile.setMemberNumber(parseInteger(mergeValue(
            profile.getMemberNumber() == null ? null : String.valueOf(profile.getMemberNumber()),
            stringValue(mergedStudent.get("memberNumber"), null),
            preferExisting
        ), profile.getMemberNumber()));
        profile.setCaste(mergeValue(profile.getCaste(), stringValue(mergedStudent.get("caste"), null), preferExisting));
        profile.setPlaceOfOrigin(firstNonBlank(
            mergeValue(profile.getPlaceOfOrigin(), stringValue(mergedStudent.get("placeOfOrigin"), null), preferExisting),
            mergeValue(profile.getPlaceOfOrigin(), stringValue(mergedStudent.get("origin"), null), preferExisting),
            mergeValue(profile.getPlaceOfOrigin(), stringValue(mergedStudent.get("city"), null), preferExisting),
            profile.getPlaceOfOrigin()
        ));
        profile.setCourse(mergeValue(profile.getCourse(), firstNonBlank(stringValue(mergedStudent.get("course"), null), stringValue(mergedStudent.get("program"), null)), preferExisting));
        profile.setDepartment(mergeValue(profile.getDepartment(), stringValue(mergedStudent.get("department"), null), preferExisting));
        profile.setSemester(mergeValue(profile.getSemester(), stringValue(mergedStudent.get("semester"), null), preferExisting));
        profile.setSection(mergeValue(profile.getSection(), firstNonBlank(stringValue(mergedStudent.get("section"), null), stringValue(mergedStudent.get("className"), null)), preferExisting));
        profile.setAdmissionYear(parseYear(firstNonBlank(stringValue(mergedStudent.get("joiningYear"), null), profile.getAdmissionYear() == null ? null : String.valueOf(profile.getAdmissionYear()))));
        profile.setCollege(StudentFieldDerivationUtils.resolveCollegeName(firstNonBlank(stringValue(mergedStudent.get("school"), null), profile.getCollege()), profile.getCourse()));
        profile.setPassingYear(StudentFieldDerivationUtils.derivePassingYear(profile.getCourse(), profile.getAdmissionYear(), profile.getPassingYear()));
        profile.setValidUpto(StudentFieldDerivationUtils.deriveValidUpto(profile.getCourse(), profile.getAdmissionYear(), profile.getPassingYear(), null));
        profile.setIdCardNumber(firstNonBlank(profile.getIdCardNumber(), "BU-" + studentId));
        profile.setUpdatedBy("Import Fusion");
        studentProfileRepository.save(profile);
    }

    private String deriveUniversityEmail(String studentId) {
        if (!StringUtils.hasText(studentId)) {
            return null;
        }
        return studentId + "@bennett.edu.in";
    }

    private Student mapStudent(Student student, StudentImportRow row) {
        student.setName(clean(row.getFullName()));
        student.setEmail(clean(row.getEmail()));
        student.setPhone(clean(row.getPhone()));
        student.setCourse(firstNonBlank(clean(row.getProgram()), clean(row.getCourse())));
        student.setSemester(clean(row.getSemester()));
        student.setDepartment(clean(row.getDepartment()));
        student.setSection(firstNonBlank(clean(row.getSection()), clean(row.getClassName())));
        student.setGender(StudentFieldDerivationUtils.inferGender(student.getName(), clean(row.getGender())));
        student.setAddress(clean(row.getAddress()));
        student.setEnrollmentYear(firstNonBlank(clean(row.getJoiningYear()), parseEnrollmentYear(row.getEnrollmentNumber())));
        try {
            student.setDob(parseDate(row.getDateOfBirth()));
        } catch (Exception ignored) {
            student.setDob(null);
        }
        return student;
    }

    private String resolveStudentId(Map<String, Object> mergedStudent) {
        String enrollment = stringValue(mergedStudent.get("enrollmentNumber"), null);
        if (hasText(enrollment)) {
            return cleanEnrollment(enrollment);
        }

        String rollNumber = stringValue(mergedStudent.get("rollNumber"), null);
        if (hasText(rollNumber)) {
            return cleanEnrollment(rollNumber);
        }

        String identityKey = stringValue(mergedStudent.get("identityKey"), null);
        if (hasText(identityKey)) {
            return "AUTO-" + Integer.toHexString(identityKey.hashCode()).toUpperCase(Locale.ROOT);
        }

        return null;
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private LocalDate parseOptionalDate(String value, LocalDate fallback) {
        if (!hasText(value)) {
            return fallback;
        }
        try {
            return parseDate(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String mergeValue(String existingValue, String incomingValue, boolean preferExisting) {
        String cleanedExisting = clean(existingValue);
        String cleanedIncoming = clean(incomingValue);
        if (preferExisting) {
            return firstNonBlank(cleanedExisting, cleanedIncoming);
        }
        return firstNonBlank(cleanedIncoming, cleanedExisting);
    }

    private Integer parseInteger(String value, Integer fallback) {
        if (!hasText(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String normalizeFoundationClassroom(String foundationClassroom, String house) {
        String cleanedFoundation = clean(foundationClassroom);
        if (!hasText(cleanedFoundation)) {
            return null;
        }
        return cleanedFoundation;
    }

    private void upsertEnrollment(Student student, String courseValue) {
        if (!StringUtils.hasText(courseValue)) {
            return;
        }

        String normalizedCourse = courseValue.trim();
        String generatedCode = generateCourseCode(normalizedCourse);

        Course course = courseRepository.findByCourseNameIgnoreCase(normalizedCourse)
            .or(() -> courseRepository.findByCode(generatedCode))
            .or(() -> courseRepository.findByCode(normalizedCourse))
            .orElseGet(() -> {
                Course created = new Course();
                created.setCode(generatedCode);
                created.setCourseName(normalizedCourse);
                created.setCredits(3);
                try {
                    return courseRepository.saveAndFlush(created);
                } catch (DataIntegrityViolationException ex) {
                    return courseRepository.findByCode(generatedCode)
                        .or(() -> courseRepository.findByCourseNameIgnoreCase(normalizedCourse))
                        .orElseThrow(() -> ex);
                }
            });

        if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(course);
            enrollment.setMarks(0.0);
            enrollmentRepository.save(enrollment);
        }
    }

    private void rollbackCreatedStudents(List<String> createdStudentIds) {
        for (String studentId : createdStudentIds) {
            if (studentRepository.existsById(studentId)) {
                studentService.deleteById(studentId);
            }
        }
    }

    private void updateJobCounts(StudentImportJob job, List<StudentImportRow> rows) {
        job.setTotalRows(rows.size());
        job.setValidRows((int) rows.stream().filter(row -> "VALID".equals(row.getStatus())).count());
        job.setInvalidRows((int) rows.stream().filter(row -> "INVALID".equals(row.getStatus())).count());
        job.setFailureCount((int) rows.stream().filter(row -> "INVALID".equals(row.getStatus())).count());
    }

    private void markRowsForCluster(List<StudentImportRow> rows,
                                    Map<String, Object> mergedStudent,
                                    String status,
                                    String errorMessage,
                                    String createdStudentId) {
        Set<Long> rowIds = new LinkedHashSet<>();
        Object sourceRows = mergedStudent.get("sourceRows");
        if (sourceRows instanceof List<?> list) {
            for (Object sourceRow : list) {
                if (sourceRow instanceof Map<?, ?> rowMap) {
                    Object id = rowMap.get("rowId");
                    if (id instanceof Number number) {
                        rowIds.add(number.longValue());
                    }
                }
            }
        }

        for (StudentImportRow row : rows) {
            if (!rowIds.contains(row.getId())) {
                continue;
            }
            row.setStatus(status);
            row.setErrorMessage(errorMessage);
            if (createdStudentId != null) {
                row.setCreatedStudentId(createdStudentId);
            }
        }
    }

    private String buildErrorLineForCluster(Map<String, Object> mergedStudent, String message) {
        Object identity = mergedStudent.get("identityKey");
        return safeMessage(message) + " | cluster=" + (identity == null ? "unknown" : identity);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private String compactForDb(String json, int maxLength) {
        if (json == null) {
            return null;
        }
        if (json.length() <= maxLength) {
            return json;
        }
        return json.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String buildMergeLogSummaryForDb(StudentFusionService.FusionResult fusion) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("clusters", fusion.clusterCount());
        summary.put("avgConfidence", fusion.averageConfidence());
        summary.put("suggestions", fusion.suggestions().size());
        return compactForDb(writeJson(summary), 240);
    }

    private Map<String, Object> buildPreviewPayload(StudentImportJob job, List<StudentImportRow> rows, ParsedImport parsed) {
        return buildPreviewPayload(job, rows, parsed, studentFusionService.analyze(rows), List.of());
    }

    private Map<String, Object> buildPreviewPayload(StudentImportJob job,
                                                    List<StudentImportRow> rows,
                                                    ParsedImport parsed,
                                                    StudentFusionService.FusionResult fusion,
                                                    List<Map<String, Object>> fileSummaries) {
        List<Map<String, Object>> previewRows = rows.stream().map(this::rowToMap).toList();
        List<Map<String, Object>> errorRows = rows.stream()
            .filter(row -> !"VALID".equals(row.getStatus()))
            .map(row -> {
                Map<String, Object> item = rowToMap(row);
                item.put("errorMessage", row.getErrorMessage());
                return item;
            })
            .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId", job.getId());
        payload.put("fileName", job.getFileName());
        payload.put("duplicateStrategy", job.getDuplicateStrategy());
        payload.put("rollbackOnFailure", job.isRollbackOnFailure());
        payload.put("sourceFileCount", job.getSourceFileCount());
        payload.put("fusedStudentCount", job.getFusedStudentCount());
        payload.put("headers", FIELD_ORDER.stream().map(FIELD_LABELS::get).toList());
        payload.put("rows", previewRows);
        payload.put("mergedStudents", fusion.mergedStudents());
        payload.put("mergeLog", fusion.mergeLog());
        payload.put("smartSuggestions", fusion.suggestions());
        payload.put("sourceFiles", fileSummaries);
        payload.put("mergeSources", fusion.sources());
        payload.put("mergeAverageConfidence", fusion.averageConfidence());
        payload.put("totalRows", rows.size());
        payload.put("validRows", rows.stream().filter(row -> "VALID".equals(row.getStatus())).count());
        payload.put("invalidRows", rows.stream().filter(row -> !"VALID".equals(row.getStatus())).count());
        payload.put("errorRows", errorRows);
        payload.put("fieldLabels", FIELD_LABELS);

        if (parsed != null) {
            payload.put("availableHeaders", parsed.availableHeaders());
            payload.put("columnMapping", parsed.mappingByField());
            payload.put("missingRequiredFields", parsed.missingRequiredFields());
            payload.put("mappingSuggestions", parsed.suggestions());
            payload.put("parseWarnings", parsed.warnings());
            payload.put("detectedHeaderRow", parsed.headerRowIndex() + 1);
        } else {
            payload.put("availableHeaders", List.of());
            payload.put("columnMapping", Collections.emptyMap());
            payload.put("missingRequiredFields", List.of());
            payload.put("mappingSuggestions", Collections.emptyMap());
            payload.put("parseWarnings", List.of());
            payload.put("detectedHeaderRow", 1);
        }

        payload.put("jobSummaryJson", job.getMergeLogJson());
        return payload;
    }

    private Map<String, Object> rowToMap(StudentImportRow row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.getId());
        item.put("rowIndex", row.getRowIndex());
        item.put("fullName", row.getFullName());
        item.put("enrollmentNumber", row.getEnrollmentNumber());
        item.put("rollNumber", row.getRollNumber());
        item.put("email", row.getEmail());
        item.put("personalEmail", row.getPersonalEmail());
        item.put("phone", row.getPhone());
        item.put("program", row.getProgram());
        item.put("course", row.getCourse());
        item.put("semester", row.getSemester());
        item.put("department", row.getDepartment());
        item.put("school", row.getSchool());
        item.put("section", row.getSection());
        item.put("className", row.getClassName());
        item.put("house", row.getHouse());
        item.put("foundationClassroom", row.getFoundationClassroom());
        item.put("teamNumber", row.getTeamNumber());
        item.put("memberNumber", row.getMemberNumber());
        item.put("joiningYear", row.getJoiningYear());
        item.put("leavingYear", row.getLeavingYear());
        item.put("dateOfBirth", row.getDateOfBirth());
        item.put("gender", row.getGender());
        item.put("address", row.getAddress());
        item.put("bloodGroup", row.getBloodGroup());
        item.put("guardianName", row.getGuardianName());
        item.put("classGroup", computeClassGroup(row.getEnrollmentNumber()));
        item.put("batchGroup", computeBatchGroup(row.getEnrollmentNumber()));
        item.put("sourceFileName", row.getSourceFileName());
        item.put("mergeGroupKey", row.getMergeGroupKey());
        item.put("identityKey", row.getIdentityKey());
        item.put("confidenceScore", resolveRowConfidence(row));
        item.put("status", row.getStatus());
        item.put("errorMessage", row.getErrorMessage());
        return item;
    }

    private double resolveRowConfidence(StudentImportRow row) {
        if (row.getConfidenceScore() != null && row.getConfidenceScore() > 0) {
            return row.getConfidenceScore();
        }

        double score = 45.0;
        if (StringUtils.hasText(row.getEnrollmentNumber())) score += 25.0;
        if (StringUtils.hasText(row.getFullName())) score += 15.0;
        if (StringUtils.hasText(row.getCourse()) || StringUtils.hasText(row.getProgram())) score += 7.5;
        if (StringUtils.hasText(row.getDepartment())) score += 5.0;
        if (StringUtils.hasText(row.getSection()) || StringUtils.hasText(row.getClassName())) score += 2.5;
        return Math.min(100.0, score);
    }

    private StudentImportJob getOwnedJob(Long jobId, String requester) {
        return jobRepository.findByIdAndUploadedBy(jobId, requester)
            .orElseThrow(() -> new IllegalArgumentException("Import job not found"));
    }

    private String normalizeDuplicateStrategy(String duplicateStrategy) {
        String normalized = duplicateStrategy == null ? "UPDATE" : duplicateStrategy.trim().toUpperCase(Locale.ROOT);
        if (List.of("SKIP", "OVERWRITE", "UPDATE", "REJECT").contains(normalized)) {
            return normalized;
        }
        return "UPDATE";
    }

    private String safeFileName(String fileName) {
        return fileName == null ? "students-import" : fileName.replaceAll("[\\\\/]+", "_");
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String value(List<String> values, Map<String, Integer> headerIndex, String field) {
        Integer index = headerIndex.get(field);
        if (index == null || index < 0 || index >= values.size()) {
            return null;
        }
        return clean(values.get(index));
    }

    private HeaderResolution resolveHeaderMapping(List<String> headers, Map<String, String> mappingOverride) {
        Map<String, Integer> headerIndexByNormalizedName = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String normalized = normalizeHeader(headers.get(i));
            if (!normalized.isBlank()) {
                headerIndexByNormalizedName.putIfAbsent(normalized, i);
            }
        }

        Map<String, Integer> fieldIndex = new HeaderIndexMap();
        Map<String, String> mappingByField = new LinkedHashMap<>();
        Map<String, String> suggestions = new LinkedHashMap<>();

        for (String field : FIELD_ORDER) {
            Integer resolved = resolveFieldIndex(field, headers, headerIndexByNormalizedName, mappingOverride);
            if (resolved != null) {
                ((HeaderIndexMap) fieldIndex).put(field, resolved);
                mappingByField.put(field, headers.get(resolved));
                continue;
            }

            String suggestion = suggestHeader(field, headers);
            if (suggestion != null) {
                suggestions.put(field, suggestion);
            }
        }

        if (fieldIndex instanceof HeaderIndexMap indexMap) {
            indexMap.setNormalizedHeaderIndex(headerIndexByNormalizedName);
        }

        // If enrollment is missing but roll number is present, use roll as enrollment fallback.
        // Many institutional sheets use roll/registration as the primary student identifier.
        if (!fieldIndex.containsKey("enrollmentNumber") && fieldIndex.containsKey("rollNumber")) {
            Integer rollIndex = fieldIndex.get("rollNumber");
            if (rollIndex != null) {
                ((HeaderIndexMap) fieldIndex).put("enrollmentNumber", rollIndex);
                mappingByField.put("enrollmentNumber", headers.get(rollIndex));
                suggestions.remove("enrollmentNumber");
            }
        }

        List<String> missingRequired = REQUIRED_FIELDS.stream()
            .filter(field -> !fieldIndex.containsKey(field))
            .map(field -> FIELD_LABELS.getOrDefault(field, field))
            .toList();

        return new HeaderResolution(fieldIndex, mappingByField, headers, missingRequired, suggestions);
    }

    private Integer resolveFieldIndex(String field,
                                      List<String> headers,
                                      Map<String, Integer> headerIndexByNormalizedName,
                                      Map<String, String> mappingOverride) {
        String overrideHeader = mappingOverride == null ? null : mappingOverride.get(field);
        if (StringUtils.hasText(overrideHeader)) {
            Integer overrideIndex = headerIndexByNormalizedName.get(normalizeHeader(overrideHeader));
            if (overrideIndex != null) {
                return overrideIndex;
            }
        }

        List<String> aliases = aliasesFor(field);
        for (String alias : aliases) {
            Integer index = headerIndexByNormalizedName.get(normalizeHeader(alias));
            if (index != null) {
                return index;
            }
        }

        return fuzzyHeaderIndex(aliases, headers);
    }

    private Integer fuzzyHeaderIndex(List<String> aliases, List<String> headers) {
        int bestScore = 0;
        Integer bestIndex = null;

        for (int i = 0; i < headers.size(); i++) {
            String normalizedHeader = normalizeHeader(headers.get(i));
            if (normalizedHeader.isBlank()) {
                continue;
            }

            for (String alias : aliases) {
                String normalizedAlias = normalizeHeader(alias);
                if (normalizedAlias.isBlank()) {
                    continue;
                }

                int score = fuzzyScore(normalizedAlias, normalizedHeader);
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = i;
                }
            }
        }

        return bestScore >= 2 ? bestIndex : null;
    }

    private int fuzzyScore(String alias, String header) {
        if (alias.equals(header)) {
            return 10;
        }
        if (header.contains(alias) || alias.contains(header)) {
            return 6;
        }

        Set<String> aliasTokens = Set.of(HEADER_TOKEN_SPLIT.split(alias));
        Set<String> headerTokens = Set.of(HEADER_TOKEN_SPLIT.split(header));
        int overlap = 0;
        for (String token : aliasTokens) {
            if (headerTokens.contains(token)) {
                overlap++;
            }
        }
        return overlap;
    }

    private String suggestHeader(String field, List<String> headers) {
        List<String> aliases = aliasesFor(field);
        return headers.stream()
            .filter(StringUtils::hasText)
            .max(Comparator.comparingInt(header -> aliases.stream()
                .map(alias -> fuzzyScore(normalizeHeader(alias), normalizeHeader(header)))
                .max(Integer::compareTo)
                .orElse(0)))
            .filter(candidate -> aliases.stream()
                .map(alias -> fuzzyScore(normalizeHeader(alias), normalizeHeader(candidate)))
                .max(Integer::compareTo)
                .orElse(0) > 0)
            .orElse(null);
    }

    private int detectHeaderRowIndex(List<List<String>> rows) {
        int scanLimit = Math.min(rows.size(), 25);
        int bestScore = Integer.MIN_VALUE;
        int bestIndex = 0;

        for (int i = 0; i < scanLimit; i++) {
            int score = scoreHeaderRow(rows.get(i));
            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private int scoreHeaderRow(List<String> row) {
        if (row == null || row.isEmpty()) {
            return Integer.MIN_VALUE;
        }

        int score = 0;
        Set<String> matchedFields = new LinkedHashSet<>();
        for (String value : row) {
            String normalized = normalizeHeader(value);
            if (normalized.isBlank()) {
                continue;
            }

            for (String field : FIELD_ORDER) {
                for (String alias : aliasesFor(field)) {
                    String normalizedAlias = normalizeHeader(alias);
                    if (normalizedAlias.equals(normalized)) {
                        matchedFields.add(field);
                        score += 3;
                        break;
                    }
                    if (fuzzyScore(normalizedAlias, normalized) >= 2) {
                        matchedFields.add(field);
                        score += 1;
                        break;
                    }
                }
            }
        }

        if (matchedFields.contains("fullName")) score += 4;
        if (matchedFields.contains("enrollmentNumber")) score += 4;
        return score;
    }

    private List<String> aliasesFor(String field) {
        return switch (field) {
            case "fullName" -> List.of("Full Name", "Name", "Student Name", "Candidate Name");
            case "enrollmentNumber" -> List.of(
                "Enrollment Number",
                "Enrolment Number",
                "Enrollment No",
                "Enrollment No.",
                "Enrolment No",
                "Enrolment No.",
                "Enrollment",
                "Enrollment Id",
                "Enrollment ID",
                "Student Enrollment",
                "Student Enrollment Number",
                "Student Id",
                "Student ID",
                "Registration Number",
                "Registration No",
                "Registration No.",
                "Register Number",
                "Admission Number",
                "Admission No",
                "Admission No.",
                "University Roll Number",
                "University Roll No",
                "University Roll No.",
                "Roll Number",
                "Roll No",
                "Roll No.",
                "Roll"
            );
            case "rollNumber" -> List.of("Roll Number", "Roll No", "Roll", "Student Roll");
            case "email" -> List.of("Email", "Email Address", "Mail");
            case "personalEmail" -> List.of("Personal Email", "Personal Email Address", "Private Email", "Alternate Email");
            case "phone" -> List.of("Phone", "Mobile", "Contact", "Phone Number", "Mobile Number");
            case "program" -> List.of("Program", "Degree", "Qualification", "Stream");
            case "course" -> List.of("Course", "Program", "Branch");
            case "semester" -> List.of("Semester", "Sem", "Term");
            case "department" -> List.of("Department", "Dept", "School");
            case "school" -> List.of("School", "Faculty", "Institute");
            case "section" -> List.of("Section", "Class", "Class Name", "Section Name", "Group");
            case "className" -> List.of("Class", "Class Name", "Class Section", "Batch Class");
            case "house" -> List.of("House", "House Name", "Hostel House");
            case "foundationClassroom" -> List.of("Foundation Classroom", "Foundation Class", "Foundation Classroom Name", "Foundation", "Classroom", "Foundation Group");
            case "teamNumber" -> List.of("Team Number", "Team No", "Team No.", "Team");
            case "memberNumber" -> List.of("Member Number", "Member No", "Member No.", "Member");
            case "joiningYear" -> List.of("Joining Year", "Admission Year", "Year Of Joining");
            case "leavingYear" -> List.of("Leaving Year", "Pass Out Year", "Year Of Leaving");
            case "dateOfBirth" -> List.of("Date of Birth", "DOB", "Birth Date", "Date Of Birth");
            case "gender" -> List.of("Gender", "Sex");
            case "address" -> List.of("Address", "Residential Address", "Current Address");
            case "bloodGroup" -> List.of("Blood Group", "BloodGroup");
            case "guardianName" -> List.of("Guardian Name", "Parent Name", "Father Name", "Mother Name");
            default -> List.of(FIELD_LABELS.getOrDefault(field, field));
        };
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String composeFullName(List<String> values, Map<String, Integer> headerIndex, List<String> headers) {
        String fromSingleColumn = valueByAliases(values, headerIndex, headers, List.of("Student Name", "Candidate Name", "Name"));
        if (hasText(fromSingleColumn)) {
            return fromSingleColumn;
        }

        String firstName = valueByAliases(values, headerIndex, headers, List.of("First Name", "Given Name"));
        String middleName = valueByAliases(values, headerIndex, headers, List.of("Middle Name"));
        String lastName = valueByAliases(values, headerIndex, headers, List.of("Last Name", "Surname", "Family Name"));
        return clean(String.join(" ",
            hasText(firstName) ? firstName.trim() : "",
            hasText(middleName) ? middleName.trim() : "",
            hasText(lastName) ? lastName.trim() : "").trim());
    }

    private String valueByAliases(List<String> values,
                                  Map<String, Integer> headerIndex,
                                  List<String> headers,
                                  List<String> aliases) {
        Integer index = findHeaderIndexByAliases(headerIndex, aliases);
        if (index == null) {
            index = findHeaderIndexByAliases(headers, aliases);
        }
        if (index == null || index < 0 || index >= values.size()) {
            return null;
        }
        return clean(values.get(index));
    }

    private Integer findHeaderIndexByAliases(Map<String, Integer> headerIndex, List<String> aliases) {
        if (!(headerIndex instanceof HeaderIndexMap indexMap)) {
            return null;
        }
        for (String alias : aliases) {
            Integer index = indexMap.getNormalizedHeaderIndex().get(normalizeHeader(alias));
            if (index != null) {
                return index;
            }
        }
        return null;
    }

    private Integer findHeaderIndexByAliases(List<String> headers, List<String> aliases) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        for (int i = 0; i < headers.size(); i++) {
            String normalizedHeader = normalizeHeader(headers.get(i));
            for (String alias : aliases) {
                String normalizedAlias = normalizeHeader(alias);
                if (normalizedAlias.equals(normalizedHeader) || fuzzyScore(normalizedAlias, normalizedHeader) >= 2) {
                    return i;
                }
            }
        }
        return null;
    }

    private boolean isRowEmpty(List<String> values) {
        if (values == null || values.isEmpty()) {
            return true;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return false;
            }
        }
        return true;
    }

    private void sanitizeRow(StudentImportRow row) {
        row.setFullName(cleanPersonName(row.getFullName()));
        row.setEnrollmentNumber(cleanEnrollment(row.getEnrollmentNumber()));
        row.setRollNumber(cleanEnrollment(row.getRollNumber()));
        row.setEmail(cleanEmail(row.getEmail()));
        row.setPersonalEmail(cleanEmail(row.getPersonalEmail()));
        row.setPhone(cleanPhone(row.getPhone()));
        row.setProgram(clean(row.getProgram()));
        row.setCourse(clean(row.getCourse()));
        row.setSemester(clean(row.getSemester()));
        row.setDepartment(clean(row.getDepartment()));
        row.setSchool(clean(row.getSchool()));
        row.setSection(clean(row.getSection()));
        row.setClassName(clean(row.getClassName()));
        row.setHouse(clean(row.getHouse()));
        row.setFoundationClassroom(clean(row.getFoundationClassroom()));
        row.setTeamNumber(clean(row.getTeamNumber()));
        row.setMemberNumber(clean(row.getMemberNumber()));
        row.setJoiningYear(clean(row.getJoiningYear()));
        row.setLeavingYear(clean(row.getLeavingYear()));
        row.setDateOfBirth(clean(row.getDateOfBirth()));
        row.setGender(clean(row.getGender()));
        row.setAddress(clean(row.getAddress()));
        row.setBloodGroup(clean(row.getBloodGroup()));
        row.setGuardianName(clean(row.getGuardianName()));
    }

    private String cleanEmail(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    private String cleanPhone(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        String digitsOnly = cleaned.replaceAll("[^0-9]", "");
        return digitsOnly.isBlank() ? null : digitsOnly;
    }

    private String cleanEnrollment(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        return cleaned.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private List<String> parseCsvLine(String line) {
        if (line == null) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_PATTERNS) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        throw new IllegalArgumentException("Invalid Date of Birth");
    }

    private String extractYear(String enrollmentNumber) {
        return parseEnrollmentYear(enrollmentNumber);
    }

    private Integer parseYear(String year) {
        if (!StringUtils.hasText(year)) {
            return null;
        }
        String digits = year.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(digits.substring(0, Math.min(4, digits.length())));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String computeClassGroup(String enrollmentNumber) {
        Integer serial = extractEnrollmentSerial(enrollmentNumber);
        if (serial == null || serial <= 0) {
            return null;
        }
        int classNumber = ((serial - 1) / CLASS_SIZE) + 1;
        return "Class " + classNumber;
    }

    private String computeBatchGroup(String enrollmentNumber) {
        Integer serial = extractEnrollmentSerial(enrollmentNumber);
        if (serial == null || serial <= 0) {
            return null;
        }
        int batchNumber = (((serial - 1) % CLASS_SIZE) / BATCH_SIZE) + 1;
        return "Batch " + batchNumber;
    }

    @SuppressWarnings("unused")
    private Integer extractEnrollmentSerial(String enrollmentNumber) {
        if (!StringUtils.hasText(enrollmentNumber)) {
            return null;
        }

        String cleaned = cleanEnrollment(enrollmentNumber);
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }

        String digitsOnly = cleaned.replaceAll("\\D+", "");
        if (digitsOnly.isBlank()) {
            return null;
        }

        String serialCandidate = null;
        String year = parseEnrollmentYear(cleaned);
        if (year != null && digitsOnly.startsWith(year) && digitsOnly.length() > year.length()) {
            serialCandidate = digitsOnly.substring(year.length());
        }
        if (!StringUtils.hasText(serialCandidate)) {
            Matcher trailingDigits = Pattern.compile("(\\d{2,4})$").matcher(cleaned);
            if (trailingDigits.find()) {
                serialCandidate = trailingDigits.group(1);
            }
        }
        boolean blankOrZeros = !StringUtils.hasText(serialCandidate)
            || (serialCandidate != null && serialCandidate.replace("0", "").isBlank());
        if (blankOrZeros) {
            Matcher matcher = ENROLLMENT_SERIAL_PATTERN.matcher(cleaned);
            while (matcher.find()) {
                serialCandidate = matcher.group(1);
            }
        }

        if (!StringUtils.hasText(serialCandidate)) {
            return null;
        }

        serialCandidate = serialCandidate == null ? null : serialCandidate.replaceFirst("^0+(?!$)", "");
        if (serialCandidate.length() > 4) {
            serialCandidate = serialCandidate.substring(serialCandidate.length() - 4);
        }

        if (!StringUtils.hasText(serialCandidate)) {
            return null;
        }

        try {
            return Integer.parseInt(serialCandidate);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String parseEnrollmentYear(String enrollmentNumber) {
        if (!StringUtils.hasText(enrollmentNumber)) {
            return null;
        }

        String cleaned = cleanEnrollment(enrollmentNumber);
        Matcher year4 = ENROLLMENT_YEAR_4_PATTERN.matcher(cleaned);
        if (year4.find()) {
            return year4.group(1);
        }

        Matcher year2Prefix = ENROLLMENT_YEAR_2_PREFIX_PATTERN.matcher(cleaned);
        if (year2Prefix.matches()) {
            int yy = Integer.parseInt(year2Prefix.group(1));
            int fullYear = yy <= 69 ? 2000 + yy : 1900 + yy;
            return String.valueOf(fullYear);
        }

        String digits = cleaned.replaceAll("\\D+", "");
        if (digits.length() >= 4) {
            String candidate = digits.substring(0, 4);
            try {
                int year = Integer.parseInt(candidate);
                if (year >= 1990 && year <= 2100) {
                    return candidate;
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String cleanPersonName(String value) {
        String cleaned = clean(value);
        if (!hasText(cleaned)) {
            return null;
        }

        if (cleaned.matches(".*\\d.*") || cleaned.contains("@")) {
            return cleaned;
        }

        String[] tokens = cleaned.split("\\s+");
        List<String> normalized = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            String lower = token.toLowerCase(Locale.ROOT);
            normalized.add(Character.toUpperCase(lower.charAt(0)) + lower.substring(1));
        }
        return normalized.isEmpty() ? cleaned : String.join(" ", normalized);
    }

    private String generateCourseCode(String courseName) {
        String base = courseName == null ? "COURSE" : courseName.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (base.isBlank()) {
            base = "COURSE";
        }
        return base.length() > 12 ? base.substring(0, 12) : base;
    }

    private String safeMessage(String message) {
        return message == null || message.isBlank() ? "Import failed" : message;
    }

    private String buildErrorLine(StudentImportRow row, String message) {
        return row.getRowIndex() + ": " + safeMessage(message);
    }

    private String exportErrors(List<String> errors) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("Row,Error\n".getBytes(StandardCharsets.UTF_8));
            for (String error : errors) {
                out.write((error.replace(',', ';') + "\n").getBytes(StandardCharsets.UTF_8));
            }
            String fileName = "student-import-errors-" + UUID.randomUUID() + ".csv";
            String path = importArtifactService.saveArtifact(fileName, out.toByteArray());
            return path;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void attachErrorReport(StudentImportJob job, String errorReportPath) {
        if (!StringUtils.hasText(errorReportPath)) {
            return;
        }
        job.setLastErrorReportPath(errorReportPath);
        int slash = Math.max(errorReportPath.lastIndexOf('/'), errorReportPath.lastIndexOf('\\'));
        job.setLastErrorReportName(slash >= 0 ? errorReportPath.substring(slash + 1) : errorReportPath);
    }

    private record ParsedImport(List<String> headers,
                                List<List<String>> rows,
                                Map<String, Integer> headerIndex,
                                int headerRowIndex,
                                Map<String, String> mappingByField,
                                List<String> availableHeaders,
                                List<String> missingRequiredFields,
                                Map<String, String> suggestions,
                                List<String> warnings) {}

    private record HeaderResolution(Map<String, Integer> fieldIndex,
                                    Map<String, String> mappingByField,
                                    List<String> availableHeaders,
                                    List<String> missingRequiredFields,
                                    Map<String, String> suggestions) {}

    private static final class HeaderIndexMap extends HashMap<String, Integer> {
        private int headerRowIndex;
        private Map<String, Integer> normalizedHeaderIndex = Collections.emptyMap();

        int getHeaderRowIndex() {
            return headerRowIndex;
        }

        void setHeaderRowIndex(int headerRowIndex) {
            this.headerRowIndex = headerRowIndex;
        }

        Map<String, Integer> getNormalizedHeaderIndex() {
            return normalizedHeaderIndex;
        }

        void setNormalizedHeaderIndex(Map<String, Integer> normalizedHeaderIndex) {
            this.normalizedHeaderIndex = normalizedHeaderIndex == null ? Collections.emptyMap() : new HashMap<>(normalizedHeaderIndex);
        }
    }

    private record ImportResult(boolean skipped, String message, String studentId) {
        static ImportResult skipped(String message) { return new ImportResult(true, message, null); }
        static ImportResult imported(String studentId) { return new ImportResult(false, "Imported", studentId); }
    }
}
