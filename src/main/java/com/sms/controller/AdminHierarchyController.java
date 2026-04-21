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
import com.sms.service.StudentService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHierarchyController {

    private static final int BATCH_SIZE = 30;
    private static final int DEFAULT_CLUSTER_COUNT = 4;
    private static final Pattern TRAILING_NUMBER_PATTERN = Pattern.compile("(\\d+)$");

    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentService studentService;
    private final HierarchyCatalogService hierarchyCatalogService;

    public AdminHierarchyController(StudentRepository studentRepository,
                                    StudentProfileRepository studentProfileRepository,
                                    AttendanceRepository attendanceRepository,
                                    StudentService studentService,
                                    HierarchyCatalogService hierarchyCatalogService) {
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.attendanceRepository = attendanceRepository;
        this.studentService = studentService;
        this.hierarchyCatalogService = hierarchyCatalogService;
    }

    @GetMapping("/students-hierarchy")
    public ResponseEntity<Map<String, Object>> getStudentsHierarchy(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) Integer classNumber,
            @RequestParam(required = false) Integer batchNumber,
            @RequestParam(required = false) String performance) {

        List<Student> students = studentRepository.findAllWithHierarchy();
        Map<String, StudentProfile> profileByStudentId = loadProfilesByStudentId(students);
        Map<String, Double> marksMap = studentService.getAverageMarksMap(students);
        Map<String, Double> attendanceMap = loadAttendanceRateMap();

        List<Student> filtered = students.stream()
                .filter(student -> course == null || course.isBlank() || matchesIgnoreCase(student.getCourse(), course))
                .filter(student -> semester == null || semester.isBlank() || matchesIgnoreCase(student.getSemester(), semester))
            .filter(student -> classNumber == null || extractClassNumber(student, profileByStudentId.get(student.getId())) == classNumber)
            .filter(student -> batchNumber == null || extractBatchNumber(student, profileByStudentId.get(student.getId())) == batchNumber)
                .filter(student -> performance == null || performance.isBlank()
                        || performanceBand(marksMap.getOrDefault(student.getId(), 0.0)).equalsIgnoreCase(performance))
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
                student -> extractClassNumber(student, profileByStudentId.get(student.getId())),
                Collectors.groupingBy(student -> extractBatchNumber(student, profileByStudentId.get(student.getId())))
            ));

        List<Map<String, Object>> classes = grouped.keySet().stream()
                .sorted()
                .map(classKey -> buildClassNode(classKey, grouped.get(classKey), marksMap, attendanceMap, profileByStudentId))
                .collect(Collectors.toList());

        int totalBatchCount = grouped.size() * DEFAULT_CLUSTER_COUNT;

        return ResponseEntity.ok(Map.of(
                "summary", Map.of(
                        "totalClasses", grouped.size(),
                        "totalBatches", totalBatchCount,
                        "totalStudents", filtered.size()
                ),
                "classes", classes
        ));
    }

    @GetMapping("/class/{classNumber}/analytics")
    public ResponseEntity<Map<String, Object>> getClassAnalytics(@PathVariable Integer classNumber) {
        List<Student> allStudents = studentRepository.findAllWithHierarchy();
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

        int expectedClassNumber = ((request.getBatchNumber() - 1) / DEFAULT_CLUSTER_COUNT) + 1;
        if (expectedClassNumber != request.getClassNumber()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "batchNumber does not belong to classNumber");
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

        List<Student> students = studentRepository.findAllWithHierarchy().stream()
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
                               Map<String, StudentProfile> profileByStudentId) {
        int startBatchNumber = ((classNumber - 1) * DEFAULT_CLUSTER_COUNT) + 1;
        List<Map<String, Object>> batches = new ArrayList<>();
        for (int batchNumber = startBatchNumber; batchNumber < startBatchNumber + DEFAULT_CLUSTER_COUNT; batchNumber++) {
            batches.add(buildBatchNode(classNumber, batchNumber, classBatches.getOrDefault(batchNumber, List.of()), marksMap, attendanceMap, profileByStudentId));
        }

        List<Student> classStudents = classBatches.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        Map<String, Object> classAnalytics = buildClassAnalytics(classStudents, marksMap, attendanceMap, profileByStudentId);

        Map<String, Object> node = new HashMap<>();
        node.put("id", classNumber);
        node.put("number", classNumber);
        node.put("label", "Class " + classNumber);
        node.put("totalStudents", classStudents.size());
        node.put("totalBatches", DEFAULT_CLUSTER_COUNT);
        node.put("batches", batches);
        node.put("analytics", classAnalytics);
        return node;
    }

    private Map<String, Object> buildBatchNode(Integer classNumber,
                                               Integer batchNumber,
                                               List<Student> batchStudents,
                                               Map<String, Double> marksMap,
                               Map<String, Double> attendanceMap,
                               Map<String, StudentProfile> profileByStudentId) {
        List<Map<String, Object>> students = batchStudents.stream()
                .sorted(Comparator.comparingInt(student -> extractBatchMemberOrder(student, profileByStudentId.get(student.getId()))))
            .map(student -> buildStudentNode(student, marksMap, attendanceMap, profileByStudentId.get(student.getId())))
                .collect(Collectors.toList());

        double avgMarks = average(batchStudents.stream().map(student -> marksMap.getOrDefault(student.getId(), 0.0)).collect(Collectors.toList()));
        double avgAttendance = average(batchStudents.stream().map(student -> attendanceMap.getOrDefault(student.getId(), 75.0)).collect(Collectors.toList()));
        long risk = students.stream().filter(row -> "poor".equals(row.get("performanceBand"))).count();

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("avgMarks", round(avgMarks));
        analytics.put("attendance", round(avgAttendance));
        analytics.put("riskStudents", risk);

        Map<String, Object> node = new HashMap<>();
        node.put("id", batchNumber);
        node.put("number", batchNumber);
        node.put("localNumber", ((batchNumber - 1) % DEFAULT_CLUSTER_COUNT) + 1);
        node.put("label", "Batch " + batchNumber);
        node.put("classNumber", classNumber);
        node.put("studentsCount", students.size());
        node.put("totalStudents", students.size());
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
        if (student != null && student.getAcademicClass() != null && student.getAcademicClass().getClassNumber() != null) {
            return student.getAcademicClass().getClassNumber();
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
        int batchNumber = extractBatchNumber(student, profile);
        return batchNumber <= 0 ? 1 : ((batchNumber - 1) / DEFAULT_CLUSTER_COUNT) + 1;
    }

    private int extractBatchNumber(Student student) {
        return extractBatchNumber(student, null);
    }

    private int extractBatchNumber(Student student, StudentProfile profile) {
        if (student != null && student.getAcademicBatch() != null && student.getAcademicBatch().getBatchNumber() != null) {
            return student.getAcademicBatch().getBatchNumber();
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
        int serial = extractSerialNumber(student, profile);
        return serial <= 0 ? 1 : ((serial - 1) / BATCH_SIZE) + 1;
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
