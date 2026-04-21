package com.sms.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import com.sms.model.AcademicBatch;
import com.sms.model.AcademicClass;
import com.sms.model.AcademicProgram;
import com.sms.model.Student;
import com.sms.repository.AcademicBatchRepository;
import com.sms.repository.AcademicClassRepository;
import com.sms.repository.AcademicProgramRepository;
import com.sms.repository.StudentRepository;

/**
 * Core engine for automated student grouping.
 *
 * Parses enrollment numbers (e.g. S25CSEU0001), detects course codes,
 * and assigns students into Program → Class → Batch hierarchy following
 * configurable size rules (default: 120 per class, 30 per batch).
 *
 * This service is idempotent: re-running regeneration will clear previous
 * assignments and recompute cleanly.
 */
@Service
public class StudentGroupingService {

    private static final Logger log = LoggerFactory.getLogger(StudentGroupingService.class);

    /** Maximum students per class. */
    private static final int CLASS_SIZE = 120;
    /** Maximum students per batch (4 batches per class). */
    private static final int BATCH_SIZE = 30;
    /** Batches per class = CLASS_SIZE / BATCH_SIZE. */
    private static final int BATCHES_PER_CLASS = CLASS_SIZE / BATCH_SIZE;

    /**
     * Enrollment pattern: optional year prefix (1-3 chars/digits), course code (2-5 alpha),
     * optional program type indicator (1 char), and trailing serial digits.
     *
     * Examples:
     *   S25CSEU0001 → year=S25, course=CSE, type=U, serial=0001
     *   25BBA0045   → year=25,  course=BBA, type=,  serial=0045
     *   MBA2023001  → fallback serial extraction
     */
    private static final Pattern ENROLLMENT_PATTERN =
            Pattern.compile("^([A-Z]?\\d{2,4})([A-Z]{2,5})([A-Z]?)(\\d{2,6})$", Pattern.CASE_INSENSITIVE);

    /** Fallback: extract trailing digits from any enrollment string. */
    private static final Pattern TRAILING_DIGITS = Pattern.compile("(\\d{2,6})$");

    private final StudentRepository studentRepository;
    private final AcademicProgramRepository programRepository;
    private final AcademicClassRepository classRepository;
    private final AcademicBatchRepository batchRepository;
    private final EntityManager entityManager;

    // Global counter to assign unique global classNumber / batchNumber across all programs
    private int globalClassCounter;
    private int globalBatchCounter;

    public StudentGroupingService(StudentRepository studentRepository,
                                  AcademicProgramRepository programRepository,
                                  AcademicClassRepository classRepository,
                                  AcademicBatchRepository batchRepository,
                                  EntityManager entityManager) {
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.classRepository = classRepository;
        this.batchRepository = batchRepository;
        this.entityManager = entityManager;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Full regeneration pipeline.
     *
     * 1. Load all students
     * 2. Parse enrollment numbers
     * 3. Group by course code
     * 4. Sort each group by serial number
     * 5. Create/update Program, Class, Batch entities
     * 6. Assign students to their position in the hierarchy
     *
     * @return summary map with statistics
     */
    @Transactional
    public Map<String, Object> regenerateAllGroupings() {
        log.info("Starting full student grouping regeneration...");

        List<Student> allStudents = studentRepository.findAll();
        log.info("Loaded {} students for grouping", allStudents.size());

        // Step 1: Parse and group by course code
        Map<String, List<ParsedStudent>> courseGroups = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        int skipped = 0;

        for (Student student : allStudents) {
            String enrollment = student.getId();
            if (enrollment == null || enrollment.isBlank()) {
                errors.add("Student with null/empty ID skipped");
                skipped++;
                continue;
            }

            EnrollmentInfo info = parseEnrollment(enrollment.trim());
            if (info == null) {
                // Fallback: try to use the student's course field
                String courseCode = deriveCourseCode(student);
                int serial = extractTrailingSerial(enrollment.trim());
                if (serial <= 0) {
                    log.warn("Cannot parse enrollment '{}' for student '{}' — skipping", enrollment, student.getName());
                    errors.add("Invalid enrollment: " + enrollment);
                    skipped++;
                    continue;
                }
                info = new EnrollmentInfo("", courseCode, "", serial);
            }

            courseGroups.computeIfAbsent(info.courseCode.toUpperCase(), k -> new ArrayList<>())
                    .add(new ParsedStudent(student, info));
        }

        // Step 2: Bulk-clear all hierarchy FKs in a single SQL UPDATE.
        // This is a @Modifying JPQL query that bypasses Hibernate's write-behind
        // cache and writes directly to the database, so the FK columns are NULL
        // in the DB before we issue any DELETE statements below.
        studentRepository.clearAllHierarchyAssignments();

        // Step 3: Delete existing hierarchy entities (single DELETE per table,
        // no preceding SELECT, no L1-cache interaction).
        batchRepository.deleteAllInBatch();
        classRepository.deleteAllInBatch();
        programRepository.deleteAllInBatch();

        // Step 3b: Evict ALL entities from Hibernate's first-level cache.
        // Without this, the Student objects loaded in Step 1 still hold stale
        // Java references to the now-deleted AcademicClass / AcademicBatch /
        // AcademicProgram entities. When we later call studentRepository.save(),
        // Hibernate would try to cascade or flush those dangling references,
        // causing constraint violations or unexpected re-inserts.
        entityManager.clear();

        // Step 4: Process each course group
        globalClassCounter = 0;
        globalBatchCounter = 0;

        Map<String, Object> programStats = new LinkedHashMap<>();
        int totalAssigned = 0;

        List<String> sortedCourses = new ArrayList<>(courseGroups.keySet());
        sortedCourses.sort(String.CASE_INSENSITIVE_ORDER);

        for (String courseCode : sortedCourses) {
            List<ParsedStudent> group = courseGroups.get(courseCode);

            // Re-fetch each student from the DB so we get a clean managed entity
            // (the entityManager.clear() above detached all previously loaded objects).
            for (int i = 0; i < group.size(); i++) {
                ParsedStudent ps = group.get(i);
                Student fresh = studentRepository.findById(ps.student.getId()).orElse(null);
                if (fresh != null) {
                    group.set(i, new ParsedStudent(fresh, ps.info));
                }
            }

            // Sort by serial number within the course
            group.sort(Comparator.comparingInt(ps -> ps.info.serial));

            // Determine representative enrollment info for program metadata
            EnrollmentInfo representative = group.get(0).info;
            String programType = representative.programType.isBlank() ? "UG" : representative.programType;
            String year = representative.year.isBlank() ? "" : representative.year;

            Map<String, Object> stats = assignCourseGroup(courseCode, programType, year, group);
            programStats.put(courseCode, stats);
            totalAssigned += (int) stats.get("assigned");
        }

        log.info("Grouping complete: {} courses, {} students assigned, {} skipped",
                courseGroups.size(), totalAssigned, skipped);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalStudents", allStudents.size());
        summary.put("totalAssigned", totalAssigned);
        summary.put("totalSkipped", skipped);
        summary.put("totalCourses", courseGroups.size());
        summary.put("programs", programStats);
        summary.put("errors", errors);
        return summary;
    }

    /**
     * Parse an enrollment number into its components.
     */
    public EnrollmentInfo parseEnrollment(String enrollment) {
        if (enrollment == null || enrollment.isBlank()) {
            return null;
        }

        Matcher m = ENROLLMENT_PATTERN.matcher(enrollment.trim().toUpperCase());
        if (!m.matches()) {
            return null;
        }

        String year = m.group(1);
        String course = m.group(2);
        String type = m.group(3) != null ? m.group(3) : "";
        int serial;
        try {
            serial = Integer.parseInt(m.group(4));
        } catch (NumberFormatException e) {
            return null;
        }

        return new EnrollmentInfo(year, course, type, serial);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE PIPELINE METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Assigns all students in a single course group to Program → Class → Batch.
     */
    private Map<String, Object> assignCourseGroup(String courseCode, String programType,
                                                   String year, List<ParsedStudent> students) {
        int totalStudents = students.size();

        // Create (or re-create) the program
        AcademicProgram program = new AcademicProgram();
        program.setCode(courseCode);
        program.setName(resolveProgramName(courseCode));
        program.setProgramType(programType);
        program.setAdmissionYear(year);
        program.setTotalStudents(totalStudents);
        program = programRepository.save(program);

        // Determine class/batch sizing
        int classSize = CLASS_SIZE;
        int batchSize = BATCH_SIZE;

        if (totalStudents < CLASS_SIZE) {
            // Small course: 1 class, dynamic batch size
            classSize = totalStudents;
            if (totalStudents < BATCH_SIZE) {
                batchSize = totalStudents; // single batch
            } else {
                batchSize = (int) Math.ceil((double) totalStudents / BATCHES_PER_CLASS);
            }
        }

        int totalClasses = (int) Math.ceil((double) totalStudents / CLASS_SIZE);
        if (totalClasses == 0) totalClasses = 1;

        int classesCreated = 0;
        int batchesCreated = 0;
        int assigned = 0;

        for (int classIdx = 0; classIdx < totalClasses; classIdx++) {
            int classStart = classIdx * CLASS_SIZE;
            int classEnd = Math.min(classStart + CLASS_SIZE, totalStudents);
            int studentsInThisClass = classEnd - classStart;

            globalClassCounter++;
            int localClassNumber = classIdx + 1;

            AcademicClass clazz = new AcademicClass();
            clazz.setClassNumber(globalClassCounter);
            clazz.setLocalClassNumber(localClassNumber);
            clazz.setAcademicProgram(program);
            clazz.setTotalStudents(studentsInThisClass);
            clazz = classRepository.save(clazz);
            classesCreated++;

            // Determine actual batch size for this class
            int effectiveBatchSize;
            if (studentsInThisClass < BATCH_SIZE) {
                effectiveBatchSize = studentsInThisClass;
            } else if (studentsInThisClass < CLASS_SIZE) {
                effectiveBatchSize = (int) Math.ceil((double) studentsInThisClass / BATCHES_PER_CLASS);
            } else {
                effectiveBatchSize = BATCH_SIZE;
            }

            int batchesInThisClass = effectiveBatchSize > 0
                    ? (int) Math.ceil((double) studentsInThisClass / effectiveBatchSize)
                    : 1;

            for (int batchIdx = 0; batchIdx < batchesInThisClass; batchIdx++) {
                int batchStart = classStart + (batchIdx * effectiveBatchSize);
                int batchEnd = Math.min(batchStart + effectiveBatchSize, classEnd);
                int studentsInThisBatch = batchEnd - batchStart;

                globalBatchCounter++;
                int localBatchNumber = (classIdx * BATCHES_PER_CLASS) + batchIdx + 1;

                AcademicBatch batch = new AcademicBatch();
                batch.setBatchNumber(globalBatchCounter);
                batch.setLocalBatchNumber(localBatchNumber);
                batch.setAcademicClass(clazz);
                batch.setAcademicProgram(program);
                batch.setTotalStudents(studentsInThisBatch);
                batch = batchRepository.save(batch);
                batchesCreated++;

                // Assign students
                for (int i = batchStart; i < batchEnd && i < totalStudents; i++) {
                    Student student = students.get(i).student;
                    student.setAcademicProgram(program);
                    student.setAcademicClass(clazz);
                    student.setAcademicBatch(batch);
                    student.setClassGroup("Class " + localClassNumber);
                    student.setBatchGroup("Batch " + localBatchNumber);
                    studentRepository.save(student);
                    assigned++;
                }
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("courseCode", courseCode);
        stats.put("programName", program.getName());
        stats.put("totalStudents", totalStudents);
        stats.put("assigned", assigned);
        stats.put("classesCreated", classesCreated);
        stats.put("batchesCreated", batchesCreated);
        return stats;
    }


    /**
     * Derive a course code from the student's course field when enrollment parsing fails.
     */
    private String deriveCourseCode(Student student) {
        String course = student.getCourse();
        if (course == null || course.isBlank()) {
            return "UNKNOWN";
        }

        String normalized = course.trim().toUpperCase()
                .replace(".", "")
                .replace(" ", "")
                .replace("-", "");

        // Map common course names to standard codes
        if (normalized.contains("BTECH") || normalized.contains("BACHELOR") && normalized.contains("TECH")) {
            return "CSE"; // Default B.Tech to CSE — could be more specific
        }
        if (normalized.contains("BBA")) return "BBA";
        if (normalized.contains("MBA")) return "MBA";
        if (normalized.contains("BCA")) return "BCA";
        if (normalized.contains("MCA")) return "MCA";
        if (normalized.contains("MTECH")) return "MTECH";
        if (normalized.contains("LAW") || normalized.contains("BALLB") || normalized.contains("LLB")) return "LAW";
        if (normalized.contains("BCOM")) return "BCOM";
        if (normalized.contains("BSC")) return "BSC";
        if (normalized.contains("BA")) return "BA";

        // Use first 3-5 chars as code
        return normalized.length() <= 5 ? normalized : normalized.substring(0, 5);
    }

    /**
     * Extract trailing serial number as a fallback for non-standard enrollment formats.
     */
    private int extractTrailingSerial(String enrollment) {
        Matcher m = TRAILING_DIGITS.matcher(enrollment.trim().toUpperCase());
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Resolve a human-readable program name from a course code.
     */
    private String resolveProgramName(String courseCode) {
        if (courseCode == null) return "Unknown Program";
        return switch (courseCode.toUpperCase()) {
            case "CSE" -> "Computer Science & Engineering";
            case "ECE" -> "Electronics & Communication";
            case "ME", "MECH" -> "Mechanical Engineering";
            case "CE", "CIVIL" -> "Civil Engineering";
            case "EE", "ELEC" -> "Electrical Engineering";
            case "IT" -> "Information Technology";
            case "BBA" -> "Bachelor of Business Administration";
            case "MBA" -> "Master of Business Administration";
            case "BCA" -> "Bachelor of Computer Applications";
            case "MCA" -> "Master of Computer Applications";
            case "MTECH" -> "Master of Technology";
            case "LAW" -> "Law";
            case "BCOM" -> "Bachelor of Commerce";
            case "BSC" -> "Bachelor of Science";
            case "BA" -> "Bachelor of Arts";
            case "BIOTEC", "BIOTECH" -> "Biotechnology";
            default -> courseCode + " Program";
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA RECORDS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Parsed components from an enrollment number.
     */
    public record EnrollmentInfo(String year, String courseCode, String programType, int serial) {}

    /**
     * A student paired with its parsed enrollment info.
     */
    private record ParsedStudent(Student student, EnrollmentInfo info) {}
}
