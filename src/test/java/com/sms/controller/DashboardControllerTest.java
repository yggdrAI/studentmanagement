package com.sms.controller;

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sms.model.Course;
import com.sms.model.TaskItem;
import com.sms.repository.CourseRepository;
import com.sms.repository.TaskItemRepository;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskItemRepository taskItemRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    @WithMockUser(roles = "STUDENT")
    void shouldRedirectRootToDashboard() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRenderAdminDashboardTemplate() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void shouldRenderStudentProfilePage() throws Exception {
        mockMvc.perform(get("/student/profile"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void shouldReturnDashboardPayload() throws Exception {
        mockMvc.perform(get("/api/student/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("S-1001"))
                .andExpect(jsonPath("$.subjects").isArray())
                .andExpect(jsonPath("$.tasks").isArray());
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void shouldReturnStudentProfilePayload() throws Exception {
        mockMvc.perform(get("/api/student/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("S-1001"))
                .andExpect(jsonPath("$.fullName").value("Aarav Patel"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void shouldCompleteTaskFromApi() throws Exception {
        Long taskId = taskItemRepository.findByCourseIdIn(courseRepository.findAll().stream().map(Course::getId).toList())
            .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/student/task/{taskId}/complete", taskId))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId").value("S-1001"))
            .andExpect(jsonPath("$.tasks").isArray())
            .andExpect(jsonPath("$.overallProgress").isNumber());
    }

        @Test
        @WithMockUser(username = "teacher", roles = "TEACHER")
        void shouldBlockTeacherFromStudentEndpoint() throws Exception {
        TaskItem task = taskItemRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(post("/api/student/task/{taskId}/complete", task.getId()))
            .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "teacher", roles = "TEACHER")
        void shouldCreateSubjectWithTeacherApi() throws Exception {
        mockMvc.perform(post("/api/teacher/subject")
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{" +
                    "\"code\":\"CS450\"," +
                    "\"courseName\":\"Cloud Native Systems\"," +
                    "\"credits\":4" +
                    "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("CS450"));
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldEnrollStudentFromAdminApi() throws Exception {
        Long courseId = courseRepository.findAll().stream().findFirst().orElseThrow().getId();

        mockMvc.perform(post("/api/admin/enroll")
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content("{" +
                    "\"studentId\":\"S-1001\"," +
                    "\"courseId\":" + courseId +
                    "}"))
            .andExpect(status().is4xxClientError());
        }
}
