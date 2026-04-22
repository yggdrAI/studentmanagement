package com.sms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.model.Student;
import com.sms.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Preservation Tests for Bug 2:
 * Valid Image Upload Preservation (Requirements 3.2).
 *
 * Ensures that a small valid data-URI image can be stored via the admin profile update API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ProfileImageUploadPreservationTest {

    // 1x1 transparent-ish PNG (tiny).
    private static final String TINY_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/w8AAusB9Yt0qL8AAAAASUVORK5CYII=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void validProfileImageDataUriShouldSucceed() throws Exception {
        Student student = new Student("BUG2_VALID_STUDENT", "Bug2 Valid Student");
        studentRepository.save(student);
        studentRepository.flush();

        String dataUri = "data:image/png;base64," + TINY_PNG_BASE64;
        String body = objectMapper.writeValueAsString(Map.of("profileImage", dataUri));

        MvcResult result = mockMvc.perform(put("/api/admin/student/{studentId}/profile", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> payload = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Object profileImage = payload.get("profileImage");
        assertNotNull(profileImage, "profileImage should be present in response payload");
        assertTrue(String.valueOf(profileImage).startsWith("data:image/"),
                "profileImage should remain a data URI for valid uploads");
    }
}

