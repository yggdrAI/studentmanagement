package com.sms.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sms.model.AcademicBatch;
import com.sms.model.AcademicClass;
import com.sms.model.AcademicProgram;
import com.sms.model.Student;
import com.sms.repository.AcademicBatchRepository;
import com.sms.repository.AcademicClassRepository;
import com.sms.repository.AcademicProgramRepository;
import com.sms.repository.StudentRepository;

/**
 * Provides catalogue queries and manual-reassignment operations
 * for the Program → Class → Batch hierarchy.
 */
@Service
public class HierarchyCatalogService {

    private static final int BATCHES_PER_CLASS = 4;

    private final AcademicProgramRepository programRepository;
    private final AcademicClassRepository classRepository;
    private final AcademicBatchRepository batchRepository;
    private final StudentRepository studentRepository;

    public HierarchyCatalogService(AcademicProgramRepository programRepository,
                                   AcademicClassRepository classRepository,
                                   AcademicBatchRepository batchRepository,
                                   StudentRepository studentRepository) {
        this.programRepository = programRepository;
        this.classRepository = classRepository;
        this.batchRepository = batchRepository;
        this.studentRepository = studentRepository;
    }

    // ──────────────────────────── Manual Reassignment ────────────────────────────

    /**
     * Manually reassign a student to a specific class and batch (used by drag-drop in UI).
     */
    @Transactional
    public void assignStudent(Student student, int classNumber, int batchNumber) {
        AcademicClass targetClass = classRepository.findByClassNumber(classNumber).orElse(null);
        AcademicBatch targetBatch = batchRepository.findByBatchNumber(batchNumber).orElse(null);

        if (targetClass != null) {
            student.setAcademicClass(targetClass);
            student.setClassGroup("Class " + classNumber);
        }
        if (targetBatch != null) {
            student.setAcademicBatch(targetBatch);
            student.setBatchGroup("Batch " + batchNumber);
            if (targetBatch.getAcademicProgram() != null) {
                student.setAcademicProgram(targetBatch.getAcademicProgram());
            }
        }

        studentRepository.save(student);
    }

    // ──────────────────────────── Program Tree Queries ────────────────────────────

    /**
     * Returns the full Program → Class → Batch tree with student counts.
     */
    public List<Map<String, Object>> getProgramTree() {
        List<AcademicProgram> programs = programRepository.findAll();
        programs.sort(Comparator.comparing(AcademicProgram::getName, String.CASE_INSENSITIVE_ORDER));

        List<Map<String, Object>> result = new ArrayList<>();
        for (AcademicProgram program : programs) {
            result.add(buildProgramNode(program));
        }
        return result;
    }

    /**
     * Returns lightweight program summaries for dashboard cards.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProgramSummaries() {
        List<AcademicProgram> programs = programRepository.findAll();
        programs.sort(Comparator.comparing(AcademicProgram::getName, String.CASE_INSENSITIVE_ORDER));

        List<Map<String, Object>> result = new ArrayList<>();
        for (AcademicProgram program : programs) {
            Map<String, Object> summary = new HashMap<>();
            summary.put("id", program.getId());
            summary.put("name", program.getName());
            summary.put("code", program.getCode());
            summary.put("programType", program.getProgramType());
            summary.put("admissionYear", program.getAdmissionYear());
            summary.put("totalStudents", program.getTotalStudents() != null ? program.getTotalStudents() : 0);
            summary.put("totalClasses", program.getClasses() != null ? program.getClasses().size() : 0);

            int batchCount = 0;
            if (program.getClasses() != null) {
                for (AcademicClass clazz : program.getClasses()) {
                    batchCount += clazz.getBatches() != null ? clazz.getBatches().size() : 0;
                }
            }
            summary.put("totalBatches", batchCount);
            result.add(summary);
        }
        return result;
    }

    /**
     * Returns students belonging to a specific program.
     */
    public List<Map<String, Object>> getStudentsForProgram(Long programId) {
        List<Student> students = studentRepository.findAllWithHierarchy().stream()
                .filter(s -> s.getAcademicProgram() != null && programId.equals(s.getAcademicProgram().getId()))
                .sorted(Comparator.comparing(Student::getId))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Student student : students) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", student.getId());
            node.put("name", student.getName());
            node.put("enrollmentNumber", student.getId());
            node.put("email", student.getEmail() == null ? "" : student.getEmail());
            node.put("course", student.getCourse());
            node.put("classNumber", student.getAcademicClass() != null ? student.getAcademicClass().getLocalClassNumber() : null);
            node.put("batchNumber", student.getAcademicBatch() != null ? student.getAcademicBatch().getLocalBatchNumber() : null);
            node.put("className", student.getClassGroup());
            node.put("batchName", student.getBatchGroup());
            result.add(node);
        }
        return result;
    }

    // ──────────────────────────── Private Helpers ────────────────────────────

    private Map<String, Object> buildProgramNode(AcademicProgram program) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", program.getId());
        node.put("name", program.getName());
        node.put("code", program.getCode());
        node.put("programType", program.getProgramType());
        node.put("admissionYear", program.getAdmissionYear());
        node.put("totalStudents", program.getTotalStudents() != null ? program.getTotalStudents() : 0);

        List<AcademicClass> classes = classRepository.findByAcademicProgram_IdOrderByLocalClassNumberAsc(program.getId());
        List<Map<String, Object>> classNodes = new ArrayList<>();

        for (AcademicClass clazz : classes) {
            classNodes.add(buildClassNode(clazz));
        }

        node.put("totalClasses", classNodes.size());
        node.put("classes", classNodes);
        return node;
    }

    private Map<String, Object> buildClassNode(AcademicClass clazz) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", clazz.getId());
        node.put("classNumber", clazz.getClassNumber());
        node.put("localClassNumber", clazz.getLocalClassNumber());
        node.put("label", "Class " + clazz.getLocalClassNumber());
        node.put("totalStudents", clazz.getTotalStudents() != null ? clazz.getTotalStudents() : 0);

        List<AcademicBatch> batches = batchRepository.findByAcademicClass_IdOrderByLocalBatchNumberAsc(clazz.getId());
        List<Map<String, Object>> batchNodes = new ArrayList<>();

        for (AcademicBatch batch : batches) {
            batchNodes.add(buildBatchNode(batch));
        }

        node.put("totalBatches", batchNodes.size());
        node.put("batches", batchNodes);
        return node;
    }

    private Map<String, Object> buildBatchNode(AcademicBatch batch) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", batch.getId());
        node.put("batchNumber", batch.getBatchNumber());
        node.put("localBatchNumber", batch.getLocalBatchNumber());
        node.put("label", "Batch " + batch.getLocalBatchNumber());
        node.put("totalStudents", batch.getTotalStudents() != null ? batch.getTotalStudents() : 0);
        node.put("classId", batch.getAcademicClass() != null ? batch.getAcademicClass().getId() : null);

        // Include students in batch
        List<Map<String, Object>> studentNodes = new ArrayList<>();
        if (batch.getStudents() != null) {
            for (Student student : batch.getStudents()) {
                Map<String, Object> sNode = new HashMap<>();
                sNode.put("id", student.getId());
                sNode.put("name", student.getName());
                sNode.put("enrollmentNumber", student.getId());
                sNode.put("email", student.getEmail() == null ? "" : student.getEmail());
                sNode.put("course", student.getCourse());
                studentNodes.add(sNode);
            }
            studentNodes.sort(Comparator.comparing(s -> String.valueOf(s.get("id"))));
        }
        node.put("students", studentNodes);
        return node;
    }
}
