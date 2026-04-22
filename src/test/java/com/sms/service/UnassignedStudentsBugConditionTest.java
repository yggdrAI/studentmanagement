package com.sms.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.sms.model.Student;
import com.sms.repository.StudentRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
 * - Check if they are included in hierarchy queries
 * 
 * EXPECTED OUTCOME: Test FAILS (this is correct - it proves the bug exists)
 */
@SpringBootTest
@Transactional
public class UnassignedStudentsBugConditionTest {

    @Autowired
    private StudentRepository studentRepository;

    /**
     * Test the bug condition: Students without class/batch assignments should not be filtered out
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
     * This test creates unassigned students and checks if they are included
     * in repository queries that should return all students.
     */
    @Test
    public void testUnassignedStudentsShouldBeIncludedInRepositoryQueries() {
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
        
        // Test 1: Check if findAll() includes unassigned students
        List<Student> allStudents = studentRepository.findAll();
        
        // Debug: Print student details
        System.out.println("DEBUG: Total students in repository: " + allStudents.size());
        for (Student student : allStudents) {
            System.out.println("DEBUG: Student ID: " + student.getId() + 
                             ", Name: " + student.getName() +
                             ", Class: " + student.getAcademicClass() +
                             ", Batch: " + student.getAcademicBatch());
        }
        
        // Assertion 1: The repository should contain our test students
        boolean foundStudent1 = allStudents.stream()
                .anyMatch(s -> "TEST001".equals(s.getId()));
        boolean foundStudent2 = allStudents.stream()
                .anyMatch(s -> "TEST002".equals(s.getId()));
        
        // This assertion should PASS - students should be in repository
        assertTrue(foundStudent1, "Unassigned student TEST001 should be in repository");
        assertTrue(foundStudent2, "Unassigned student TEST002 should be in repository");
        
        // Test 2: Check if findAllWithFullHierarchy() includes unassigned students
        List<Student> allStudentsWithHierarchy = studentRepository.findAllWithFullHierarchy();
        
        // Debug: Print hierarchy student details
        System.out.println("DEBUG: Total students with hierarchy: " + allStudentsWithHierarchy.size());
        for (Student student : allStudentsWithHierarchy) {
            System.out.println("DEBUG: Hierarchy Student ID: " + student.getId() + 
                             ", Name: " + student.getName() +
                             ", Class: " + student.getAcademicClass() +
                             ", Batch: " + student.getAcademicBatch());
        }
        
        // Assertion 2: The hierarchy query should also contain our test students
        boolean foundStudent1InHierarchy = allStudentsWithHierarchy.stream()
                .anyMatch(s -> "TEST001".equals(s.getId()));
        boolean foundStudent2InHierarchy = allStudentsWithHierarchy.stream()
                .anyMatch(s -> "TEST002".equals(s.getId()));
        
        // This is the CRITICAL assertion that should FAIL on unfixed code
        // If the bug exists, unassigned students might be filtered out in hierarchy queries
        assertTrue(foundStudent1InHierarchy, 
            "Unassigned student TEST001 should be included in hierarchy query (currently may be filtered out)");
        assertTrue(foundStudent2InHierarchy, 
            "Unassigned student TEST002 should be included in hierarchy query (currently may be filtered out)");
        
        // Test 3: Verify the bug condition - students have null class/batch assignments
        Student retrievedStudent1 = studentRepository.findById("TEST001").orElse(null);
        Student retrievedStudent2 = studentRepository.findById("TEST002").orElse(null);
        
        assertNotNull(retrievedStudent1, "Should be able to retrieve TEST001");
        assertNotNull(retrievedStudent2, "Should be able to retrieve TEST002");
        
        // Verify they have null class/batch assignments (the bug condition)
        assertNull(retrievedStudent1.getAcademicClass(), 
            "TEST001 should have null academicClass (bug condition)");
        assertNull(retrievedStudent1.getAcademicBatch(), 
            "TEST001 should have null academicBatch (bug condition)");
        assertNull(retrievedStudent1.getAcademicProgram(), 
            "TEST001 should have null academicProgram (bug condition)");
        
        assertNull(retrievedStudent2.getAcademicClass(), 
            "TEST002 should have null academicClass (bug condition)");
        assertNull(retrievedStudent2.getAcademicBatch(), 
            "TEST002 should have null academicBatch (bug condition)");
        assertNull(retrievedStudent2.getAcademicProgram(), 
            "TEST002 should have null academicProgram (bug condition)");
        
        // Summary: This test demonstrates the bug condition
        // If the test FAILS, it proves unassigned students are being filtered out
        // which confirms the bug exists (as expected for bug condition exploration)
    }
    
    /**
     * Test to understand the current behavior: How does the system currently handle unassigned students?
     */
    @Test
    public void testCurrentBehaviorWithUnassignedStudents() {
        // Create an unassigned student
        Student student = new Student("UNASSIGNED_TEST", "Test Unassigned");
        student.setCourse("Mathematics");
        student.setSemester("Semester 2");
        
        studentRepository.save(student);
        
        // Check various repository methods
        long totalCount = studentRepository.count();
        System.out.println("DEBUG: Total student count: " + totalCount);
        
        List<Student> allStudents = studentRepository.findAll();
        System.out.println("DEBUG: findAll() returned " + allStudents.size() + " students");
        
        List<Student> hierarchyStudents = studentRepository.findAllWithFullHierarchy();
        System.out.println("DEBUG: findAllWithFullHierarchy() returned " + hierarchyStudents.size() + " students");
        
        // Check if our student is in each result
        boolean inFindAll = allStudents.stream()
                .anyMatch(s -> "UNASSIGNED_TEST".equals(s.getId()));
        boolean inHierarchy = hierarchyStudents.stream()
                .anyMatch(s -> "UNASSIGNED_TEST".equals(s.getId()));
        
        System.out.println("DEBUG: Student in findAll(): " + inFindAll);
        System.out.println("DEBUG: Student in hierarchy query: " + inHierarchy);
        
        // This test helps us understand the current behavior
        // If inFindAll is true but inHierarchy is false, that's the bug!
        // Students are saved to database but filtered out in hierarchy queries
        
        // Record the observation
        if (inFindAll && !inHierarchy) {
            System.out.println("BUG CONFIRMED: Student exists in repository but is filtered out in hierarchy query!");
            System.out.println("This confirms the bug condition: unassigned students are not shown in hierarchy.");
        } else if (inFindAll && inHierarchy) {
            System.out.println("UNEXPECTED: Student appears in hierarchy query - bug might already be fixed or work differently.");
        }
        
        // Don't assert - just observe and document
        // This is for exploration to understand the bug
    }
}