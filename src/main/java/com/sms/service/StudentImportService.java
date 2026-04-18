package com.sms.service;

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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Comparator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.sms.dto.imports.StudentImportRowUpdateRequest;
import com.sms.model.Enrollment;
import com.sms.model.Student;
import com.sms.model.StudentImportJob;
import com.sms.model.StudentImportRow;
import com.sms.model.Course;
import com.sms.repository.CourseRepository;
import com.sms.repository.EnrollmentRepository;
import com.sms.repository.StudentImportJobRepository;
import com.sms.repository.StudentImportRowRepository;
import com.sms.repository.StudentRepository;

@Service
public class StudentImportService {

    private static final List<String> FIELD_ORDER = List.of(
        "fullName",
        "enrollmentNumber",
        "email",
        "phone",
        "course",
        "semester",
        "department",
        "section",
        "dateOfBirth",
        "gender",
        "address",
        "bloodGroup",
        "guardianName"
    );

    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
        Map.entry("fullName", "Full Name"),
        Map.entry("enrollmentNumber", "Enrollment Number"),
        Map.entry("email", "Email"),
        Map.entry("phone", "Phone"),
        Map.entry("course", "Course"),
        Map.entry("semester", "Semester"),
        Map.entry("department", "Department"),
        Map.entry("section", "Section / Class"),
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
    private final ImportArtifactService importArtifactService;
    private final AnalyticsRealtimeNotifier analyticsRealtimeNotifier;
    private final AnalyticsCacheService analyticsCacheService;

    @Value("${app.import.students.max-file-size-mb:10}")
    private int maxFileSizeMb;

    public StudentImportService(StudentImportJobRepository jobRepository,
                                StudentImportRowRepository rowRepository,
                                StudentRepository studentRepository,
                                EnrollmentRepository enrollmentRepository,
                                CourseRepository courseRepository,
                                StudentService studentService,
                                ImportArtifactService importArtifactService,
                                AnalyticsRealtimeNotifier analyticsRealtimeNotifier,
                                AnalyticsCacheService analyticsCacheService) {
        this.jobRepository = jobRepository;
        this.rowRepository = rowRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.studentService = studentService;
        this.importArtifactService = importArtifactService;
        this.analyticsRealtimeNotifier = analyticsRealtimeNotifier;
        this.analyticsCacheService = analyticsCacheService;
    }

    @Transactional
    public Map<String, Object> uploadAndPreview(MultipartFile file,
                                                String uploadedBy,
                                                String duplicateStrategy,
                                                Boolean rollbackOnFailure,
                                                Map<String, String> mappingOverride) {
        validateUpload(file);
        StudentImportJob job = new StudentImportJob();
        job.setFileName(safeFileName(file.getOriginalFilename()));
        job.setUploadedBy(uploadedBy);
        job.setDuplicateStrategy(normalizeDuplicateStrategy(duplicateStrategy));
        job.setRollbackOnFailure(Boolean.TRUE.equals(rollbackOnFailure));
        job.setStatus(StudentImportJob.Status.UPLOADED);
        job = jobRepository.save(job);

        ParsedImport parsed = parse(file, mappingOverride == null ? Collections.emptyMap() : mappingOverride);
        List<StudentImportRow> rows = validateRows(job, parsed.rows(), parsed.headerIndex());
        rowRepository.saveAll(rows);

        updateJobCounts(job, rows);
        job.setStatus(StudentImportJob.Status.PREVIEW_READY);
        jobRepository.save(job);

        return buildPreviewPayload(job, rows, parsed);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPreview(Long jobId, String requester) {
        StudentImportJob job = getOwnedJob(jobId, requester);
        List<StudentImportRow> rows = rowRepository.findByJobOrderByRowIndexAsc(job);
        return buildPreviewPayload(job, rows, null);
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
        if (request.getPhone() != null) row.setPhone(clean(request.getPhone()));
        if (request.getCourse() != null) row.setCourse(clean(request.getCourse()));
        if (request.getSemester() != null) row.setSemester(clean(request.getSemester()));
        if (request.getDepartment() != null) row.setDepartment(clean(request.getDepartment()));
        if (request.getSection() != null) row.setSection(clean(request.getSection()));
        if (request.getDateOfBirth() != null) row.setDateOfBirth(clean(request.getDateOfBirth()));
        if (request.getGender() != null) row.setGender(clean(request.getGender()));
        if (request.getAddress() != null) row.setAddress(clean(request.getAddress()));
        if (request.getBloodGroup() != null) row.setBloodGroup(clean(request.getBloodGroup()));
        if (request.getGuardianName() != null) row.setGuardianName(clean(request.getGuardianName()));

        sanitizeRow(row);
        rowRepository.save(row);
        List<StudentImportRow> rows = revalidateAllRows(job);
        return buildPreviewPayload(job, rows, null);
    }

    @Transactional
    public Map<String, Object> deleteRow(Long jobId, Long rowId, String requester) {
        StudentImportJob job = getOwnedJob(jobId, requester);
        StudentImportRow row = rowRepository.findById(rowId)
            .filter(existing -> existing.getJob().getId().equals(job.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Import row not found"));

        rowRepository.delete(row);
        List<StudentImportRow> rows = revalidateAllRows(job);
        return buildPreviewPayload(job, rows, null);
    }

    @Transactional
    public Map<String, Object> confirmImport(Long jobId, String requester, String duplicateStrategy, Boolean rollbackOnFailure) {
        StudentImportJob job = getOwnedJob(jobId, requester);
        List<StudentImportRow> rows = rowRepository.findByJobOrderByRowIndexAsc(job);
        String strategy = normalizeDuplicateStrategy(duplicateStrategy != null ? duplicateStrategy : job.getDuplicateStrategy());
        boolean rollback = rollbackOnFailure == null ? job.isRollbackOnFailure() : rollbackOnFailure;

        List<StudentImportRow> validRows = rows.stream()
            .filter(row -> "VALID".equals(row.getStatus()))
            .toList();

        List<String> createdStudentIds = new ArrayList<>();
        int success = 0;
        int failure = 0;
        List<String> errors = new ArrayList<>();
        String errorReportPath = null;

        for (StudentImportRow row : validRows) {
            try {
                ImportResult result = importRow(row, strategy);
                if (result.skipped()) {
                    row.setStatus("SKIPPED");
                    row.setErrorMessage(result.message());
                    failure++;
                    errors.add(buildErrorLine(row, result.message()));
                } else {
                    row.setStatus("IMPORTED");
                    row.setCreatedStudentId(result.studentId());
                    createdStudentIds.add(result.studentId());
                    success++;
                }
            } catch (Exception ex) {
                row.setStatus("FAILED");
                row.setErrorMessage(safeMessage(ex.getMessage()));
                failure++;
                errors.add(buildErrorLine(row, safeMessage(ex.getMessage())));
                if (rollback) {
                    rollbackCreatedStudents(createdStudentIds);
                    job.setStatus(StudentImportJob.Status.FAILED);
                    job.setSuccessCount(0);
                    job.setFailureCount(rows.size());
                    if (!errors.isEmpty()) {
                        errorReportPath = exportErrors(errors);
                        attachErrorReport(job, errorReportPath);
                    }
                    jobRepository.save(job);
                    rowRepository.saveAll(rows);
                    return Map.of(
                        "jobId", job.getId(),
                        "status", "ROLLED_BACK",
                        "successCount", 0,
                        "failureCount", rows.size(),
                        "message", "Import failed and transaction was rolled back",
                        "errorReport", errorReportPath
                    );
                }
            }
        }

        if (!errors.isEmpty()) {
            errorReportPath = exportErrors(errors);
            attachErrorReport(job, errorReportPath);
        }

        job.setStatus(success > 0 ? StudentImportJob.Status.CONFIRMED : StudentImportJob.Status.FAILED);
        job.setSuccessCount(success);
        job.setFailureCount(failure);
        job.setDuplicateStrategy(strategy);
        jobRepository.save(job);
        rowRepository.saveAll(rows);
        analyticsRealtimeNotifier.notifyStudentBulkImport(job.getId(), success);
        analyticsCacheService.evictAnalyticsCaches();

        return Map.of(
            "jobId", job.getId(),
            "status", job.getStatus().name(),
            "successCount", success,
            "failureCount", failure,
            "message", success + " students imported",
            "errorReport", errorReportPath
        );
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

    private ParsedImport parse(MultipartFile file, Map<String, String> mappingOverride) {
        try (InputStream inputStream = file.getInputStream()) {
            if (safeFileName(file.getOriginalFilename()).toLowerCase(Locale.ROOT).endsWith(".csv")) {
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

    private List<StudentImportRow> validateRows(StudentImportJob job, List<List<String>> rows, Map<String, Integer> headerIndex) {
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
            row.setEmail(value(values, headerIndex, "email"));
            row.setPhone(value(values, headerIndex, "phone"));
            row.setCourse(value(values, headerIndex, "course"));
            row.setSemester(value(values, headerIndex, "semester"));
            row.setDepartment(value(values, headerIndex, "department"));
            row.setSection(value(values, headerIndex, "section"));
            row.setDateOfBirth(value(values, headerIndex, "dateOfBirth"));
            row.setGender(value(values, headerIndex, "gender"));
            row.setAddress(value(values, headerIndex, "address"));
            row.setBloodGroup(value(values, headerIndex, "bloodGroup"));
            row.setGuardianName(value(values, headerIndex, "guardianName"));

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

    private Student mapStudent(Student student, StudentImportRow row) {
        student.setName(clean(row.getFullName()));
        student.setEmail(clean(row.getEmail()));
        student.setPhone(clean(row.getPhone()));
        student.setCourse(clean(row.getCourse()));
        student.setSemester(clean(row.getSemester()));
        student.setDepartment(clean(row.getDepartment()));
        student.setSection(clean(row.getSection()));
        student.setGender(clean(row.getGender()));
        student.setAddress(clean(row.getAddress()));
        student.setEnrollmentYear(extractYear(row.getEnrollmentNumber()));
        try {
            student.setDob(parseDate(row.getDateOfBirth()));
        } catch (Exception ignored) {
            student.setDob(null);
        }
        return student;
    }

    private void upsertEnrollment(Student student, String courseValue) {
        if (!StringUtils.hasText(courseValue)) {
            return;
        }

        Course course = courseRepository.findByCode(courseValue.trim())
            .orElseGet(() -> courseRepository.findAll().stream()
                .filter(candidate -> courseValue.trim().equalsIgnoreCase(candidate.getCourseName()))
                .findFirst()
                .orElseGet(() -> {
                    Course created = new Course();
                    created.setCode(generateCourseCode(courseValue));
                    created.setCourseName(courseValue.trim());
                    created.setCredits(3);
                    return courseRepository.save(created);
                }));

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

    private Map<String, Object> buildPreviewPayload(StudentImportJob job, List<StudentImportRow> rows, ParsedImport parsed) {
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
        payload.put("headers", FIELD_ORDER.stream().map(FIELD_LABELS::get).toList());
        payload.put("rows", previewRows);
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

        return payload;
    }

    private Map<String, Object> rowToMap(StudentImportRow row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.getId());
        item.put("rowIndex", row.getRowIndex());
        item.put("fullName", row.getFullName());
        item.put("enrollmentNumber", row.getEnrollmentNumber());
        item.put("email", row.getEmail());
        item.put("phone", row.getPhone());
        item.put("course", row.getCourse());
        item.put("semester", row.getSemester());
        item.put("department", row.getDepartment());
        item.put("section", row.getSection());
        item.put("dateOfBirth", row.getDateOfBirth());
        item.put("gender", row.getGender());
        item.put("address", row.getAddress());
        item.put("bloodGroup", row.getBloodGroup());
        item.put("guardianName", row.getGuardianName());
        item.put("classGroup", computeClassGroup(row.getEnrollmentNumber()));
        item.put("batchGroup", computeBatchGroup(row.getEnrollmentNumber()));
        item.put("status", row.getStatus());
        item.put("errorMessage", row.getErrorMessage());
        return item;
    }

    private StudentImportJob getOwnedJob(Long jobId, String requester) {
        return jobRepository.findByIdAndUploadedBy(jobId, requester)
            .orElseThrow(() -> new IllegalArgumentException("Import job not found"));
    }

    private String normalizeDuplicateStrategy(String duplicateStrategy) {
        String normalized = duplicateStrategy == null ? "SKIP" : duplicateStrategy.trim().toUpperCase(Locale.ROOT);
        if (List.of("SKIP", "OVERWRITE", "UPDATE", "REJECT").contains(normalized)) {
            return normalized;
        }
        return "SKIP";
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
            case "enrollmentNumber" -> List.of("Enrollment Number", "Enrollment No", "Enrollment", "Roll Number", "Student ID", "Enrollment Id");
            case "email" -> List.of("Email", "Email Address", "Mail");
            case "phone" -> List.of("Phone", "Mobile", "Contact", "Phone Number", "Mobile Number");
            case "course" -> List.of("Course", "Program", "Branch");
            case "semester" -> List.of("Semester", "Sem", "Term");
            case "department" -> List.of("Department", "Dept", "School");
            case "section" -> List.of("Section", "Class", "Class Name", "Section Name", "Group");
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
        row.setFullName(clean(row.getFullName()));
        row.setEnrollmentNumber(cleanEnrollment(row.getEnrollmentNumber()));
        row.setEmail(cleanEmail(row.getEmail()));
        row.setPhone(cleanPhone(row.getPhone()));
        row.setCourse(clean(row.getCourse()));
        row.setSemester(clean(row.getSemester()));
        row.setDepartment(clean(row.getDepartment()));
        row.setSection(clean(row.getSection()));
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
        if (!StringUtils.hasText(enrollmentNumber)) {
            return null;
        }
        String digits = enrollmentNumber.replaceAll("\\D+", "");
        return digits.length() >= 4 ? digits.substring(0, 4) : null;
    }

    private String computeClassGroup(String enrollmentNumber) {
        Integer serial = extractEnrollmentSerial(enrollmentNumber);
        if (serial == null || serial <= 0) {
            return null;
        }
        int classNumber = ((serial - 1) / 120) + 1;
        return "Class " + classNumber;
    }

    private String computeBatchGroup(String enrollmentNumber) {
        Integer serial = extractEnrollmentSerial(enrollmentNumber);
        if (serial == null || serial <= 0) {
            return null;
        }
        int batchNumber = (((serial - 1) % 120) / 30) + 1;
        return "Batch " + batchNumber;
    }

    @SuppressWarnings("unused")
    private Integer extractEnrollmentSerial(String enrollmentNumber) {
        if (!StringUtils.hasText(enrollmentNumber)) {
            return null;
        }
        Matcher matcher = ENROLLMENT_SERIAL_PATTERN.matcher(enrollmentNumber);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        if (last == null) {
            return null;
        }
        try {
            return Integer.parseInt(last);
        } catch (NumberFormatException ex) {
            return null;
        }
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

        int getHeaderRowIndex() {
            return headerRowIndex;
        }

        void setHeaderRowIndex(int headerRowIndex) {
            this.headerRowIndex = headerRowIndex;
        }
    }

    private record ImportResult(boolean skipped, String message, String studentId) {
        static ImportResult skipped(String message) { return new ImportResult(true, message, null); }
        static ImportResult imported(String studentId) { return new ImportResult(false, "Imported", studentId); }
    }
}
