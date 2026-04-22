package com.sms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.model.Student;
import com.sms.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for Manage Students filters:
 * UI semester dropdown sends "2", but we often persist "Semester 2".
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminHierarchySemesterFilterRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void numericSemesterFilterShouldMatchStoredSemesterLabel() throws Exception {
        Student s = new Student("SEM_FILTER_1", "Semester Filter Student");
        s.setCourse("Bachelor of Technology (Computer Science and Engineering)");
        s.setSemester("Semester 2");
        studentRepository.save(s);
        studentRepository.flush();

        MvcResult result = mockMvc.perform(get("/api/admin/students-hierarchy")
                        .param("semester", "2")
                        .param("includeStudents", "true"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> classes = (List<Map<String, Object>>) response.get("classes");
        assertNotNull(classes, "classes should be present");
        assertTrue(!classes.isEmpty(), "expected at least one class group to be returned for semester=2");
    }
}

