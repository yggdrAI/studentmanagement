package com.sms.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sms.dto.student.StudentProfileDTO;
import com.sms.model.Enrollment;
import com.sms.model.Role;
import com.sms.model.Student;
import com.sms.model.StudentProfile;
import com.sms.model.User;
import com.sms.repository.AcademicRecordRepository;
import com.sms.repository.AttendanceRepository;
import com.sms.repository.DietLogRepository;
import com.sms.repository.EnrollmentRepository;
import com.sms.repository.FaceDataRepository;
import com.sms.repository.StudentDocumentRepository;
import com.sms.repository.StudentLocationRepository;
import com.sms.repository.StudentProfileRepository;
import com.sms.repository.StudentRepository;
import com.sms.repository.StudentTaskRepository;
import com.sms.repository.UserRepository;

@Service
public class StudentService {

    private static final int BATCH_SIZE = 30;
    private static final int CLASS_SIZE = 120;
    private static final Pattern TRAILING_NUMBER_PATTERN = Pattern.compile("(\\d+)$");
    private static final Pattern ENROLLMENT_YEAR_4_PATTERN = Pattern.compile("(19\\d{2}|20\\d{2})");
    private static final Pattern ENROLLMENT_YEAR_2_PREFIX_PATTERN = Pattern.compile("^(\\d{2})[A-Z].*");

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final StudentLocationRepository studentLocationRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final AttendanceRepository attendanceRepository;
    private final FaceDataRepository faceDataRepository;
    private final StudentTaskRepository studentTaskRepository;
    private final DietLogRepository dietLogRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AnalyticsRealtimeNotifier analyticsRealtimeNotifier;
    private final AnalyticsCacheService analyticsCacheService;

    public StudentService(StudentRepository studentRepository,
                          EnrollmentRepository enrollmentRepository,
                          StudentDocumentRepository studentDocumentRepository,
                          StudentLocationRepository studentLocationRepository,
                          AcademicRecordRepository academicRecordRepository,
                          AttendanceRepository attendanceRepository,
                          FaceDataRepository faceDataRepository,
                          StudentTaskRepository studentTaskRepository,
                          DietLogRepository dietLogRepository,
                          StudentProfileRepository studentProfileRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AnalyticsRealtimeNotifier analyticsRealtimeNotifier,
                          AnalyticsCacheService analyticsCacheService) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentDocumentRepository = studentDocumentRepository;
        this.studentLocationRepository = studentLocationRepository;
        this.academicRecordRepository = academicRecordRepository;
        this.attendanceRepository = attendanceRepository;
        this.faceDataRepository = faceDataRepository;
        this.studentTaskRepository = studentTaskRepository;
        this.dietLogRepository = dietLogRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.analyticsRealtimeNotifier = analyticsRealtimeNotifier;
        this.analyticsCacheService = analyticsCacheService;
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
        return getStudentsPage(
            search,
            course,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            page,
            size,
            sortBy,
            sortDir
        );
    }

    public Page<Student> getStudentsPage(String search,
                                         String course,
                                         String degree,
                                         String school,
                                         String house,
                                         String gender,
                                         String classGroup,
                                         String batchGroup,
                                         String religion,
                                         String caste,
                                         String placeOfOrigin,
                                         Integer minAge,
                                         Integer maxAge,
                                         String semester,
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
                    cb.like(cb.lower(root.get("email")), searchToken),
                    cb.like(cb.lower(root.get("phone")), searchToken),
                    cb.like(cb.lower(root.get("course")), searchToken)
            ));
        }

        if (course != null && !course.isBlank()) {
            String normalizedCourse = course.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("course")), normalizedCourse));
        }

        if (semester != null && !semester.isBlank()) {
            String normalizedSemester = semester.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("semester")), normalizedSemester));
        }

        if (gender != null && !gender.isBlank()) {
            String normalizedGender = gender.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("gender")), normalizedGender));
        }

        if (classGroup != null && !classGroup.isBlank()) {
            String normalizedClassGroup = classGroup.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("classGroup")), normalizedClassGroup));
        }

        if (batchGroup != null && !batchGroup.isBlank()) {
            String normalizedBatchGroup = batchGroup.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("batchGroup")), normalizedBatchGroup));
        }

        boolean hasProfileFilters = hasText(degree)
            || hasText(school)
            || hasText(house)
            || hasText(religion)
            || hasText(caste)
            || hasText(placeOfOrigin)
            || minAge != null
            || maxAge != null;

        if (!hasProfileFilters) {
            return studentRepository.findAll(spec, pageable);
        }

        List<Student> scopedStudents = studentRepository.findAll(spec, Sort.by(direction, normalizedSortBy));
        if (scopedStudents.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Map<String, StudentProfile> profileByStudentId = studentProfileRepository.findAllById(
            scopedStudents.stream().map(Student::getId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(StudentProfile::getStudentId, profile -> profile));

        List<Student> filtered = scopedStudents.stream()
            .filter(student -> matchProfileFilters(
                student,
                profileByStudentId.get(student.getId()),
                degree,
                school,
                house,
                religion,
                caste,
                placeOfOrigin,
                minAge,
                maxAge
            ))
            .toList();

        int from = Math.min(pageable.getOffset() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pageable.getOffset(), filtered.size());
        int to = Math.min(from + pageable.getPageSize(), filtered.size());
        List<Student> content = from >= to ? List.of() : filtered.subList(from, to);
        return new PageImpl<>(content, pageable, filtered.size());
    }

    private boolean matchProfileFilters(Student student,
                                        StudentProfile profile,
                                        String degree,
                                        String school,
                                        String house,
                                        String religion,
                                        String caste,
                                        String placeOfOrigin,
                                        Integer minAge,
                                        Integer maxAge) {
        if (hasText(degree) && !containsIgnoreCase(firstNonBlank(profile == null ? null : profile.getCourse(), student.getCourse()), degree)) {
            return false;
        }

        if (hasText(school) && !containsIgnoreCase(profile == null ? null : profile.getCollege(), school)) {
            return false;
        }

        if (hasText(house) && !containsIgnoreCase(profile == null ? null : profile.getHouse(), house)) {
            return false;
        }

        if (hasText(religion) && !containsIgnoreCase(profile == null ? null : profile.getReligion(), religion)) {
            return false;
        }

        if (hasText(caste) && !containsIgnoreCase(profile == null ? null : profile.getCaste(), caste)) {
            return false;
        }

        if (hasText(placeOfOrigin) && !containsIgnoreCase(profile == null ? null : profile.getPlaceOfOrigin(), placeOfOrigin)) {
            return false;
        }

        if (minAge != null || maxAge != null) {
            LocalDate dob = profile == null ? null : profile.getDob();
            if (dob == null) {
                return false;
            }
            int age = LocalDate.now().getYear() - dob.getYear();
            if (dob.plusYears(age).isAfter(LocalDate.now())) {
                age--;
            }
            if (minAge != null && age < minAge) {
                return false;
            }
            if (maxAge != null && age > maxAge) {
                return false;
            }
        }

        return true;
    }

    private boolean containsIgnoreCase(String value, String token) {
        if (!hasText(token)) {
            return true;
        }
        if (!hasText(value)) {
            return false;
        }
        return value.toLowerCase().contains(token.trim().toLowerCase());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
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
        if (students == null || students.isEmpty()) {
            return averages;
        }

        Map<String, List<Enrollment>> enrollmentsByStudentId = enrollmentRepository.findByStudentIdIn(
                students.stream().map(Student::getId).collect(Collectors.toList())
        ).stream().collect(Collectors.groupingBy(enrollment -> enrollment.getStudent() == null ? null : enrollment.getStudent().getId()));

        for (Student student : students) {
            List<Enrollment> enrollments = enrollmentsByStudentId.getOrDefault(student.getId(), List.of());
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

    @Transactional
    public Student save(Student student) {
        Student studentToSave = java.util.Objects.requireNonNull(student, "Student must not be null");
        String studentId = java.util.Objects.requireNonNull(studentToSave.getId(), "Student id must not be null");
        String derivedEmail = deriveStudentEmail(studentId);

        if (studentToSave.getEmail() == null || studentToSave.getEmail().isBlank()) {
            studentToSave.setEmail(derivedEmail);
        }
        studentToSave.setGender(StudentFieldDerivationUtils.inferGender(studentToSave.getName(), studentToSave.getGender()));
        assignClassAndBatchGroups(studentToSave);

        if (studentToSave.getUser() == null) {
            User user = userRepository.findByUsername(studentId)
                    .orElseGet(User::new);
            user.setUsername(studentId);
            user.setPassword(passwordEncoder.encode(studentId));
            user.setRole(Role.STUDENT);
            user.setIsFirstLogin(true);
            user.setIsActive(true);
            try {
                studentToSave.setUser(userRepository.save(user));
            } catch (DataIntegrityViolationException ex) {
                User existingUser = userRepository.findByUsername(studentId)
                        .orElseThrow(() -> ex);
                studentToSave.setUser(existingUser);
            }
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
                user.setIsFirstLogin(true);
            }
            if (user.getIsActive() == null) {
                user.setIsActive(true);
            }
            studentToSave.setUser(userRepository.save(user));
        }

        Student savedStudent = java.util.Objects.requireNonNull(studentRepository.save(studentToSave), "Saved student must not be null");
        upsertStudentProfile(savedStudent);
        analyticsRealtimeNotifier.notifyStudentAdded(savedStudent.getId(), savedStudent.getName());
        analyticsCacheService.evictAnalyticsCaches();
        return savedStudent;
    }

    @Transactional
    public int recomputeCohortsForAllStudents() {
        List<Student> students = studentRepository.findAll();
        int updated = 0;

        for (Student student : students) {
            String oldClassGroup = student.getClassGroup();
            String oldBatchGroup = student.getBatchGroup();

            assignClassAndBatchGroups(student);

            if (!java.util.Objects.equals(oldClassGroup, student.getClassGroup())
                    || !java.util.Objects.equals(oldBatchGroup, student.getBatchGroup())) {
                studentRepository.save(student);
                upsertStudentProfile(student);
                updated++;
            }
        }

        return updated;
    }

    @Transactional
    public void deleteById(String id) {
        String studentId = java.util.Objects.requireNonNull(id, "Student id must not be null");
        studentRepository.findById(studentId).ifPresent(student -> {
            deleteStudentOwnedData(studentId);
            studentRepository.delete(student);

            User user = student.getUser();
            if (user != null) {
                userRepository.delete(user);
            }
        });
        analyticsCacheService.evictAnalyticsCaches();
    }

    @Transactional
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

        dto.setGender(StudentFieldDerivationUtils.inferGender(student.getName(), student.getGender()));
        dto.setDob(student.getDob() != null ? student.getDob().toString() : null);
        dto.setAddress(student.getAddress());

        dto.setCourse(student.getCourse());
        dto.setDepartment(student.getDepartment());
        dto.setSemester(student.getSemester());
        if (student.getSection() != null && !student.getSection().isBlank()) {
            dto.setDepartment((student.getDepartment() == null || student.getDepartment().isBlank())
                ? "Section " + student.getSection()
                : student.getDepartment() + " (Section " + student.getSection() + ")");
        }
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
        profile.setGender(StudentFieldDerivationUtils.inferGender(student.getName(), student.getGender()));
        profile.setPhone(student.getPhone());
        String universityEmail = student.getEmail() == null || student.getEmail().isBlank()
            ? deriveStudentEmail(student.getId())
            : student.getEmail();
        profile.setUniversityEmail(universityEmail);
        profile.setEmail(universityEmail);
        if (profile.getPersonalEmail() == null || profile.getPersonalEmail().isBlank()) {
            profile.setPersonalEmail(null);
        }
        profile.setAddress(student.getAddress());
        profile.setCourse(student.getCourse());
        profile.setDepartment(student.getDepartment());
        profile.setSemester(student.getSemester());
        if (student.getSection() != null && !student.getSection().isBlank()) {
            profile.setSection(student.getSection());
        } else if (student.getClassGroup() != null && student.getBatchGroup() != null) {
            profile.setSection(student.getClassGroup() + " / " + student.getBatchGroup());
        }
        profile.setAdmissionYear(parseBatchYear(student.getEnrollmentYear()));
        profile.setCollege(StudentFieldDerivationUtils.resolveCollegeName(profile.getCollege(), student.getCourse()));
        profile.setPassingYear(StudentFieldDerivationUtils.derivePassingYear(student.getCourse(), profile.getAdmissionYear(), profile.getPassingYear()));
        profile.setValidUpto(StudentFieldDerivationUtils.deriveValidUpto(student.getCourse(), profile.getAdmissionYear(), profile.getPassingYear(), null));
        profile.setIdCardNumber("BU-" + student.getId());
        profile.setUpdatedBy("Admin Create");

        studentProfileRepository.save(profile);
    }

    private void deleteStudentOwnedData(String studentId) {
        enrollmentRepository.deleteByStudentId(studentId);
        studentDocumentRepository.deleteByStudentId(studentId);
        studentLocationRepository.deleteByStudentId(studentId);
        academicRecordRepository.deleteByStudentId(studentId);
        attendanceRepository.deleteByStudentId(studentId);
        faceDataRepository.deleteByStudentId(studentId);
        studentTaskRepository.deleteByStudentId(studentId);
        dietLogRepository.deleteByStudentId(studentId);
        studentProfileRepository.deleteByStudentId(studentId);
    }

    private Integer parseBatchYear(String batch) {
        if (batch == null || batch.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(batch.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void assignClassAndBatchGroups(Student student) {
        Integer serial = extractStudentSerial(student.getId());
        if (serial == null || serial <= 0) {
            return;
        }

        int classNumber = ((serial - 1) / CLASS_SIZE) + 1;
        int batchNumber = (((serial - 1) % CLASS_SIZE) / BATCH_SIZE) + 1;

        student.setClassGroup("Class " + classNumber);
        student.setBatchGroup("Batch " + batchNumber);
    }

    private Integer extractStudentSerial(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return null;
        }

        String normalized = studentId.trim().toUpperCase();
        String digitsOnly = normalized.replaceAll("\\D+", "");
        if (digitsOnly.isBlank()) {
            return null;
        }

        String serialCandidate = null;
        String year = parseEnrollmentYear(normalized);
        if (year != null && digitsOnly.startsWith(year) && digitsOnly.length() > year.length()) {
            serialCandidate = digitsOnly.substring(year.length());
        }

        if (serialCandidate == null || serialCandidate.isBlank()) {
            Matcher matcher = Pattern.compile("(\\d{2,4})$").matcher(normalized);
            if (matcher.find()) {
                serialCandidate = matcher.group(1);
            }
        }

        if (serialCandidate == null || serialCandidate.isBlank() || serialCandidate.replace("0", "").isBlank()) {
            Matcher matcher = TRAILING_NUMBER_PATTERN.matcher(normalized);
            if (matcher.find()) {
                serialCandidate = matcher.group(1);
            }
        }

        if (serialCandidate == null || serialCandidate.isBlank()) {
            return null;
        }

        serialCandidate = serialCandidate.replaceFirst("^0+(?!$)", "");
        if (serialCandidate.length() > 4) {
            serialCandidate = serialCandidate.substring(serialCandidate.length() - 4);
        }

        try {
            return Integer.parseInt(serialCandidate);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String parseEnrollmentYear(String enrollment) {
        if (enrollment == null || enrollment.isBlank()) {
            return null;
        }

        String normalized = enrollment.trim().toUpperCase();
        Matcher year4 = ENROLLMENT_YEAR_4_PATTERN.matcher(normalized);
        if (year4.find()) {
            return year4.group(1);
        }

        Matcher year2Prefix = ENROLLMENT_YEAR_2_PREFIX_PATTERN.matcher(normalized);
        if (year2Prefix.matches()) {
            int yy = Integer.parseInt(year2Prefix.group(1));
            int fullYear = yy <= 69 ? 2000 + yy : 1900 + yy;
            return String.valueOf(fullYear);
        }

        String digits = normalized.replaceAll("\\D+", "");
        if (digits.length() >= 4) {
            String candidate = digits.substring(0, 4);
            try {
                int year = Integer.parseInt(candidate);
                if (year >= 1990 && year <= 2100) {
                    return candidate;
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
