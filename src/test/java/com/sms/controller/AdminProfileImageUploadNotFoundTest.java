package com.sms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Ensures admin profile image updates don't surface as 500 when the studentId is invalid.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminProfileImageUploadNotFoundTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void missingStudentShouldReturn404Not500() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "profileImage", "data:image/jpeg;base64,THIS_IS_NOT_BASE64!!!"
        ));

        mockMvc.perform(put("/api/admin/student/{studentId}/profile", "DOES_NOT_EXIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}

