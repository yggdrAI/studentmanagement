package com.sms.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.model.StudentImportRow;

@Service
public class StudentFusionService {

    private final ObjectMapper objectMapper;

    public StudentFusionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FusionResult analyze(List<StudentImportRow> rows) {
        List<FusionCluster> clusters = new ArrayList<>();
        Map<String, FusionCluster> exactIndex = new LinkedHashMap<>();
        Map<String, List<FusionCluster>> blockedClusters = new LinkedHashMap<>();

        for (StudentImportRow row : rows) {
            row.setIdentityKey(identityKey(row));
            FusionCluster cluster = null;

            String exactKey = strongKey(row);
            if (exactKey != null) {
                cluster = exactIndex.get(exactKey);
                if (cluster == null) {
                    cluster = new FusionCluster(newClusterId(row));
                    exactIndex.put(exactKey, cluster);
                    clusters.add(cluster);
                }
            }

            if (cluster == null) {
                String blockKey = blockingKey(row);
                List<FusionCluster> candidates = blockedClusters.computeIfAbsent(blockKey, ignored -> new ArrayList<>());
                cluster = bestClusterCandidate(row, candidates);
                if (cluster == null) {
                    cluster = new FusionCluster(newClusterId(row));
                    candidates.add(cluster);
                    clusters.add(cluster);
                }
            }

            cluster.members.add(row);
        }

        List<Map<String, Object>> mergedStudents = new ArrayList<>();
        List<Map<String, Object>> mergeLog = new ArrayList<>();
        List<Map<String, Object>> suggestions = new ArrayList<>();

        for (FusionCluster cluster : clusters) {
            ClusterSummary summary = summarize(cluster);
            mergedStudents.add(summary.preview());
            mergeLog.add(summary.mergeLog());
            suggestions.addAll(summary.suggestions());

            for (StudentImportRow row : cluster.members) {
                row.setMergeGroupKey(cluster.id);
                row.setConfidenceScore(summary.confidence());
            }
        }

        return new FusionResult(
            mergedStudents,
            mergeLog,
            suggestions,
            clusters.stream().map(FusionCluster::toSourceManifest).toList(),
            clusters.size(),
            averageConfidence(mergedStudents),
            toJsonSafe(mergeLog)
        );
    }

    private FusionCluster bestClusterCandidate(StudentImportRow row, List<FusionCluster> candidates) {
        FusionCluster best = null;
        double bestScore = 0.0;

        for (FusionCluster candidate : candidates) {
            double score = compare(row, candidate.representative());
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        if (best != null && bestScore >= 0.72) {
            best.scoreHint = Math.max(best.scoreHint, bestScore);
            return best;
        }

        return null;
    }

    private ClusterSummary summarize(FusionCluster cluster) {
        List<StudentImportRow> members = cluster.members;
        Map<String, List<String>> fieldValues = new LinkedHashMap<>();
        addValues(fieldValues, "fullName", members.stream().map(StudentImportRow::getFullName).toList());
        addValues(fieldValues, "enrollmentNumber", members.stream().map(StudentImportRow::getEnrollmentNumber).toList());
        addValues(fieldValues, "rollNumber", members.stream().map(StudentImportRow::getRollNumber).toList());
        addValues(fieldValues, "email", members.stream().map(StudentImportRow::getEmail).toList());
        addValues(fieldValues, "phone", members.stream().map(StudentImportRow::getPhone).toList());
        addValues(fieldValues, "course", members.stream().map(StudentImportRow::getCourse).toList());
        addValues(fieldValues, "program", members.stream().map(StudentImportRow::getProgram).toList());
        addValues(fieldValues, "school", members.stream().map(StudentImportRow::getSchool).toList());
        addValues(fieldValues, "semester", members.stream().map(StudentImportRow::getSemester).toList());
        addValues(fieldValues, "department", members.stream().map(StudentImportRow::getDepartment).toList());
        addValues(fieldValues, "section", members.stream().map(StudentImportRow::getSection).toList());
        addValues(fieldValues, "house", members.stream().map(StudentImportRow::getHouse).toList());
        addValues(fieldValues, "joiningYear", members.stream().map(StudentImportRow::getJoiningYear).toList());
        addValues(fieldValues, "leavingYear", members.stream().map(StudentImportRow::getLeavingYear).toList());
        addValues(fieldValues, "className", members.stream().map(StudentImportRow::getClassName).toList());
        addValues(fieldValues, "gender", members.stream().map(StudentImportRow::getGender).toList());
        addValues(fieldValues, "address", members.stream().map(StudentImportRow::getAddress).toList());
        addValues(fieldValues, "bloodGroup", members.stream().map(StudentImportRow::getBloodGroup).toList());
        addValues(fieldValues, "guardianName", members.stream().map(StudentImportRow::getGuardianName).toList());

        String fullName = select(fieldValues, "fullName");
        NameParts nameParts = splitName(fullName);

        String enrollment = normalizeEnrollment(select(fieldValues, "enrollmentNumber"));
        String rollNumber = normalizeEnrollment(select(fieldValues, "rollNumber"));
        String program = normalizeProgram(select(fieldValues, "program", "course"));
        String department = normalizeDepartment(select(fieldValues, "department"), program);
        String school = normalizeSchool(select(fieldValues, "school"), department, program);
        String house = select(fieldValues, "house");
        String joiningYear = normalizeYear(select(fieldValues, "joiningYear"));
        String leavingYear = normalizeYear(select(fieldValues, "leavingYear"));
        String className = normalizeClassName(select(fieldValues, "className", "section"));
        String section = normalizeSection(select(fieldValues, "section", "className"));

        String chosenIdentity = enrollment != null ? enrollment : (rollNumber != null ? rollNumber : cluster.id);
        double confidence = scoreConfidence(members, fieldValues, chosenIdentity);

        Map<String, Double> fieldConfidence = new LinkedHashMap<>();
        fieldConfidence.put("fullName", confidenceFor(fieldValues.get("fullName")));
        fieldConfidence.put("enrollmentNumber", confidenceFor(fieldValues.get("enrollmentNumber")));
        fieldConfidence.put("rollNumber", confidenceFor(fieldValues.get("rollNumber")));
        fieldConfidence.put("course", confidenceFor(fieldValues.get("course")));
        fieldConfidence.put("department", confidenceFor(fieldValues.get("department")));
        fieldConfidence.put("section", confidenceFor(fieldValues.get("section")));
        fieldConfidence.put("house", confidenceFor(fieldValues.get("house")));

        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : fieldValues.entrySet()) {
            Set<String> distinct = distinctValues(entry.getValue());
            if (distinct.size() > 1) {
                conflicts.add(Map.of(
                    "field", entry.getKey(),
                    "values", new ArrayList<>(distinct)
                ));
            }
        }

        List<Map<String, Object>> sourceRows = members.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rowId", row.getId());
            item.put("rowIndex", row.getRowIndex());
            item.put("sourceFileName", row.getSourceFileName());
            item.put("confidenceScore", row.getConfidenceScore());
            item.put("fullName", row.getFullName());
            item.put("enrollmentNumber", row.getEnrollmentNumber());
            item.put("rollNumber", row.getRollNumber());
            item.put("course", row.getCourse());
            item.put("department", row.getDepartment());
            item.put("section", row.getSection());
            item.put("house", row.getHouse());
            return item;
        }).toList();

        List<Map<String, Object>> suggestions = new ArrayList<>();
        if (nameParts.lastName == null && sourceRows.size() > 1) {
            suggestions.add(Map.of(
                "clusterId", cluster.id,
                "type", "missing-last-name",
                "message", "Missing last name can be backfilled from another matching file",
                "confidence", confidence
            ));
        }
        if (conflicts.stream().anyMatch(item -> "department".equals(item.get("field")))) {
            suggestions.add(Map.of(
                "clusterId", cluster.id,
                "type", "department-normalization",
                "message", "Department values differ. Prefer the most specific/complete label.",
                "confidence", confidence
            ));
        }

        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("clusterId", cluster.id);
        preview.put("firstName", nameParts.firstName);
        preview.put("middleName", nameParts.middleName);
        preview.put("lastName", nameParts.lastName);
        preview.put("fullName", fullName);
        preview.put("enrollmentNumber", enrollment);
        preview.put("rollNumber", rollNumber);
        preview.put("program", program);
        preview.put("department", department);
        preview.put("school", school);
        preview.put("joiningYear", joiningYear);
        preview.put("leavingYear", leavingYear);
        preview.put("className", className);
        preview.put("section", section);
        preview.put("house", house);
        preview.put("sources", sourceRows.stream().map(row -> row.get("sourceFileName")).filter(v -> v != null).distinct().toList());
        preview.put("sourceRows", sourceRows);
        preview.put("confidenceScore", round(confidence * 100.0));
        preview.put("fieldConfidence", fieldConfidence);
        preview.put("conflicts", conflicts);
        preview.put("identityKey", chosenIdentity);

        Map<String, Object> mergeLog = new LinkedHashMap<>();
        mergeLog.put("clusterId", cluster.id);
        mergeLog.put("sources", preview.get("sources"));
        mergeLog.put("memberCount", members.size());
        mergeLog.put("confidenceScore", round(confidence * 100.0));
        mergeLog.put("selectedFields", preview);
        mergeLog.put("conflicts", conflicts);

        return new ClusterSummary(preview, mergeLog, suggestions, confidence * 100.0);
    }

    private StudentImportRow chooseRepresentative(List<StudentImportRow> members) {
        return members.stream()
            .max(Comparator.comparing((StudentImportRow row) -> confidenceFor(row)).thenComparing(row -> row.getSourceFileName() == null ? "" : row.getSourceFileName()))
            .orElse(members.get(0));
    }

    private double confidenceFor(StudentImportRow row) {
        double score = 0.2;
        if (hasText(row.getEnrollmentNumber())) score += 0.25;
        if (hasText(row.getRollNumber())) score += 0.15;
        if (hasText(row.getFullName())) score += 0.2;
        if (hasText(row.getCourse()) || hasText(row.getProgram())) score += 0.1;
        if (hasText(row.getDepartment())) score += 0.1;
        if (hasText(row.getSection()) || hasText(row.getHouse())) score += 0.1;
        return Math.min(1.0, score);
    }

    private double scoreConfidence(List<StudentImportRow> members, Map<String, List<String>> fieldValues, String chosenIdentity) {
        double base = 0.55;
        if (fieldValues.getOrDefault("enrollmentNumber", List.of()).stream().anyMatch(StudentFusionService::hasText)) {
            base += 0.25;
        }
        if (fieldValues.getOrDefault("fullName", List.of()).stream().filter(StudentFusionService::hasText).distinct().count() == 1) {
            base += 0.1;
        }
        if (fieldValues.getOrDefault("course", List.of()).stream().anyMatch(StudentFusionService::hasText)) {
            base += 0.05;
        }
        if (members.size() > 1) {
            base += 0.05;
        }
        if (chosenIdentity != null && chosenIdentity.startsWith("AUTO-")) {
            base -= 0.08;
        }
        return Math.max(0.0, Math.min(1.0, base));
    }

    private double confidenceFor(List<String> values) {
        Set<String> distinct = distinctValues(values);
        if (distinct.isEmpty()) {
            return 0.0;
        }
        if (distinct.size() == 1) {
            return 1.0;
        }
        return Math.max(0.45, 1.0 - ((distinct.size() - 1) * 0.15));
    }

    private String select(Map<String, List<String>> fieldValues, String field) {
        List<String> values = fieldValues.getOrDefault(field, List.of());
        return select(values);
    }

    private String select(Map<String, List<String>> fieldValues, String primary, String fallback) {
        String value = select(fieldValues.getOrDefault(primary, List.of()));
        if (hasText(value)) {
            return value;
        }
        return select(fieldValues.getOrDefault(fallback, List.of()));
    }

    private String select(List<String> values) {
        return values.stream()
            .filter(StudentFusionService::hasText)
            .map(StudentFusionService::clean)
            .filter(StudentFusionService::hasText)
            .collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()))
            .entrySet().stream()
            .sorted((left, right) -> {
                int compareCount = right.getValue().compareTo(left.getValue());
                if (compareCount != 0) {
                    return compareCount;
                }
                return Integer.compare(right.getKey().length(), left.getKey().length());
            })
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    private void addValues(Map<String, List<String>> target, String key, Collection<String> values) {
        target.put(key, values.stream().map(value -> value == null ? null : clean(value)).filter(StudentFusionService::hasText).toList());
    }

    private FusionCluster bestClusterCandidate(StudentImportRow row, Collection<FusionCluster> candidates) {
        return bestClusterCandidate(row, new ArrayList<>(candidates));
    }

    private String strongKey(StudentImportRow row) {
        if (hasText(row.getEnrollmentNumber())) {
            return "enrollment:" + normalizeIdentity(row.getEnrollmentNumber());
        }
        if (hasText(row.getRollNumber())) {
            return "roll:" + normalizeIdentity(row.getRollNumber());
        }
        if (hasText(row.getEmail())) {
            return "email:" + normalizeIdentity(row.getEmail());
        }
        return null;
    }

    private String blockingKey(StudentImportRow row) {
        String name = normalizeName(row.getFullName());
        String course = normalizeIdentity(firstText(row.getProgram(), row.getCourse()));
        String department = normalizeIdentity(row.getDepartment());
        String year = normalizeYear(firstText(row.getJoiningYear(), extractYear(row.getEnrollmentNumber())));
        String suffix = lastToken(name);
        return String.join("|", suffix, course, department, year);
    }

    private double compare(StudentImportRow left, StudentImportRow right) {
        if (left == null || right == null) {
            return 0.0;
        }

        if (hasText(left.getEnrollmentNumber()) && hasText(right.getEnrollmentNumber())
            && normalizeIdentity(left.getEnrollmentNumber()).equals(normalizeIdentity(right.getEnrollmentNumber()))) {
            return 1.0;
        }

        double name = jaroWinkler(normalizeName(left.getFullName()), normalizeName(right.getFullName()));
        double course = fieldOverlap(firstText(left.getProgram(), left.getCourse()), firstText(right.getProgram(), right.getCourse()));
        double department = fieldOverlap(left.getDepartment(), right.getDepartment());
        double year = fieldOverlap(firstText(left.getJoiningYear(), extractYear(left.getEnrollmentNumber())), firstText(right.getJoiningYear(), extractYear(right.getEnrollmentNumber())));
        double roll = fieldOverlap(left.getRollNumber(), right.getRollNumber());

        double weighted = (name * 0.5) + (course * 0.2) + (department * 0.15) + (year * 0.1) + (roll * 0.05);
        if (hasText(left.getEmail()) && hasText(right.getEmail())
            && normalizeIdentity(left.getEmail()).equals(normalizeIdentity(right.getEmail()))) {
            weighted = Math.max(weighted, 0.92);
        }
        if (hasText(left.getPhone()) && hasText(right.getPhone())
            && normalizeIdentity(left.getPhone()).equals(normalizeIdentity(right.getPhone()))) {
            weighted = Math.max(weighted, 0.9);
        }
        return weighted;
    }

    private double fieldOverlap(String left, String right) {
        if (!hasText(left) || !hasText(right)) {
            return 0.0;
        }
        String normalizedLeft = normalizeIdentity(left);
        String normalizedRight = normalizeIdentity(right);
        if (normalizedLeft.equals(normalizedRight)) {
            return 1.0;
        }
        if (normalizedLeft.contains(normalizedRight) || normalizedRight.contains(normalizedLeft)) {
            return 0.7;
        }
        return jaroWinkler(normalizedLeft, normalizedRight);
    }

    private String identityKey(StudentImportRow row) {
        if (hasText(row.getEnrollmentNumber())) {
            return normalizeIdentity(row.getEnrollmentNumber());
        }
        if (hasText(row.getRollNumber())) {
            return normalizeIdentity(row.getRollNumber());
        }
        String name = normalizeName(row.getFullName());
        String course = normalizeIdentity(firstText(row.getProgram(), row.getCourse()));
        String department = normalizeIdentity(row.getDepartment());
        String year = normalizeYear(firstText(row.getJoiningYear(), extractYear(row.getEnrollmentNumber())));
        return String.join("|", name, course, department, year);
    }

    private String newClusterId(StudentImportRow row) {
        String seed = identityKey(row) + "|" + UUID.randomUUID();
        return "cluster-" + Integer.toHexString(seed.hashCode()).toUpperCase(Locale.ROOT);
    }

    private Set<String> distinctValues(List<String> values) {
        Set<String> distinct = new java.util.LinkedHashSet<>();
        for (String value : values) {
            if (hasText(value)) {
                String cleaned = clean(value);
                if (hasText(cleaned)) {
                    distinct.add(cleaned);
                }
            }
        }
        return distinct;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String normalizeIdentity(String value) {
        if (!hasText(value)) {
            return "";
        }
        String cleaned = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", " ")
            .trim();
        return cleaned.replaceAll("\\s+", " ");
    }

    private String normalizeEnrollment(String value) {
        String cleaned = normalizeIdentity(value);
        return cleaned.replaceAll("\\s+", "");
    }

    private String normalizeName(String value) {
        if (!hasText(value)) {
            return "";
        }
        String cleaned = normalizeIdentity(value);
        if (cleaned.contains(",")) {
            String[] parts = cleaned.split(",", 2);
            cleaned = parts.length == 2 ? parts[1].trim() + " " + parts[0].trim() : cleaned;
        }
        return cleaned.replaceAll("\\s+", " ");
    }

    private String normalizeProgram(String value) {
        if (!hasText(value)) {
            return null;
        }
        String cleaned = clean(value).replaceAll("\\s+", " ");
        if (cleaned.equalsIgnoreCase("cs") || cleaned.equalsIgnoreCase("cse")) {
            return "Computer Science Engineering";
        }
        if (cleaned.equalsIgnoreCase("btech cse") || cleaned.equalsIgnoreCase("b.tech cse")) {
            return "B.Tech CSE";
        }
        return cleaned;
    }

    private String normalizeDepartment(String department, String program) {
        if (hasText(department)) {
            return clean(department);
        }
        if (!hasText(program)) {
            return null;
        }
        String normalized = normalizeIdentity(program);
        if (normalized.contains("computer science")) {
            return "Computer Science";
        }
        if (normalized.contains("management")) {
            return "Management";
        }
        return clean(program);
    }

    private String normalizeSchool(String school, String department, String program) {
        if (hasText(school)) {
            return clean(school);
        }
        String normalizedDepartment = normalizeIdentity(department);
        String normalizedProgram = normalizeIdentity(program);
        if (normalizedDepartment.contains("computer science") || normalizedProgram.contains("cse")) {
            return "School of Engineering";
        }
        if (normalizedDepartment.contains("management") || normalizedProgram.contains("mba")) {
            return "School of Management";
        }
        return null;
    }

    private String normalizeYear(String year) {
        if (!hasText(year)) {
            return null;
        }
        String cleaned = clean(year).replaceAll("[^0-9]", "");
        if (cleaned.length() >= 4) {
            return cleaned.substring(0, 4);
        }
        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalizeClassName(String className, String section) {
        String value = firstText(className, section);
        if (!hasText(value)) {
            return null;
        }
        String cleaned = clean(value);
        if (cleaned.toLowerCase(Locale.ROOT).contains("class")) {
            return cleaned;
        }
        return "Class " + cleaned;
    }

    private String normalizeSection(String section, String className) {
        String value = firstText(section, className);
        if (!hasText(value)) {
            return null;
        }
        return clean(value);
    }

    private String extractYear(String enrollmentNumber) {
        if (!hasText(enrollmentNumber)) {
            return null;
        }
        String digits = enrollmentNumber.replaceAll("\\D+", "");
        if (digits.length() >= 4) {
            return digits.substring(0, 4);
        }
        return null;
    }

    private NameParts splitName(String fullName) {
        if (!hasText(fullName)) {
            return new NameParts(null, null, null);
        }

        String normalized = clean(fullName).replaceAll("\\s+", " ");
        if (normalized.contains(",")) {
            String[] parts = normalized.split(",", 2);
            String last = clean(parts[0]);
            String remaining = parts.length > 1 ? clean(parts[1]) : null;
            if (!hasText(remaining)) {
                return new NameParts(null, null, last);
            }
            List<String> tokens = Arrays.asList(remaining.split("\\s+"));
            String first = tokens.isEmpty() ? remaining : tokens.get(0);
            String middle = tokens.size() > 2 ? String.join(" ", tokens.subList(1, tokens.size())) : (tokens.size() == 2 ? tokens.get(1) : null);
            return new NameParts(first, middle, last);
        }

        List<String> tokens = Arrays.asList(normalized.split("\\s+"));
        if (tokens.size() == 1) {
            return new NameParts(tokens.get(0), null, null);
        }
        if (tokens.size() == 2) {
            return new NameParts(tokens.get(0), null, tokens.get(1));
        }
        return new NameParts(tokens.get(0), String.join(" ", tokens.subList(1, tokens.size() - 1)), tokens.get(tokens.size() - 1));
    }

    private String lastToken(String value) {
        if (!hasText(value)) {
            return "";
        }
        String[] tokens = value.trim().split("\\s+");
        return tokens.length == 0 ? "" : tokens[tokens.length - 1];
    }

    private static String clean(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private String toJsonSafe(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private double averageConfidence(List<Map<String, Object>> mergedStudents) {
        if (mergedStudents.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (Map<String, Object> student : mergedStudents) {
            Object confidence = student.get("confidenceScore");
            if (confidence instanceof Number number) {
                total += number.doubleValue();
            }
        }
        return round(total / mergedStudents.size());
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String normalizeClassName(String className) {
        return normalizeClassName(className, null);
    }

    private String normalizeSection(String section) {
        return normalizeSection(section, null);
    }

    private static double jaroWinkler(String left, String right) {
        if (left == null || right == null) {
            return 0.0;
        }
        if (left.equals(right)) {
            return 1.0;
        }

        int matchDistance = Math.max(left.length(), right.length()) / 2 - 1;
        boolean[] leftMatches = new boolean[left.length()];
        boolean[] rightMatches = new boolean[right.length()];

        int matches = 0;
        for (int i = 0; i < left.length(); i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, right.length());
            for (int j = start; j < end; j++) {
                if (rightMatches[j]) {
                    continue;
                }
                if (left.charAt(i) != right.charAt(j)) {
                    continue;
                }
                leftMatches[i] = true;
                rightMatches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        double transpositions = 0;
        int k = 0;
        for (int i = 0; i < left.length(); i++) {
            if (!leftMatches[i]) {
                continue;
            }
            while (!rightMatches[k]) {
                k++;
            }
            if (left.charAt(i) != right.charAt(k)) {
                transpositions++;
            }
            k++;
        }
        transpositions /= 2.0;

        double m = matches;
        double jaro = ((m / left.length()) + (m / right.length()) + ((m - transpositions) / m)) / 3.0;

        int prefix = 0;
        int maxPrefix = Math.min(4, Math.min(left.length(), right.length()));
        while (prefix < maxPrefix && left.charAt(prefix) == right.charAt(prefix)) {
            prefix++;
        }

        return jaro + (prefix * 0.1 * (1.0 - jaro));
    }

    private record NameParts(String firstName, String middleName, String lastName) {}

    public record FusionResult(List<Map<String, Object>> mergedStudents,
                               List<Map<String, Object>> mergeLog,
                               List<Map<String, Object>> suggestions,
                               List<Map<String, Object>> sources,
                               int clusterCount,
                               double averageConfidence,
                               String mergeLogJson) {}

    private static final class FusionCluster {
        private final String id;
        private final List<StudentImportRow> members = new ArrayList<>();
        private double scoreHint;

        private FusionCluster(String id) {
            this.id = id;
        }

        private StudentImportRow representative() {
            return members.isEmpty() ? null : members.get(0);
        }

        private Map<String, Object> toSourceManifest() {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("clusterId", id);
            item.put("sourceFiles", members.stream().map(StudentImportRow::getSourceFileName).filter(StudentFusionService::hasText).distinct().toList());
            item.put("rowCount", members.size());
            item.put("scoreHint", scoreHint);
            return item;
        }
    }

    private record ClusterSummary(Map<String, Object> preview,
                                  Map<String, Object> mergeLog,
                                  List<Map<String, Object>> suggestions,
                                  double confidence) {}
}