package com.sms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.model.AcademicBatch;
import com.sms.model.AcademicClass;
import com.sms.model.AcademicProgram;
import com.sms.model.Student;
import com.sms.repository.AcademicBatchRepository;
import com.sms.repository.AcademicClassRepository;
import com.sms.repository.AcademicProgramRepository;
import com.sms.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Preservation Property Tests for Bug 1:
 * Assigned Students Hierarchy Preservation (Requirements 3.1).
 *
 * Observation-first: these tests encode the current correct behavior for students
 * who have valid program/class/batch assignments. They should PASS on unfixed code
 * and continue to PASS after we implement the "Unassigned" grouping fix.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.cache.type=simple"
})
@Import(com.sms.config.TestCacheConfig.class)
public class AdminHierarchyControllerPreservationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AcademicProgramRepository programRepository;

    @Autowired
    private AcademicClassRepository classRepository;

    @Autowired
    private AcademicBatchRepository batchRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void property_assignedStudentsAppearUnderTheirClassAndBatch() throws Exception {
        // Deterministic pseudo-random generation (property-style) without extra dependencies.
        Random rnd = new Random(1337L);

        // Run multiple independent scenarios in one test method so we get broader coverage
        // while remaining deterministic and easy to debug.
        for (int scenario = 0; scenario < 12; scenario++) {
            clearHierarchyCache();

            String course = "PRESERVE_COURSE_" + UUID.randomUUID().toString().replace("-", "");
            String semester = "PRESERVE_SEM_" + UUID.randomUUID().toString().replace("-", "");
            String programCode = "PRG" + Math.abs(rnd.nextInt(900_000));

            int classNumber = 100_000 + Math.abs(rnd.nextInt(800_000));
            int localClassNumber = 1 + rnd.nextInt(9);
            int batchNumber = 1_000_000 + Math.abs(rnd.nextInt(8_000_000));
            int localBatchNumber = 1 + rnd.nextInt(4);

            int studentCount = 1 + rnd.nextInt(5);
            List<String> studentIds = new ArrayList<>();

            AcademicProgram program = createProgram(programCode);
            AcademicClass clazz = createClass(program, classNumber, localClassNumber);
            AcademicBatch batch = createBatch(program, clazz, batchNumber, localBatchNumber);

            for (int i = 0; i < studentCount; i++) {
                String id = "PRESERVE_" + scenario + "_" + i + "_" + Math.abs(rnd.nextInt(1_000_000));
                studentIds.add(id);
                createAssignedStudent(id, "Preserve Student " + id, course, semester, program, clazz, batch);
            }

            // Make sure everything is written before the controller reads it.
            batchRepository.flush();
            classRepository.flush();
            programRepository.flush();
            studentRepository.flush();

            MvcResult result = mockMvc.perform(get("/api/admin/students-hierarchy")
                            .param("course", course)
                            .param("semester", semester)
                            .param("includeStudents", "true"))
                    .andExpect(status().isOk())
                    .andReturn();

            Map<String, Object> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
            assertNotNull(response.get("summary"), "summary should be present");
            assertNotNull(response.get("classes"), "classes should be present");

            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) response.get("summary");
            int totalStudents = ((Number) summary.get("totalStudents")).intValue();
            assertEquals(studentCount, totalStudents,
                    "Preservation: assigned-only dataset should report exactly the inserted student count");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> classes = (List<Map<String, Object>>) response.get("classes");

            Map<String, Object> classNode = findNodeById(classes, classNumber, "class");
            String classLabel = String.valueOf(classNode.get("label"));
            assertTrue(classLabel.contains("Class " + localClassNumber),
                    "Preservation: class label should contain local class number");
            assertTrue(classLabel.contains(programCode),
                    "Preservation: class label should include program code when program is present");
            assertFalse(classLabel.toLowerCase().contains("unassigned"),
                    "Preservation: assigned-only dataset should not introduce an 'Unassigned' label");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> batches = (List<Map<String, Object>>) classNode.get("batches");
            Map<String, Object> batchNode = findNodeById(batches, batchNumber, "batch");
            String batchLabel = String.valueOf(batchNode.get("label"));
            assertTrue(batchLabel.contains("Batch " + localBatchNumber),
                    "Preservation: batch label should contain local batch number");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> students = (List<Map<String, Object>>) batchNode.get("students");
            assertNotNull(students, "students list should be present when includeStudents=true");

            for (String expectedId : studentIds) {
                boolean found = students.stream().anyMatch(s -> expectedId.equals(String.valueOf(s.get("id"))));
                assertTrue(found, "Preservation: expected assigned student " + expectedId + " should be present in batch students list");
            }
        }
    }

    private void clearHierarchyCache() {
        if (cacheManager == null) return;
        Cache cache = cacheManager.getCache("hierarchyCache");
        if (cache != null) {
            cache.clear();
        }
    }

    private AcademicProgram createProgram(String programCode) {
        AcademicProgram program = new AcademicProgram();
        program.setCode(programCode);
        program.setName("Program " + programCode);
        program.setProgramType("UG");
        program.setAdmissionYear("2026");
        return programRepository.save(program);
    }

    private AcademicClass createClass(AcademicProgram program, int classNumber, int localClassNumber) {
        AcademicClass clazz = new AcademicClass();
        clazz.setAcademicProgram(program);
        clazz.setClassNumber(classNumber);
        clazz.setLocalClassNumber(localClassNumber);
        clazz.setTotalStudents(0);
        return classRepository.save(clazz);
    }

    private AcademicBatch createBatch(AcademicProgram program, AcademicClass clazz, int batchNumber, int localBatchNumber) {
        AcademicBatch batch = new AcademicBatch();
        batch.setAcademicProgram(program);
        batch.setAcademicClass(clazz);
        batch.setBatchNumber(batchNumber);
        batch.setLocalBatchNumber(localBatchNumber);
        batch.setTotalStudents(0);
        return batchRepository.save(batch);
    }

    private Student createAssignedStudent(String id,
                                          String name,
                                          String course,
                                          String semester,
                                          AcademicProgram program,
                                          AcademicClass clazz,
                                          AcademicBatch batch) {
        Student s = new Student(id, name);
        s.setCourse(course);
        s.setSemester(semester);
        s.setAcademicProgram(program);
        s.setAcademicClass(clazz);
        s.setAcademicBatch(batch);
        s.setClassGroup("Class " + clazz.getLocalClassNumber());
        s.setBatchGroup("Batch " + batch.getLocalBatchNumber());
        return studentRepository.save(s);
    }

    private Map<String, Object> findNodeById(List<Map<String, Object>> nodes, int expectedId, String nodeType) {
        assertNotNull(nodes, nodeType + " list should not be null");
        return nodes.stream()
                .filter(node -> {
                    Object idObj = node.get("id");
                    if (!(idObj instanceof Number)) return false;
                    return ((Number) idObj).intValue() == expectedId;
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected to find " + nodeType + " node with id=" + expectedId));
    }
}

