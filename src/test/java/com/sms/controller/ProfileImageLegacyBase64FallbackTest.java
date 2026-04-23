package com.sms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.model.Student;
import com.sms.model.StudentProfile;
import com.sms.repository.StudentProfileRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ProfileImageLegacyBase64FallbackTest {

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
    public void moderateProfileImageShouldStoreWithoutStorageLimitError() throws Exception {
        Student student = new Student("LEGACY_IMG_STUDENT", "Legacy Image Student");
        studentRepository.save(student);
        studentRepository.flush();

        StudentProfile profile = new StudentProfile();
        profile.setStudentId(student.getId());
        profile.setFullName(student.getName());
        profile.setProfileImage("/images/default-avatar.png");
        profile.setUpdatedBy("test");
        studentProfileRepository.save(profile);
        studentProfileRepository.flush();

        String body = objectMapper.writeValueAsString(Map.of(
                "profileImage", "data:image/jpeg;base64," + "A".repeat(6000),
                "phone", "9999999999"
        ));

        mockMvc.perform(put("/api/admin/student/{studentId}/profile", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
