package com.sms.controller;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.sms.dto.dashboard.AssignTeacherRequest;
import com.sms.dto.dashboard.EnrollStudentRequest;
import com.sms.model.Course;
import com.sms.model.Enrollment;
import com.sms.model.SecurityAudit;
import com.sms.model.Student;
import com.sms.model.StudentProfile;
import com.sms.repository.SecurityAuditRepository;
import com.sms.repository.StudentProfileRepository;
import com.sms.service.DashboardService;
import com.sms.service.DatabaseMigrationService;
import com.sms.service.FaceVerificationService;
import com.sms.service.StudentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    private final DashboardService dashboardService;
    private final StudentService studentService;
    private final StudentProfileRepository studentProfileRepository;
    private final SecurityAuditRepository securityAuditRepository;
    private final FaceVerificationService faceVerificationService;
    private final DatabaseMigrationService databaseMigrationService;

    public AdminApiController(DashboardService dashboardService,
                              StudentService studentService,
                              StudentProfileRepository studentProfileRepository,
                              SecurityAuditRepository securityAuditRepository,
                              FaceVerificationService faceVerificationService,
                              DatabaseMigrationService databaseMigrationService) {
        this.dashboardService = dashboardService;
        this.studentService = studentService;
        this.studentProfileRepository = studentProfileRepository;
        this.securityAuditRepository = securityAuditRepository;
        this.faceVerificationService = faceVerificationService;
        this.databaseMigrationService = databaseMigrationService;
    }

    @PostMapping(value = "/upload-face", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MANAGE_STUDENTS') and @permissionEngine.canAccessTenant(#tenantId)")
    public ResponseEntity<Map<String, Object>> uploadFace(
        @RequestParam("file") MultipartFile file,
        @RequestParam("studentId") String studentId,
        @RequestParam(name = "tenantId", required = false) Long tenantId,
        @RequestParam(name = "livenessPrompt", defaultValue = "blink-and-turn") String livenessPrompt,
        @RequestParam(name = "livenessVerified", defaultValue = "true") Boolean livenessVerified) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Face image file is required");
        }

        studentService.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        try {
            FaceVerificationService.FaceVerificationResult result = faceVerificationService.registerFaceFromImageUpload(
                studentId,
                tenantId,
                file.getBytes(),
                file.getOriginalFilename(),
                livenessVerified,
                livenessPrompt,
                true,
                true,
                3
            );

            return ResponseEntity.ok(Map.of(
                "success", result.isVerified(),
                "message", result.getMessage(),
                "studentId", studentId,
                "tenantId", tenantId == null ? 1L : tenantId,
                "model", "Facenet512"
            ));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/students")
    public ResponseEntity<Map<String, Object>> getStudents(@RequestParam(name = "page", defaultValue = "0") int page,
                                                           @RequestParam(name = "size", defaultValue = "20") int size,
                                                           @RequestParam(name = "search", required = false) String search,
                                                           @RequestParam(name = "course", required = false) String course,
                                                           @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
                                                           @RequestParam(name = "sortDir", defaultValue = "asc") String sortDir) {

        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Page<Student> studentsPage = studentService.getStudentsPage(search, course, page, normalizedSize, sortBy, sortDir);

        Map<String, Double> averageMap = studentService.getAverageMarksMap(studentsPage.getContent());
        List<Map<String, Object>> items = new ArrayList<>();

        for (Student student : studentsPage.getContent()) {
            StudentProfile profile = studentProfileRepository.findByStudentId(student.getId()).orElse(null);
            Map<String, Object> row = new HashMap<>();
            row.put("id", student.getId());
            row.put("name", student.getName());
            row.put("enrollment", profile != null && profile.getEnrollmentNumber() != null ? profile.getEnrollmentNumber() : student.getId());
            row.put("email", student.getEmail());
            row.put("course", profile != null && profile.getCourse() != null ? profile.getCourse() : student.getCourse());
            row.put("semester", student.getSemester());
            row.put("section", profile != null && profile.getSection() != null ? profile.getSection() : student.getSection());
            row.put("batch", student.getEnrollmentYear());
            row.put("classGroup", student.getClassGroup());
            row.put("batchGroup", student.getBatchGroup());
            row.put("averageMarks", averageMap.getOrDefault(student.getId(), 0.0));
            row.put("avatar", buildAvatar(student.getName()));
            items.add(row);
        }

        return ResponseEntity.ok(Map.of(
                "items", items,
                "page", studentsPage.getNumber(),
                "size", studentsPage.getSize(),
                "totalElements", studentsPage.getTotalElements(),
                "totalPages", studentsPage.getTotalPages(),
                "hasNext", studentsPage.hasNext(),
                "hasPrevious", studentsPage.hasPrevious()
        ));
    }

    @GetMapping("/database/export")
    public ResponseEntity<byte[]> exportDatabase() {
        try {
            DatabaseMigrationService.ExportedDatabaseBackup backup = databaseMigrationService.exportCurrentDatabase();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + backup.fileName())
                .contentType(MediaType.TEXT_PLAIN)
                .body(backup.content());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database export failed: " + ex.getMessage(), ex);
        }
    }

    @PostMapping(value = "/database/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> restoreDatabase(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Backup file is required");
        }

        try {
            databaseMigrationService.restoreDatabaseFromScript(file.getBytes());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", databaseMigrationService.getLastMigrationMessage()
            ));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Database restore failed: " + ex.getMessage(), ex);
        }
    }

    @PostMapping("/students")
    public ResponseEntity<Map<String, Object>> createStudentViaApi(@RequestBody Map<String, String> payload) {
        String studentId = payload.get("id");
        String studentName = payload.get("name");

        if (studentId == null || studentId.isBlank() || studentName == null || studentName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student ID and Name are required");
        }

        if (studentService.findById(studentId.trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student ID already exists");
        }

        Student student = new Student(studentId.trim(), studentName.trim());
        student.setCourse(trimValue(payload.get("course")));
        student.setSemester(trimValue(payload.get("semester")));
        student.setSection(trimValue(payload.get("section")));
        student.setEnrollmentYear(trimValue(payload.get("batch")));
        student.setPhone(trimValue(payload.get("phone")));
        Student saved = studentService.save(student);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "name", saved.getName(),
                "email", saved.getEmail(),
                "course", saved.getCourse(),
                "semester", saved.getSemester(),
                "section", saved.getSection(),
                "batch", saved.getEnrollmentYear(),
                "classGroup", saved.getClassGroup(),
                "batchGroup", saved.getBatchGroup(),
                "message", "Student created successfully"
        ));
    }

    @PostMapping("/students/recompute-cohorts")
    public ResponseEntity<Map<String, Object>> recomputeStudentCohorts() {
        int updated = studentService.recomputeCohortsForAllStudents();
        return ResponseEntity.ok(Map.of(
            "updated", updated,
            "message", "Student class/batch groups recomputed"
        ));
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<Map<String, Object>> deleteStudent(@PathVariable("id") String id) {
        studentService.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Student deleted", "id", id));
    }

    @PostMapping("/students/bulk-delete")
    public ResponseEntity<Map<String, Object>> bulkDeleteStudents(@RequestBody Map<String, List<String>> payload) {
        List<String> ids = payload.get("ids");
        int deletedCount = studentService.deleteByIds(ids);
        return ResponseEntity.ok(Map.of("deleted", deletedCount));
    }

    @GetMapping("/students/export")
    public ResponseEntity<byte[]> exportStudentsCsv() {
        List<Student> students = studentService.getAllStudentsSortedById();
        Map<String, Double> averageMap = studentService.getAverageMarksMap(students);

        StringBuilder csv = new StringBuilder();
                 csv.append("ID,Name,Email,Course,Semester,Section,Batch,ClassGroup,BatchGroup,AverageMarks\n");
        for (Student student : students) {
            csv.append(escapeCsv(student.getId())).append(',')
               .append(escapeCsv(student.getName())).append(',')
               .append(escapeCsv(student.getEmail())).append(',')
               .append(escapeCsv(student.getCourse())).append(',')
             .append(escapeCsv(student.getSemester())).append(',')
             .append(escapeCsv(student.getSection())).append(',')
             .append(escapeCsv(student.getEnrollmentYear())).append(',')
                         .append(escapeCsv(student.getClassGroup())).append(',')
                         .append(escapeCsv(student.getBatchGroup())).append(',')
               .append(String.format("%.2f", averageMap.getOrDefault(student.getId(), 0.0)))
               .append("\n");
        }

        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    @GetMapping("/students/activity")
    public ResponseEntity<List<Map<String, Object>>> getActivity(@RequestParam(name = "limit", defaultValue = "15") int limit) {
        int normalizedLimit = Math.min(Math.max(limit, 1), 50);
        List<SecurityAudit> audits = securityAuditRepository.findAll(
                PageRequest.of(0, normalizedLimit, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<Map<String, Object>> items = new ArrayList<>();
        for (SecurityAudit audit : audits) {
            Map<String, Object> row = new HashMap<>();
            row.put("type", audit.getViolationType());
            row.put("severity", audit.getSeverityLevel());
            row.put("studentId", audit.getStudentId());
            row.put("description", audit.getDescription());
            row.put("time", audit.getCreatedAt() != null ? audit.getCreatedAt().format(formatter) : "");
            items.add(row);
        }
        return ResponseEntity.ok(items);
    }

    @PostMapping("/enroll")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> enrollStudent(@Valid @RequestBody EnrollStudentRequest request) {
        Enrollment enrollment;
        try {
            enrollment = dashboardService.enrollStudent(request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }

        return ResponseEntity.ok(Map.of(
                "enrollmentId", enrollment.getId(),
                "studentId", enrollment.getStudent().getId(),
                "subjectId", enrollment.getCourse().getId()
        ));
    }

    @PostMapping("/assign-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> assignTeacher(@Valid @RequestBody AssignTeacherRequest request) {
        Course course = dashboardService.assignTeacher(request);
        return ResponseEntity.ok(Map.of(
                "subjectId", course.getId(),
                "teacherId", course.getTeacher().getId()
        ));
    }

    private String buildAvatar(String name) {
        if (name == null || name.isBlank()) {
            return "ST";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }

    private String trimValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
