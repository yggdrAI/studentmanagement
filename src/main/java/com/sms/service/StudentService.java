package com.sms.service;

import com.sms.dto.student.StudentProfileDTO;
import com.sms.model.Enrollment;
import com.sms.model.Student;
import com.sms.repository.EnrollmentRepository;
import com.sms.repository.StudentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;

    public StudentService(StudentRepository studentRepository, EnrollmentRepository enrollmentRepository) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
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
        return java.util.Objects.requireNonNull(
                studentRepository.save(java.util.Objects.requireNonNull(student, "Student must not be null")),
                "Saved student must not be null");
    }

    public void deleteById(String id) {
        studentRepository.deleteById(java.util.Objects.requireNonNull(id, "Student id must not be null"));
    }

    public StudentProfileDTO getStudentProfileByUsername(String username) {
        Student student = studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found for user: " + username));

        StudentProfileDTO dto = new StudentProfileDTO();
        dto.setFullName(student.getName());
        dto.setStudentId(student.getId());
        dto.setEmail(student.getEmail());
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
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));
    }
}
