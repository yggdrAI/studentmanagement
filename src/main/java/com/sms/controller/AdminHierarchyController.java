package com.sms.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sms.model.Student;
import com.sms.model.StudentProfile;
import com.sms.repository.AttendanceRepository;
import com.sms.repository.StudentProfileRepository;
import com.sms.repository.StudentRepository;
import com.sms.service.HierarchyCatalogService;
import com.sms.service.StudentGroupingService;
import com.sms.service.StudentService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHierarchyController {

    private static final int BATCH_SIZE = 30;
    private static final int DEFAULT_CLUSTER_COUNT = 4;
    /**
     * Synthetic group identifiers for students with no class/batch assignments.
     * We keep them distinct from real class/batch numbers so we don't merge them
     * into "Class 1 / Batch 1" by accident.
     */
    private static final int UNASSIGNED_CLASS_NUMBER = 0;
    private static final int UNASSIGNED_BATCH_NUMBER = 0;
    private static final Pattern TRAILING_NUMBER_PATTERN = Pattern.compile("(\\d+)$");

    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentService studentService;
    private final HierarchyCatalogService hierarchyCatalogService;
    private final StudentGroupingService studentGroupingService;

    public AdminHierarchyController(StudentRepository studentRepository,
                                    StudentProfileRepository studentProfileRepository,
                                    AttendanceRepository attendanceRepository,
                                    StudentService studentService,
                                    HierarchyCatalogService hierarchyCatalogService,
                                    StudentGroupingService studentGroupingService) {
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.attendanceRepository = attendanceRepository;
        this.studentService = studentService;
        this.hierarchyCatalogService = hierarchyCatalogService;
        this.studentGroupingService = studentGroupingService;
    }

    // ─── Grouping Pipeline Endpoints ────────────────────────────────────────

    @PostMapping("/grouping/regenerate")
    @CacheEvict(value = "hierarchyCache", allEntries = true)
    public ResponseEntity<Map<String, Object>> regenerateGroupings() {
        Map<String, Object> result = studentGroupingService.regenerateAllGroupings();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/grouping/programs")
    public ResponseEntity<List<Map<String, Object>>> getProgramTree() {
        List<Map<String, Object>> tree = hierarchyCatalogService.getProgramTree();
        return ResponseEntity.ok(tree);
    }

    @GetMapping("/grouping/programs/summaries")
    public ResponseEntity<List<Map<String, Object>>> getProgramSummaries() {
        List<Map<String, Object>> summaries = hierarchyCatalogService.getProgramSummaries();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/grouping/programs/{programId}/students")
    public ResponseEntity<List<Map<String, Object>>> getProgramStudents(@PathVariable Long programId) {
        List<Map<String, Object>> students = hierarchyCatalogService.getStudentsForProgram(programId);
        return ResponseEntity.ok(students);
    }

    // ─── Existing Hierarchy Endpoints ───────────────────────────────────────

    @GetMapping("/students-hierarchy")
    @Cacheable(value = "hierarchyCache", key = "#course + ':' + #semester + ':' + #classNumber + ':' + #batchNumber + ':' + #performance + ':' + #includeStudents")
    public ResponseEntity<Map<String, Object>> getStudentsHierarchy(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) Integer classNumber,
            @RequestParam(required = false) Integer batchNumber,
            @RequestParam(required = false) String performance,
            @RequestParam(required = false, defaultValue = "true") boolean includeStudents) {

        // UI sends semester as "2" (from the dropdown), while we often persist "Semester 2".
        // It also sends short course labels like "B.Tech", while we may store full names.
        // Normalize common "all" sentinel values and tolerate numeric semester filters.
        final String normalizedCourse = normalizeAllSentinel(course);
        final String normalizedSemester = normalizeAllSentinel(semester);
        final String normalizedPerformance = normalizeAllSentinel(performance);
        final Integer normalizedClassNumber = normalizeNonPositiveToNull(classNumber);
        final Integer normalizedBatchNumber = normalizeNonPositiveToNull(batchNumber);

        List<Student> students = studentRepository.findAllWithFullHierarchy();
        Map<String, StudentProfile> profileByStudentId = loadProfilesByStudentId(students);
        Map<String, Double> marksMap = studentService.getAverageMarksMap(students);
        Map<String, Double> attendanceMap = loadAttendanceRateMap();

        List<Student> filtered = students.stream()
                .filter(student -> normalizedCourse == null || normalizedCourse.isBlank() || matchesCourse(student.getCourse(), normalizedCourse))
                .filter(student -> normalizedSemester == null || normalizedSemester.isBlank() || matchesSemester(student.getSemester(), normalizedSemester))
            .filter(student -> normalizedClassNumber == null || classGroupingKey(student, profileByStudentId.get(student.getId())) == normalizedClassNumber)
            .filter(student -> normalizedBatchNumber == null || batchGroupingKey(student, profileByStudentId.get(student.getId())) == normalizedBatchNumber)
                .filter(student -> normalizedPerformance == null || normalizedPerformance.isBlank()
                        || performanceBand(marksMap.getOrDefault(student.getId(), 0.0)).equalsIgnoreCase(normalizedPerformance))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "summary", Map.of(
                            "totalClasses", 0,
                            "totalBatches", 0,
                            "totalStudents", 0
                    ),
                    "classes", List.of()
            ));
        }

        Map<Integer, Map<Integer, List<Student>>> grouped = filtered.stream()
            .collect(Collectors.groupingBy(
                student -> classGroupingKey(student, profileByStudentId.get(student.getId())),
                Collectors.groupingBy(student -> batchGroupingKey(student, profileByStudentId.get(student.getId())))
            ));

        List<Map<String, Object>> classes = grouped.keySet().stream()
                .sorted()
            .map(classKey -> buildClassNode(classKey, grouped.get(classKey), marksMap, attendanceMap, profileByStudentId, includeStudents))
                .collect(Collectors.toList());

        int totalBatchCount = grouped.values().stream()
                .mapToInt(classBatches -> classBatches.size())
                .sum();

        long totalProgramCount = filtered.stream()
                .map(Student::getAcademicProgram)
                .filter(java.util.Objects::nonNull)
                .map(p -> p.getId())
                .distinct()
                .count();

        return ResponseEntity.ok(Map.of(
                "summary", Map.of(
                        "totalPrograms", totalProgramCount,
                        "totalClasses", grouped.size(),
                        "totalBatches", totalBatchCount,
                        "totalStudents", filtered.size()
                ),
                "classes", classes
        ));
    }

    @GetMapping("/class/{classNumber}/analytics")
    public ResponseEntity<Map<String, Object>> getClassAnalytics(@PathVariable Integer classNumber) {
        List<Student> allStudents = studentRepository.findAllWithFullHierarchy();
        Map<String, StudentProfile> profileByStudentId = loadProfilesByStudentId(allStudents);
        List<Student> classStudents = allStudents.stream()
                .filter(student -> extractClassNumber(student, profileByStudentId.get(student.getId())) == classNumber)
                .collect(Collectors.toList());

        if (classStudents.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Double> marksMap = studentService.getAverageMarksMap(classStudents);
        Map<String, Double> attendanceMap = loadAttendanceRateMap();

        Map<String, Object> analytics = buildClassAnalytics(classStudents, marksMap, attendanceMap, profileByStudentId);
        Map<String, Object> payload = new HashMap<>(analytics);
        payload.put("classNumber", classNumber);
        payload.put("totalStudents", classStudents.size());

        return ResponseEntity.ok(payload);
    }

    @PostMapping("/students-hierarchy/reassign")
    @CacheEvict(value = "hierarchyCache", allEntries = true)
    public ResponseEntity<Map<String, Object>> reassignStudent(@RequestBody ReassignRequest request) {
        if (request == null || request.getStudentId() == null || request.getStudentId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId is required");
        }
        if (request.getClassNumber() == null || request.getClassNumber() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "classNumber must be >= 1");
        }
        if (request.getBatchNumber() == null || request.getBatchNumber() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "batchNumber must be >= 1");
        }

        Student student = studentRepository.findById(request.getStudentId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        hierarchyCatalogService.assignStudent(student, request.getClassNumber(), request.getBatchNumber());

        Map<String, Object> payload = new HashMap<>();
        payload.put("studentId", student.getId());
        payload.put("classGroup", student.getClassGroup());
        payload.put("batchGroup", student.getBatchGroup());
        payload.put("message", "Student reassigned successfully");

        return ResponseEntity.ok(payload);
    }

    @PostMapping("/students-hierarchy/ai-grouping")
    public ResponseEntity<Map<String, Object>> suggestAiGrouping(@RequestBody(required = false) AiGroupingRequest request) {
        AiGroupingRequest req = request == null ? new AiGroupingRequest() : request;
        Integer requestedClusters = req.getClusters();
        int requestedClusterCount = requestedClusters == null ? DEFAULT_CLUSTER_COUNT : requestedClusters;
        int clusters = Math.max(2, Math.min(4, requestedClusterCount));

        List<Student> students = studentRepository.findAllWithFullHierarchy().stream()
                .filter(student -> req.getCourse() == null || req.getCourse().isBlank() || matchesIgnoreCase(student.getCourse(), req.getCourse()))
                .filter(student -> req.getSemester() == null || req.getSemester().isBlank() || matchesIgnoreCase(student.getSemester(), req.getSemester()))
                .filter(student -> req.getClassNumber() == null || extractClassNumber(student) == req.getClassNumber())
                .collect(Collectors.toList());

        if (students.size() < clusters) {
            return ResponseEntity.ok(Map.of(
                    "clusterCount", clusters,
                    "studentsConsidered", students.size(),
                    "suggestions", List.of(),
                    "message", "Not enough students for clustering"
            ));
        }

        Map<String, Double> marksMap = studentService.getAverageMarksMap(students);
        Map<String, Double> attendanceMap = loadAttendanceRateMap();

        List<FeaturePoint> points = students.stream()
                .map(student -> new FeaturePoint(
                        student,
                        marksMap.getOrDefault(student.getId(), 0.0),
                        attendanceMap.getOrDefault(student.getId(), 75.0)
                ))
                .collect(Collectors.toList());

        Map<String, Integer> labels = runKMeans(points, clusters, 20);

        List<Map<String, Object>> suggestions = points.stream()
                .map(point -> {
                    int clusterLabel = labels.getOrDefault(point.student().getId(), 0);
                    int currentClass = extractClassNumber(point.student());
                    int suggestedBatch = ((currentClass - 1) * DEFAULT_CLUSTER_COUNT) + ((clusterLabel % DEFAULT_CLUSTER_COUNT) + 1);
                    int currentBatch = extractBatchNumber(point.student());

                    Map<String, Object> row = new HashMap<>();
                    row.put("studentId", point.student().getId());
                    row.put("name", point.student().getName());
                    row.put("marks", round(point.marks()));
                    row.put("attendance", round(point.attendance()));
                    row.put("currentBatch", currentBatch);
                    row.put("suggestedBatch", suggestedBatch);
                    row.put("changed", currentBatch != suggestedBatch);
                    return row;
                })
                .sorted(Comparator.comparing(row -> String.valueOf(row.get("studentId"))))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "clusterCount", clusters,
                "studentsConsidered", students.size(),
                "suggestions", suggestions
        ));
    }

    private Map<String, Object> buildClassNode(Integer classNumber,
                                               Map<Integer, List<Student>> classBatches,
                                               Map<String, Double> marksMap,
                               Map<String, Double> attendanceMap,
                               Map<String, StudentProfile> profileByStudentId,
                               boolean includeStudents) {
        if (isUnassignedClassKey(classNumber)) {
            List<Map<String, Object>> batches = new ArrayList<>();
            List<Integer> batchKeys = new ArrayList<>(classBatches.keySet());
            batchKeys.sort(Integer::compareTo);
            for (int batchNumber : batchKeys) {
                batches.add(buildBatchNode(classNumber, batchNumber, classBatches.getOrDefault(batchNumber, List.of()), marksMap, attendanceMap, profileByStudentId, includeStudents));
            }

            List<Student> allClassStudents = classBatches.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
            Map<String, Object> classAnalytics = buildClassAnalytics(allClassStudents, marksMap, attendanceMap, profileByStudentId);

            Map<String, Object> node = new HashMap<>();
            node.put("id", UNASSIGNED_CLASS_NUMBER);
            node.put("number", UNASSIGNED_CLASS_NUMBER);
            node.put("localNumber", UNASSIGNED_CLASS_NUMBER);
            node.put("label", "Unassigned");
            node.put("programCode", "");
            node.put("totalStudents", allClassStudents.size());
            node.put("totalBatches", batches.size());
            node.put("batches", batches);
            node.put("analytics", classAnalytics);
            return node;
        }

        // Determine program code from the first student in this class
        String programCode = "";
        int localClassNum = classNumber;
        List<Student> allClassStudents = classBatches.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        if (!allClassStudents.isEmpty()) {
            Student firstStudent = allClassStudents.get(0);
            if (firstStudent.getAcademicProgram() != null && firstStudent.getAcademicProgram().getCode() != null) {
                programCode = firstStudent.getAcademicProgram().getCode();
            }
            if (firstStudent.getAcademicClass() != null && firstStudent.getAcademicClass().getLocalClassNumber() != null) {
                localClassNum = firstStudent.getAcademicClass().getLocalClassNumber();
            }
        }
        String classLabel = programCode.isEmpty()
                ? "Class " + localClassNum
                : programCode + " — Class " + localClassNum;

        List<Map<String, Object>> batches = new ArrayList<>();
        List<Integer> batchKeys = new ArrayList<>(classBatches.keySet());
        batchKeys.sort(Integer::compareTo);
        for (int batchNumber : batchKeys) {
            batches.add(buildBatchNode(classNumber, batchNumber, classBatches.getOrDefault(batchNumber, List.of()), marksMap, attendanceMap, profileByStudentId, includeStudents));
        }

        List<Student> classStudents = allClassStudents;
        Map<String, Object> classAnalytics = buildClassAnalytics(classStudents, marksMap, attendanceMap, profileByStudentId);

        Map<String, Object> node = new HashMap<>();
        node.put("id", classNumber);
        node.put("number", classNumber);
        node.put("localNumber", localClassNum);
        node.put("label", classLabel);
        node.put("programCode", programCode);
        node.put("totalStudents", classStudents.size());
        node.put("totalBatches", batches.size());
        node.put("batches", batches);
        node.put("analytics", classAnalytics);
        return node;
    }

    private Map<String, Object> buildBatchNode(Integer classNumber,
                                               Integer batchNumber,
                                               List<Student> batchStudents,
                                               Map<String, Double> marksMap,
                               Map<String, Double> attendanceMap,
                       Map<String, StudentProfile> profileByStudentId,
                       boolean includeStudents) {
        if (isUnassignedBatchKey(batchNumber)) {
            List<Map<String, Object>> students = includeStudents
                    ? batchStudents.stream()
                    .sorted(Comparator.comparingInt(student -> extractBatchMemberOrder(student, profileByStudentId.get(student.getId()))))
                    .map(student -> buildStudentNode(student, marksMap, attendanceMap, profileByStudentId.get(student.getId())))
                    .collect(Collectors.toList())
                    : List.of();

            double avgMarks = average(batchStudents.stream().map(student -> marksMap.getOrDefault(student.getId(), 0.0)).collect(Collectors.toList()));
            double avgAttendance = average(batchStudents.stream().map(student -> attendanceMap.getOrDefault(student.getId(), 75.0)).collect(Collectors.toList()));
            long risk = batchStudents.stream().filter(student -> marksMap.getOrDefault(student.getId(), 0.0) < 50.0).count();

            Map<String, Object> analytics = new HashMap<>();
            analytics.put("avgMarks", round(avgMarks));
            analytics.put("attendance", round(avgAttendance));
            analytics.put("riskStudents", risk);

            Map<String, Object> node = new HashMap<>();
            node.put("id", UNASSIGNED_BATCH_NUMBER);
            node.put("number", UNASSIGNED_BATCH_NUMBER);
            node.put("localNumber", UNASSIGNED_BATCH_NUMBER);
            node.put("label", "Unassigned");
            node.put("classNumber", classNumber);
            node.put("studentsCount", batchStudents.size());
            node.put("totalStudents", batchStudents.size());
            node.put("analytics", analytics);
            node.put("students", students);
            return node;
        }

        // Derive the local batch number from the first student (if available)
        int localBatchNum = batchNumber;
        if (!batchStudents.isEmpty()) {
            Student first = batchStudents.get(0);
            if (first.getAcademicBatch() != null && first.getAcademicBatch().getLocalBatchNumber() != null) {
                localBatchNum = first.getAcademicBatch().getLocalBatchNumber();
            }
        }

        List<Map<String, Object>> students = includeStudents
            ? batchStudents.stream()
                .sorted(Comparator.comparingInt(student -> extractBatchMemberOrder(student, profileByStudentId.get(student.getId()))))
                .map(student -> buildStudentNode(student, marksMap, attendanceMap, profileByStudentId.get(student.getId())))
                .collect(Collectors.toList())
            : List.of();

        double avgMarks = average(batchStudents.stream().map(student -> marksMap.getOrDefault(student.getId(), 0.0)).collect(Collectors.toList()));
        double avgAttendance = average(batchStudents.stream().map(student -> attendanceMap.getOrDefault(student.getId(), 75.0)).collect(Collectors.toList()));
        long risk = batchStudents.stream().filter(student -> marksMap.getOrDefault(student.getId(), 0.0) < 50.0).count();

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("avgMarks", round(avgMarks));
        analytics.put("attendance", round(avgAttendance));
        analytics.put("riskStudents", risk);

        Map<String, Object> node = new HashMap<>();
        node.put("id", batchNumber);
        node.put("number", batchNumber);
        node.put("localNumber", localBatchNum);
        node.put("label", "Batch " + localBatchNum);
        node.put("classNumber", classNumber);
        node.put("studentsCount", batchStudents.size());
        node.put("totalStudents", batchStudents.size());
        node.put("analytics", analytics);
        node.put("students", students);
        return node;
    }

    private Map<String, Object> buildStudentNode(Student student,
                                                 Map<String, Double> marksMap,
                             Map<String, Double> attendanceMap,
                             StudentProfile profile) {
        String enrollment = profile != null && profile.getEnrollmentNumber() != null && !profile.getEnrollmentNumber().isBlank()
                ? profile.getEnrollmentNumber()
                : student.getId();

        double marks = marksMap.getOrDefault(student.getId(), 0.0);
        double attendance = attendanceMap.getOrDefault(student.getId(), 75.0);

        Map<String, Object> node = new HashMap<>();
        node.put("id", student.getId());
        node.put("name", student.getName());
        node.put("enrollmentNumber", enrollment);
        node.put("enrollment", enrollment);
        node.put("email", student.getEmail() == null ? "" : student.getEmail());
        node.put("faceStatus", hasFace(student) ? "registered" : "missing");
        node.put("marks", round(marks));
        node.put("attendance", round(attendance));
        node.put("performanceBand", performanceBand(marks));
        node.put("classNumber", extractClassNumber(student));
        node.put("batchNumber", extractBatchNumber(student));
        return node;
    }

    private Map<String, Object> buildClassAnalytics(List<Student> students,
                                                    Map<String, Double> marksMap,
                                                    Map<String, Double> attendanceMap,
                                                    Map<String, StudentProfile> profileByStudentId) {
        Map<Integer, List<Student>> byBatch = students.stream().collect(Collectors.groupingBy(student -> extractBatchNumber(student, profileByStudentId.get(student.getId()))));

        int topBatch = 1;
        double topBatchMarks = -1;
        int lowAttendanceBatch = 1;
        double lowAttendance = Double.MAX_VALUE;
        long riskStudents = 0;

        List<Map<String, Object>> trend = new ArrayList<>();

        for (Map.Entry<Integer, List<Student>> entry : byBatch.entrySet()) {
            int batch = entry.getKey();
            List<Student> list = entry.getValue();

            double batchMarks = average(list.stream().map(student -> marksMap.getOrDefault(student.getId(), 0.0)).collect(Collectors.toList()));
            double batchAttendance = average(list.stream().map(student -> attendanceMap.getOrDefault(student.getId(), 75.0)).collect(Collectors.toList()));

            if (batchMarks > topBatchMarks) {
                topBatchMarks = batchMarks;
                topBatch = batch;
            }
            if (batchAttendance < lowAttendance) {
                lowAttendance = batchAttendance;
                lowAttendanceBatch = batch;
            }

            riskStudents += list.stream().filter(student -> marksMap.getOrDefault(student.getId(), 0.0) < 50.0).count();

            Map<String, Object> point = new HashMap<>();
            point.put("batch", "B" + batch);
            point.put("marks", round(batchMarks));
            point.put("attendance", round(batchAttendance));
            trend.add(point);
        }

        trend.sort(Comparator.comparingInt(point -> Integer.parseInt(String.valueOf(point.get("batch")).substring(1))));

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("avgMarks", round(average(students.stream().map(student -> marksMap.getOrDefault(student.getId(), 0.0)).collect(Collectors.toList()))));
        analytics.put("attendance", round(average(students.stream().map(student -> attendanceMap.getOrDefault(student.getId(), 75.0)).collect(Collectors.toList()))));
        analytics.put("topPerformingBatch", topBatch);
        analytics.put("lowestAttendanceBatch", lowAttendanceBatch);
        analytics.put("riskStudents", riskStudents);
        analytics.put("trend", trend);
        return analytics;
    }

    private Map<String, Double> loadAttendanceRateMap() {
        List<Object[]> rows = attendanceRepository.studentAttendanceRates(1);
        Map<String, Double> result = new HashMap<>();
        for (Object[] row : rows) {
            String studentId = String.valueOf(row[0]);
            long present = toLong(row[1]);
            long total = toLong(row[2]);
            double rate = total == 0 ? 0.0 : ((double) present * 100.0) / (double) total;
            result.put(studentId, rate);
        }
        return result;
    }

    private Map<String, StudentProfile> loadProfilesByStudentId(Collection<Student> students) {
        List<String> studentIds = students.stream().map(Student::getId).collect(Collectors.toList());
        Map<String, StudentProfile> result = new HashMap<>();
        studentProfileRepository.findAllById(studentIds).forEach(profile -> result.put(profile.getStudentId(), profile));
        return result;
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private boolean matchesIgnoreCase(String actual, String expected) {
        return actual != null && actual.equalsIgnoreCase(expected);
    }

    private String normalizeAllSentinel(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return "";
        if ("all".equalsIgnoreCase(trimmed)
                || "all courses".equalsIgnoreCase(trimmed)
                || "all semesters".equalsIgnoreCase(trimmed)
                || "all students".equalsIgnoreCase(trimmed)
                || "all performance".equalsIgnoreCase(trimmed)) {
            return "";
        }
        return trimmed;
    }

    private Integer normalizeNonPositiveToNull(Integer value) {
        if (value == null) return null;
        return value <= 0 ? null : value;
    }

    private boolean matchesCourse(String actualCourse, String filterCourse) {
        if (actualCourse == null) return false;
        if (filterCourse == null || filterCourse.isBlank()) return true;

        if (matchesIgnoreCase(actualCourse, filterCourse)) {
            return true;
        }

        // Loose matching to support dropdown values like "B.Tech" against stored strings like
        // "Bachelor of Technology (Computer Science and Engineering)".
        String normActual = normalizeLoose(actualCourse);
        String normFilter = normalizeLoose(filterCourse);
        if (normActual.isEmpty() || normFilter.isEmpty()) return false;
        return normActual.contains(normFilter) || normFilter.contains(normActual);
    }

    private boolean matchesSemester(String actualSemester, String filterSemester) {
        if (actualSemester == null) return false;
        if (filterSemester == null || filterSemester.isBlank()) return true;

        if (matchesIgnoreCase(actualSemester, filterSemester)) {
            return true;
        }

        // Accept "2" vs "Semester 2" mismatches.
        Integer filterNumber = parseTrailingInt(filterSemester);
        Integer actualNumber = parseTrailingInt(actualSemester);
        return filterNumber != null && actualNumber != null && filterNumber.equals(actualNumber);
    }

    private Integer parseTrailingInt(String value) {
        if (value == null || value.isBlank()) return null;
        Matcher matcher = TRAILING_NUMBER_PATTERN.matcher(value.trim());
        if (!matcher.find()) return null;
        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeLoose(String value) {
        if (value == null) return "";
        return value.toLowerCase()
                .replace('&', ' ')
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private boolean isUnassigned(Student student) {
        return student != null && student.getAcademicClass() == null && student.getAcademicBatch() == null;
    }

    private int classGroupingKey(Student student, StudentProfile profile) {
        if (isUnassigned(student)) {
            return UNASSIGNED_CLASS_NUMBER;
        }
        return extractClassNumber(student, profile);
    }

    private int batchGroupingKey(Student student, StudentProfile profile) {
        if (isUnassigned(student)) {
            return UNASSIGNED_BATCH_NUMBER;
        }
        return extractBatchNumber(student, profile);
    }

    private boolean isUnassignedClassKey(Integer classNumber) {
        return classNumber != null && classNumber == UNASSIGNED_CLASS_NUMBER;
    }

    private boolean isUnassignedBatchKey(Integer batchNumber) {
        return batchNumber != null && batchNumber == UNASSIGNED_BATCH_NUMBER;
    }

    private boolean hasFace(Student student) {
        return student.getProfileImageUrl() != null && !student.getProfileImageUrl().isBlank();
    }

    private String performanceBand(double marks) {
        if (marks >= 75.0) {
            return "excellent";
        }
        if (marks >= 60.0) {
            return "good";
        }
        if (marks >= 50.0) {
            return "average";
        }
        return "poor";
    }

    private int extractClassNumber(Student student) {
        return extractClassNumber(student, null);
    }

    private int extractClassNumber(Student student, StudentProfile profile) {
        // Use the GLOBAL classNumber from the AcademicClass entity so that
        // classes across different programs get unique numbers and don't
        // merge together in the flat hierarchy view.
        if (student != null && student.getAcademicClass() != null) {
            Integer global = student.getAcademicClass().getClassNumber();
            if (global != null && global > 0) return global;
            Integer local = student.getAcademicClass().getLocalClassNumber();
            if (local != null && local > 0) return local;
        }

        Integer profileClassNumber = extractTrailingInteger(profile == null ? null : profile.getFoundationClassroom());
        if (profileClassNumber != null && profileClassNumber > 0) {
            return profileClassNumber;
        }

        String classGroup = student == null ? null : student.getClassGroup();
        Integer classGroupNumber = extractTrailingInteger(classGroup);
        if (classGroupNumber != null && classGroupNumber > 0) {
            return classGroupNumber;
        }
        // Fall back to class 1 so unassigned students still appear in the hierarchy.
        return 1;
    }

    private int extractBatchNumber(Student student) {
        return extractBatchNumber(student, null);
    }

    private int extractBatchNumber(Student student, StudentProfile profile) {
        // Use the GLOBAL batchNumber so batches across different programs
        // get unique numbers and don't collide.
        if (student != null && student.getAcademicBatch() != null) {
            Integer global = student.getAcademicBatch().getBatchNumber();
            if (global != null && global > 0) return global;
            Integer local = student.getAcademicBatch().getLocalBatchNumber();
            if (local != null && local > 0) return local;
        }

        if (profile != null && profile.getTeamNumber() != null && profile.getTeamNumber() > 0) {
            Integer classNumber = extractTrailingInteger(profile.getFoundationClassroom());
            if (classNumber != null && classNumber > 0) {
                return ((classNumber - 1) * DEFAULT_CLUSTER_COUNT) + profile.getTeamNumber();
            }
        }

        String batchGroup = student == null ? null : student.getBatchGroup();
        Integer batchGroupNumber = extractTrailingInteger(batchGroup);
        if (batchGroupNumber != null && batchGroupNumber > 0) {
            return batchGroupNumber;
        }
        // Fall back to batch 1 so unassigned students still appear in the hierarchy.
        return 1;
    }

    private int extractSerialNumber(Student student, StudentProfile profile) {
        String enrollment = profile != null && profile.getEnrollmentNumber() != null && !profile.getEnrollmentNumber().isBlank()
                ? profile.getEnrollmentNumber().trim()
                : student.getId();

        Matcher matcher = TRAILING_NUMBER_PATTERN.matcher(enrollment);
        if (!matcher.find()) {
            return 0;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int extractBatchMemberOrder(Student student, StudentProfile profile) {
        if (profile != null && profile.getMemberNumber() != null && profile.getMemberNumber() > 0) {
            return profile.getMemberNumber();
        }
        return extractSerialNumber(student, profile);
    }

    private Integer extractTrailingInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        Matcher matcher = TRAILING_NUMBER_PATTERN.matcher(value.trim());
        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        return values.stream().filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private Map<String, Integer> runKMeans(List<FeaturePoint> points, int clusters, int maxIterations) {
        List<double[]> centroids = initializeCentroids(points, clusters);
        Map<String, Integer> labels = new HashMap<>();

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            boolean changed = false;

            for (FeaturePoint point : points) {
                int nearest = nearestCentroid(point, centroids);
                Integer previous = labels.put(point.student().getId(), nearest);
                if (previous == null || previous != nearest) {
                    changed = true;
                }
            }

            centroids = recomputeCentroids(points, labels, clusters, centroids);

            if (!changed) {
                break;
            }
        }

        return labels;
    }

    private List<double[]> initializeCentroids(List<FeaturePoint> points, int clusters) {
        List<double[]> centroids = new ArrayList<>();
        List<FeaturePoint> shuffled = new ArrayList<>(points);
        java.util.Collections.shuffle(shuffled);

        for (int i = 0; i < clusters; i++) {
            FeaturePoint selected = shuffled.get(i % shuffled.size());
            centroids.add(new double[]{selected.marks(), selected.attendance()});
        }
        return centroids;
    }

    private int nearestCentroid(FeaturePoint point, List<double[]> centroids) {
        double bestDistance = Double.MAX_VALUE;
        int bestIndex = 0;

        for (int i = 0; i < centroids.size(); i++) {
            double[] centroid = centroids.get(i);
            double distance = squaredDistance(point.marks(), point.attendance(), centroid[0], centroid[1]);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private List<double[]> recomputeCentroids(List<FeaturePoint> points,
                                              Map<String, Integer> labels,
                                              int clusters,
                                              List<double[]> previous) {
        List<double[]> updated = new ArrayList<>();

        for (int cluster = 0; cluster < clusters; cluster++) {
            final int currentCluster = cluster;
            List<FeaturePoint> assigned = points.stream()
                    .filter(point -> labels.getOrDefault(point.student().getId(), -1) == currentCluster)
                    .collect(Collectors.toList());

            if (assigned.isEmpty()) {
                FeaturePoint random = points.get(ThreadLocalRandom.current().nextInt(points.size()));
                updated.add(new double[]{random.marks(), random.attendance()});
                continue;
            }

            double meanMarks = assigned.stream().mapToDouble(FeaturePoint::marks).average().orElse(previous.get(cluster)[0]);
            double meanAttendance = assigned.stream().mapToDouble(FeaturePoint::attendance).average().orElse(previous.get(cluster)[1]);
            updated.add(new double[]{meanMarks, meanAttendance});
        }

        return updated;
    }

    private double squaredDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return (dx * dx) + (dy * dy);
    }

    private record FeaturePoint(Student student, double marks, double attendance) {}

    public static class ReassignRequest {
        private String studentId;
        private Integer classNumber;
        private Integer batchNumber;

        public String getStudentId() {
            return studentId;
        }

        public void setStudentId(String studentId) {
            this.studentId = studentId;
        }

        public Integer getClassNumber() {
            return classNumber;
        }

        public void setClassNumber(Integer classNumber) {
            this.classNumber = classNumber;
        }

        public Integer getBatchNumber() {
            return batchNumber;
        }

        public void setBatchNumber(Integer batchNumber) {
            this.batchNumber = batchNumber;
        }
    }

    public static class AiGroupingRequest {
        private String course;
        private String semester;
        private Integer classNumber;
        private Integer clusters;

        public String getCourse() {
            return course;
        }

        public void setCourse(String course) {
            this.course = course;
        }

        public String getSemester() {
            return semester;
        }

        public void setSemester(String semester) {
            this.semester = semester;
        }

        public Integer getClassNumber() {
            return classNumber;
        }

        public void setClassNumber(Integer classNumber) {
            this.classNumber = classNumber;
        }

        public Integer getClusters() {
            return clusters;
        }

        public void setClusters(Integer clusters) {
            this.clusters = clusters;
        }
    }
}
