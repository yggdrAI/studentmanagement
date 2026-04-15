package com.sms.config;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.sms.model.ClassSession;
import com.sms.model.Course;
import com.sms.model.Enrollment;
import com.sms.model.Role;
import com.sms.model.Student;
import com.sms.model.StudentTask;
import com.sms.model.TaskItem;
import com.sms.model.Teacher;
import com.sms.model.User;
import com.sms.repository.ClassSessionRepository;
import com.sms.repository.CourseRepository;
import com.sms.repository.EnrollmentRepository;
import com.sms.repository.StudentRepository;
import com.sms.repository.StudentTaskRepository;
import com.sms.repository.TaskItemRepository;
import com.sms.repository.TeacherRepository;
import com.sms.repository.UserRepository;

@Component
public class DemoDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TaskItemRepository taskItemRepository;
    private final StudentTaskRepository studentTaskRepository;
    private final ClassSessionRepository classSessionRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataLoader(UserRepository userRepository,
                          StudentRepository studentRepository,
                          TeacherRepository teacherRepository,
                          CourseRepository courseRepository,
                          EnrollmentRepository enrollmentRepository,
                          TaskItemRepository taskItemRepository,
                          StudentTaskRepository studentTaskRepository,
                          ClassSessionRepository classSessionRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
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
        if (userRepository.count() > 0) {
            return;
        }

        User admin = createUser("admin", "1234", Role.ADMIN);
        User teacherUser = createUser("teacher", "1234", Role.TEACHER);
        User studentUser = createUser("student", "1234", Role.STUDENT);

        userRepository.save(admin);
        userRepository.save(teacherUser);
        userRepository.save(studentUser);

        Teacher teacher = new Teacher();
        teacher.setName("Dr. Rahul Sharma");
        teacher.setEmail("rahul.sharma@sms.com");
        teacher.setUser(teacherUser);
        teacher = teacherRepository.save(teacher);

        Student student = new Student("S-1001", "Aarav Patel");
        student.setUser(studentUser);
        student.setEmail("aarav.patel@bennett.edu.in");
        student.setPhone("+91-9876543210");
        student.setGender("Male");
        student.setDob(LocalDate.of(2005, 7, 14));
        student.setAddress("Noida, Uttar Pradesh, India");
        student.setCourse("B.Tech Computer Science and Engineering");
        student.setDepartment("Computer Science");
        student.setSemester("Semester 4");
        student.setRollNumber("CSE24-1107");
        student.setEnrollmentYear("2024");
        student.setProfileImageUrl(null);
        student = studentRepository.save(student);

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

    private User createUser(String username, String password, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return user;
    }
}
