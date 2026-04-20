package com.sms.controller;

import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.model.Role;
import com.sms.model.Student;
import com.sms.model.StudentProfile;
import com.sms.model.User;
import com.sms.repository.StudentProfileRepository;
import com.sms.repository.StudentRepository;
import com.sms.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String STUDENT_USERNAME = "UTEST_STU_1001";
    private static final String STUDENT_ID_ALIAS = "UTEST_STUDENT_ID_2001";
    private static final String ENROLLMENT_ALIAS = "UTEST_ENROLL_2001";
    private static final String DEFAULT_PASSWORD = "UTEST_STU_1001";
    private static final String STRONG_PASSWORD = "NewPass@123";

    @BeforeEach
    void setUp() {
        userRepository.deleteByUsername(STUDENT_USERNAME);
    }

    @Test
    void loginWithCorrectCredentialsShouldReturnJwtAndFirstLoginFlag() throws Exception {
        createStudentUser(DEFAULT_PASSWORD, true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"" + STUDENT_USERNAME + "\"," +
                                "\"password\":\"" + DEFAULT_PASSWORD + "\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.firstLoginRequired").value(true));
    }

    @Test
    void loginShouldAcceptTrimmedAndCaseInsensitiveUsername() throws Exception {
        createStudentUser(DEFAULT_PASSWORD, true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"  " + STUDENT_USERNAME.toLowerCase() + "  \"," +
                                "\"password\":\"" + DEFAULT_PASSWORD + "\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void loginShouldAcceptEnrollmentNumberAlias() throws Exception {
        createStudentUserWithEnrollmentAlias(DEFAULT_PASSWORD, true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"" + ENROLLMENT_ALIAS.toLowerCase() + "\"," +
                                "\"password\":\"" + DEFAULT_PASSWORD + "\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void formLoginShouldAcceptEnrollmentNumberAliasOnStudentPortal() throws Exception {
        createStudentUserWithEnrollmentAlias(DEFAULT_PASSWORD, true);

        mockMvc.perform(post("/login")
                        .param("username", ENROLLMENT_ALIAS.toLowerCase())
                        .param("password", DEFAULT_PASSWORD)
                        .param("loginPortal", "student"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void loginWithWrongPasswordShouldReturnUnauthorized() throws Exception {
        createStudentUser(DEFAULT_PASSWORD, true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"" + STUDENT_USERNAME + "\"," +
                                "\"password\":\"wrong-password\"" +
                                "}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordShouldDisableFirstLoginAndAllowNewLogin() throws Exception {
        createStudentUser(DEFAULT_PASSWORD, true);

        String token = loginAndGetToken(STUDENT_USERNAME, DEFAULT_PASSWORD);

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{" +
                                "\"currentPassword\":\"" + DEFAULT_PASSWORD + "\"," +
                                "\"newPassword\":\"" + STRONG_PASSWORD + "\"," +
                                "\"confirmPassword\":\"" + STRONG_PASSWORD + "\"" +
                                "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"" + STUDENT_USERNAME + "\"," +
                                "\"password\":\"" + STRONG_PASSWORD + "\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstLoginRequired").value(false));
    }

    @Test
    void studentShouldNotAccessAdminApi() throws Exception {
        createStudentUser(DEFAULT_PASSWORD, false);

        String token = loginAndGetToken(STUDENT_USERNAME, DEFAULT_PASSWORD);

        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void firstLoginShouldBlockStudentApiUntilPasswordChanged() throws Exception {
        createStudentUser(DEFAULT_PASSWORD, true);

        String token = loginAndGetToken(STUDENT_USERNAME, DEFAULT_PASSWORD);

        mockMvc.perform(get("/api/student/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("PASSWORD_CHANGE_REQUIRED"));
    }

    private void createStudentUser(String rawPassword, boolean firstLogin) {
        User user = new User();
        user.setUsername(STUDENT_USERNAME);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(Role.STUDENT);
        user.setTenantId(1L);
        user.setIsActive(true);
        user.setIsFirstLogin(firstLogin);
        userRepository.save(user);
    }

    private void createStudentUserWithEnrollmentAlias(String rawPassword, boolean firstLogin) {
        User user = new User();
        user.setUsername(STUDENT_USERNAME);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(Role.STUDENT);
        user.setTenantId(1L);
        user.setIsActive(true);
        user.setIsFirstLogin(firstLogin);
        user = userRepository.save(user);

        Student student = new Student(STUDENT_ID_ALIAS, "Alias Student");
        student.setUser(user);
        studentRepository.save(student);

        StudentProfile profile = new StudentProfile();
        profile.setStudentId(STUDENT_ID_ALIAS);
        profile.setEnrollmentNumber(ENROLLMENT_ALIAS);
        profile.setFullName("Alias Student");
        profile.setUserId(user.getId());
        studentProfileRepository.save(profile);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String responseBody = Objects.requireNonNull(
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{" +
                                        "\"username\":\"" + username + "\"," +
                                        "\"password\":\"" + password + "\"" +
                                        "}"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        );

        JsonNode json = objectMapper.readTree(responseBody);
        return json.get("token").asText();
    }
}
