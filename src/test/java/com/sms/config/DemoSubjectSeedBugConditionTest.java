package com.sms.config;

import com.sms.model.Course;
import com.sms.model.Teacher;
import com.sms.repository.CourseRepository;
import com.sms.repository.TeacherRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bug Condition Exploration Test: Java Subject Missing After Fresh Install
 *
 * Property 1: Bug Condition - Java Subject Missing After Fresh Install
 *
 * This test MUST FAIL on unfixed code — failure confirms the bug exists.
 * DO NOT attempt to fix the test or the code when it fails.
 *
 * Scoped PBT Approach: fresh install (empty DB) with demo teacher seeded via
 * DemoDataLoader.run(). Asserts that courseRepository.findByTeacherId(demoTeacher.getId())
 * contains a course with courseName = "Java".
 *
 * EXPECTED OUTCOME: Test FAILS (proves the bug exists).
 * Counterexample: findByTeacherId returns [CS301, CS305] — no Java course present.
 *
 * Validates: Requirements 1.1, 1.2
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.cache.type=simple"
})
@Import(TestCacheConfig.class)
public class DemoSubjectSeedBugConditionTest {

    @Autowired
    private DemoDataLoader demoDataLoader;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Property 1: Bug Condition — Java Subject Assigned to Demo Teacher After Startup
     *
     * Formal Specification (from design):
     *   FUNCTION isBugCondition(startupState)
     *     demoTeacher := teacherRepository.findAll()
     *                      .filter(t -> "Dr. Rahul Sharma".equals(t.getName()))
     *                      .findFirst()
     *     IF demoTeacher is absent THEN RETURN false
     *     javaCourse := courseRepository.findByCourseNameIgnoreCase("Java")
     *     IF javaCourse is absent THEN RETURN true
     *     RETURN javaCourse.getTeacher() IS NULL
     *         OR NOT javaCourse.getTeacher().getId().equals(demoTeacher.getId())
     *   END FUNCTION
     *
     * On unfixed code this test FAILS because seedInitialAcademicData() never creates
     * a "Java" course — findByTeacherId returns [CS301, CS305] with no "Java" entry.
     *
     * Validates: Requirements 1.1, 1.2
     */
    @Test
    public void property1_javaSubjectShouldBeAssignedToDemoTeacherAfterFreshInstall() throws Exception {
        // Invoke DemoDataLoader.run() on an empty DB (the @Transactional annotation
        // ensures each test starts with a clean slate within the transaction).
        demoDataLoader.run();

        // Locate the demo teacher seeded by DemoDataLoader
        Optional<Teacher> demoTeacherOpt = teacherRepository.findAll().stream()
                .filter(t -> "Dr. Rahul Sharma".equals(t.getName()))
                .findFirst();

        assertTrue(demoTeacherOpt.isPresent(),
                "Demo teacher 'Dr. Rahul Sharma' should exist after DemoDataLoader.run()");

        Teacher demoTeacher = demoTeacherOpt.get();

        // Retrieve all courses assigned to the demo teacher
        List<Course> teacherCourses = courseRepository.findByTeacherId(demoTeacher.getId());

        // Debug: print what courses are actually present
        System.out.println("DEBUG [BugCondition]: Courses assigned to demo teacher after run():");
        for (Course c : teacherCourses) {
            System.out.println("  - " + c.getCode() + " / " + c.getCourseName());
        }

        // CRITICAL ASSERTION — will FAIL on unfixed code:
        // findByTeacherId returns [CS301 Algorithms, CS305 Distributed Databases] but NO "Java"
        boolean hasJavaCourse = teacherCourses.stream()
                .anyMatch(c -> "Java".equalsIgnoreCase(c.getCourseName()));

        assertTrue(hasJavaCourse,
                "COUNTEREXAMPLE: courseRepository.findByTeacherId(demoTeacher.getId()) returned "
                + teacherCourses.stream().map(Course::getCourseName).toList()
                + " — no 'Java' course present. Bug confirmed: seedInitialAcademicData() never seeds a Java subject.");
    }

    /**
     * Property 1 (endpoint variant): GET /api/teacher/attendance/subjects as the demo teacher
     * should return a non-empty list after a fresh install.
     *
     * On unfixed code this test FAILS because the subjects endpoint returns [] when no
     * "Java" course is assigned to the demo teacher.
     *
     * Validates: Requirements 1.1, 1.2
     */
    @Test
    @WithMockUser(username = "Teacher", roles = "TEACHER")
    public void property1_subjectsEndpointShouldReturnNonEmptyListForDemoTeacher() throws Exception {
        // Invoke DemoDataLoader.run() to simulate fresh install
        demoDataLoader.run();

        // Call GET /api/teacher/attendance/subjects as the demo teacher
        // (WithMockUser uses username "Teacher" which is set by ensureTeacherAliasCredentials)
        MvcResult result = mockMvc.perform(get("/api/teacher/attendance/subjects"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("DEBUG [BugCondition]: /api/teacher/attendance/subjects response: " + responseBody);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subjects = objectMapper.readValue(responseBody, List.class);

        System.out.println("DEBUG [BugCondition]: Number of subjects returned: " + subjects.size());
        for (Map<String, Object> subject : subjects) {
            System.out.println("  - subjectId=" + subject.get("subjectId")
                    + ", subjectName=" + subject.get("subjectName"));
        }

        // CRITICAL ASSERTION — will FAIL on unfixed code:
        // The endpoint returns [] because no "Java" course is seeded
        assertFalse(subjects.isEmpty(),
                "COUNTEREXAMPLE: GET /api/teacher/attendance/subjects returned an empty list. "
                + "Bug confirmed: demo teacher has no subjects after fresh install.");

        // Additionally assert that "Java" is specifically present
        boolean hasJava = subjects.stream()
                .anyMatch(s -> "Java".equalsIgnoreCase((String) s.get("subjectName")));

        assertTrue(hasJava,
                "COUNTEREXAMPLE: subjects list " + subjects.stream().map(s -> s.get("subjectName")).toList()
                + " does not contain 'Java'. Bug confirmed.");
    }
}
