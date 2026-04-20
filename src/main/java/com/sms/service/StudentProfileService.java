package com.sms.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class StudentProfileService {

    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final ImageUploadService imageUploadService;

    public StudentProfileService(StudentRepository studentRepository,
                                 StudentProfileRepository studentProfileRepository,
                                 StudentDocumentRepository studentDocumentRepository,
                                 AcademicRecordRepository academicRecordRepository,
                                 ImageUploadService imageUploadService) {
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentDocumentRepository = studentDocumentRepository;
        this.academicRecordRepository = academicRecordRepository;
        this.imageUploadService = imageUploadService;
    }

    @Cacheable(value = "studentProfile", key = "'student:' + #username")
    public StudentProfileResponseDTO getProfileForStudent(String username) {
        String normalizedUsername = java.util.Objects.requireNonNull(username, "Username must not be null");
        Student student = studentRepository.findByUserUsername(normalizedUsername)
            .orElseThrow(() -> new IllegalArgumentException("Student not found for username: " + normalizedUsername));
        return mapProfile(student.getId(), "STUDENT");
    }

    @Cacheable(value = "studentProfile", key = "'admin:' + #studentId")
    public StudentProfileResponseDTO getProfileForAdmin(String studentId) {
        String normalizedStudentId = java.util.Objects.requireNonNull(studentId, "Student id must not be null");
        studentRepository.findById(normalizedStudentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + normalizedStudentId));
        return mapProfile(normalizedStudentId, "ADMIN");
    }

    @Transactional
    @CacheEvict(value = "studentProfile", allEntries = true)
    public StudentProfileResponseDTO updateByAdmin(String studentId,
                                                   AdminUpdateStudentProfileRequest request,
                                                   String actorUsername) {
        String normalizedStudentId = java.util.Objects.requireNonNull(studentId, "Student id must not be null");
        Student student = studentRepository.findById(normalizedStudentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + normalizedStudentId));

        StudentProfile profile = studentProfileRepository.findByStudentId(normalizedStudentId)
            .orElseGet(() -> createProfileFromStudent(student));

        String fullName = firstNonBlank(request.getFullName(), profile.getFullName(), student.getName());
        String profileImage = firstNonBlank(request.getProfileImage(), profile.getProfileImage(), student.getProfileImageUrl());
        
        // Handle base64 image upload
        if (profileImage != null && profileImage.startsWith("data:image")) {
            try {
                profileImage = imageUploadService.uploadBase64Image(profileImage, normalizedStudentId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload profile image: " + e.getMessage(), e);
            }
        }
        
        String gender = firstNonBlank(request.getGender(), profile.getGender(), student.getGender());
        String religion = firstNonBlank(request.getReligion(), profile.getReligion());
        String bloodGroup = firstNonBlank(request.getBloodGroup(), profile.getBloodGroup());
        String phone = firstNonBlank(request.getPhone(), profile.getPhone(), student.getPhone());
        String universityEmail = firstNonBlank(request.getUniversityEmail(), request.getEmail(), profile.getUniversityEmail(), profile.getEmail(), student.getEmail(), deriveStudentEmail(normalizedStudentId));
        String personalEmail = firstNonBlank(request.getPersonalEmail(), profile.getPersonalEmail());
        String address = firstNonBlank(request.getAddress(), profile.getAddress(), student.getAddress());
        String guardianName = firstNonBlank(request.getGuardianName(), profile.getGuardianName());
        String guardianPhone = firstNonBlank(request.getGuardianPhone(), profile.getGuardianPhone());
        String college = firstNonBlank(request.getCollege(), profile.getCollege(), "Bennett University");
        String course = firstNonBlank(request.getCourse(), profile.getCourse(), student.getCourse());
        String department = firstNonBlank(request.getDepartment(), profile.getDepartment(), student.getDepartment());
        String semester = firstNonBlank(request.getSemester(), profile.getSemester(), student.getSemester());
        String section = firstNonBlank(request.getSection(), profile.getSection());
        String foundationClassroom = firstNonBlank(request.getFoundationClassroom(), profile.getFoundationClassroom());
        Integer teamNumber = request.getTeamNumber() != null ? request.getTeamNumber() : profile.getTeamNumber();
        Integer memberNumber = request.getMemberNumber() != null ? request.getMemberNumber() : profile.getMemberNumber();
        Integer admissionYear = request.getAdmissionYear() != null ? request.getAdmissionYear() : profile.getAdmissionYear();
        Integer passingYear = request.getPassingYear() != null ? request.getPassingYear() : profile.getPassingYear();
        LocalDate dob = request.getDob() != null ? request.getDob() : (profile.getDob() != null ? profile.getDob() : student.getDob());
        LocalDate validUpto = request.getValidUpto() != null ? request.getValidUpto() : profile.getValidUpto();
        String idCardNumber = firstNonBlank(request.getIdCardNumber(), profile.getIdCardNumber(), "BU-" + normalizedStudentId);

        profile.setStudentId(normalizedStudentId);
        profile.setFullName(fullName);
        profile.setProfileImage(profileImage);
        profile.setDob(dob);
        profile.setGender(gender);
        profile.setReligion(religion);
        profile.setBloodGroup(bloodGroup);
        profile.setPhone(phone);
        profile.setUniversityEmail(universityEmail);
        profile.setPersonalEmail(personalEmail);
        profile.setEmail(universityEmail);
        profile.setAddress(address);
        profile.setGuardianName(guardianName);
        profile.setGuardianPhone(guardianPhone);
        profile.setCollege(college);
        profile.setCourse(course);
        profile.setDepartment(department);
        profile.setSemester(semester);
        profile.setSection(section);
        profile.setFoundationClassroom(foundationClassroom);
        profile.setTeamNumber(teamNumber);
        profile.setMemberNumber(memberNumber);
        profile.setAdmissionYear(admissionYear);
        profile.setPassingYear(passingYear);
        profile.setValidUpto(validUpto);
        profile.setIdCardNumber(idCardNumber);
        profile.setUpdatedBy(actorUsername);

        if (profile.getEnrollmentNumber() == null || profile.getEnrollmentNumber().isBlank()) {
            profile.setEnrollmentNumber(student.getId());
        }

        studentProfileRepository.save(profile);

        student.setName(fullName);
        student.setEmail(universityEmail);
        student.setPhone(phone);
        student.setGender(gender);
        student.setDob(dob);
        student.setAddress(address);
        student.setCourse(course);
        student.setDepartment(department);
        student.setSemester(semester);
        student.setProfileImageUrl(profileImage);
        if (admissionYear != null) {
            student.setEnrollmentYear(String.valueOf(admissionYear));
        }
        studentRepository.save(student);

        return mapProfile(normalizedStudentId, "ADMIN");
    }

    @Transactional
    @CacheEvict(value = "studentProfile", allEntries = true)
    public StudentProfileResponseDTO updateByStudent(String username, StudentSelfUpdateProfileRequest request) {
        String normalizedUsername = java.util.Objects.requireNonNull(username, "Username must not be null");
        Student student = studentRepository.findByUserUsername(normalizedUsername)
            .orElseThrow(() -> new IllegalArgumentException("Student not found for username: " + normalizedUsername));

        StudentProfile profile = studentProfileRepository.findByStudentId(student.getId())
            .orElseGet(() -> createProfileFromStudent(student));

        String profileImage = request.getProfileImage();
        
        // Handle base64 image upload
        if (profileImage != null && profileImage.startsWith("data:image")) {
            try {
                profileImage = imageUploadService.uploadBase64Image(profileImage, student.getId());
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload profile image: " + e.getMessage(), e);
            }
        }

        profile.setPhone(request.getPhone());
        String universityEmail = firstNonBlank(profile.getUniversityEmail(), profile.getEmail(), deriveStudentEmail(student.getId()));
        profile.setUniversityEmail(universityEmail);
        profile.setEmail(universityEmail);
        profile.setAddress(request.getAddress());
        profile.setProfileImage(profileImage);
        profile.setUpdatedBy(normalizedUsername);
        studentProfileRepository.save(profile);

        student.setPhone(request.getPhone());
        student.setAddress(request.getAddress());
        student.setProfileImageUrl(profileImage);
        student.setEmail(universityEmail);
        studentRepository.save(student);

        return mapProfile(student.getId(), "STUDENT");
    }

    private StudentProfileResponseDTO mapProfile(String studentId, String viewerRole) {
        String normalizedStudentId = java.util.Objects.requireNonNull(studentId, "Student id must not be null");
        Student student = studentRepository.findById(normalizedStudentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + normalizedStudentId));

        StudentProfile profile = studentProfileRepository.findByStudentId(normalizedStudentId)
            .orElseGet(() -> createProfileFromStudent(student));

        List<StudentDocument> documents = studentDocumentRepository.findByStudentIdOrderByUploadedAtDesc(normalizedStudentId);
        List<AcademicRecord> records = academicRecordRepository.findByStudentIdOrderBySubjectAsc(normalizedStudentId);

        StudentProfileResponseDTO dto = new StudentProfileResponseDTO();
        dto.setStudentId(normalizedStudentId);
        dto.setFullName(firstNonBlank(profile.getFullName(), student.getName()));
        dto.setEnrollmentNumber(firstNonBlank(profile.getEnrollmentNumber(), student.getId()));
        dto.setProfileImage(firstNonBlank(profile.getProfileImage(), student.getProfileImageUrl()));

        dto.setDob(profile.getDob() != null ? profile.getDob() : student.getDob());
        dto.setGender(firstNonBlank(profile.getGender(), student.getGender()));
        dto.setReligion(profile.getReligion());
        dto.setBloodGroup(profile.getBloodGroup());

        dto.setPhone(firstNonBlank(profile.getPhone(), student.getPhone()));
        String universityEmail = firstNonBlank(profile.getUniversityEmail(), profile.getEmail(), student.getEmail(), deriveStudentEmail(studentId));
        dto.setUniversityEmail(universityEmail);
        dto.setPersonalEmail(profile.getPersonalEmail());
        dto.setEmail(universityEmail);
        dto.setAddress(firstNonBlank(profile.getAddress(), student.getAddress()));

        dto.setGuardianName(profile.getGuardianName());
        dto.setGuardianPhone(profile.getGuardianPhone());

        dto.setCollege(firstNonBlank(profile.getCollege(), "Bennett University"));
        dto.setCourse(firstNonBlank(profile.getCourse(), student.getCourse()));
        dto.setDepartment(firstNonBlank(profile.getDepartment(), student.getDepartment()));
        dto.setSemester(firstNonBlank(profile.getSemester(), student.getSemester()));
        dto.setSection(profile.getSection());
        dto.setFoundationClassroom(profile.getFoundationClassroom());
        dto.setTeamNumber(profile.getTeamNumber());
        dto.setMemberNumber(profile.getMemberNumber());
        dto.setAdmissionYear(profile.getAdmissionYear() != null ? profile.getAdmissionYear() : parseYear(student.getEnrollmentYear()));
        dto.setPassingYear(profile.getPassingYear());

        dto.setValidUpto(profile.getValidUpto());
        dto.setIdCardNumber(firstNonBlank(profile.getIdCardNumber(), "BU-" + studentId));

        dto.setCreatedAt(profile.getCreatedAt());
        dto.setUpdatedAt(profile.getUpdatedAt());
        dto.setUpdatedBy(firstNonBlank(profile.getUpdatedBy(), "System"));

        dto.setProfileQrUrl("/student/profile?studentId=" + normalizedStudentId);
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
        profile.setReligion(null);
        profile.setPhone(student.getPhone());
        String universityEmail = firstNonBlank(student.getEmail(), deriveStudentEmail(student.getId()));
        profile.setUniversityEmail(universityEmail);
        profile.setPersonalEmail(null);
        profile.setEmail(universityEmail);
        profile.setAddress(student.getAddress());
        profile.setCourse(student.getCourse());
        profile.setDepartment(student.getDepartment());
        profile.setSemester(student.getSemester());
        profile.setFoundationClassroom(null);
        profile.setTeamNumber(null);
        profile.setMemberNumber(null);
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
        int total = 28;

        List<Object> fields = Arrays.asList(
            dto.getFullName(), dto.getEnrollmentNumber(), dto.getProfileImage(),
            dto.getDob(), dto.getGender(), dto.getReligion(), dto.getBloodGroup(),
            dto.getPhone(), dto.getEmail(), dto.getAddress(),
            dto.getUniversityEmail(), dto.getPersonalEmail(),
            dto.getGuardianName(), dto.getGuardianPhone(),
            dto.getCollege(), dto.getCourse(), dto.getDepartment(), dto.getSemester(), dto.getSection(),
            dto.getFoundationClassroom(), dto.getTeamNumber(), dto.getMemberNumber(),
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

    private String firstNonBlank(String... candidates) {
        if (candidates == null || candidates.length == 0) {
            return null;
        }

        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }

        return candidates[candidates.length - 1];
    }

    private String deriveStudentEmail(String studentId) {
        return studentId + "@bennett.edu.in";
    }
}
