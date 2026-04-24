package com.sms.service;

import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sms.model.Student;
import com.sms.model.Teacher;
import com.sms.model.User;
import com.sms.repository.StudentRepository;
import com.sms.repository.TeacherRepository;
import com.sms.repository.UserRepository;

@Service
public class CredentialService {

    private static final Pattern UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL = Pattern.compile(".*[^A-Za-z0-9].*");
    private static final String FIXED_ADMIN_USERNAME = "bhavya";
    private static final String DEFAULT_TEACHER_PASSWORD = "1234";

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    public CredentialService(UserRepository userRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        if (isFixedAdminAccount(user)) {
            throw new IllegalArgumentException("Admin credentials are fixed and cannot be changed");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }
        validateStrongPassword(newPassword);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setIsFirstLogin(false);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void adminSetStudentPassword(String studentId, String newPassword, String confirmPassword) {
        validatePasswordPair(newPassword, confirmPassword);
        validateStrongPassword(newPassword);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        User user = student.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Student account is not linked to a user: " + studentId);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setIsFirstLogin(false);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void adminResetStudentPassword(String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        User user = student.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Student account is not linked to a user: " + studentId);
        }

        user.setPassword(passwordEncoder.encode(studentId));
        user.setIsFirstLogin(true);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void adminSetTeacherPassword(Long teacherId, String newPassword, String confirmPassword) {
        validatePasswordPair(newPassword, confirmPassword);
        validateStrongPassword(newPassword);
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));
        User user = teacher.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Teacher account is not linked to a user: " + teacherId);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setIsFirstLogin(false);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void adminResetTeacherPassword(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));
        User user = teacher.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Teacher account is not linked to a user: " + teacherId);
        }

        String defaultPassword = DEFAULT_TEACHER_PASSWORD;
        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setIsFirstLogin(true);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
    }

    private void validatePasswordPair(String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.isBlank() || confirmPassword == null || confirmPassword.isBlank()) {
            throw new IllegalArgumentException("Password fields are required");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }
    }

    private void validateStrongPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new IllegalArgumentException("Password must be between 8 and 128 characters");
        }

        if (!UPPER.matcher(password).matches()
                || !LOWER.matcher(password).matches()
                || !DIGIT.matcher(password).matches()
                || !SPECIAL.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Password must contain uppercase, lowercase, number, and special character");
        }
    }
    
    private boolean isFixedAdminAccount(User user) {
        if (user == null || user.getRole() != com.sms.model.Role.ADMIN) {
            return false;
        }

        String username = user.getUsername();
        return username != null && FIXED_ADMIN_USERNAME.equalsIgnoreCase(username.trim());
    }
}