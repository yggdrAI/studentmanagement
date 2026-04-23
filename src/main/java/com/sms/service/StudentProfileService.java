package com.sms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sms.dto.profile.AcademicRecordDTO;
import com.sms.dto.profile.AdminUpdateStudentProfileRequest;
import com.sms.dto.profile.StudentDemographicConsentRequest;
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
    private static final Logger log = LoggerFactory.getLogger(StudentProfileService.class);

    private static final String DEMOGRAPHIC_CONSENT_VERSION = "1.0";

    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final ImageUploadService imageUploadService;
    private final FaceVerificationService faceVerificationService;

    public StudentProfileService(StudentRepository studentRepository,
            StudentProfileRepository studentProfileRepository,
            StudentDocumentRepository studentDocumentRepository,
            AcademicRecordRepository academicRecordRepository,
            ImageUploadService imageUploadService,
            FaceVerificationService faceVerificationService) {
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentDocumentRepository = studentDocumentRepository;
        this.academicRecordRepository = academicRecordRepository;
        this.imageUploadService = imageUploadService;
        this.faceVerificationService = faceVerificationService;
    }

    @Cacheable(value = "studentProfile", key = "'student:' + #username")
    public StudentProfileResponseDTO getProfileForStudent(String username) {
        String normalizedUsername = java.util.Objects.requireNonNull(username, "Username must not be null");
        Student student = studentRepository.findByUserUsername(normalizedUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        return mapProfile(student.getId(), "STUDENT");
    }

    @Cacheable(value = "studentProfile", key = "'admin:' + #studentId")
    public StudentProfileResponseDTO getProfileForAdmin(String studentId) {
        String normalizedStudentId = java.util.Objects.requireNonNull(studentId, "Student id must not be null");
        studentRepository.findById(normalizedStudentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        return mapProfile(normalizedStudentId, "ADMIN");
    }

    @Transactional
    @CacheEvict(value = "studentProfile", allEntries = true)
    public StudentProfileResponseDTO updateByAdmin(String studentId,
            AdminUpdateStudentProfileRequest request,
            String actorUsername) {
        String normalizedStudentId = java.util.Objects.requireNonNull(studentId, "Student id must not be null");
        if (request == null) {
            request = new AdminUpdateStudentProfileRequest();
        }
        String safeActorUsername = actorUsername == null || actorUsername.isBlank() ? "ADMIN" : actorUsername;
        Student student = studentRepository.findById(normalizedStudentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        try {

        StudentProfile profile = studentProfileRepository.findByStudentId(normalizedStudentId)
                .orElseGet(() -> createProfileFromStudent(student));

        String fullName = firstNonBlank(request.getFullName(), profile.getFullName(), student.getName());
        String profileImage = firstNonBlank(request.getProfileImage(), profile.getProfileImage(),
                student.getProfileImageUrl());

        // Handle base64 image upload — only process if it's a NEW upload from the
        // request
        String requestedImage = request.getProfileImage();
        if (isDataImageUri(requestedImage)) {
            try {
                profileImage = imageUploadService.uploadBase64Image(requestedImage, normalizedStudentId);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Profile image upload failed: "
                                + safeMessage(e, "Invalid image format or image size exceeds the limit."),
                        e);
            }
            try {
                byte[] imageBytes = decodeDataImage(profileImage);
                faceVerificationService.registerFaceFromImageUpload(
                        normalizedStudentId,
                        1L,
                        imageBytes,
                        normalizedStudentId + "-profile-upload.jpg",
                        true,
                        "profile-image-upload");
            } catch (Exception e) {
                // Do not block profile image upload when embedding service is unavailable.
                log.warn("Face registration skipped for student {} after profile image upload: {}", normalizedStudentId,
                        safeMessage(e, e.getClass().getSimpleName()));
            }
        }

        String gender = StudentFieldDerivationUtils.inferGender(fullName,
                firstNonBlank(request.getGender(), profile.getGender(), student.getGender()));
        String religion = firstNonBlank(request.getReligion(), profile.getReligion());
        String bloodGroup = firstNonBlank(request.getBloodGroup(), profile.getBloodGroup());
        String phone = firstNonBlank(request.getPhone(), profile.getPhone(), student.getPhone());
        String universityEmail = firstNonBlank(request.getUniversityEmail(), request.getEmail(),
                profile.getUniversityEmail(), profile.getEmail(), student.getEmail(),
                deriveStudentEmail(normalizedStudentId));
        String personalEmail = firstNonBlank(request.getPersonalEmail(), profile.getPersonalEmail());
        String address = firstNonBlank(request.getAddress(), profile.getAddress(), student.getAddress());
        String guardianName = firstNonBlank(request.getGuardianName(), profile.getGuardianName());
        String guardianPhone = firstNonBlank(request.getGuardianPhone(), profile.getGuardianPhone());
        String college = StudentFieldDerivationUtils.resolveCollegeName(
                firstNonBlank(request.getCollege(), profile.getCollege()),
                firstNonBlank(request.getCourse(), profile.getCourse(), student.getCourse()));
        String course = firstNonBlank(request.getCourse(), profile.getCourse(), student.getCourse());
        String department = firstNonBlank(request.getDepartment(), profile.getDepartment(), student.getDepartment());
        String semester = firstNonBlank(request.getSemester(), profile.getSemester(), student.getSemester());
        String section = firstNonBlank(request.getSection(), profile.getSection());
        String house = StudentFieldDerivationUtils.resolveHouse(firstNonBlank(request.getHouse(), profile.getHouse()),
                null);
        String foundationClassroom = normalizeFoundationClassroom(
                firstNonBlank(request.getFoundationClassroom(), profile.getFoundationClassroom()), house);
        Integer teamNumber = request.getTeamNumber() != null ? request.getTeamNumber() : profile.getTeamNumber();
        Integer memberNumber = request.getMemberNumber() != null ? request.getMemberNumber()
                : profile.getMemberNumber();
        Integer admissionYear = request.getAdmissionYear() != null ? request.getAdmissionYear()
                : profile.getAdmissionYear();
        Integer passingYear = StudentFieldDerivationUtils.derivePassingYear(course, admissionYear,
                request.getPassingYear() != null ? request.getPassingYear() : profile.getPassingYear());
        LocalDate dob = request.getDob() != null ? request.getDob()
                : (profile.getDob() != null ? profile.getDob() : student.getDob());
        LocalDate validUpto = StudentFieldDerivationUtils.deriveValidUpto(course, admissionYear, passingYear,
                request.getValidUpto());
        String idCardNumber = firstNonBlank(request.getIdCardNumber(), profile.getIdCardNumber(),
                "BU-" + normalizedStudentId);

        profile.setStudentId(normalizedStudentId);
        if (student.getUser() != null) {
            profile.setUserId(student.getUser().getId());
        }
        profile.setFullName(fullName);
        profile.setProfileImage(profileImage);
        // For profilePhotoUrl (VARCHAR), skip data URIs; for @Lob profileImage, store
        // directly
        profile.setProfilePhotoUrl(normalizePhotoUrl(profileImage, profile.getProfilePhotoUrl()));
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
        profile.setHouse(house);
        profile.setFoundationClassroom(foundationClassroom);
        profile.setTeamNumber(teamNumber);
        profile.setMemberNumber(memberNumber);
        profile.setAdmissionYear(admissionYear);
        profile.setPassingYear(passingYear);
        profile.setValidUpto(validUpto);
        profile.setIdCardNumber(idCardNumber);
        profile.setUpdatedBy(safeActorUsername);

        if (profile.getEnrollmentNumber() == null || profile.getEnrollmentNumber().isBlank()) {
            profile.setEnrollmentNumber(student.getId());
        }

        try {
            studentProfileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException ex) {
            throw profileImageTooLargeException(ex);
        }

        student.setName(fullName);
        student.setEmail(universityEmail);
        student.setPhone(phone);
        student.setGender(gender);
        student.setDob(dob);
        student.setAddress(address);
        student.setCourse(course);
        student.setDepartment(department);
        student.setSemester(semester);
        // Keep large/base64 payload in student_profile (LOB) and avoid duplicating into
        // student.profileImageUrl, which may still be a constrained column in existing DBs.
        student.setProfileImageUrl(normalizePhotoUrl(profileImage, student.getProfileImageUrl()));
        if (admissionYear != null) {
            student.setEnrollmentYear(String.valueOf(admissionYear));
        }
        try {
            studentRepository.saveAndFlush(student);
        } catch (DataIntegrityViolationException ex) {
            throw profileImageTooLargeException(ex);
        }

        return mapProfile(normalizedStudentId, "ADMIN");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to update admin profile for student {}", normalizedStudentId, ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Profile update failed: " + safeMessage(ex, "Unexpected error"), ex);
        }
    }

    @Transactional
    @CacheEvict(value = "studentProfile", allEntries = true)
    public StudentProfileResponseDTO updateByStudent(String username, StudentSelfUpdateProfileRequest request) {
        String normalizedUsername = java.util.Objects.requireNonNull(username, "Username must not be null");
        Student student = studentRepository.findByUserUsername(normalizedUsername)
                .orElseThrow(
                        () -> new IllegalArgumentException("Student not found for username: " + normalizedUsername));

        try {

        StudentProfile profile = studentProfileRepository.findByStudentId(student.getId())
                .orElseGet(() -> createProfileFromStudent(student));

        String profileImage = request.getProfileImage();

        // Handle base64 image upload
        if (isDataImageUri(profileImage)) {
            try {
                profileImage = imageUploadService.uploadBase64Image(profileImage, student.getId());
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Profile image upload failed: "
                                + safeMessage(e, "Invalid image format or image size exceeds the limit."),
                        e);
            }
            try {
                byte[] imageBytes = decodeDataImage(profileImage);
                faceVerificationService.registerFaceFromImageUpload(
                        student.getId(),
                        1L,
                        imageBytes,
                        student.getId() + "-profile-upload.jpg",
                        true,
                        "profile-image-upload");
            } catch (Exception e) {
                // Do not block profile image upload when embedding service is unavailable.
                log.warn("Face registration skipped for student {} after profile image upload: {}", student.getId(),
                        safeMessage(e, e.getClass().getSimpleName()));
            }
        }

        profile.setPhone(request.getPhone());
        String universityEmail = firstNonBlank(profile.getUniversityEmail(), profile.getEmail(),
                deriveStudentEmail(student.getId()));
        profile.setUniversityEmail(universityEmail);
        profile.setEmail(universityEmail);
        profile.setAddress(request.getAddress());
        profile.setProfileImage(profileImage);
        profile.setProfilePhotoUrl(normalizePhotoUrl(profileImage, profile.getProfilePhotoUrl()));
        if (student.getUser() != null) {
            profile.setUserId(student.getUser().getId());
        }
        profile.setUpdatedBy(normalizedUsername);
        try {
            studentProfileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException ex) {
            throw profileImageTooLargeException(ex);
        }

        student.setPhone(request.getPhone());
        student.setAddress(request.getAddress());
        // Keep large/base64 payload in student_profile (LOB) and avoid duplicating into
        // student.profileImageUrl, which may still be a constrained column in existing DBs.
        student.setProfileImageUrl(normalizePhotoUrl(profileImage, student.getProfileImageUrl()));
        student.setEmail(universityEmail);
        try {
            studentRepository.saveAndFlush(student);
        } catch (DataIntegrityViolationException ex) {
            throw profileImageTooLargeException(ex);
        }

        return mapProfile(student.getId(), "STUDENT");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to update student profile for {}", normalizedUsername, ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Profile update failed: " + safeMessage(ex, "Unexpected error"), ex);
        }
    }

    @Transactional
    @CacheEvict(value = "studentProfile", allEntries = true)
    public StudentProfileResponseDTO submitDemographicConsent(String username,
            StudentDemographicConsentRequest request) {
        String normalizedUsername = java.util.Objects.requireNonNull(username, "Username must not be null");
        Student student = studentRepository.findByUserUsername(normalizedUsername)
                .orElseThrow(
                        () -> new IllegalArgumentException("Student not found for username: " + normalizedUsername));

        if (!Boolean.TRUE.equals(request.getConsentGiven())) {
            throw new IllegalArgumentException("Consent is required before saving demographics");
        }

        StudentProfile profile = studentProfileRepository.findByStudentId(student.getId())
                .orElseGet(() -> createProfileFromStudent(student));

        profile.setGender(clean(request.getGender()));
        profile.setGenderSource("SELF_DECLARED");
        profile.setReligion(clean(request.getReligion()));
        profile.setReligionSource("SELF_DECLARED");
        profile.setCaste(clean(request.getSpecificCaste()));
        profile.setCasteCategory(clean(request.getCategory()));
        profile.setCasteSource("SELF_DECLARED");
        profile.setDemographicConsentGiven(true);
        profile.setDemographicConsentAt(LocalDateTime.now());
        profile.setDemographicConsentVersion(DEMOGRAPHIC_CONSENT_VERSION);
        profile.setUpdatedBy(normalizedUsername);

        student.setGender(clean(request.getGender()));
        studentRepository.save(student);
        studentProfileRepository.save(profile);

        return mapProfile(student.getId(), "STUDENT");
    }

    private StudentProfileResponseDTO mapProfile(String studentId, String viewerRole) {
        String normalizedStudentId = java.util.Objects.requireNonNull(studentId, "Student id must not be null");
        Student student = studentRepository.findById(normalizedStudentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + normalizedStudentId));

        StudentProfile profile = studentProfileRepository.findByStudentId(normalizedStudentId)
                .orElseGet(() -> createProfileFromStudent(student));

        List<StudentDocument> documents = studentDocumentRepository
                .findByStudentIdOrderByUploadedAtDesc(normalizedStudentId);
        List<AcademicRecord> records = academicRecordRepository.findByStudentIdOrderBySubjectAsc(normalizedStudentId);

        StudentProfileResponseDTO dto = new StudentProfileResponseDTO();
        dto.setStudentId(normalizedStudentId);
        dto.setFullName(firstNonBlank(profile.getFullName(), student.getName()));
        dto.setEnrollmentNumber(firstNonBlank(profile.getEnrollmentNumber(), student.getId()));
        dto.setProfileImage(
                firstNonBlank(profile.getProfilePhotoUrl(), profile.getProfileImage(), student.getProfileImageUrl()));

        dto.setDob(profile.getDob() != null ? profile.getDob() : student.getDob());
        dto.setGender(firstNonBlank(profile.getGender(), student.getGender()));
        dto.setReligion(profile.getReligion());
        dto.setBloodGroup(profile.getBloodGroup());
        dto.setCasteCategory(profile.getCasteCategory());

        dto.setPhone(firstNonBlank(profile.getPhone(), student.getPhone()));
        String universityEmail = firstNonBlank(profile.getUniversityEmail(), profile.getEmail(), student.getEmail(),
                deriveStudentEmail(studentId));
        dto.setUniversityEmail(universityEmail);
        dto.setPersonalEmail(profile.getPersonalEmail());
        dto.setEmail(universityEmail);
        dto.setAddress(firstNonBlank(profile.getAddress(), student.getAddress()));

        dto.setGuardianName(profile.getGuardianName());
        dto.setGuardianPhone(profile.getGuardianPhone());

        dto.setCollege(StudentFieldDerivationUtils.resolveCollegeName(profile.getCollege(), profile.getCourse()));
        dto.setCourse(firstNonBlank(profile.getCourse(), student.getCourse()));
        dto.setDepartment(firstNonBlank(profile.getDepartment(), student.getDepartment()));
        dto.setSemester(firstNonBlank(profile.getSemester(), student.getSemester()));
        dto.setSection(profile.getSection());
        dto.setHouse(profile.getHouse());
        dto.setFoundationClassroom(normalizeFoundationClassroom(profile.getFoundationClassroom(), profile.getHouse()));
        dto.setTeamNumber(profile.getTeamNumber());
        dto.setMemberNumber(profile.getMemberNumber());
        dto.setAdmissionYear(profile.getAdmissionYear() != null ? profile.getAdmissionYear()
                : parseYear(student.getEnrollmentYear()));
        dto.setPassingYear(StudentFieldDerivationUtils.derivePassingYear(dto.getCourse(), dto.getAdmissionYear(),
                profile.getPassingYear()));

        dto.setValidUpto(profile.getValidUpto());
        dto.setIdCardNumber(firstNonBlank(profile.getIdCardNumber(), "BU-" + studentId));

        dto.setCreatedAt(profile.getCreatedAt());
        dto.setUpdatedAt(profile.getUpdatedAt());
        dto.setUpdatedBy(firstNonBlank(profile.getUpdatedBy(), "System"));
        dto.setDemographicConsentGiven(Boolean.TRUE.equals(profile.getDemographicConsentGiven()));
        dto.setDemographicConsentAt(profile.getDemographicConsentAt());
        dto.setDemographicConsentVersion(profile.getDemographicConsentVersion());
        dto.setGenderSource(profile.getGenderSource());
        dto.setReligionSource(profile.getReligionSource());
        dto.setCasteSource(profile.getCasteSource());

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
        if (student.getUser() != null) {
            profile.setUserId(student.getUser().getId());
        }
        profile.setFullName(student.getName());
        profile.setEnrollmentNumber(student.getId());
        profile.setProfilePhotoUrl(normalizePhotoUrl(student.getProfileImageUrl(), null));
        profile.setDob(student.getDob());
        profile.setGender(StudentFieldDerivationUtils.inferGender(student.getName(), student.getGender()));
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
        profile.setHouse(null);
        profile.setFoundationClassroom(null);
        profile.setTeamNumber(null);
        profile.setMemberNumber(null);
        profile.setAdmissionYear(parseYear(student.getEnrollmentYear()));
        profile.setCollege(StudentFieldDerivationUtils.resolveCollegeName(null, student.getCourse()));
        profile.setPassingYear(
                StudentFieldDerivationUtils.derivePassingYear(student.getCourse(), profile.getAdmissionYear(), null));
        profile.setValidUpto(StudentFieldDerivationUtils.deriveValidUpto(student.getCourse(),
                profile.getAdmissionYear(), profile.getPassingYear(), null));
        profile.setIdCardNumber("BU-" + student.getId());
        profile.setProfileImage(student.getProfileImageUrl());
        profile.setUpdatedBy("System");
        profile.setDemographicConsentGiven(Boolean.FALSE);
        profile.setDemographicConsentVersion(DEMOGRAPHIC_CONSENT_VERSION);
        profile.setGenderSource("SYSTEM_DEFAULT");
        profile.setReligionSource("SYSTEM_DEFAULT");
        profile.setCasteSource("SYSTEM_DEFAULT");
        return studentProfileRepository.save(profile);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeFoundationClassroom(String foundationClassroom, String house) {
        String cleanedFoundation = clean(foundationClassroom);
        if (cleanedFoundation == null) {
            return null;
        }
        return cleanedFoundation;
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
        List<Object> fields = Arrays.asList(
                dto.getFullName(), dto.getEnrollmentNumber(), dto.getProfileImage(),
                dto.getDob(), dto.getGender(), dto.getReligion(), dto.getBloodGroup(),
                dto.getPhone(), dto.getEmail(), dto.getAddress(),
                dto.getUniversityEmail(), dto.getPersonalEmail(),
                dto.getGuardianName(), dto.getGuardianPhone(),
                dto.getCollege(), dto.getCourse(), dto.getDepartment(), dto.getSemester(), dto.getSection(),
                dto.getHouse(),
                dto.getFoundationClassroom(), dto.getTeamNumber(), dto.getMemberNumber(),
                dto.getAdmissionYear(), dto.getPassingYear(),
                dto.getValidUpto(), dto.getIdCardNumber(),
                dto.getCreatedAt(), dto.getUpdatedAt());

        int filled = 0;
        int total = fields.size();

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

    private String normalizePhotoUrl(String profileImage, String existingPhotoUrl) {
        if (profileImage == null || profileImage.isBlank()) {
            return existingPhotoUrl;
        }
        String normalized = profileImage.trim();
        if (isDataImageUri(normalized) || looksLikeRawBase64Payload(normalized) || normalized.length() > 2048) {
            return existingPhotoUrl;
        }
        return normalized;
    }

    private boolean isDataImageUri(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.startsWith("data:image");
    }

    private boolean looksLikeRawBase64Payload(String value) {
        if (value == null || value.length() < 256 || value.contains("://") || value.contains("/")) {
            return false;
        }
        return value.matches("^[A-Za-z0-9+/=\\s]+$");
    }

    private byte[] decodeDataImage(String dataUri) {
        if (dataUri == null || dataUri.isBlank() || !isDataImageUri(dataUri)) {
            throw new IllegalArgumentException("Profile image must be a valid data:image URI");
        }
        int commaIndex = dataUri.indexOf(',');
        if (commaIndex < 0 || commaIndex >= dataUri.length() - 1) {
            throw new IllegalArgumentException("Invalid image payload");
        }
        String payload = dataUri.substring(commaIndex + 1).replaceAll("\\s+", "");
        return Base64.getDecoder().decode(payload);
    }

    private String safeMessage(Exception ex, String fallback) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return fallback;
        }
        return ex.getMessage();
    }

    private ResponseStatusException profileImageTooLargeException(DataIntegrityViolationException ex) {
        String message = "Profile image is too large for current storage limits. "
                + "Please upload a smaller image.";
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message, ex);
    }
}