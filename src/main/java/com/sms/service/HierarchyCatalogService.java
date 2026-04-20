package com.sms.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sms.model.AcademicBatch;
import com.sms.model.AcademicClass;
import com.sms.model.Student;
import com.sms.model.StudentProfile;
import com.sms.repository.AcademicBatchRepository;
import com.sms.repository.AcademicClassRepository;
import com.sms.repository.StudentProfileRepository;
import com.sms.repository.StudentRepository;

@Service
public class HierarchyCatalogService {

    private static final int CLASS_BATCH_COUNT = 4;
    private static final int BATCH_SIZE = 30;
    private static final Pattern TRAILING_NUMBER_PATTERN = Pattern.compile("(\\d+)$");

    private final AcademicClassRepository academicClassRepository;
    private final AcademicBatchRepository academicBatchRepository;
    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;

    @Value("${app.hierarchy.sync-on-startup:true}")
    private boolean syncOnStartup;

    public HierarchyCatalogService(AcademicClassRepository academicClassRepository,
                                   AcademicBatchRepository academicBatchRepository,
                                   StudentRepository studentRepository,
                                   StudentProfileRepository studentProfileRepository) {
        this.academicClassRepository = academicClassRepository;
        this.academicBatchRepository = academicBatchRepository;
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void synchronizeHierarchyCatalog() {
        if (!syncOnStartup) {
            return;
        }

        List<Student> students = studentRepository.findAll();
        if (students.isEmpty()) {
            return;
        }

        Map<String, StudentProfile> profiles = loadProfiles(students);
        int maxClassNumber = 0;
        List<Student> dirtyStudents = new ArrayList<>();

        for (Student student : students) {
            StudentProfile profile = profiles.get(student.getId());
            int classNumber = deriveClassNumber(student, profile);
            int batchNumber = deriveBatchNumber(student, profile);
            AcademicClass academicClass = resolveClass(classNumber);
            AcademicBatch academicBatch = resolveBatch(batchNumber, academicClass);

            student.setAcademicClass(academicClass);
            student.setAcademicBatch(academicBatch);
            student.setClassGroup("Class " + classNumber);
            student.setBatchGroup("Batch " + batchNumber);
            dirtyStudents.add(student);
            maxClassNumber = Math.max(maxClassNumber, classNumber);
        }

        for (int classNumber = 1; classNumber <= maxClassNumber; classNumber++) {
            AcademicClass academicClass = resolveClass(classNumber);
            for (int localBatch = 1; localBatch <= CLASS_BATCH_COUNT; localBatch++) {
                int batchNumber = ((classNumber - 1) * CLASS_BATCH_COUNT) + localBatch;
                resolveBatch(batchNumber, academicClass);
            }
        }

        studentRepository.saveAll(dirtyStudents);
    }

    @Transactional
    public void assignStudent(Student student, int classNumber, int batchNumber) {
        AcademicClass academicClass = resolveClass(classNumber);
        AcademicBatch academicBatch = resolveBatch(batchNumber, academicClass);
        student.setAcademicClass(academicClass);
        student.setAcademicBatch(academicBatch);
        student.setClassGroup("Class " + classNumber);
        student.setBatchGroup("Batch " + batchNumber);
        studentRepository.save(student);
    }

    public AcademicClass resolveClass(int classNumber) {
        return academicClassRepository.findByClassNumber(classNumber)
                .orElseGet(() -> {
                    AcademicClass academicClass = new AcademicClass();
                    academicClass.setClassNumber(classNumber);
                    return academicClassRepository.save(academicClass);
                });
    }

    public AcademicBatch resolveBatch(int batchNumber, AcademicClass academicClass) {
        return academicBatchRepository.findByBatchNumber(batchNumber)
                .orElseGet(() -> {
                    AcademicBatch academicBatch = new AcademicBatch();
                    academicBatch.setBatchNumber(batchNumber);
                    academicBatch.setAcademicClass(academicClass);
                    return academicBatchRepository.save(academicBatch);
                });
    }

    public int deriveClassNumber(Student student, StudentProfile profile) {
        if (student != null && student.getAcademicClass() != null && student.getAcademicClass().getClassNumber() != null) {
            return student.getAcademicClass().getClassNumber();
        }

        Integer profileClassNumber = extractTrailingInteger(profile == null ? null : profile.getFoundationClassroom());
        if (profileClassNumber != null && profileClassNumber > 0) {
            return profileClassNumber;
        }

        Integer classGroupNumber = extractTrailingInteger(student == null ? null : student.getClassGroup());
        if (classGroupNumber != null && classGroupNumber > 0) {
            return classGroupNumber;
        }

        int batchNumber = deriveBatchNumber(student, profile);
        return Math.max(1, ((batchNumber - 1) / CLASS_BATCH_COUNT) + 1);
    }

    public int deriveBatchNumber(Student student, StudentProfile profile) {
        if (student != null && student.getAcademicBatch() != null && student.getAcademicBatch().getBatchNumber() != null) {
            return student.getAcademicBatch().getBatchNumber();
        }

        Integer batchGroupNumber = extractTrailingInteger(student == null ? null : student.getBatchGroup());
        if (batchGroupNumber != null && batchGroupNumber > 0) {
            return batchGroupNumber;
        }

        int serial = extractSerialNumber(student, profile);
        if (serial > 0) {
            return ((serial - 1) / BATCH_SIZE) + 1;
        }

        Integer teamNumber = profile == null ? null : profile.getTeamNumber();
        Integer classNumber = extractTrailingInteger(profile == null ? null : profile.getFoundationClassroom());
        if (teamNumber != null && teamNumber > 0 && classNumber != null && classNumber > 0) {
            return ((classNumber - 1) * CLASS_BATCH_COUNT) + teamNumber;
        }

        return 1;
    }

    public int deriveSerialNumber(Student student, StudentProfile profile) {
        return extractSerialNumber(student, profile);
    }

    private Map<String, StudentProfile> loadProfiles(List<Student> students) {
        List<String> studentIds = students.stream().map(Student::getId).toList();
        Map<String, StudentProfile> result = new HashMap<>();
        studentProfileRepository.findAllById(studentIds).forEach(profile -> result.put(profile.getStudentId(), profile));
        return result;
    }

    private int extractSerialNumber(Student student, StudentProfile profile) {
        String enrollment = profile != null && profile.getEnrollmentNumber() != null && !profile.getEnrollmentNumber().isBlank()
                ? profile.getEnrollmentNumber().trim()
                : student == null ? null : student.getId();

        if (enrollment == null || enrollment.isBlank()) {
            return 0;
        }

        Matcher matcher = TRAILING_NUMBER_PATTERN.matcher(enrollment);
        if (!matcher.find()) {
            return 0;
        }

        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
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
}