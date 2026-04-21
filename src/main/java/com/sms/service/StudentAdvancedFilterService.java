package com.sms.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sms.dto.student.filter.StudentAdvancedFilterRequest;
import com.sms.dto.student.filter.StudentAdvancedFilterResponse;
import com.sms.dto.student.filter.StudentFilterNode;
import com.sms.model.Attendance;
import com.sms.model.Enrollment;
import com.sms.model.SecurityAudit;
import com.sms.model.Student;
import com.sms.model.StudentProfile;
import com.sms.repository.AttendanceRepository;
import com.sms.repository.EnrollmentRepository;
import com.sms.repository.SecurityAuditRepository;
import com.sms.repository.StudentProfileRepository;
import com.sms.repository.StudentRepository;

@Service
public class StudentAdvancedFilterService {

    private static final Pattern CLASS_PATTERN = Pattern.compile("\\bclass\\s+(\\d+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BATCH_PATTERN = Pattern.compile("\\bbatch\\s+([\\w-]+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEMESTER_PATTERN = Pattern.compile("\\bsemester\\s+(\\d+)\\b", Pattern.CASE_INSENSITIVE);

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "religion", "caste", "castecategory", "disability", "healthstatus", "bloodgroup", "category"
    );

    private static final Set<String> SUPPORTED_FIELDS = Set.of(
        "id", "name", "email", "phone", "course", "semester", "class", "classgroup", "batch", "batchgroup",
        "enrollment", "enrollmentnumber", "gender", "age", "school", "degree", "house", "religion", "caste",
        "castecategory", "placeoforigin", "bloodgroup", "guardianname", "guardiancontact", "attendance",
        "attendancepct", "marks", "averagemarks", "rank", "percentile", "dropoutprobability", "needsintervention",
        "atrisk", "topperformer", "irregularattendancepattern", "aitag", "aitags", "healthstatus"
    );

    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final SecurityAuditRepository securityAuditRepository;

    public StudentAdvancedFilterService(StudentRepository studentRepository,
                                        StudentProfileRepository studentProfileRepository,
                                        EnrollmentRepository enrollmentRepository,
                                        AttendanceRepository attendanceRepository,
                                        SecurityAuditRepository securityAuditRepository) {
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.securityAuditRepository = securityAuditRepository;
    }

    public StudentAdvancedFilterResponse search(StudentAdvancedFilterRequest request, Authentication authentication) {
        StudentAdvancedFilterRequest safeRequest = request == null ? new StudentAdvancedFilterRequest() : request;
        Integer requestPage = safeRequest.getPage();
        Integer requestSize = safeRequest.getSize();
        int page = Math.max(0, requestPage == null ? 0 : requestPage.intValue());
        int size = Math.min(Math.max(1, requestSize == null ? 50 : requestSize.intValue()), 200);

        List<StudentFilterNode> smartFilters = parseSmartQuery(safeRequest.getSmartQuery());
        StudentFilterNode root = mergeFilters(safeRequest.getFilterGroup(), smartFilters);
        validateFilterNode(root, new HashSet<>());

        boolean sensitiveRequested = containsSensitiveFilter(root);
        if (sensitiveRequested && !Boolean.TRUE.equals(safeRequest.getIncludeSensitive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sensitive filters require explicit includeSensitive=true");
        }
        if (sensitiveRequested && !canUseSensitiveFilters(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing permission for sensitive filters");
        }

        List<Student> students = studentRepository.findAll();
        if (students.isEmpty()) {
            StudentAdvancedFilterResponse response = new StudentAdvancedFilterResponse();
            response.setItems(List.of());
            response.setAppliedFilters(List.of());
            response.setSmartSuggestions(List.of());
            response.setInterpretedSmartQuery(String.join("; ", describeNodes(smartFilters)));
            return response;
        }

        Map<String, StudentProfile> profileById = studentProfileRepository.findAllById(
            students.stream().map(Student::getId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(StudentProfile::getStudentId, profile -> profile));

        List<String> studentIds = students.stream().map(Student::getId).toList();
        Map<String, List<Enrollment>> enrollmentByStudentId = enrollmentRepository.findByStudentIdIn(studentIds).stream()
            .filter(enrollment -> enrollment.getStudent() != null && enrollment.getStudent().getId() != null)
            .collect(Collectors.groupingBy(enrollment -> enrollment.getStudent().getId()));

        Map<String, AttendanceStats> attendanceByStudentId = buildAttendanceStats();

        List<StudentSnapshot> snapshots = new ArrayList<>();
        for (Student student : students) {
            StudentProfile profile = profileById.get(student.getId());
            List<Enrollment> enrollments = enrollmentByStudentId.getOrDefault(student.getId(), List.of());
            AttendanceStats attendance = attendanceByStudentId.getOrDefault(student.getId(), AttendanceStats.empty());
            snapshots.add(StudentSnapshot.of(student, profile, enrollments, attendance));
        }

        List<StudentSnapshot> filtered = snapshots.stream()
            .filter(snapshot -> evaluate(root, snapshot))
            .sorted(buildComparator(safeRequest.getSortBy(), safeRequest.getSortDir()))
            .toList();

        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<StudentSnapshot> pageItems = from >= to ? List.of() : filtered.subList(from, to);

        StudentAdvancedFilterResponse response = new StudentAdvancedFilterResponse();
        response.setItems(pageItems.stream().map(this::toRow).toList());
        response.setAppliedFilters(describeNodes(root == null ? List.of() : List.of(root)));
        response.setSmartSuggestions(buildSuggestions(filtered));
        response.setInterpretedSmartQuery(String.join("; ", describeNodes(smartFilters)));
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements((long) filtered.size());
        response.setTotalPages(filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / size));
        response.setHasPrevious(page > 0);
        response.setHasNext(to < filtered.size());

        if (sensitiveRequested) {
            writeSensitiveAudit(authentication, root);
        }

        return response;
    }

    private Map<String, AttendanceStats> buildAttendanceStats() {
        Map<String, AttendanceStats> stats = new HashMap<>();
        List<Attendance> allAttendance = attendanceRepository.findAll();

        for (Attendance attendance : allAttendance) {
            if (attendance.getStudentId() == null) {
                continue;
            }
            AttendanceStats current = stats.computeIfAbsent(attendance.getStudentId(), key -> AttendanceStats.empty());
            current.total++;
            if ("PRESENT".equalsIgnoreCase(attendance.getStatus())) {
                current.present++;
            }
            current.lastStatusWindow.add(attendance.getStatus() == null ? "" : attendance.getStatus().toUpperCase(Locale.ROOT));
            if (current.lastStatusWindow.size() > 20) {
                current.lastStatusWindow.remove(0);
            }
        }

        return stats;
    }

    private Comparator<StudentSnapshot> buildComparator(String sortBy, String sortDir) {
        String normalizedSort = normalize(sortBy);
        boolean desc = "desc".equalsIgnoreCase(sortDir);

        Comparator<StudentSnapshot> comparator = switch (normalizedSort) {
            case "name" -> Comparator.comparing(snapshot -> nullSafe(snapshot.student().getName()), String.CASE_INSENSITIVE_ORDER);
            case "email" -> Comparator.comparing(snapshot -> nullSafe(snapshot.student().getEmail()), String.CASE_INSENSITIVE_ORDER);
            case "course" -> Comparator.comparing(snapshot -> nullSafe(snapshot.student().getCourse()), String.CASE_INSENSITIVE_ORDER);
            case "semester" -> Comparator.comparing(snapshot -> nullSafe(snapshot.student().getSemester()), String.CASE_INSENSITIVE_ORDER);
            case "averagemarks", "marks" -> Comparator.comparingDouble(StudentSnapshot::averageMarks);
            case "attendance", "attendancepct" -> Comparator.comparingDouble(StudentSnapshot::attendancePercent);
            default -> Comparator.comparing(snapshot -> nullSafe(snapshot.student().getId()), String.CASE_INSENSITIVE_ORDER);
        };

        return desc ? comparator.reversed() : comparator;
    }

    private boolean evaluate(StudentFilterNode node, StudentSnapshot snapshot) {
        if (node == null) {
            return true;
        }
        if (node.isGroup()) {
            String logic = normalize(node.getLogic());
            List<StudentFilterNode> children = node.getFilters() == null ? List.of() : node.getFilters();
            if (children.isEmpty()) {
                return true;
            }
            if ("or".equals(logic)) {
                for (StudentFilterNode child : children) {
                    if (evaluate(child, snapshot)) {
                        return true;
                    }
                }
                return false;
            }
            for (StudentFilterNode child : children) {
                if (!evaluate(child, snapshot)) {
                    return false;
                }
            }
            return true;
        }

        Object actual = resolveFieldValue(snapshot, node.getField());
        return applyOperator(actual, normalize(node.getOperator()), node.getValue());
    }

    private Object resolveFieldValue(StudentSnapshot snapshot, String field) {
        String key = normalize(field);
        Student student = snapshot.student();
        StudentProfile profile = snapshot.profile();

        return switch (key) {
            case "id" -> student.getId();
            case "name" -> student.getName();
            case "email" -> firstNonBlank(student.getEmail(), profile == null ? null : profile.getEmail());
            case "phone" -> firstNonBlank(student.getPhone(), profile == null ? null : profile.getPhone());
            case "course", "degree" -> firstNonBlank(student.getCourse(), profile == null ? null : profile.getCourse());
            case "semester" -> firstNonBlank(student.getSemester(), profile == null ? null : profile.getSemester());
            case "class", "classgroup" -> student.getClassGroup();
            case "batch", "batchgroup" -> firstNonBlank(student.getBatchGroup(), student.getEnrollmentYear());
            case "enrollment", "enrollmentnumber" -> profile == null ? student.getId() : firstNonBlank(profile.getEnrollmentNumber(), student.getId());
            case "gender" -> firstNonBlank(student.getGender(), profile == null ? null : profile.getGender());
            case "age" -> snapshot.age();
            case "school" -> profile == null ? null : profile.getCollege();
            case "house" -> profile == null ? null : profile.getHouse();
            case "religion" -> profile == null ? null : profile.getReligion();
            case "caste" -> profile == null ? null : profile.getCaste();
            case "castecategory", "category" -> profile == null ? null : profile.getCasteCategory();
            case "placeoforigin" -> profile == null ? null : profile.getPlaceOfOrigin();
            case "bloodgroup" -> profile == null ? null : profile.getBloodGroup();
            case "guardianname" -> profile == null ? null : profile.getGuardianName();
            case "guardiancontact" -> profile == null ? null : profile.getGuardianPhone();
            case "attendance", "attendancepct" -> snapshot.attendancePercent();
            case "marks", "averagemarks" -> snapshot.averageMarks();
            case "rank" -> snapshot.rankScore();
            case "percentile" -> snapshot.percentile();
            case "dropoutprobability" -> snapshot.dropoutProbability();
            case "needsintervention" -> snapshot.needsIntervention();
            case "atrisk" -> snapshot.atRisk();
            case "topperformer" -> snapshot.topPerformer();
            case "irregularattendancepattern" -> snapshot.irregularAttendancePattern();
            case "healthstatus" -> snapshot.healthStatus();
            case "aitag", "aitags" -> snapshot.aiTags();
            default -> null;
        };
    }

    private boolean applyOperator(Object actual, String operator, Object expected) {
        if ("range".equals(operator)) {
            double actualValue = asDouble(actual);
            if (expected instanceof Map<?, ?> map) {
                double min = map.containsKey("min") ? asDouble(map.get("min")) : Double.NEGATIVE_INFINITY;
                double max = map.containsKey("max") ? asDouble(map.get("max")) : Double.POSITIVE_INFINITY;
                return actualValue >= min && actualValue <= max;
            }
            if (expected instanceof Collection<?> list && list.size() >= 2) {
                List<?> values = new ArrayList<>(list);
                return actualValue >= asDouble(values.get(0)) && actualValue <= asDouble(values.get(1));
            }
            return false;
        }

        if ("in".equals(operator)) {
            if (expected instanceof Collection<?> values) {
                String actualToken = normalize(String.valueOf(actual));
                for (Object value : values) {
                    if (actualToken.equals(normalize(String.valueOf(value)))) {
                        return true;
                    }
                }
                if (actual instanceof Collection<?> actualValues) {
                    Set<String> expectedSet = values.stream().map(String::valueOf).map(this::normalize).collect(Collectors.toSet());
                    for (Object value : actualValues) {
                        if (expectedSet.contains(normalize(String.valueOf(value)))) {
                            return true;
                        }
                    }
                }
                return false;
            }
            return false;
        }

        if ("contains".equals(operator)) {
            if (actual instanceof Collection<?> collection) {
                String expectedToken = normalize(String.valueOf(expected));
                for (Object value : collection) {
                    if (normalize(String.valueOf(value)).contains(expectedToken)) {
                        return true;
                    }
                }
                return false;
            }
            return normalize(String.valueOf(actual)).contains(normalize(String.valueOf(expected)));
        }

        if ("greater".equals(operator)) {
            return asDouble(actual) > asDouble(expected);
        }

        if ("less".equals(operator)) {
            return asDouble(actual) < asDouble(expected);
        }

        if ("equals".equals(operator) || operator.isBlank()) {
            if (actual instanceof Boolean) {
                return asBoolean(actual) == asBoolean(expected);
            }
            return normalize(String.valueOf(actual)).equals(normalize(String.valueOf(expected)));
        }

        return false;
    }

    private List<StudentFilterNode> parseSmartQuery(String smartQuery) {
        if (smartQuery == null || smartQuery.trim().isBlank()) {
            return List.of();
        }

        String query = smartQuery.trim().toLowerCase(Locale.ROOT);
        List<StudentFilterNode> nodes = new ArrayList<>();

        Matcher classMatcher = CLASS_PATTERN.matcher(query);
        if (classMatcher.find()) {
            nodes.add(leaf("class", "contains", "Class " + classMatcher.group(1)));
        }

        Matcher batchMatcher = BATCH_PATTERN.matcher(query);
        if (batchMatcher.find()) {
            nodes.add(leaf("batch", "contains", batchMatcher.group(1)));
        }

        Matcher semesterMatcher = SEMESTER_PATTERN.matcher(query);
        if (semesterMatcher.find()) {
            nodes.add(leaf("semester", "contains", "Semester " + semesterMatcher.group(1)));
        }

        if (query.contains("low attendance") || query.contains("irregular attendance")) {
            nodes.add(leaf("attendance", "less", 60));
        }

        if (query.contains("high marks") || query.contains("top performers") || query.contains("top students")) {
            nodes.add(leaf("averageMarks", "greater", 80));
        }

        if (query.contains("at-risk") || query.contains("at risk")) {
            nodes.add(leaf("atRisk", "equals", true));
        }

        if (query.contains("needs intervention") || query.contains("needs attention")) {
            nodes.add(leaf("needsIntervention", "equals", true));
        }

        if (query.contains("dropout")) {
            nodes.add(leaf("dropoutProbability", "greater", 0.65));
        }

        if (query.contains("high potential")) {
            nodes.add(leaf("aiTag", "contains", "High Potential"));
        }

        return nodes;
    }

    private StudentFilterNode mergeFilters(StudentFilterNode base, List<StudentFilterNode> smartNodes) {
        if (smartNodes == null || smartNodes.isEmpty()) {
            return base;
        }

        StudentFilterNode merged = new StudentFilterNode();
        merged.setLogic("AND");
        List<StudentFilterNode> children = new ArrayList<>();

        if (base != null) {
            children.add(base);
        }
        children.addAll(smartNodes);

        merged.setFilters(children);
        return merged;
    }

    private StudentFilterNode leaf(String field, String operator, Object value) {
        StudentFilterNode node = new StudentFilterNode();
        node.setField(field);
        node.setOperator(operator);
        node.setValue(value);
        return node;
    }

    private void validateFilterNode(StudentFilterNode node, Set<String> dedup) {
        if (node == null) {
            return;
        }

        if (node.isGroup()) {
            String logic = normalize(node.getLogic());
            if (!"and".equals(logic) && !"or".equals(logic)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filter group logic must be AND or OR");
            }
            if (node.getFilters().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty filter group is not allowed");
            }
            for (StudentFilterNode child : node.getFilters()) {
                validateFilterNode(child, dedup);
            }
            return;
        }

        String field = normalize(node.getField());
        if (field.isBlank() || !SUPPORTED_FIELDS.contains(field)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported filter field: " + node.getField());
        }

        String operator = normalize(node.getOperator());
        if (!Set.of("equals", "range", "contains", "greater", "less", "in", "").contains(operator)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported operator: " + node.getOperator());
        }

        if (node.getValue() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty filter value is not allowed for field " + node.getField());
        }

        String signature = field + "|" + operator + "|" + normalize(String.valueOf(node.getValue()));
        if (!dedup.add(signature)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate filter detected for field " + node.getField());
        }

        if ("age".equals(field) || "attendance".equals(field) || "attendancepct".equals(field)
            || "marks".equals(field) || "averagemarks".equals(field)) {
            validateRangeIfNeeded(operator, node.getValue(), field);
        }
    }

    private void validateRangeIfNeeded(String operator, Object value, String field) {
        if (!"range".equals(operator)) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            double min = map.containsKey("min") ? asDouble(map.get("min")) : Double.NEGATIVE_INFINITY;
            double max = map.containsKey("max") ? asDouble(map.get("max")) : Double.POSITIVE_INFINITY;
            if (min > max) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid range for field " + field + ": min cannot exceed max");
            }
        }
    }

    private boolean containsSensitiveFilter(StudentFilterNode node) {
        if (node == null) {
            return false;
        }
        if (node.isGroup()) {
            for (StudentFilterNode child : node.getFilters()) {
                if (containsSensitiveFilter(child)) {
                    return true;
                }
            }
            return false;
        }
        return SENSITIVE_FIELDS.contains(normalize(node.getField()));
    }

    private void writeSensitiveAudit(Authentication authentication, StudentFilterNode root) {
        SecurityAudit audit = new SecurityAudit();
        audit.setStudentId("SYSTEM");
        audit.setSeverityLevel("LOW");
        audit.setViolationType("SENSITIVE_FILTER_ACCESS");
        String actor = authentication == null ? "unknown" : authentication.getName();
        audit.setDescription("Sensitive student filters used by " + actor + ": " + String.join(", ", sensitiveFields(root)));
        securityAuditRepository.save(audit);
    }

    private Set<String> sensitiveFields(StudentFilterNode node) {
        Set<String> fields = new LinkedHashSet<>();
        collectSensitiveFields(node, fields);
        return fields;
    }

    private void collectSensitiveFields(StudentFilterNode node, Set<String> collector) {
        if (node == null) {
            return;
        }
        if (node.isGroup()) {
            for (StudentFilterNode child : node.getFilters()) {
                collectSensitiveFields(child, collector);
            }
            return;
        }
        String field = normalize(node.getField());
        if (SENSITIVE_FIELDS.contains(field)) {
            collector.add(field);
        }
    }

    private boolean canUseSensitiveFilters(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority == null ? "" : authority.getAuthority();
            if ("ROLE_ADMIN".equals(value)
                || "VIEW_SENSITIVE_STUDENT_FILTERS".equals(value)
                || "MANAGE_STUDENTS".equals(value)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> toRow(StudentSnapshot snapshot) {
        Student student = snapshot.student();
        StudentProfile profile = snapshot.profile();
        Map<String, Object> row = new HashMap<>();
        row.put("id", student.getId());
        row.put("name", student.getName());
        row.put("email", firstNonBlank(student.getEmail(), profile == null ? null : profile.getEmail()));
        row.put("phone", firstNonBlank(student.getPhone(), profile == null ? null : profile.getPhone()));
        row.put("course", firstNonBlank(student.getCourse(), profile == null ? null : profile.getCourse()));
        row.put("semester", firstNonBlank(student.getSemester(), profile == null ? null : profile.getSemester()));
        row.put("classGroup", student.getClassGroup());
        row.put("batchGroup", student.getBatchGroup());
        row.put("enrollment", profile == null ? student.getId() : firstNonBlank(profile.getEnrollmentNumber(), student.getId()));
        row.put("averageMarks", snapshot.averageMarks());
        row.put("attendancePercent", snapshot.attendancePercent());
        row.put("dropoutProbability", snapshot.dropoutProbability());
        row.put("aiTags", snapshot.aiTags());
        row.put("healthStatus", snapshot.healthStatus());
        row.put("avatar", buildAvatar(student.getName()));
        return row;
    }

    private String buildAvatar(String name) {
        if (name == null || name.isBlank()) {
            return "ST";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        String first = parts[0].isBlank() ? "S" : parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        String second = parts[1].isBlank() ? "T" : parts[1].substring(0, 1).toUpperCase(Locale.ROOT);
        return first + second;
    }

    private List<String> describeNodes(List<StudentFilterNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        List<String> labels = new ArrayList<>();
        for (StudentFilterNode node : nodes) {
            labels.addAll(describeNodes(node));
        }
        return labels;
    }

    private List<String> describeNodes(StudentFilterNode node) {
        if (node == null) {
            return List.of();
        }
        if (node.isGroup()) {
            List<String> labels = new ArrayList<>();
            for (StudentFilterNode child : node.getFilters()) {
                labels.addAll(describeNodes(child));
            }
            return labels;
        }
        String field = node.getField() == null ? "field" : node.getField();
        String operator = node.getOperator() == null ? "equals" : node.getOperator();
        return List.of(field + " " + operator + " " + String.valueOf(node.getValue()));
    }

    private List<String> buildSuggestions(List<StudentSnapshot> filtered) {
        List<String> suggestions = new ArrayList<>();
        if (filtered.isEmpty()) {
            return suggestions;
        }

        long lowAttendance = filtered.stream().filter(snapshot -> snapshot.attendancePercent() < 50).count();
        long lowMarks = filtered.stream().filter(snapshot -> snapshot.averageMarks() < 50).count();
        long highRisk = filtered.stream().filter(StudentSnapshot::atRisk).count();

        if (lowAttendance > 0) {
            suggestions.add("Also filter by averageMarks < 50 for " + lowAttendance + " low-attendance students");
        }
        if (lowMarks > 0) {
            suggestions.add("Consider adding attendance < 60 for " + lowMarks + " low-score students");
        }
        if (highRisk > 0) {
            suggestions.add("Enable needsIntervention = true to focus on " + highRisk + " high-risk students");
        }

        return suggestions.stream().limit(4).toList();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private double asDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String token = normalize(String.valueOf(value));
        return "true".equals(token) || "1".equals(token) || "yes".equals(token);
    }

    private static final class AttendanceStats {
        private long present;
        private long total;
        private final List<String> lastStatusWindow = new ArrayList<>();

        private static AttendanceStats empty() {
            return new AttendanceStats();
        }

        private double attendancePercent() {
            if (total <= 0) {
                return 0.0;
            }
            return (present * 100.0) / total;
        }

        private boolean irregularPattern() {
            if (lastStatusWindow.size() < 6) {
                return false;
            }
            int flips = 0;
            for (int index = 1; index < lastStatusWindow.size(); index++) {
                if (!Objects.equals(lastStatusWindow.get(index - 1), lastStatusWindow.get(index))) {
                    flips++;
                }
            }
            return flips >= Math.max(3, lastStatusWindow.size() / 3);
        }
    }

    private record StudentSnapshot(
        Student student,
        StudentProfile profile,
        double averageMarks,
        double attendancePercent,
        int age,
        double rankScore,
        double percentile,
        double dropoutProbability,
        boolean atRisk,
        boolean needsIntervention,
        boolean topPerformer,
        boolean irregularAttendancePattern,
        String healthStatus,
        List<String> aiTags
    ) {
        private static StudentSnapshot of(Student student,
                                          StudentProfile profile,
                                          List<Enrollment> enrollments,
                                          AttendanceStats attendanceStats) {
            double averageMarks = enrollments.stream()
                .map(Enrollment::getMarks)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

            double attendancePercent = attendanceStats.attendancePercent();
            int age = computeAge(profile == null ? null : profile.getDob());
            double rankScore = (averageMarks * 0.65) + (attendancePercent * 0.35);
            double percentile = Math.max(0, Math.min(100, rankScore));
            double dropoutProbability = Math.max(0, Math.min(1,
                ((100 - attendancePercent) * 0.0065) + ((60 - averageMarks) * 0.004)
            ));

            boolean atRisk = attendancePercent < 60 || averageMarks < 45 || dropoutProbability >= 0.7;
            boolean topPerformer = averageMarks >= 85 && attendancePercent >= 80;
            boolean needsIntervention = !topPerformer && (attendancePercent < 70 || averageMarks < 55 || dropoutProbability >= 0.55);
            boolean irregularPattern = attendanceStats.irregularPattern();

            List<String> tags = new ArrayList<>();
            if (atRisk) {
                tags.add("High Risk");
            }
            if (needsIntervention && !atRisk) {
                tags.add("Needs Attention");
            }
            if (topPerformer) {
                tags.add("High Potential");
            }
            if (!atRisk && !needsIntervention && !topPerformer) {
                tags.add("Healthy");
            }

            String healthStatus = atRisk ? "at-risk" : (needsIntervention ? "watch" : "fit");

            return new StudentSnapshot(
                student,
                profile,
                averageMarks,
                attendancePercent,
                age,
                rankScore,
                percentile,
                dropoutProbability,
                atRisk,
                needsIntervention,
                topPerformer,
                irregularPattern,
                healthStatus,
                tags
            );
        }

        private static int computeAge(LocalDate dob) {
            if (dob == null) {
                return 0;
            }
            LocalDate now = LocalDate.now();
            int years = now.getYear() - dob.getYear();
            if (dob.plusYears(years).isAfter(now)) {
                years--;
            }
            return Math.max(0, years);
        }
    }
}
