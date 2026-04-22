package com.sms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.model.Role;
import com.sms.model.Student;
import com.sms.model.Teacher;
import com.sms.model.User;
import com.sms.repository.StudentRepository;
import com.sms.repository.TeacherRepository;
import com.sms.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "spring.cache.type=simple"
})
@Import(com.sms.config.TestCacheConfig.class)
public class AcademicPublicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    public void classScopedPublicationShouldBeVisibleToMatchingStudentOnly() throws Exception {
        Student matchingStudent = saveStudent("publish-student-a", "Class 1", "Batch 1", "B.Tech CSE", "Semester 2");
        saveStudent("publish-student-b", "Class 2", "Batch 1", "B.Tech CSE", "Semester 2");

        String body = objectMapper.writeValueAsString(Map.of(
            "category", "TIMETABLE",
            "audience", "BOTH",
            "scope", "CLASS",
            "title", "Updated class timetable",
            "summary", "Class 1 timetable published",
            "classGroup", "Class 1",
            "course", "B.Tech CSE",
            "semester", "Semester 2",
            "payload", Map.of("kind", "weekly", "version", "2026.04")
        ));

        MvcResult adminResult = mockMvc.perform(post("/api/admin/publications")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin.publisher").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn();

        Map<String, Object> adminResponse = readMap(adminResult);
        assertThat(adminResponse.get("count")).isEqualTo(1);

        MvcResult studentResult = mockMvc.perform(get("/api/student/publications")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(matchingStudent.getUser().getUsername()).roles("STUDENT")))
            .andExpect(status().isOk())
            .andReturn();

        List<Map<String, Object>> publications = readList(studentResult);
        assertThat(publications).extracting(item -> item.get("title")).contains("Updated class timetable");

        MvcResult otherStudentResult = mockMvc.perform(get("/api/student/publications")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("publish-student-b-user").roles("STUDENT")))
            .andExpect(status().isOk())
            .andReturn();

        List<Map<String, Object>> otherPublications = readList(otherStudentResult);
        assertThat(otherPublications).extracting(item -> item.get("title")).doesNotContain("Updated class timetable");
    }

    @Test
    public void globalTeacherPublicationShouldBeVisibleToTeacher() throws Exception {
        Teacher teacher = saveTeacher("teacher-publication");

        String body = objectMapper.writeValueAsString(Map.of(
            "category", "EXAM_SCHEDULE",
            "audience", "TEACHER",
            "scope", "GLOBAL",
            "title", "Faculty exam schedule",
            "payload", Map.of("exam", "Mid Term", "slot", "09:00")
        ));

        mockMvc.perform(post("/api/admin/publications")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin.publisher").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());

        MvcResult teacherResult = mockMvc.perform(get("/api/teacher/publications")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(teacher.getUser().getUsername()).roles("TEACHER")))
            .andExpect(status().isOk())
            .andReturn();

        List<Map<String, Object>> publications = readList(teacherResult);
        assertThat(publications).extracting(item -> item.get("title")).contains("Faculty exam schedule");
    }

    @Test
    public void studentScopedBulkPublicationShouldCreateOneRecordPerStudent() throws Exception {
        saveStudent("publish-target-1", "Class 3", "Batch 2", "MBA", "Semester 1");
        saveStudent("publish-target-2", "Class 3", "Batch 2", "MBA", "Semester 1");

        String body = objectMapper.writeValueAsString(Map.of(
            "category", "RESULTS",
            "audience", "STUDENT",
            "scope", "STUDENT",
            "title", "Result published",
            "studentIds", List.of("publish-target-1", "publish-target-2"),
            "payload", Map.of("status", "live")
        ));

        MvcResult result = mockMvc.perform(post("/api/admin/publications")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin.publisher").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn();

        Map<String, Object> response = readMap(result);
        assertThat(response.get("count")).isEqualTo(2);
    }

    private Student saveStudent(String studentId, String classGroup, String batchGroup, String course, String semester) {
        User user = new User();
        user.setUsername(studentId + "-user");
        user.setEmail(studentId + "@example.com");
        user.setPassword("test");
        user.setRole(Role.STUDENT);
        user.setIsActive(true);
        user.setIsFirstLogin(false);
        user = userRepository.save(user);

        Student student = new Student(studentId, "Student " + studentId);
        student.setUser(user);
        student.setEmail(studentId + "@example.com");
        student.setCourse(course);
        student.setSemester(semester);
        student.setClassGroup(classGroup);
        student.setBatchGroup(batchGroup);
        return studentRepository.save(student);
    }

    private Teacher saveTeacher(String prefix) {
        String unique = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername(unique + "-user");
        user.setEmail(unique + "@example.com");
        user.setPassword("test");
        user.setRole(Role.TEACHER);
        user.setIsActive(true);
        user.setIsFirstLogin(false);
        user = userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setFirstName("Teacher");
        teacher.setLastName("One");
        teacher.setFullName("Teacher One");
        teacher.setEmail(unique + "@school.example");
        teacher.setEmployeeId("EMP-" + unique);
        return teacherRepository.save(teacher);
    }

    private Map<String, Object> readMap(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {});
    }

    private List<Map<String, Object>> readList(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Map<String, Object>>>() {});
    }
}
