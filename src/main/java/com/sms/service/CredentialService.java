package com.sms.service;

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
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void adminSetStudentPassword(String studentId, String newPassword, String confirmPassword) {
        validatePasswordPair(newPassword, confirmPassword);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        User user = student.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Student account is not linked to a user: " + studentId);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
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
        userRepository.save(user);
    }

    @Transactional
    public void adminSetTeacherPassword(Long teacherId, String newPassword, String confirmPassword) {
        validatePasswordPair(newPassword, confirmPassword);
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));
        User user = teacher.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Teacher account is not linked to a user: " + teacherId);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
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

        String defaultPassword = String.valueOf(teacherId);
        user.setPassword(passwordEncoder.encode(defaultPassword));
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
}