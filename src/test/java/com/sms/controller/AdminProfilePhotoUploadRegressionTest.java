package com.sms.controller;

import java.util.Map;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.model.Student;
import com.sms.model.StudentProfile;
import com.sms.repository.StudentProfileRepository;
import com.sms.repository.StudentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminProfilePhotoUploadRegressionTest {

    private static final String TINY_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/w8AAusB9Yt0qL8AAAAASUVORK5CYII=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void profilePhotoUploadShouldNotFailOnUrlColumnConstraints() throws Exception {
        Student student = new Student("BUG2_PHOTO_STUDENT", "Bug2 Photo Student");
        studentRepository.save(student);
        studentRepository.flush();

        String dataUri = "data:image/png;base64," + TINY_PNG_BASE64;
        String body = objectMapper.writeValueAsString(Map.of("photoBase64", dataUri));

        mockMvc.perform(post("/api/admin/student/{studentId}/photo", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"success\":true")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Profile photo uploaded successfully")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void profilePhotoUploadShouldPreserveOriginalImageBytes() throws Exception {
        Student student = new Student("QUALITY_PHOTO_STUDENT", "Quality Photo Student");
        studentRepository.save(student);
        studentRepository.flush();

        String dataUri = "data:image/png;base64," + TINY_PNG_BASE64;
        String body = objectMapper.writeValueAsString(Map.of("photoBase64", dataUri));

        mockMvc.perform(post("/api/admin/student/{studentId}/photo", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        StudentProfile profile = studentProfileRepository.findById(student.getId()).orElseThrow();
        String storedImage = profile.getProfileImage();
        org.junit.jupiter.api.Assertions.assertTrue(storedImage.startsWith("data:image/png;base64,"));

        String storedBase64 = storedImage.substring(storedImage.indexOf(",") + 1);
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                Base64.getDecoder().decode(TINY_PNG_BASE64),
                Base64.getDecoder().decode(storedBase64));
    }
}
