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
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bug Condition Exploration Test for Bug 2: Profile Picture 500 Error.
 *
 * Expected behavior: invalid/corrupted image payloads should return HTTP 400 (not 500).
 *
 * Note: If this test passes on current code, it likely means the bug has already been fixed
 * for this code path (admin profile update). We'll keep it as a regression test either way.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ProfileImageUploadBugConditionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void invalidBase64ProfileImageShouldReturn400Not500() throws Exception {
        // Arrange: a student exists.
        Student student = new Student("BUG2_TEST_STUDENT", "Bug2 Test Student");
        studentRepository.save(student);
        studentRepository.flush();

        // "data:image/..;base64," prefix is important because the service only processes new uploads
        // when they look like a data URI.
        String invalidDataUri = "data:image/jpeg;base64,THIS_IS_NOT_BASE64!!!";

        String body = objectMapper.writeValueAsString(Map.of(
                "profileImage", invalidDataUri
        ));

        // Act + Assert
        mockMvc.perform(put("/api/admin/student/{studentId}/profile", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}

