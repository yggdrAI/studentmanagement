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
import com.sms.model.Teacher;
import com.sms.model.TeacherAssignment;
import com.sms.model.TeacherCredentials;
import com.sms.model.TeacherProfile;
import com.sms.repository.TeacherAssignmentRepository;
import com.sms.repository.TeacherCredentialsRepository;
import com.sms.repository.TeacherProfileRepository;
import com.sms.repository.TeacherRepository;

@RestController
@RequestMapping("/api/teachers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTeacherController {
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
    // Upload teacher profile picture
    @PostMapping(value = "/{id}/profile-picture", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadTeacherProfilePicture(@PathVariable Long id, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        Optional<Teacher> teacherOpt = teacherRepository.findById(id);
        if (teacherOpt.isEmpty()) return ResponseEntity.notFound().build();
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("Profile image file is required");

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
        // Validate uniqueness
        if (teacherRepository.findByEmail(req.teacher.email).isPresent())
            return ResponseEntity.badRequest().body("Email already exists");
        if (teacherRepository.findByEmployeeId(req.teacher.employeeId).isPresent())
            return ResponseEntity.badRequest().body("Employee ID already exists");
        if (credentialsRepository.existsByUsername(req.credentials.username))
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
        teacher.setDateOfJoining(req.teacher.dateOfJoining != null ? java.time.LocalDate.parse(req.teacher.dateOfJoining) : null);
        teacherRepository.save(teacher);

        // Credentials
        TeacherCredentials creds = new TeacherCredentials();
        creds.setTeacher(teacher);
        creds.setUsername(req.credentials.username);
        creds.setPasswordHash(passwordEncoder.encode(req.credentials.password));
        creds.setPasswordResetRequired(Boolean.TRUE.equals(req.credentials.passwordResetRequired));
        credentialsRepository.save(creds);

        // Assignments
        List<TeacherAssignment> assignments = new ArrayList<>();
        for (var a : req.assignments) {
            if (assignmentRepository.existsByTeacherIdAndClassIdAndBatchIdAndSubject(teacher.getId(), a.classId, a.batchId, a.subject))
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

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTeacher(@PathVariable Long id, @RequestBody CreateTeacherRequest req) {
        Optional<Teacher> teacherOpt = teacherRepository.findById(id);
        if (teacherOpt.isEmpty()) return ResponseEntity.notFound().build();
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
        teacher.setDateOfJoining(req.teacher.dateOfJoining != null ? java.time.LocalDate.parse(req.teacher.dateOfJoining) : null);
        teacherRepository.save(teacher);
        // Credentials update (optional)
        // Assignments: replace all
        assignmentRepository.deleteAll(assignmentRepository.findByTeacherId(id));
        List<TeacherAssignment> assignments = new ArrayList<>();
        for (var a : req.assignments) {
            if (assignmentRepository.existsByTeacherIdAndClassIdAndBatchIdAndSubject(teacher.getId(), a.classId, a.batchId, a.subject))
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
    public ResponseEntity<List<Teacher>> getTeachersByClassBatch(@RequestParam Long classId, @RequestParam Long batchId) {
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
}
