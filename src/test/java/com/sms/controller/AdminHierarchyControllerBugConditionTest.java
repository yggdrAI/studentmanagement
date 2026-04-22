package com.sms.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.model.Student;
import com.sms.repository.StudentRepository;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bug Condition Exploration Test for Bug 1: Unassigned Students Not Shown
 * 
 * This test MUST FAIL on unfixed code - failure confirms the bug exists
 * 
 * Validates: Requirements 2.1
 * Property 1: Bug Condition - Unassigned Students Display Bug
 * 
 * Test implementation details from Bug Condition in design:
 * - Create students without class/batch assignments
 * - Call hierarchy service
 * - Assert they appear under "Unassigned" class group
 * 
 * EXPECTED OUTCOME: Test FAILS (this is correct - it proves the bug exists)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "spring.cache.type=simple"
})
@Import(com.sms.config.TestCacheConfig.class)
public class AdminHierarchyControllerBugConditionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test for bug condition: Students without class/batch assignments should appear under "Unassigned"
     * 
     * Formal Specification from design:
     * FUNCTION isBugCondition1(student)
     *   INPUT: student of type Student
     *   OUTPUT: boolean
     *   
     *   RETURN student.classAssignment IS NULL 
     *          AND student.batchAssignment IS NULL
     *          AND student IS NOT included in hierarchy display
     * END FUNCTION
     * 
     * Expected Behavior: Student included in hierarchy display under "Unassigned" class group
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUnassignedStudentsShouldAppearUnderUnassignedGroup() throws Exception {
        // Create test students without class/batch assignments
        Student unassignedStudent1 = new Student("TEST001", "Unassigned Student 1");
        unassignedStudent1.setCourse("Computer Science");
        unassignedStudent1.setSemester("Semester 1");
        // Note: academicClass, academicBatch, academicProgram are null by default
        // classGroup and batchGroup are also null
        
        Student unassignedStudent2 = new Student("TEST002", "Unassigned Student 2");
        unassignedStudent2.setCourse("Computer Science");
        unassignedStudent2.setSemester("Semester 1");
        
        // Save students to repository
        studentRepository.save(unassignedStudent1);
        studentRepository.save(unassignedStudent2);
        
        // Call the hierarchy endpoint
        MvcResult result = mockMvc.perform(get("/api/admin/students-hierarchy")
                .param("course", "Computer Science")
                .param("semester", "Semester 1"))
                .andExpect(status().isOk())
                .andReturn();
        
        String responseContent = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseContent, Map.class);
        
        // Debug: Print response to understand structure
        System.out.println("DEBUG: Response structure: " + response.keySet());
        
        // Extract classes from response
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> classes = (List<Map<String, Object>>) response.get("classes");
        
        // Debug: Print classes structure
        System.out.println("DEBUG: Number of classes: " + (classes != null ? classes.size() : "null"));
        if (classes != null) {
            for (int i = 0; i < classes.size(); i++) {
                Map<String, Object> classObj = classes.get(i);
                System.out.println("DEBUG: Class " + i + ": " + classObj);
            }
        }
        
        // Extract summary from response
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) response.get("summary");
        System.out.println("DEBUG: Summary: " + summary);
        
        // Assertion 1: The hierarchy should not be empty
        assertNotNull(classes, "Classes list should not be null");
        
        // Assertion 2: There should be at least one class (including "Unassigned")
        assertFalse(classes.isEmpty(), "Classes list should not be empty");
        
        // Assertion 3: Find the "Unassigned" class group
        boolean foundUnassignedClass = false;
        for (Map<String, Object> classObj : classes) {
            String className = (String) classObj.get("label");
            if (className != null && className.contains("Unassigned")) {
                foundUnassignedClass = true;
                
                // Assertion 4: The "Unassigned" class should contain our test students
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> batches = (List<Map<String, Object>>) classObj.get("batches");
                assertNotNull(batches, "Batches list should not be null for Unassigned class");
                
                boolean foundStudents = false;
                for (Map<String, Object> batch : batches) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> students = (List<Map<String, Object>>) batch.get("students");
                    if (students != null) {
                        for (Map<String, Object> student : students) {
                            String studentId = (String) student.get("id");
                            if ("TEST001".equals(studentId) || "TEST002".equals(studentId)) {
                                foundStudents = true;
                                break;
                            }
                        }
                    }
                    if (foundStudents) break;
                }
                
                assertTrue(foundStudents, "Unassigned students should be found in the Unassigned class group");
                break;
            }
        }
        
        // Assertion 5: We must have found the "Unassigned" class group
        assertTrue(foundUnassignedClass, "Should find 'Unassigned' class group in hierarchy");
        
        // Assertion 6: Total students count should include our unassigned students
        Integer totalStudents = (Integer) summary.get("totalStudents");
        assertNotNull(totalStudents, "Total students count should not be null");
        assertTrue(totalStudents >= 2, "Total students should include at least our 2 unassigned students");
    }
    
    /**
     * Alternative test focusing on the exact bug condition: 
     * When students have null class/batch assignments, they should not be filtered out
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    public void testStudentsWithoutClassBatchAssignmentsShouldNotBeFilteredOut() throws Exception {
        // Create a student with null class/batch assignments
        Student student = new Student("UNASSIGNED001", "Test Unassigned Student");
        student.setCourse("Mathematics");
        student.setSemester("Semester 2");
        // academicClass, academicBatch, academicProgram are null
        // classGroup and batchGroup are null
        
        studentRepository.save(student);
        
        // Call hierarchy endpoint
        MvcResult result = mockMvc.perform(get("/api/admin/students-hierarchy")
                .param("course", "Mathematics")
                .param("semester", "Semester 2"))
                .andExpect(status().isOk())
                .andReturn();
        
        String responseContent = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseContent, Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) response.get("summary");
        
        // The bug condition: student should be included in hierarchy
        // On unfixed code, this assertion will likely FAIL because:
        // 1. The student has null class/batch assignments
        // 2. The current implementation may filter them out
        // 3. They won't appear under "Unassigned" class group
        
        Integer totalStudents = (Integer) summary.get("totalStudents");
        assertNotNull(totalStudents, "Total students count should not be null");
        
        // This assertion should FAIL on unfixed code, proving the bug exists
        assertTrue(totalStudents > 0, "Students without class/batch assignments should be included in hierarchy (currently filtered out)");
        
        // Additional check: look for "Unassigned" label in classes
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> classes = (List<Map<String, Object>>) response.get("classes");
        
        boolean hasUnassignedGroup = false;
        if (classes != null) {
            for (Map<String, Object> classObj : classes) {
                String label = (String) classObj.get("label");
                if (label != null && label.contains("Unassigned")) {
                    hasUnassignedGroup = true;
                    break;
                }
            }
        }
        
        // This assertion should also FAIL on unfixed code
        assertTrue(hasUnassignedGroup, "Should have 'Unassigned' class group for students without assignments");
    }
}