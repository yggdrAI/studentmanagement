package com.sms.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sms.dto.teacher.CreateTeacherRequest;
import com.sms.model.Role;
import com.sms.model.Teacher;
import com.sms.model.TeacherAssignment;
import com.sms.model.TeacherCredentials;
import com.sms.model.TeacherProfile;
import com.sms.model.User;
import com.sms.repository.TeacherAssignmentRepository;
import com.sms.repository.TeacherCredentialsRepository;
import com.sms.repository.TeacherProfileRepository;
import com.sms.repository.TeacherRepository;
import com.sms.repository.UserRepository;

@RestController
@RequestMapping("/api/teachers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTeacherController {
    private static final String DEFAULT_TEACHER_USERNAME = "Teacher";
    private static final String DEFAULT_TEACHER_PASSWORD = "1234";
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private TeacherCredentialsRepository credentialsRepository;
    @Autowired
    private TeacherAssignmentRepository assignmentRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private TeacherProfileRepository teacherProfileRepository;
    @Autowired
    private UserRepository userRepository;

    // Upload teacher profile picture
    @PostMapping(value = "/{id}/profile-picture", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadTeacherProfilePicture(@PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        Optional<Teacher> teacherOpt = teacherRepository.findById(id);
        if (teacherOpt.isEmpty())
            return ResponseEntity.notFound().build();
        if (file == null || file.isEmpty())
            return ResponseEntity.badRequest().body("Profile image file is required");

        try {
            String base64 = java.util.Base64.getEncoder().encodeToString(file.getBytes());
            TeacherProfile profile = teacherProfileRepository.findByTeacherId(id).orElseGet(() -> {
                TeacherProfile p = new TeacherProfile();
                p.setTeacherId(id);
                return p;
            });
            profile.setProfileImage(base64);
            profile.setProfilePhotoUrl(null); // Optionally set a URL if using external storage
            teacherProfileRepository.save(profile);
            return ResponseEntity.ok().body("Profile picture uploaded successfully");
        } catch (java.io.IOException ex) {
            return ResponseEntity.status(500).body("Failed to upload profile picture: " + ex.getMessage());
        }
    }

    // Get teacher profile picture (base64)
    @GetMapping("/{id}/profile-picture")
    public ResponseEntity<?> getTeacherProfilePicture(@PathVariable Long id) {
        Optional<TeacherProfile> profileOpt = teacherProfileRepository.findByTeacherId(id);
        if (profileOpt.isEmpty() || profileOpt.get().getProfileImage() == null) {
            return ResponseEntity.notFound().build();
        }
        String base64 = profileOpt.get().getProfileImage();
        byte[] imageBytes = java.util.Base64.getDecoder().decode(base64);
        return ResponseEntity.ok()
                .header("Content-Type", "image/jpeg")
                .body(imageBytes);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listTeachers() {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (Teacher teacher : teacherRepository.findAll()) {
            payload.add(mapTeacherRow(teacher));
        }
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTeacherById(@PathVariable Long id) {
        Optional<Teacher> teacherOpt = teacherRepository.findById(id);
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapTeacherRow(teacherOpt.get()));
    }

    @PostMapping
    public ResponseEntity<?> createTeacher(@RequestBody CreateTeacherRequest req) {
        if (req == null || req.teacher == null) {
            return ResponseEntity.badRequest().body("Teacher payload is required");
        }
        // Validate uniqueness
        if (teacherRepository.findByEmail(req.teacher.email).isPresent())
            return ResponseEntity.badRequest().body("Email already exists");
        if (teacherRepository.findByEmployeeId(req.teacher.employeeId).isPresent())
            return ResponseEntity.badRequest().body("Employee ID already exists");
        String username = buildUniqueTeacherUsername(DEFAULT_TEACHER_USERNAME);
        if (credentialsRepository.existsByUsername(username)
                || userRepository.findByUsernameIgnoreCase(username).isPresent())
            return ResponseEntity.badRequest().body("Username already exists");

        // Create Teacher
        Teacher teacher = new Teacher();
        teacher.setFirstName(req.teacher.firstName);
        teacher.setLastName(req.teacher.lastName);
        teacher.setFullName(req.teacher.fullName);
        teacher.setEmail(req.teacher.email);
        teacher.setPhone(req.teacher.phone);
        teacher.setEmployeeId(req.teacher.employeeId);
        teacher.setDepartment(req.teacher.department);
        teacher.setDesignation(req.teacher.designation);
        teacher.setQualification(req.teacher.qualification);
        teacher.setExperienceYears(req.teacher.experienceYears);
        teacher.setSpecialization(req.teacher.specialization);
        teacher.setStatus(req.teacher.status != null ? req.teacher.status : "ACTIVE");
        teacher.setDateOfJoining(
                req.teacher.dateOfJoining != null ? java.time.LocalDate.parse(req.teacher.dateOfJoining) : null);

        User user = new User();
        user.setUsername(username);
        user.setEmail(req.teacher.email);
        user.setPhone(req.teacher.phone);
        user.setPassword(passwordEncoder.encode(DEFAULT_TEACHER_PASSWORD));
        user.setRole(Role.TEACHER);
        user.setIsFirstLogin(true);
        user.setIsActive(true);
        user.setIsVerifiedEmail(false);
        user.setIsVerifiedPhone(false);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
        teacher.setUser(user);
        teacherRepository.save(teacher);

        // Credentials
        TeacherCredentials creds = new TeacherCredentials();
        creds.setTeacher(teacher);
        creds.setUsername(username);
        creds.setPasswordHash(passwordEncoder.encode(DEFAULT_TEACHER_PASSWORD));
        creds.setPasswordResetRequired(true);
        credentialsRepository.save(creds);

        // Assignments
        List<TeacherAssignment> assignments = new ArrayList<>();
        if (req.assignments != null) {
            for (var a : req.assignments) {
                if (assignmentRepository.existsByTeacherIdAndClassIdAndBatchIdAndSubject(teacher.getId(), a.classId,
                        a.batchId, a.subject))
                    continue; // skip duplicates
                TeacherAssignment ta = new TeacherAssignment();
                ta.setTeacher(teacher);
                ta.setClassId(a.classId);
                ta.setBatchId(a.batchId);
                ta.setSubject(a.subject);
                ta.setIsClassTeacher(Boolean.TRUE.equals(a.isClassTeacher));
                assignments.add(ta);
            }
        }
        assignmentRepository.saveAll(assignments);
        Map<String, Object> response = new HashMap<>();
        response.put("teacher", teacher);
        response.put("username", username);
        response.put("defaultPassword", DEFAULT_TEACHER_PASSWORD);
        response.put("passwordResetRequired", true);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTeacher(@PathVariable Long id, @RequestBody CreateTeacherRequest req) {
        Optional<Teacher> teacherOpt = teacherRepository.findById(id);
        if (teacherOpt.isEmpty())
            return ResponseEntity.notFound().build();
        Teacher teacher = teacherOpt.get();
        // Update fields
        teacher.setFirstName(req.teacher.firstName);
        teacher.setLastName(req.teacher.lastName);
        teacher.setFullName(req.teacher.fullName);
        teacher.setEmail(req.teacher.email);
        teacher.setPhone(req.teacher.phone);
        teacher.setEmployeeId(req.teacher.employeeId);
        teacher.setDepartment(req.teacher.department);
        teacher.setDesignation(req.teacher.designation);
        teacher.setQualification(req.teacher.qualification);
        teacher.setExperienceYears(req.teacher.experienceYears);
        teacher.setSpecialization(req.teacher.specialization);
        teacher.setStatus(req.teacher.status != null ? req.teacher.status : "ACTIVE");
        teacher.setDateOfJoining(
                req.teacher.dateOfJoining != null ? java.time.LocalDate.parse(req.teacher.dateOfJoining) : null);
        teacherRepository.save(teacher);
        // Credentials update (optional)
        // Assignments: replace all
        assignmentRepository.deleteAll(assignmentRepository.findByTeacherId(id));
        List<TeacherAssignment> assignments = new ArrayList<>();
        for (var a : req.assignments) {
            if (assignmentRepository.existsByTeacherIdAndClassIdAndBatchIdAndSubject(teacher.getId(), a.classId,
                    a.batchId, a.subject))
                continue; // skip duplicates
            TeacherAssignment ta = new TeacherAssignment();
            ta.setTeacher(teacher);
            ta.setClassId(a.classId);
            ta.setBatchId(a.batchId);
            ta.setSubject(a.subject);
            ta.setIsClassTeacher(Boolean.TRUE.equals(a.isClassTeacher));
            assignments.add(ta);
        }
        assignmentRepository.saveAll(assignments);
        return ResponseEntity.ok(teacher);
    }

    @GetMapping("/by-class-batch")
    public ResponseEntity<List<Teacher>> getTeachersByClassBatch(@RequestParam Long classId,
            @RequestParam Long batchId) {
        List<TeacherAssignment> assignments = assignmentRepository.findByClassIdAndBatchId(classId, batchId);
        List<Teacher> teachers = new ArrayList<>();
        for (TeacherAssignment a : assignments) {
            teachers.add(a.getTeacher());
        }
        return ResponseEntity.ok(teachers);
    }

    private Map<String, Object> mapTeacherRow(Teacher teacher) {
        TeacherProfile profile = teacherProfileRepository.findByTeacherId(teacher.getId()).orElse(null);
        Map<String, Object> row = new HashMap<>();
        row.put("id", teacher.getId());
        row.put("firstName", teacher.getFirstName());
        row.put("lastName", teacher.getLastName());
        row.put("fullName", teacher.getFullName());
        row.put("name", teacher.getName());
        row.put("email", teacher.getEmail());
        row.put("phone", teacher.getPhone());
        row.put("employeeId", teacher.getEmployeeId());
        row.put("department", teacher.getDepartment());
        row.put("designation", teacher.getDesignation());
        row.put("qualification", teacher.getQualification());
        row.put("experienceYears", teacher.getExperienceYears());
        row.put("specialization", teacher.getSpecialization());
        row.put("status", teacher.getStatus());
        row.put("dateOfJoining", teacher.getDateOfJoining());
        row.put("profileImage", profile != null ? profile.getProfileImage() : null);
        row.put("profilePhotoUrl", profile != null ? profile.getProfilePhotoUrl() : null);
        return row;
    }

    private String buildUniqueTeacherUsername(String fullName) {
        String base = firstNonBlank(fullName, DEFAULT_TEACHER_USERNAME).trim();
        if (base.isBlank()) {
            base = DEFAULT_TEACHER_USERNAME;
        }
        String candidate = base;
        int counter = 1;
        while (userRepository.findByUsernameIgnoreCase(candidate).isPresent()
                || credentialsRepository.existsByUsername(candidate)) {
            candidate = base + counter;
            counter++;
        }
        return candidate;
    }

    private String firstNonBlank(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return fallback == null ? "" : fallback;
    }
}