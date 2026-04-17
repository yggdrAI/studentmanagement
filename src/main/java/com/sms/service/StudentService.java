package com.sms.service;

import com.sms.dto.student.StudentProfileDTO;
import com.sms.model.Enrollment;
import com.sms.model.Student;
import com.sms.model.Role;
import com.sms.model.StudentProfile;
import com.sms.model.User;
import com.sms.repository.EnrollmentRepository;
import com.sms.repository.StudentProfileRepository;
import com.sms.repository.StudentRepository;
import com.sms.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository,
                          EnrollmentRepository enrollmentRepository,
                          StudentProfileRepository studentProfileRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Page<Student> getStudentsPage(String search,
                                         String course,
                                         int page,
                                         int size,
                                         String sortBy,
                                         String sortDir) {
        String normalizedSortBy = normalizeSortBy(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(direction, normalizedSortBy));

        Specification<Student> spec = Specification.where(null);

        if (search != null && !search.isBlank()) {
            String searchToken = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("id")), searchToken),
                    cb.like(cb.lower(root.get("name")), searchToken),
                    cb.like(cb.lower(root.get("email")), searchToken)
            ));
        }

        if (course != null && !course.isBlank()) {
            String normalizedCourse = course.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("course")), normalizedCourse));
        }

        return studentRepository.findAll(spec, pageable);
    }

    public List<Student> getAllStudentsSortedByName() {
        return studentRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public List<Student> getAllStudentsSortedById() {
        return studentRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public List<Student> getAllStudentsSortedByMarks() {
        List<Student> students = studentRepository.findAll();
        Map<String, Double> averageMap = getAverageMarksMap(students);
        students.sort(Comparator.comparingDouble(student -> averageMap.getOrDefault(student.getId(), 0.0)));
        return students;
    }

    public Map<String, Double> getAverageMarksMap(List<Student> students) {
        Map<String, Double> averages = new HashMap<>();
        for (Student student : students) {
            List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());
            double avg = enrollments.stream()
                    .map(Enrollment::getMarks)
                    .filter(java.util.Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            averages.put(student.getId(), avg);
        }
        return averages;
    }

    public Optional<Student> findById(String id) {
        return studentRepository.findById(java.util.Objects.requireNonNull(id, "Student id must not be null"));
    }

    public Student save(Student student) {
        Student studentToSave = java.util.Objects.requireNonNull(student, "Student must not be null");
        String studentId = java.util.Objects.requireNonNull(studentToSave.getId(), "Student id must not be null");
        String derivedEmail = deriveStudentEmail(studentId);

        studentToSave.setEmail(derivedEmail);

        if (studentToSave.getUser() == null) {
            User user = userRepository.findByUsername(studentId)
                    .orElseGet(User::new);
            user.setUsername(studentId);
            user.setPassword(passwordEncoder.encode(studentId));
            user.setRole(Role.STUDENT);
            studentToSave.setUser(userRepository.save(user));
        } else {
            User user = studentToSave.getUser();
            if (user.getUsername() == null || !studentId.equals(user.getUsername())) {
                user.setUsername(studentId);
            }
            if (user.getRole() == null) {
                user.setRole(Role.STUDENT);
            }
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(studentId));
            }
            studentToSave.setUser(userRepository.save(user));
        }

        Student savedStudent = java.util.Objects.requireNonNull(studentRepository.save(studentToSave), "Saved student must not be null");
        upsertStudentProfile(savedStudent);
        return savedStudent;
    }

    public void deleteById(String id) {
        String studentId = java.util.Objects.requireNonNull(id, "Student id must not be null");
        studentRepository.findById(studentId).ifPresent(student -> {
            User user = student.getUser();
            if (user != null) {
                userRepository.delete(user);
            }
        });
        studentRepository.deleteById(studentId);
    }

    public int deleteByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        int deletedCount = 0;
        List<String> normalizedIds = new ArrayList<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                normalizedIds.add(id.trim());
            }
        }

        for (String studentId : normalizedIds) {
            String normalizedStudentId = java.util.Objects.requireNonNull(studentId, "Student id must not be null");
            if (studentRepository.existsById(normalizedStudentId)) {
                deleteById(normalizedStudentId);
                deletedCount++;
            }
        }

        return deletedCount;
    }

    public StudentProfileDTO getStudentProfileByUsername(String username) {
        Student student = studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found for user: " + username));

        StudentProfileDTO dto = new StudentProfileDTO();
        dto.setFullName(student.getName());
        dto.setStudentId(student.getId());
        dto.setEmail(deriveStudentEmail(student.getId()));
        dto.setPhone(student.getPhone());

        dto.setGender(student.getGender());
        dto.setDob(student.getDob() != null ? student.getDob().toString() : null);
        dto.setAddress(student.getAddress());

        dto.setCourse(student.getCourse());
        dto.setDepartment(student.getDepartment());
        dto.setSemester(student.getSemester());
        dto.setRollNumber(student.getRollNumber());
        dto.setEnrollmentYear(student.getEnrollmentYear());

        dto.setRole(student.getUser() != null && student.getUser().getRole() != null
                ? student.getUser().getRole().name()
                : "STUDENT");
        dto.setProfileImageUrl(student.getProfileImageUrl());

        return dto;
    }

    public Student getStudentByUsername(String username) {
        return studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Student not found for username: " + username));
    }

    public Student getStudentById(String studentId) {
        String normalizedStudentId = java.util.Objects.requireNonNull(studentId, "Student id must not be null");
        return studentRepository.findById(normalizedStudentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));
    }

    private String deriveStudentEmail(String studentId) {
        return studentId + "@bennett.edu.in";
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "id";
        }
        return switch (sortBy) {
            case "id", "name", "email", "course", "semester" -> sortBy;
            default -> "id";
        };
    }

    private void upsertStudentProfile(Student student) {
        StudentProfile profile = studentProfileRepository.findByStudentId(student.getId())
                .orElseGet(StudentProfile::new);

        profile.setStudentId(student.getId());
        profile.setFullName(student.getName());
        profile.setEnrollmentNumber(student.getId());
        profile.setProfileImage(student.getProfileImageUrl());
        profile.setDob(student.getDob());
        profile.setGender(student.getGender());
        profile.setPhone(student.getPhone());
        profile.setEmail(deriveStudentEmail(student.getId()));
        profile.setAddress(student.getAddress());
        profile.setCourse(student.getCourse());
        profile.setDepartment(student.getDepartment());
        profile.setSemester(student.getSemester());
        profile.setCollege("Bennett University");
        profile.setValidUpto(LocalDate.now().plusYears(4));
        profile.setIdCardNumber("BU-" + student.getId());
        profile.setUpdatedBy("Admin Create");

        studentProfileRepository.save(profile);
    }
}
