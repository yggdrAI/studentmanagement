package com.sms.service;

import com.sms.dto.profile.AcademicRecordDTO;
import com.sms.dto.profile.AdminUpdateStudentProfileRequest;
import com.sms.dto.profile.StudentDocumentDTO;
import com.sms.dto.profile.StudentProfileResponseDTO;
import com.sms.dto.profile.StudentSelfUpdateProfileRequest;
import com.sms.model.AcademicRecord;
import com.sms.model.Student;
import com.sms.model.StudentDocument;
import com.sms.model.StudentProfile;
import com.sms.repository.AcademicRecordRepository;
import com.sms.repository.StudentDocumentRepository;
import com.sms.repository.StudentProfileRepository;
import com.sms.repository.StudentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class StudentProfileService {

    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final AcademicRecordRepository academicRecordRepository;

    public StudentProfileService(StudentRepository studentRepository,
                                 StudentProfileRepository studentProfileRepository,
                                 StudentDocumentRepository studentDocumentRepository,
                                 AcademicRecordRepository academicRecordRepository) {
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentDocumentRepository = studentDocumentRepository;
        this.academicRecordRepository = academicRecordRepository;
    }

    @Cacheable(value = "studentProfile", key = "'student:' + #username")
    public StudentProfileResponseDTO getProfileForStudent(String username) {
        Student student = studentRepository.findByUserUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Student not found for username: " + username));
        return mapProfile(student.getId(), "STUDENT");
    }

    @Cacheable(value = "studentProfile", key = "'admin:' + #studentId")
    public StudentProfileResponseDTO getProfileForAdmin(String studentId) {
        studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));
        return mapProfile(studentId, "ADMIN");
    }

    @Transactional
    @CacheEvict(value = "studentProfile", allEntries = true)
    public StudentProfileResponseDTO updateByAdmin(String studentId,
                                                   AdminUpdateStudentProfileRequest request,
                                                   String actorUsername) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        StudentProfile profile = studentProfileRepository.findByStudentId(studentId)
            .orElseGet(() -> createProfileFromStudent(student));

        profile.setStudentId(studentId);
        profile.setFullName(request.getFullName());
        profile.setProfileImage(request.getProfileImage());
        profile.setDob(request.getDob());
        profile.setGender(request.getGender());
        profile.setBloodGroup(request.getBloodGroup());
        profile.setPhone(request.getPhone());
        profile.setEmail(request.getEmail());
        profile.setAddress(request.getAddress());
        profile.setGuardianName(request.getGuardianName());
        profile.setGuardianPhone(request.getGuardianPhone());
        profile.setCollege(request.getCollege());
        profile.setCourse(request.getCourse());
        profile.setDepartment(request.getDepartment());
        profile.setSemester(request.getSemester());
        profile.setSection(request.getSection());
        profile.setAdmissionYear(request.getAdmissionYear());
        profile.setPassingYear(request.getPassingYear());
        profile.setValidUpto(request.getValidUpto());
        profile.setIdCardNumber(request.getIdCardNumber());
        profile.setUpdatedBy(actorUsername);

        if (profile.getEnrollmentNumber() == null || profile.getEnrollmentNumber().isBlank()) {
            profile.setEnrollmentNumber(student.getId());
        }

        studentProfileRepository.save(profile);

        student.setName(request.getFullName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setGender(request.getGender());
        student.setDob(request.getDob());
        student.setAddress(request.getAddress());
        student.setCourse(request.getCourse());
        student.setDepartment(request.getDepartment());
        student.setSemester(request.getSemester());
        student.setProfileImageUrl(request.getProfileImage());
        if (request.getAdmissionYear() != null) {
            student.setEnrollmentYear(String.valueOf(request.getAdmissionYear()));
        }
        studentRepository.save(student);

        return mapProfile(studentId, "ADMIN");
    }

    @Transactional
    @CacheEvict(value = "studentProfile", allEntries = true)
    public StudentProfileResponseDTO updateByStudent(String username, StudentSelfUpdateProfileRequest request) {
        Student student = studentRepository.findByUserUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Student not found for username: " + username));

        StudentProfile profile = studentProfileRepository.findByStudentId(student.getId())
            .orElseGet(() -> createProfileFromStudent(student));

        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());
        profile.setProfileImage(request.getProfileImage());
        profile.setUpdatedBy(username);
        studentProfileRepository.save(profile);

        student.setPhone(request.getPhone());
        student.setAddress(request.getAddress());
        student.setProfileImageUrl(request.getProfileImage());
        studentRepository.save(student);

        return mapProfile(student.getId(), "STUDENT");
    }

    private StudentProfileResponseDTO mapProfile(String studentId, String viewerRole) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        StudentProfile profile = studentProfileRepository.findByStudentId(studentId)
            .orElseGet(() -> createProfileFromStudent(student));

        List<StudentDocument> documents = studentDocumentRepository.findByStudentIdOrderByUploadedAtDesc(studentId);
        List<AcademicRecord> records = academicRecordRepository.findByStudentIdOrderBySubjectAsc(studentId);

        StudentProfileResponseDTO dto = new StudentProfileResponseDTO();
        dto.setStudentId(studentId);
        dto.setFullName(firstNonBlank(profile.getFullName(), student.getName()));
        dto.setEnrollmentNumber(firstNonBlank(profile.getEnrollmentNumber(), student.getId()));
        dto.setProfileImage(firstNonBlank(profile.getProfileImage(), student.getProfileImageUrl()));

        dto.setDob(profile.getDob() != null ? profile.getDob() : student.getDob());
        dto.setGender(firstNonBlank(profile.getGender(), student.getGender()));
        dto.setBloodGroup(profile.getBloodGroup());

        dto.setPhone(firstNonBlank(profile.getPhone(), student.getPhone()));
        dto.setEmail(firstNonBlank(profile.getEmail(), student.getEmail()));
        dto.setAddress(firstNonBlank(profile.getAddress(), student.getAddress()));

        dto.setGuardianName(profile.getGuardianName());
        dto.setGuardianPhone(profile.getGuardianPhone());

        dto.setCollege(firstNonBlank(profile.getCollege(), "Bennett University"));
        dto.setCourse(firstNonBlank(profile.getCourse(), student.getCourse()));
        dto.setDepartment(firstNonBlank(profile.getDepartment(), student.getDepartment()));
        dto.setSemester(firstNonBlank(profile.getSemester(), student.getSemester()));
        dto.setSection(profile.getSection());
        dto.setAdmissionYear(profile.getAdmissionYear() != null ? profile.getAdmissionYear() : parseYear(student.getEnrollmentYear()));
        dto.setPassingYear(profile.getPassingYear());

        dto.setValidUpto(profile.getValidUpto());
        dto.setIdCardNumber(firstNonBlank(profile.getIdCardNumber(), "BU-" + studentId));

        dto.setCreatedAt(profile.getCreatedAt());
        dto.setUpdatedAt(profile.getUpdatedAt());
        dto.setUpdatedBy(firstNonBlank(profile.getUpdatedBy(), "System"));

        dto.setProfileQrUrl("/student/profile?studentId=" + studentId);
        dto.setViewerRole(viewerRole);
        dto.setAdminEditable("ADMIN".equalsIgnoreCase(viewerRole));

        dto.setDocuments(mapDocuments(documents));
        dto.setAcademicRecords(mapAcademicRecords(records));
        dto.setCompletionPercentage(calculateCompletion(dto));

        return dto;
    }

    private StudentProfile createProfileFromStudent(Student student) {
        StudentProfile profile = new StudentProfile();
        profile.setStudentId(student.getId());
        profile.setFullName(student.getName());
        profile.setEnrollmentNumber(student.getId());
        profile.setProfileImage(student.getProfileImageUrl());
        profile.setDob(student.getDob());
        profile.setGender(student.getGender());
        profile.setPhone(student.getPhone());
        profile.setEmail(student.getEmail());
        profile.setAddress(student.getAddress());
        profile.setCourse(student.getCourse());
        profile.setDepartment(student.getDepartment());
        profile.setSemester(student.getSemester());
        profile.setAdmissionYear(parseYear(student.getEnrollmentYear()));
        profile.setCollege("Bennett University");
        profile.setValidUpto(LocalDate.now().plusYears(4));
        profile.setIdCardNumber("BU-" + student.getId());
        profile.setUpdatedBy("System");
        return studentProfileRepository.save(profile);
    }

    private List<StudentDocumentDTO> mapDocuments(List<StudentDocument> documents) {
        List<StudentDocumentDTO> mapped = new ArrayList<>();
        for (StudentDocument document : documents) {
            StudentDocumentDTO dto = new StudentDocumentDTO();
            dto.setId(document.getId());
            dto.setDocumentType(document.getDocumentType());
            dto.setFileUrl(document.getFileUrl());
            dto.setUploadedAt(document.getUploadedAt());
            mapped.add(dto);
        }
        return mapped;
    }

    private List<AcademicRecordDTO> mapAcademicRecords(List<AcademicRecord> records) {
        List<AcademicRecordDTO> mapped = new ArrayList<>();
        for (AcademicRecord record : records) {
            AcademicRecordDTO dto = new AcademicRecordDTO();
            dto.setId(record.getId());
            dto.setSubject(record.getSubject());
            dto.setGrade(record.getGrade());
            dto.setAttendance(record.getAttendance());
            mapped.add(dto);
        }
        return mapped;
    }

    private int calculateCompletion(StudentProfileResponseDTO dto) {
        int filled = 0;
        int total = 22;

        List<Object> fields = Arrays.asList(
            dto.getFullName(), dto.getEnrollmentNumber(), dto.getProfileImage(),
            dto.getDob(), dto.getGender(), dto.getBloodGroup(),
            dto.getPhone(), dto.getEmail(), dto.getAddress(),
            dto.getGuardianName(), dto.getGuardianPhone(),
            dto.getCollege(), dto.getCourse(), dto.getDepartment(), dto.getSemester(), dto.getSection(),
            dto.getAdmissionYear(), dto.getPassingYear(),
            dto.getValidUpto(), dto.getIdCardNumber(),
            dto.getCreatedAt(), dto.getUpdatedAt()
        );

        for (Object field : fields) {
            if (field instanceof String s) {
                if (s != null && !s.isBlank()) {
                    filled++;
                }
            } else if (field != null) {
                filled++;
            }
        }

        return (int) Math.round((filled * 100.0) / total);
    }

    private Integer parseYear(String year) {
        if (year == null || year.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(year.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }
}
