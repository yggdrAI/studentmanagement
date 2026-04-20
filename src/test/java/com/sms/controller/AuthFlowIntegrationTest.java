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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.model.OtpVerification;
import com.sms.model.Role;
import com.sms.model.User;
import com.sms.repository.OtpVerificationRepository;
import com.sms.repository.UserRepository;

@SpringBootTest(properties = "app.hierarchy.sync-on-startup=false")
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String STUDENT_USERNAME = "UTEST_STU_1001";
    private static final String STUDENT_EMAIL = "utest.student@univ.edu";
    private static final String STUDENT_PHONE = "9876543210";
    private static final String DEFAULT_PASSWORD = "UTEST_STU_1001";
    private static final String STRONG_PASSWORD = "NewPass@123";

    @BeforeEach
    void setUp() {
        userRepository.deleteByUsername(STUDENT_USERNAME);
        userRepository.findByEmailIgnoreCase(STUDENT_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByPhone(STUDENT_PHONE).ifPresent(userRepository::delete);
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
    void loginShouldAcceptRegisteredEmail() throws Exception {
        createStudentUser(DEFAULT_PASSWORD, true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"" + STUDENT_EMAIL.toUpperCase() + "\"," +
                                "\"password\":\"" + DEFAULT_PASSWORD + "\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void loginShouldAcceptRegisteredPhone() throws Exception {
        createStudentUser(DEFAULT_PASSWORD, true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"" + STUDENT_PHONE + "\"," +
                                "\"password\":\"" + DEFAULT_PASSWORD + "\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("STUDENT"));
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
    void accountShouldLockAfterFiveFailedAttempts() throws Exception {
        createStudentUser(DEFAULT_PASSWORD, false);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{" +
                                    "\"username\":\"" + STUDENT_USERNAME + "\"," +
                                    "\"password\":\"wrong-password\"" +
                                    "}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"" + STUDENT_USERNAME + "\"," +
                                "\"password\":\"" + DEFAULT_PASSWORD + "\"" +
                                "}"))
                .andExpect(status().isLocked());
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
            void otpExpiryShouldRejectVerification() throws Exception {
            createStudentUser(DEFAULT_PASSWORD, false);

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{" +
                        "\"identifier\":\"" + STUDENT_EMAIL + "\"" +
                        "}"))
                .andExpect(status().isOk());

            User user = userRepository.findByUsername(STUDENT_USERNAME).orElseThrow();
            OtpVerification otp = otpVerificationRepository.findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user).orElseThrow();
            otp.setExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));
            otpVerificationRepository.save(otp);

            mockMvc.perform(post("/api/auth/verify-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{" +
                        "\"identifier\":\"" + STUDENT_EMAIL + "\"," +
                        "\"otpCode\":\"" + otp.getOtpCode() + "\"" +
                        "}"))
                .andExpect(status().isUnauthorized());
            }

            @Test
            void forgotPasswordFlowShouldAllowResetAndLogin() throws Exception {
            createStudentUser(DEFAULT_PASSWORD, false);

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{" +
                        "\"identifier\":\"" + STUDENT_PHONE + "\"" +
                        "}"))
                .andExpect(status().isOk());

            User user = userRepository.findByUsername(STUDENT_USERNAME).orElseThrow();
            OtpVerification otp = otpVerificationRepository.findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user).orElseThrow();

            String verifyResponseBody = Objects.requireNonNull(
                mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                            "\"identifier\":\"" + STUDENT_PHONE + "\"," +
                            "\"otpCode\":\"" + otp.getOtpCode() + "\"" +
                            "}"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString()
            );
            String resetToken = objectMapper.readTree(verifyResponseBody).get("resetToken").asText();

            mockMvc.perform(post("/api/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{" +
                        "\"identifier\":\"" + STUDENT_PHONE + "\"," +
                        "\"resetToken\":\"" + resetToken + "\"," +
                        "\"newPassword\":\"" + STRONG_PASSWORD + "\"," +
                        "\"confirmPassword\":\"" + STRONG_PASSWORD + "\"" +
                        "}"))
                .andExpect(status().isOk());

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{" +
                        "\"username\":\"" + STUDENT_EMAIL + "\"," +
                        "\"password\":\"" + STRONG_PASSWORD + "\"" +
                        "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
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
        user.setEmail(STUDENT_EMAIL);
        user.setPhone(STUDENT_PHONE);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(Role.STUDENT);
        user.setTenantId(1L);
        user.setIsActive(true);
        user.setIsFirstLogin(firstLogin);
        userRepository.save(user);
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
