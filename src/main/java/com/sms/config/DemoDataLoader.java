package com.sms.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.sms.model.AcademicRecord;
import com.sms.model.ClassSession;
import com.sms.model.Course;
import com.sms.model.Enrollment;
import com.sms.model.Role;
import com.sms.model.Student;
import com.sms.model.StudentDocument;
import com.sms.model.StudentProfile;
import com.sms.model.StudentTask;
import com.sms.model.TaskItem;
import com.sms.model.Teacher;
import com.sms.model.User;
import com.sms.repository.AcademicRecordRepository;
import com.sms.repository.ClassSessionRepository;
import com.sms.repository.CourseRepository;
import com.sms.repository.EnrollmentRepository;
import com.sms.repository.StudentDocumentRepository;
import com.sms.repository.StudentProfileRepository;
import com.sms.repository.StudentRepository;
import com.sms.repository.StudentTaskRepository;
import com.sms.repository.TaskItemRepository;
import com.sms.repository.TeacherRepository;
import com.sms.repository.UserRepository;
import com.sms.service.StudentFieldDerivationUtils;

@Component
public class DemoDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentDocumentRepository studentDocumentRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TaskItemRepository taskItemRepository;
    private final StudentTaskRepository studentTaskRepository;
    private final ClassSessionRepository classSessionRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataLoader(UserRepository userRepository,
                          StudentRepository studentRepository,
                          StudentProfileRepository studentProfileRepository,
                          StudentDocumentRepository studentDocumentRepository,
                          AcademicRecordRepository academicRecordRepository,
                          TeacherRepository teacherRepository,
                          CourseRepository courseRepository,
                          EnrollmentRepository enrollmentRepository,
                          TaskItemRepository taskItemRepository,
                          StudentTaskRepository studentTaskRepository,
                          ClassSessionRepository classSessionRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentDocumentRepository = studentDocumentRepository;
        this.academicRecordRepository = academicRecordRepository;
        this.teacherRepository = teacherRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.taskItemRepository = taskItemRepository;
        this.studentTaskRepository = studentTaskRepository;
        this.classSessionRepository = classSessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        ensureAdminLogin();

        if (studentRepository.count() == 0 && teacherRepository.count() == 0) {
            seedInitialAcademicData();
        }

        ensureStudentLoginPolicy();
        ensureTeacherLoginPolicy();
        ensureTeacherAliasCredentials();
        ensureDemoTeacherEntity();
        ensureTeacherHasAtLeastOneSubject();
    }

    private void ensureDemoTeacherEntity() {
        if (!teacherRepository.findAll().isEmpty()) {
            return;
        }

        User teacherUser = userRepository.findFirstByRoleOrderByIdAsc(Role.TEACHER)
                .orElseGet(() -> createUser("temp-teacher", "temp-teacher", Role.TEACHER));
        teacherUser = userRepository.save(teacherUser);

        Teacher teacher = new Teacher();
        teacher.setFirstName("Rahul");
        teacher.setLastName("Sharma");
        teacher.setName("Dr. Rahul Sharma");
        teacher.setEmail(teacherUser.getEmail() != null && !teacherUser.getEmail().isBlank()
                ? teacherUser.getEmail()
                : "rahul.sharma@sms.com");
        teacher.setPhone(teacherUser.getPhone());
        teacher.setEmployeeId(teacherUser.getId() == null ? "TEACHER-DEMO" : "TEACHER-" + teacherUser.getId());
        teacher.setDepartment("Computer Science");
        teacher.setDesignation("Assistant Professor");
        teacher.setQualification("M.Tech");
        teacher.setSpecialization("Programming Languages");
        teacher.setExperienceYears(5);
        teacher.setStatus("ACTIVE");
        teacher.setUser(teacherUser);
        teacherRepository.save(teacher);
    }

    private void ensureTeacherHasAtLeastOneSubject() {
        List<Student> students = studentRepository.findAll();
        for (Teacher teacher : teacherRepository.findAll()) {
            if (teacher.getId() == null) {
                continue;
            }
            boolean hasJavaCourse = courseRepository.findByTeacherId(teacher.getId()).stream()
                    .anyMatch(course -> course.getCourseName() != null
                            && course.getCourseName().equalsIgnoreCase("Java"));
            if (hasJavaCourse) {
                continue;
            }

            Course javaCourse = new Course();
            javaCourse.setCourseName("Java");
            javaCourse.setCode(resolveUniqueCourseCode("JAVA", teacher.getId()));
            javaCourse.setCredits(3);
            javaCourse.setTeacher(teacher);
            javaCourse = courseRepository.save(javaCourse);

            // Seed minimal enrollments so Teacher Attendance has a usable student list.
            for (Student student : students) {
                String studentId = student.getId();
                if (studentId == null || studentId.isBlank()) {
                    continue;
                }
                if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, javaCourse.getId())) {
                    continue;
                }
                Enrollment enrollment = new Enrollment();
                enrollment.setStudent(student);
                enrollment.setCourse(javaCourse);
                enrollment.setMarks(0.0);
                enrollmentRepository.save(enrollment);
            }
        }
    }

    private String resolveUniqueCourseCode(String base, Long teacherId) {
        String normalizedBase = (base == null || base.isBlank() ? "JAVA" : base.trim())
                .toUpperCase(Locale.ROOT);
        if (courseRepository.findByCode(normalizedBase).isEmpty()) {
            return normalizedBase;
        }

        String teacherCandidate = normalizedBase + "-" + teacherId;
        if (courseRepository.findByCode(teacherCandidate).isEmpty()) {
            return teacherCandidate;
        }

        for (int suffix = 2; suffix <= 50; suffix++) {
            String candidate = teacherCandidate + "-" + suffix;
            if (courseRepository.findByCode(candidate).isEmpty()) {
                return candidate;
            }
        }

        return teacherCandidate + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private void seedInitialAcademicData() {
        User teacherUser = createUser("temp-teacher", "temp-teacher", Role.TEACHER);
        teacherUser = userRepository.save(java.util.Objects.requireNonNull(teacherUser));

        Teacher teacher = new Teacher();
        teacher.setName("Dr. Rahul Sharma");
        teacher.setEmail("rahul.sharma@sms.com");
        teacher.setUser(teacherUser);
        teacher = teacherRepository.save(teacher);

        // Teacher login policy: username/password = teacher id (initial login)
        teacherUser.setUsername(String.valueOf(teacher.getId()));
        teacherUser.setPassword(passwordEncoder.encode(String.valueOf(teacher.getId())));
        userRepository.save(teacherUser);

        User studentUser = createUser("S25CSEU1006", "S25CSEU1006", Role.STUDENT);
        studentUser = userRepository.save(java.util.Objects.requireNonNull(studentUser));

        Student student = new Student("S25CSEU1006", "Bhavya Jain");
        student.setUser(studentUser);
        student.setEmail("S25CSEU1006@bennett.edu.in");
        student.setPhone("+91-7668464847");
        student.setGender(StudentFieldDerivationUtils.inferGender(student.getName(), "Female"));
        student.setDob(LocalDate.of(2007, 3, 15));
        student.setAddress("Delhi Road, Meerut");
        student.setCourse("Bachelor of Technology (Computer Science and Engineering)");
        student.setDepartment("CSE");
        student.setSemester("Semester 2");
        student.setRollNumber("S25CSEU1006");
        student.setEnrollmentYear("2025");
        student.setProfileImageUrl(null);
        student = studentRepository.save(student);

        StudentProfile profile = new StudentProfile();
        profile.setStudentId(student.getId());
        profile.setFullName("Bhavya Jain");
        profile.setEnrollmentNumber("S25CSEU1006");
        profile.setCollege(StudentFieldDerivationUtils.resolveCollegeName("Bennett University", student.getCourse()));
        profile.setCourse("Bachelor of Technology (Computer Science and Engineering)");
        profile.setDepartment("CSE");
        profile.setSemester("Semester 2");
        profile.setSection("A");
        profile.setPhone("+91-7668464847");
        profile.setEmail("S25CSEU1006@bennett.edu.in");
        profile.setBloodGroup("O+ve");
        profile.setDob(LocalDate.of(2007, 3, 15));
        profile.setGender(StudentFieldDerivationUtils.inferGender(student.getName(), "Female"));
        profile.setReligion("Hindu");
        profile.setGuardianName("Ashok Kumar Jain");
        profile.setGuardianPhone("+91-9999988888");
        profile.setAddress("Delhi Road, Meerut");
        profile.setAdmissionYear(2025);
        profile.setPassingYear(StudentFieldDerivationUtils.derivePassingYear(profile.getCourse(), profile.getAdmissionYear(), 2029));
        profile.setValidUpto(StudentFieldDerivationUtils.deriveValidUpto(profile.getCourse(), profile.getAdmissionYear(), profile.getPassingYear(), null));
        profile.setIdCardNumber("BU-2025-S25CSEU1006");
        profile.setUpdatedBy("System Seed");
        studentProfileRepository.save(profile);

        StudentDocument aadhaar = new StudentDocument();
        aadhaar.setStudentId(student.getId());
        aadhaar.setDocumentType("Aadhaar");
        aadhaar.setFileUrl("https://example.edu/docs/bhavya-jain-aadhaar.pdf");
        studentDocumentRepository.save(aadhaar);

        StudentDocument marksheet = new StudentDocument();
        marksheet.setStudentId(student.getId());
        marksheet.setDocumentType("Marksheet");
        marksheet.setFileUrl("https://example.edu/docs/bhavya-jain-marksheet.pdf");
        studentDocumentRepository.save(marksheet);

        AcademicRecord dsRecord = new AcademicRecord();
        dsRecord.setStudentId(student.getId());
        dsRecord.setSubject("Data Structures");
        dsRecord.setGrade(9.1);
        dsRecord.setAttendance(92.0);
        academicRecordRepository.save(dsRecord);

        AcademicRecord mathRecord = new AcademicRecord();
        mathRecord.setStudentId(student.getId());
        mathRecord.setSubject("Discrete Mathematics");
        mathRecord.setGrade(8.8);
        mathRecord.setAttendance(89.0);
        academicRecordRepository.save(mathRecord);

        Course algo = new Course();
        algo.setCode("CS301");
        algo.setCourseName("Algorithms");
        algo.setCredits(4);
        algo.setTeacher(teacher);
        algo = courseRepository.save(algo);

        Course dbms = new Course();
        dbms.setCode("CS305");
        dbms.setCourseName("Distributed Databases");
        dbms.setCredits(3);
        dbms.setTeacher(teacher);
        dbms = courseRepository.save(dbms);

        Enrollment enrollment1 = new Enrollment();
        enrollment1.setStudent(student);
        enrollment1.setCourse(algo);
        enrollment1.setMarks(82.0);
        enrollmentRepository.save(enrollment1);

        Enrollment enrollment2 = new Enrollment();
        enrollment2.setStudent(student);
        enrollment2.setCourse(dbms);
        enrollment2.setMarks(90.0);
        enrollmentRepository.save(enrollment2);

        TaskItem task1 = new TaskItem();
        task1.setTitle("Assignment 3");
        task1.setDescription("Dynamic programming practice set");
        task1.setDueAt(LocalDateTime.now().plusDays(2));
        task1.setCourse(algo);
        task1.setCreatedBy(teacher);
        task1 = taskItemRepository.save(task1);

        TaskItem task2 = new TaskItem();
        task2.setTitle("Quiz Revision");
        task2.setDescription("Review indexing and replication");
        task2.setDueAt(LocalDateTime.now().plusDays(1));
        task2.setCourse(dbms);
        task2.setCreatedBy(teacher);
        task2 = taskItemRepository.save(task2);

        StudentTask completedTask = new StudentTask();
        completedTask.setStudent(student);
        completedTask.setTask(task1);
        completedTask.setCompleted(true);
        studentTaskRepository.save(completedTask);

        StudentTask pendingTask = new StudentTask();
        pendingTask.setStudent(student);
        pendingTask.setTask(task2);
        pendingTask.setCompleted(false);
        studentTaskRepository.save(pendingTask);

        ClassSession session1 = new ClassSession();
        session1.setTitle("Algorithms Live Problem Solving");
        session1.setStartsAt(LocalDateTime.now().plusHours(5));
        session1.setEndsAt(LocalDateTime.now().plusHours(6));
        session1.setRoom("Lab A-12");
        session1.setStudent(student);
        session1.setCourse(algo);
        classSessionRepository.save(session1);

        ClassSession session2 = new ClassSession();
        session2.setTitle("Database Consistency Seminar");
        session2.setStartsAt(LocalDateTime.now().plusHours(28));
        session2.setEndsAt(LocalDateTime.now().plusHours(29));
        session2.setRoom("Room D-04");
        session2.setStudent(student);
        session2.setCourse(dbms);
        classSessionRepository.save(session2);
    }

    private void ensureAdminLogin() {
        User adminUser = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .findFirst()
                .orElseGet(() -> createUser("bhavya", "999", Role.ADMIN));

        adminUser.setUsername("bhavya");
        adminUser.setPassword(passwordEncoder.encode("999"));
        adminUser.setRole(Role.ADMIN);
        adminUser.setIsActive(true);
        if (adminUser.getIsFirstLogin() == null) {
            adminUser.setIsFirstLogin(false);
        }
        userRepository.save(adminUser);
    }

    private void ensureStudentLoginPolicy() {
        for (Student student : studentRepository.findAll()) {
            String studentId = student.getId();
            if (studentId == null || studentId.isBlank()) {
                continue;
            }

            User user = student.getUser();
            if (user == null) {
                user = findExistingUserByUsername(studentId).orElseGet(() -> {
                    User created = new User();
                    created.setRole(Role.STUDENT);
                    created.setTenantId(1L);
                    return created;
                });
            }

            user.setUsername(studentId);
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(studentId));
                user.setIsFirstLogin(true);
            }
            if (user.getIsFirstLogin() == null) {
                user.setIsFirstLogin(true);
            }
            user.setRole(Role.STUDENT);
            user.setIsActive(true);
            user = userRepository.save(user);

            if (student.getUser() == null || !user.getId().equals(student.getUser().getId())) {
                student.setUser(user);
                studentRepository.save(student);
            }
        }
    }

    private void ensureTeacherLoginPolicy() {
        for (Teacher teacher : teacherRepository.findAll()) {
            if (teacher.getId() == null) {
                continue;
            }
            String teacherId = String.valueOf(teacher.getId());

            User user = teacher.getUser();
            if (user == null) {
                user = findExistingUserByUsername(teacherId).orElseGet(() -> {
                    User created = new User();
                    created.setRole(Role.TEACHER);
                    created.setTenantId(1L);
                    return created;
                });
            }

            user.setUsername(teacherId);
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(teacherId));
                user.setIsFirstLogin(true);
            }
            if (user.getIsFirstLogin() == null) {
                user.setIsFirstLogin(true);
            }
            user.setRole(Role.TEACHER);
            user.setIsActive(true);
            user = userRepository.save(user);

            if (teacher.getUser() == null || !user.getId().equals(teacher.getUser().getId())) {
                teacher.setUser(user);
                teacherRepository.save(teacher);
            }
        }
    }

    private void ensureTeacherAliasCredentials() {
        userRepository.findFirstByRoleOrderByIdAsc(Role.TEACHER).ifPresent(user -> {
            user.setUsername("Teacher");
            user.setPassword(passwordEncoder.encode("1234"));
            user.setIsFirstLogin(true);
            user.setRole(Role.TEACHER);
            user.setIsActive(true);
            userRepository.save(user);
        });
    }

    private Optional<User> findExistingUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    private User createUser(String username, String password, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setIsActive(true);
        user.setIsFirstLogin(role != Role.ADMIN);
        return user;
    }
}
