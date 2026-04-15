package com.sms.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sms.dto.dashboard.AssignTeacherRequest;
import com.sms.dto.dashboard.ClassDTO;
import com.sms.dto.dashboard.CourseProgressDto;
import com.sms.dto.dashboard.DashboardDTO;
import com.sms.dto.dashboard.CreateSubjectRequest;
import com.sms.dto.dashboard.CreateTaskRequest;
import com.sms.dto.dashboard.DashboardResponse;
import com.sms.dto.dashboard.EnrollStudentRequest;
import com.sms.dto.dashboard.ScheduleClassRequest;
import com.sms.dto.dashboard.StudentProgressViewDto;
import com.sms.dto.dashboard.SubjectDTO;
import com.sms.dto.dashboard.TaskDto;
import com.sms.dto.dashboard.UpcomingClassDto;
import com.sms.model.ClassSession;
import com.sms.model.Course;
import com.sms.model.Enrollment;
import com.sms.model.Student;
import com.sms.model.StudentTask;
import com.sms.model.TaskItem;
import com.sms.model.TaskStatus;
import com.sms.model.Teacher;
import com.sms.repository.ClassSessionRepository;
import com.sms.repository.CourseRepository;
import com.sms.repository.EnrollmentRepository;
import com.sms.repository.StudentRepository;
import com.sms.repository.StudentTaskRepository;
import com.sms.repository.TaskItemRepository;
import com.sms.repository.TeacherRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class DashboardService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TaskItemRepository taskItemRepository;
    private final StudentTaskRepository studentTaskRepository;
    private final ClassSessionRepository classSessionRepository;

    public DashboardService(StudentRepository studentRepository,
                TeacherRepository teacherRepository,
                CourseRepository courseRepository,
                            EnrollmentRepository enrollmentRepository,
                            TaskItemRepository taskItemRepository,
                StudentTaskRepository studentTaskRepository,
                            ClassSessionRepository classSessionRepository) {
        this.studentRepository = studentRepository;
    this.teacherRepository = teacherRepository;
    this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.taskItemRepository = taskItemRepository;
    this.studentTaskRepository = studentTaskRepository;
        this.classSessionRepository = classSessionRepository;
    }

    @Cacheable(value = "dashboardSummary", key = "#studentId")
    @Transactional(readOnly = true)
    public DashboardResponse getStudentDashboard(String studentId) {
        Student student = studentRepository.findById(Objects.requireNonNull(studentId, "studentId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Student not found: " + studentId));

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);

    List<Long> courseIds = enrollments.stream()
        .map(enrollment -> enrollment.getCourse().getId())
        .toList();

    List<TaskItem> tasks = courseIds.isEmpty() ? List.of() : taskItemRepository.findByCourseIdIn(courseIds);

    Set<Long> completedTaskIds = loadCompletedTaskIds(studentId, courseIds);

        DashboardResponse response = new DashboardResponse();
        response.setStudentId(student.getId());
        response.setStudentName(student.getName());

        List<CourseProgressDto> courseDtos = enrollments.stream().map(enrollment -> {
        Course course = enrollment.getCourse();
            CourseProgressDto dto = new CourseProgressDto();
        dto.setCourseId(course.getId());
        dto.setCourseCode(course.getCode());
        dto.setCourseName(course.getCourseName());
        dto.setFacultyName(course.getTeacher() != null ? course.getTeacher().getName() : "N/A");
        dto.setCredits(course.getCredits());
        dto.setProgressPercent(round(calculateProgress(studentId, course.getId())));
            return dto;
        }).toList();

        List<TaskDto> taskDtos = tasks.stream().map(task -> {
            TaskDto dto = new TaskDto();
            dto.setId(task.getId());
            dto.setTitle(task.getTitle());
            dto.setDueAt(task.getDueAt());
        dto.setStatus(completedTaskIds.contains(task.getId()) ? TaskStatus.COMPLETED : TaskStatus.PENDING);
            dto.setCourseCode(task.getCourse() != null ? task.getCourse().getCode() : null);
            return dto;
        }).toList();

    List<UpcomingClassDto> upcomingDtos = getUpcomingClasses(studentId);

        int totalCredits = enrollments.stream()
                .map(Enrollment::getCourse)
                .map(Course::getCredits)
                .filter(credits -> credits != null)
                .mapToInt(Integer::intValue)
                .sum();

    double overallProgress = enrollments.isEmpty() ? 0.0 : enrollments.stream()
        .mapToDouble(enrollment -> calculateProgress(studentId, enrollment.getCourse().getId()))
        .average()
        .orElse(0.0);

    double taskCompletionRate = tasks.isEmpty() ? 0.0
        : (completedTaskIds.size() * 100.0) / tasks.size();

        response.setTotalCredits(totalCredits);
        response.setOverallProgress(round(overallProgress));
        response.setTaskCompletionRate(round(taskCompletionRate));
        response.setCourses(courseDtos);
        response.setTasks(taskDtos);
        response.setUpcomingClasses(upcomingDtos);

        return response;
    }

    @Cacheable(value = "dashboardSummary", key = "#studentId + ':v2'")
    @Transactional(readOnly = true)
    public DashboardDTO buildDashboard(String studentId) {
        Student student = studentRepository.findById(Objects.requireNonNull(studentId, "studentId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Student not found: " + studentId));

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        List<Long> subjectIds = enrollments.stream()
                .map(enrollment -> enrollment.getCourse().getId())
                .toList();

        List<TaskItem> tasks = subjectIds.isEmpty() ? List.of() : taskItemRepository.findByCourseIdIn(subjectIds);
        Set<Long> completedTaskIds = loadCompletedTaskIds(studentId, subjectIds);

        List<SubjectDTO> subjects = enrollments.stream().map(enrollment -> {
            Course course = enrollment.getCourse();
            SubjectDTO dto = new SubjectDTO();
            dto.setSubjectId(course.getId());
            dto.setSubjectCode(course.getCode());
            dto.setSubjectName(course.getCourseName());
            dto.setFacultyName(course.getTeacher() != null ? course.getTeacher().getName() : "N/A");
            dto.setCredits(course.getCredits());
            dto.setProgressPercent(round(calculateProgress(studentId, course.getId())));
            return dto;
        }).toList();

        List<TaskDto> taskDtos = tasks.stream().map(task -> {
            TaskDto dto = new TaskDto();
            dto.setId(task.getId());
            dto.setTitle(task.getTitle());
            dto.setCourseCode(task.getCourse() != null ? task.getCourse().getCode() : null);
            dto.setDueAt(task.getDueAt());
            dto.setStatus(completedTaskIds.contains(task.getId()) ? TaskStatus.COMPLETED : TaskStatus.PENDING);
            return dto;
        }).toList();

        List<ClassDTO> classDtos = classSessionRepository
                .findTop10ByStudentIdAndStartsAtAfterOrderByStartsAtAsc(studentId, LocalDateTime.now())
                .stream()
                .map(session -> {
                    ClassDTO dto = new ClassDTO();
                    dto.setClassId(session.getId());
                    dto.setTitle(session.getTitle());
                    dto.setRoom(session.getRoom());
                    dto.setStartsAt(session.getStartsAt());
                    dto.setEndsAt(session.getEndsAt());
                    dto.setSubjectCode(session.getCourse() != null ? session.getCourse().getCode() : null);
                    dto.setFacultyName(session.getCourse() != null && session.getCourse().getTeacher() != null
                            ? session.getCourse().getTeacher().getName()
                            : "N/A");
                    return dto;
                })
                .toList();

        int totalCredits = enrollments.stream()
                .map(Enrollment::getCourse)
                .map(Course::getCredits)
                .filter(credits -> credits != null)
                .mapToInt(Integer::intValue)
                .sum();

        int totalTasks = tasks.size();
        int completedTasks = completedTaskIds.size();
        double overallProgress = totalTasks == 0 ? 0.0 : (completedTasks * 100.0) / totalTasks;

        DashboardDTO dto = new DashboardDTO();
        dto.setStudentId(student.getId());
        dto.setStudentName(student.getName());
        dto.setTotalCredits(totalCredits);
        dto.setTotalTasks(totalTasks);
        dto.setCompletedTasks(completedTasks);
        dto.setOverallProgress(round(overallProgress));
        dto.setSubjects(subjects);
        dto.setTasks(taskDtos);
        dto.setUpcomingClasses(classDtos);
        return dto;
    }

    @Cacheable(value = "upcomingClasses", key = "#studentId")
    @Transactional(readOnly = true)
    public List<UpcomingClassDto> getUpcomingClasses(String studentId) {
        return classSessionRepository
                .findTop10ByStudentIdAndStartsAtAfterOrderByStartsAtAsc(studentId, LocalDateTime.now())
                .stream()
                .map(session -> {
                    UpcomingClassDto dto = new UpcomingClassDto();
                    dto.setSessionId(session.getId());
                    dto.setTitle(session.getTitle());
                    dto.setRoom(session.getRoom());
                    dto.setStartsAt(session.getStartsAt());
                    dto.setEndsAt(session.getEndsAt());
                    dto.setCourseCode(session.getCourse() != null ? session.getCourse().getCode() : null);
                    dto.setFacultyName(session.getCourse() != null && session.getCourse().getTeacher() != null
                            ? session.getCourse().getTeacher().getName()
                            : "N/A");
                    return dto;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public double calculateProgress(String studentId, Long subjectId) {
        long total = taskItemRepository.countByCourseId(subjectId);
        long completed = studentTaskRepository.countByStudentIdAndTaskCourseIdAndCompletedTrue(studentId, subjectId);

        if (total == 0) {
            return 0.0;
        }
        return (completed * 100.0) / total;
    }

    @Transactional
    @CacheEvict(value = {"dashboardSummary", "upcomingClasses"}, key = "#studentId")
    public TaskDto markTaskCompleted(String studentId, Long taskId) {
        TaskItem taskItem = taskItemRepository.findById(Objects.requireNonNull(taskId, "taskId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));

        if (!enrollmentRepository.existsByStudentIdAndCourseId(studentId, taskItem.getCourse().getId())) {
            throw new AccessDeniedException("Task does not belong to student " + studentId);
        }

        StudentTask studentTask = studentTaskRepository.findByStudentIdAndTaskId(studentId, taskId)
                .orElseGet(() -> {
                    StudentTask created = new StudentTask();
                        created.setStudent(studentRepository.getReferenceById(
                            Objects.requireNonNull(studentId, "studentId is required")
                        ));
                    created.setTask(taskItem);
                    return created;
                });
        studentTask.setCompleted(true);
        studentTaskRepository.save(studentTask);

        TaskDto dto = new TaskDto();
        dto.setId(taskItem.getId());
        dto.setTitle(taskItem.getTitle());
        dto.setStatus(TaskStatus.COMPLETED);
        dto.setDueAt(taskItem.getDueAt());
        dto.setCourseCode(taskItem.getCourse() != null ? taskItem.getCourse().getCode() : null);
        return dto;
    }

    @Transactional(readOnly = true)
    public Student resolveStudentByUsername(String username) {
        return studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found for user: " + username));
    }

    @Transactional(readOnly = true)
    public Teacher resolveTeacherByUsername(String username) {
        return teacherRepository.findByUserUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Teacher profile not found for user: " + username));
    }

    @Transactional
    public Course createSubject(String teacherUsername, CreateSubjectRequest request) {
        Teacher teacher = resolveTeacherByUsername(teacherUsername);

        Course course = new Course();
        course.setCode(request.getCode());
        course.setCourseName(request.getCourseName());
        course.setCredits(request.getCredits());
        course.setTeacher(teacher);
        return courseRepository.save(course);
    }

    @Transactional
    public TaskItem createTask(String teacherUsername, CreateTaskRequest request) {
        Teacher teacher = resolveTeacherByUsername(teacherUsername);
        Course course = courseRepository.findById(Objects.requireNonNull(request.getCourseId(), "courseId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Subject not found: " + request.getCourseId()));

        if (course.getTeacher() == null || !course.getTeacher().getId().equals(teacher.getId())) {
            throw new AccessDeniedException("Teacher does not own subject " + request.getCourseId());
        }

        TaskItem taskItem = new TaskItem();
        taskItem.setCourse(course);
        taskItem.setTitle(request.getTitle());
        taskItem.setDescription(request.getDescription());
        taskItem.setDueAt(request.getDueAt());
        taskItem.setCreatedBy(teacher);
        return taskItemRepository.save(taskItem);
    }

    @Transactional(readOnly = true)
    public List<StudentProgressViewDto> getSubjectProgress(String teacherUsername, Long subjectId) {
        Teacher teacher = resolveTeacherByUsername(teacherUsername);
        Course course = courseRepository.findById(Objects.requireNonNull(subjectId, "subjectId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Subject not found: " + subjectId));

        if (course.getTeacher() == null || !course.getTeacher().getId().equals(teacher.getId())) {
            throw new AccessDeniedException("Teacher does not own subject " + subjectId);
        }

        return enrollmentRepository.findByCourseId(subjectId).stream().map(enrollment -> {
            StudentProgressViewDto dto = new StudentProgressViewDto();
            dto.setStudentId(enrollment.getStudent().getId());
            dto.setStudentName(enrollment.getStudent().getName());
            dto.setProgressPercent(round(calculateProgress(enrollment.getStudent().getId(), subjectId)));
            return dto;
        }).toList();
    }

    @Transactional
    public int scheduleClass(String teacherUsername, ScheduleClassRequest request) {
        Teacher teacher = resolveTeacherByUsername(teacherUsername);
        Course course = courseRepository.findById(Objects.requireNonNull(request.getCourseId(), "courseId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Subject not found: " + request.getCourseId()));

        if (course.getTeacher() == null || !course.getTeacher().getId().equals(teacher.getId())) {
            throw new AccessDeniedException("Teacher does not own subject " + request.getCourseId());
        }

        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(request.getCourseId());
        for (Enrollment enrollment : enrollments) {
            ClassSession session = new ClassSession();
            session.setCourse(course);
            session.setStudent(enrollment.getStudent());
            session.setTitle(request.getTitle());
            session.setRoom(request.getRoom());
            session.setStartsAt(request.getStartsAt());
            session.setEndsAt(request.getEndsAt());
            classSessionRepository.save(session);
        }

        return enrollments.size();
    }

    @Transactional
    public Enrollment enrollStudent(EnrollStudentRequest request) {
        if (enrollmentRepository.existsByStudentIdAndCourseId(request.getStudentId(), request.getCourseId())) {
            throw new IllegalArgumentException("Student is already enrolled in subject");
        }

        Student student = studentRepository.findById(Objects.requireNonNull(request.getStudentId(), "studentId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Student not found: " + request.getStudentId()));
        Course course = courseRepository.findById(Objects.requireNonNull(request.getCourseId(), "courseId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Subject not found: " + request.getCourseId()));

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        return enrollmentRepository.save(enrollment);
    }

    @Transactional
    public Course assignTeacher(AssignTeacherRequest request) {
        Course course = courseRepository.findById(Objects.requireNonNull(request.getCourseId(), "courseId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Subject not found: " + request.getCourseId()));
        Teacher teacher = teacherRepository.findById(Objects.requireNonNull(request.getTeacherId(), "teacherId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found: " + request.getTeacherId()));

        course.setTeacher(teacher);
        return courseRepository.save(course);
    }

    private Set<Long> loadCompletedTaskIds(String studentId, List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Set.of();
        }

        return studentTaskRepository.findByStudentIdAndTaskCourseIdInAndCompletedTrue(studentId, courseIds)
                .stream()
                .map(studentTask -> studentTask.getTask().getId())
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
