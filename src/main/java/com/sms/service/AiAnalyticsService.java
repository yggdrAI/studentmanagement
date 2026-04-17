package com.sms.service;

import com.sms.model.Attendance;
import com.sms.model.Enrollment;
import com.sms.model.Student;
import com.sms.model.Teacher;
import com.sms.repository.AttendanceRepository;
import com.sms.repository.EnrollmentRepository;
import com.sms.repository.StudentRepository;
import com.sms.repository.TeacherRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiAnalyticsService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;

    public AiAnalyticsService(StudentRepository studentRepository,
                              TeacherRepository teacherRepository,
                              EnrollmentRepository enrollmentRepository,
                              AttendanceRepository attendanceRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Cacheable(cacheNames = "analytics-dashboard", key = "#role + ':' + #username + ':' + #course + ':' + #semester + ':' + #section + ':' + #from + ':' + #to")
    public Map<String, Object> buildDashboard(String role,
                                              String username,
                                              String course,
                                              String semester,
                                              String section,
                                              LocalDate from,
                                              LocalDate to) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(29);
        if (effectiveFrom.isAfter(effectiveTo)) {
            LocalDate temp = effectiveFrom;
            effectiveFrom = effectiveTo;
            effectiveTo = temp;
        }

        List<Student> scopedStudents = scopeStudents(role, username);
        List<Student> filteredStudents = scopedStudents.stream()
                .filter(student -> filterStudent(student, course, semester, section))
                .toList();

        List<Enrollment> allEnrollments = enrollmentRepository.findAll();
        List<Attendance> allAttendance = attendanceRepository.findAll();

        Set<String> studentIds = filteredStudents.stream().map(Student::getId).collect(Collectors.toSet());
        List<Enrollment> scopedEnrollments = allEnrollments.stream()
                .filter(enrollment -> enrollment.getStudent() != null && studentIds.contains(enrollment.getStudent().getId()))
                .toList();

        List<Attendance> scopedAttendance = allAttendance.stream()
                .filter(att -> att.getStudentId() != null && studentIds.contains(att.getStudentId()))
                .toList();

        Map<String, StudentStats> statsByStudent = buildStudentStats(filteredStudents, scopedEnrollments, scopedAttendance, effectiveFrom, effectiveTo);
        List<Map<String, Object>> smartCards = buildSmartCards(filteredStudents, statsByStudent, effectiveFrom, effectiveTo);
        List<Map<String, Object>> taggedStudents = buildTaggedStudents(filteredStudents, statsByStudent);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("scope", role == null ? "UNKNOWN" : role.toUpperCase(Locale.ROOT));
        response.put("timeRange", Map.of("from", effectiveFrom.toString(), "to", effectiveTo.toString()));
        response.put("metrics", buildMetrics(filteredStudents, statsByStudent, scopedAttendance));
        response.put("smartCards", smartCards);
        response.put("studentTags", taggedStudents);
        response.put("charts", buildCharts(filteredStudents, statsByStudent, scopedAttendance, scopedEnrollments, effectiveFrom, effectiveTo));
        response.put("activityFeed", buildActivityFeed(scopedAttendance, filteredStudents));
        response.put("recommendations", buildRecommendations(filteredStudents, statsByStudent));
        return response;
    }

    @Cacheable(cacheNames = "analytics-student-summary", key = "#studentId")
    public Map<String, Object> buildStudentAiSummary(String studentId) {
        Student student = studentRepository.findById(Objects.requireNonNull(studentId, "studentId is required"))
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        LocalDate now = LocalDate.now();
        LocalDate from = now.minusDays(90);

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        List<Attendance> attendance = attendanceRepository.findAll().stream()
                .filter(a -> studentId.equals(a.getStudentId()))
                .toList();

        StudentStats stats = buildStudentStats(List.of(student), enrollments, attendance, from, now).get(studentId);

        String summary = "Consistent performer";
        if (stats != null) {
            if (stats.avgMarks < 40 || stats.attendancePct < 75) {
                summary = "Needs improvement in attendance and marks";
            } else if (stats.trendDelta <= -8) {
                summary = "Performance trend is declining";
            } else if (stats.avgMarks > 80 && stats.attendancePct > 88) {
                summary = "High performer with excellent consistency";
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("studentName", student.getName());
        result.put("aiSummary", summary);
        result.put("performanceSeries", buildPerformanceSeries(studentId, attendance, enrollments));
        result.put("attendanceHeatmap", buildStudentAttendanceHeatmap(attendance));
        result.put("behaviorTrend", buildBehaviorTrend(attendance));
        return result;
    }

    @Cacheable(cacheNames = "analytics-live-snapshot")
    public Map<String, Object> buildLiveSnapshot() {
        LocalDate today = LocalDate.now();
        LocalDateTime activeThreshold = LocalDateTime.now().minusMinutes(15);

        List<Attendance> attendance = attendanceRepository.findAll();
        long activeStudents = attendance.stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(activeThreshold))
                .map(Attendance::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        long todayPresent = attendance.stream()
                .filter(a -> today.equals(a.getAttendanceDate()))
                .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()))
                .count();

        long todayTotal = attendance.stream()
                .filter(a -> today.equals(a.getAttendanceDate()))
                .count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", LocalDateTime.now().toString());
        data.put("activeStudents", activeStudents);
        data.put("liveAttendance", todayTotal == 0 ? 0.0 : round((todayPresent * 100.0) / todayTotal));
        data.put("todayEvents", todayTotal);
        data.put("totalStudents", studentRepository.count());
        data.put("recentFeed", buildActivityFeed(attendance, studentRepository.findAll()).stream().limit(10).toList());
        return data;
    }

    private List<Student> scopeStudents(String role, String username) {
        if (role == null) {
            return studentRepository.findAll();
        }

        String normalized = role.toUpperCase(Locale.ROOT);
        if ("STUDENT".equals(normalized)) {
            return studentRepository.findByUserUsername(username).map(List::of).orElse(List.of());
        }

        if ("TEACHER".equals(normalized)) {
            Teacher teacher = teacherRepository.findByUserUsername(username)
                    .orElse(null);
            if (teacher == null) {
                return List.of();
            }

            Set<String> ids = enrollmentRepository.findAll().stream()
                    .filter(e -> e.getCourse() != null && e.getCourse().getTeacher() != null)
                    .filter(e -> Objects.equals(e.getCourse().getTeacher().getId(), teacher.getId()))
                    .map(Enrollment::getStudent)
                    .filter(Objects::nonNull)
                    .map(Student::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            return studentRepository.findAll().stream()
                    .filter(student -> ids.contains(student.getId()))
                    .toList();
        }

        return studentRepository.findAll();
    }

    private boolean filterStudent(Student student, String course, String semester, String section) {
        if (student == null) {
            return false;
        }

        if (course != null && !course.isBlank() && !course.equalsIgnoreCase(nullSafe(student.getCourse()))) {
            return false;
        }
        if (semester != null && !semester.isBlank() && !semester.equalsIgnoreCase(nullSafe(student.getSemester()))) {
            return false;
        }
        if (section != null && !section.isBlank()) {
            String department = nullSafe(student.getDepartment());
            String maybeSection = department.length() >= 1 ? department.substring(0, 1).toUpperCase(Locale.ROOT) : "";
            if (!section.equalsIgnoreCase(maybeSection) && !section.equalsIgnoreCase(department)) {
                return false;
            }
        }
        return true;
    }

    private Map<String, StudentStats> buildStudentStats(List<Student> students,
                                                        List<Enrollment> enrollments,
                                                        List<Attendance> attendance,
                                                        LocalDate from,
                                                        LocalDate to) {
        Map<String, StudentStats> stats = new HashMap<>();

        Map<String, List<Enrollment>> enrollmentMap = enrollments.stream()
                .filter(e -> e.getStudent() != null && e.getStudent().getId() != null)
                .collect(Collectors.groupingBy(e -> e.getStudent().getId()));

        Map<String, List<Attendance>> attendanceMap = attendance.stream()
                .filter(a -> a.getStudentId() != null)
                .collect(Collectors.groupingBy(Attendance::getStudentId));

        LocalDate startCurrentWeek = to.minusDays(6);
        LocalDate startPreviousWeek = startCurrentWeek.minusDays(7);
        LocalDate endPreviousWeek = startCurrentWeek.minusDays(1);

        for (Student student : students) {
            String id = student.getId();
            List<Enrollment> studentEnrollments = enrollmentMap.getOrDefault(id, List.of());
            List<Attendance> studentAttendance = attendanceMap.getOrDefault(id, List.of()).stream()
                    .filter(a -> a.getAttendanceDate() != null)
                    .toList();

            double avgMarks = studentEnrollments.stream()
                    .map(Enrollment::getMarks)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            List<Attendance> windowAttendance = studentAttendance.stream()
                    .filter(a -> !a.getAttendanceDate().isBefore(from) && !a.getAttendanceDate().isAfter(to))
                    .toList();

            double attendancePct = calculatePresentPct(windowAttendance);
            double currentWeekAttendance = calculatePresentPct(studentAttendance.stream()
                    .filter(a -> !a.getAttendanceDate().isBefore(startCurrentWeek) && !a.getAttendanceDate().isAfter(to))
                    .toList());

            double previousWeekAttendance = calculatePresentPct(studentAttendance.stream()
                    .filter(a -> !a.getAttendanceDate().isBefore(startPreviousWeek) && !a.getAttendanceDate().isAfter(endPreviousWeek))
                    .toList());

            double trendDelta = currentWeekAttendance - previousWeekAttendance;
            boolean consistentDecline = detectConsistentDecline(studentAttendance, to);
            boolean irregularSpike = detectIrregularSpike(studentAttendance, to);
            boolean unusualActivity = detectUnusualActivity(studentAttendance, to);

            StudentStats s = new StudentStats();
            s.avgMarks = round(avgMarks);
            s.attendancePct = round(attendancePct);
            s.currentWeekAttendance = round(currentWeekAttendance);
            s.previousWeekAttendance = round(previousWeekAttendance);
            s.trendDelta = round(trendDelta);
            s.consistentDecline = consistentDecline;
            s.irregularSpike = irregularSpike;
            s.unusualActivity = unusualActivity;
            s.performanceScore = round((0.55 * s.avgMarks) + (0.45 * s.attendancePct));
            stats.put(id, s);
        }

        return stats;
    }

    private List<Map<String, Object>> buildSmartCards(List<Student> students,
                                                      Map<String, StudentStats> stats,
                                                      LocalDate from,
                                                      LocalDate to) {
        List<Map<String, Object>> cards = new ArrayList<>();

        List<Student> droppingAttendance = students.stream()
                .filter(s -> stats.containsKey(s.getId()))
                .filter(s -> stats.get(s.getId()).trendDelta < -5)
                .toList();

        cards.add(buildCard(
                "attendance-drop",
                "Attendance dropping for " + droppingAttendance.size() + " students this week",
                "Attendance trend comparison between previous and current week",
                "trend-down",
                "danger",
                droppingAttendance.stream().limit(12).map(Student::getName).toList()
        ));

        List<Student> topPerformers = students.stream()
                .filter(s -> stats.containsKey(s.getId()))
                .sorted(Comparator.comparingDouble((Student s) -> stats.get(s.getId()).performanceScore).reversed())
                .limit(5)
                .toList();

        double improvement = topPerformers.stream()
            .map(s -> stats.get(s.getId()))
                .filter(Objects::nonNull)
                .mapToDouble(s -> s.trendDelta)
                .average()
                .orElse(0.0);

        cards.add(buildCard(
                "top-performers",
                "Top 5 performers improved by " + round(Math.max(0.0, improvement)) + "%",
                "Top performers based on marks + attendance score",
                "spark",
                "success",
                topPerformers.stream().map(Student::getName).toList()
        ));

        List<Student> atRisk = students.stream()
                .filter(s -> stats.containsKey(s.getId()))
                .filter(s -> stats.get(s.getId()).avgMarks < 40 && stats.get(s.getId()).attendancePct < 75)
                .toList();

        cards.add(buildCard(
                "at-risk",
                atRisk.size() + " students at risk (low attendance + marks)",
                "Students breaching risk thresholds in selected period",
                "alert",
                "warning",
                atRisk.stream().map(Student::getName).toList()
        ));

        cards.add(buildCard(
                "range",
                "Analysis window: " + from + " to " + to,
                "AI engine compares trend shifts and threshold anomalies in this range",
                "calendar",
                "info",
                List.of("Thresholds: marks < 40", "Thresholds: attendance < 75%", "Pattern detection enabled")
        ));

        return cards;
    }

    private Map<String, Object> buildCard(String id,
                                          String title,
                                          String subtitle,
                                          String icon,
                                          String tone,
                                          List<String> details) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", id);
        card.put("title", title);
        card.put("subtitle", subtitle);
        card.put("icon", icon);
        card.put("tone", tone);
        card.put("details", details);
        return card;
    }

    private List<Map<String, Object>> buildTaggedStudents(List<Student> students, Map<String, StudentStats> stats) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Student student : students) {
            StudentStats s = stats.get(student.getId());
            if (s == null) {
                continue;
            }

            String tag;
            String glow;
            if (s.avgMarks < 40 || s.attendancePct < 75) {
                tag = "At Risk";
                glow = "risk";
            } else if (s.avgMarks > 80 && s.attendancePct > 88 && s.trendDelta >= 0) {
                tag = "High Performer";
                glow = "excellent";
            } else if (s.consistentDecline || s.trendDelta < -6) {
                tag = "Declining";
                glow = "warning";
            } else {
                tag = "Stable";
                glow = "stable";
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", student.getId());
            row.put("name", student.getName());
            row.put("course", nullSafe(student.getCourse()));
            row.put("semester", nullSafe(student.getSemester()));
            row.put("tag", tag);
            row.put("glow", glow);
            row.put("attendance", s.attendancePct);
            row.put("avgMarks", s.avgMarks);
            row.put("trendDelta", s.trendDelta);
            row.put("anomalies", buildAnomalyFlags(s));
            rows.add(row);
        }

        rows.sort(Comparator.comparing((Map<String, Object> r) -> String.valueOf(r.get("tag"))));
        return rows;
    }

    private List<String> buildAnomalyFlags(StudentStats s) {
        List<String> flags = new ArrayList<>();
        if (s.avgMarks < 40 && s.trendDelta < -5) {
            flags.add("Sudden drop in marks");
        }
        if (s.irregularSpike) {
            flags.add("Irregular attendance spikes");
        }
        if (s.unusualActivity) {
            flags.add("Unusual activity patterns");
        }
        if (s.consistentDecline) {
            flags.add("Consistent decline pattern");
        }
        return flags;
    }

    private Map<String, Object> buildMetrics(List<Student> students,
                                             Map<String, StudentStats> stats,
                                             List<Attendance> attendance) {
        long atRisk = students.stream()
                .filter(s -> stats.containsKey(s.getId()))
                .filter(s -> {
                    StudentStats x = stats.get(s.getId());
                    return x.avgMarks < 40 || x.attendancePct < 75;
                })
                .count();

        long highPerformer = students.stream()
                .filter(s -> stats.containsKey(s.getId()))
                .filter(s -> {
                    StudentStats x = stats.get(s.getId());
                    return x.avgMarks >= 80 && x.attendancePct >= 88;
                })
                .count();

        LocalDateTime activeCutoff = LocalDateTime.now().minusMinutes(15);
        long activeStudents = attendance.stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(activeCutoff))
                .map(Attendance::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return Map.of(
                "totalStudents", students.size(),
                "activeStudents", activeStudents,
                "atRiskStudents", atRisk,
                "highPerformers", highPerformer
        );
    }

    private Map<String, Object> buildCharts(List<Student> students,
                                            Map<String, StudentStats> stats,
                                            List<Attendance> attendance,
                                            List<Enrollment> enrollments,
                                            LocalDate from,
                                            LocalDate to) {
        Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("attendanceTrend", buildAttendanceTrend(attendance, from, to));
        charts.put("marksDistribution", buildMarksDistribution(stats.values()));
        charts.put("departmentPerformance", buildDepartmentPerformance(students, stats));
        charts.put("weeklyHeatmap", buildWeeklyHeatmap(attendance, from, to));
        return charts;
    }

    private List<Map<String, Object>> buildAttendanceTrend(List<Attendance> attendance, LocalDate from, LocalDate to) {
        Map<LocalDate, List<Attendance>> grouped = attendance.stream()
                .filter(a -> a.getAttendanceDate() != null)
                .filter(a -> !a.getAttendanceDate().isBefore(from) && !a.getAttendanceDate().isAfter(to))
                .collect(Collectors.groupingBy(Attendance::getAttendanceDate));

        List<Map<String, Object>> series = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            List<Attendance> day = grouped.getOrDefault(cursor, List.of());
            series.add(Map.of(
                    "date", cursor.toString(),
                    "value", round(calculatePresentPct(day))
            ));
            cursor = cursor.plusDays(1);
        }
        return series;
    }

    private List<Map<String, Object>> buildMarksDistribution(Collection<StudentStats> stats) {
        int[] buckets = new int[5];
        for (StudentStats s : stats) {
            if (s.avgMarks < 20) buckets[0]++;
            else if (s.avgMarks < 40) buckets[1]++;
            else if (s.avgMarks < 60) buckets[2]++;
            else if (s.avgMarks < 80) buckets[3]++;
            else buckets[4]++;
        }

        return List.of(
                Map.of("label", "0-20", "value", buckets[0]),
                Map.of("label", "20-40", "value", buckets[1]),
                Map.of("label", "40-60", "value", buckets[2]),
                Map.of("label", "60-80", "value", buckets[3]),
                Map.of("label", "80-100", "value", buckets[4])
        );
    }

    private List<Map<String, Object>> buildDepartmentPerformance(List<Student> students, Map<String, StudentStats> stats) {
        Map<String, List<StudentStats>> byDepartment = new HashMap<>();

        for (Student student : students) {
            StudentStats s = stats.get(student.getId());
            if (s == null) {
                continue;
            }
            String key = nullSafe(student.getDepartment()).isBlank() ? "General" : student.getDepartment();
            byDepartment.computeIfAbsent(key, ignore -> new ArrayList<>()).add(s);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<StudentStats>> entry : byDepartment.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(x -> x.performanceScore).average().orElse(0.0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", entry.getKey());
            row.put("value", round(avg));
            result.add(row);
        }
        result.sort(Comparator.comparingDouble(e -> -((Number) e.get("value")).doubleValue()));
        return result;
    }

    private List<Map<String, Object>> buildWeeklyHeatmap(List<Attendance> attendance, LocalDate from, LocalDate to) {
        Map<String, Integer> counts = new HashMap<>();

        for (Attendance a : attendance) {
            if (a.getAttendanceDate() == null || a.getMarkedTime() == null) {
                continue;
            }
            if (a.getAttendanceDate().isBefore(from) || a.getAttendanceDate().isAfter(to)) {
                continue;
            }
            DayOfWeek day = a.getAttendanceDate().getDayOfWeek();
            int hour = a.getMarkedTime().getHour();
            String key = day.getValue() + "-" + hour;
            counts.merge(key, 1, Integer::sum);
        }

        List<Map<String, Object>> grid = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            for (int hour = 7; hour <= 19; hour++) {
                String key = day + "-" + hour;
                grid.add(Map.of("day", day, "hour", hour, "count", counts.getOrDefault(key, 0)));
            }
        }
        return grid;
    }

    private List<Map<String, Object>> buildActivityFeed(List<Attendance> attendance, List<Student> students) {
        Map<String, String> names = students.stream()
                .collect(Collectors.toMap(Student::getId, Student::getName, (a, b) -> a));

        List<Map<String, Object>> feed = new ArrayList<>();
        attendance.stream()
            .filter(a -> a.getCreatedAt() != null)
            .sorted(Comparator.comparing(Attendance::getCreatedAt).reversed())
            .limit(40)
            .forEach(a -> {
                String name = names.getOrDefault(a.getStudentId(), a.getStudentId());
                String verb = "PRESENT".equalsIgnoreCase(a.getStatus()) ? "marked present" :
                    ("ABSENT".equalsIgnoreCase(a.getStatus()) ? "marked absent" : "updated attendance");
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("time", a.getCreatedAt().truncatedTo(ChronoUnit.SECONDS).toString());
                row.put("message", name + " " + verb);
                row.put("studentId", nullSafe(a.getStudentId()));
                row.put("status", nullSafe(a.getStatus()));
                feed.add(row);
            });
        return feed;
    }

    private List<String> buildRecommendations(List<Student> students, Map<String, StudentStats> stats) {
        Map<String, Long> riskByCourse = students.stream()
                .filter(s -> stats.containsKey(s.getId()))
                .filter(s -> {
                    StudentStats x = stats.get(s.getId());
                    return x.avgMarks < 40 || x.attendancePct < 75;
                })
                .collect(Collectors.groupingBy(s -> nullSafe(s.getCourse()).isBlank() ? "Unassigned" : s.getCourse(), Collectors.counting()));

        List<String> recommendations = new ArrayList<>();

        riskByCourse.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(2)
                .forEach(entry -> recommendations.add("Focus on " + entry.getKey() + ", performance signals are dropping."));

        long declining = students.stream()
                .filter(s -> stats.containsKey(s.getId()) && stats.get(s.getId()).consistentDecline)
                .count();
        if (declining > 0) {
            recommendations.add("" + declining + " students show consistent decline patterns; prioritize mentoring.");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("No critical drift detected. Keep monitoring real-time trend cards.");
        }

        return recommendations;
    }

    private List<Map<String, Object>> buildPerformanceSeries(String studentId,
                                                             List<Attendance> attendance,
                                                             List<Enrollment> enrollments) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(55);
        List<Map<String, Object>> points = new ArrayList<>();

        Map<LocalDate, List<Attendance>> byDate = attendance.stream()
                .filter(a -> a.getAttendanceDate() != null)
                .collect(Collectors.groupingBy(Attendance::getAttendanceDate));

        double marks = enrollments.stream()
                .map(Enrollment::getMarks)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            List<Attendance> day = byDate.getOrDefault(cursor, List.of());
            double attendanceScore = calculatePresentPct(day);
            points.add(Map.of(
                    "date", cursor.toString(),
                    "performance", round((attendanceScore * 0.5) + (marks * 0.5)),
                    "attendance", round(attendanceScore),
                    "marks", round(marks)
            ));
            cursor = cursor.plusDays(1);
        }

        return points;
    }

    private List<Map<String, Object>> buildStudentAttendanceHeatmap(List<Attendance> attendance) {
        return buildWeeklyHeatmap(attendance, LocalDate.now().minusDays(27), LocalDate.now());
    }

    private List<Map<String, Object>> buildBehaviorTrend(List<Attendance> attendance) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(27);
        List<Map<String, Object>> series = new ArrayList<>();

        for (LocalDate cursor = start; !cursor.isAfter(end); cursor = cursor.plusDays(1)) {
            LocalDate date = cursor;
            List<Attendance> day = attendance.stream()
                    .filter(a -> date.equals(a.getAttendanceDate()))
                    .toList();
            long unusual = day.stream()
                    .filter(a -> a.getMarkedTime() != null && (a.getMarkedTime().isBefore(LocalTime.of(7, 0)) || a.getMarkedTime().isAfter(LocalTime.of(20, 0))))
                    .count();
            series.add(Map.of(
                    "date", date.toString(),
                    "value", unusual
            ));
        }
        return series;
    }

    private boolean detectConsistentDecline(List<Attendance> attendance, LocalDate to) {
        double lastWeek = calculatePresentPct(attendance.stream()
                .filter(a -> within(a.getAttendanceDate(), to.minusDays(6), to))
                .toList());

        double weekMinus1 = calculatePresentPct(attendance.stream()
                .filter(a -> within(a.getAttendanceDate(), to.minusDays(13), to.minusDays(7)))
                .toList());

        double weekMinus2 = calculatePresentPct(attendance.stream()
                .filter(a -> within(a.getAttendanceDate(), to.minusDays(20), to.minusDays(14)))
                .toList());

        return lastWeek < weekMinus1 && weekMinus1 < weekMinus2;
    }

    private boolean detectIrregularSpike(List<Attendance> attendance, LocalDate to) {
        List<Long> dailyCounts = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            LocalDate date = to.minusDays(i);
            long count = attendance.stream().filter(a -> date.equals(a.getAttendanceDate())).count();
            dailyCounts.add(count);
        }

        double avg = dailyCounts.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double variance = dailyCounts.stream().mapToDouble(v -> Math.pow(v - avg, 2)).average().orElse(0.0);
        return Math.sqrt(variance) > 1.7;
    }

    private boolean detectUnusualActivity(List<Attendance> attendance, LocalDate to) {
        long unusual = attendance.stream()
                .filter(a -> a.getAttendanceDate() != null && !a.getAttendanceDate().isBefore(to.minusDays(14)))
                .filter(a -> a.getMarkedTime() != null)
                .filter(a -> a.getMarkedTime().isBefore(LocalTime.of(7, 0)) || a.getMarkedTime().isAfter(LocalTime.of(20, 0)))
                .count();
        return unusual >= 2;
    }

    private boolean within(LocalDate value, LocalDate start, LocalDate end) {
        return value != null && !value.isBefore(start) && !value.isAfter(end);
    }

    private double calculatePresentPct(List<Attendance> records) {
        if (records == null || records.isEmpty()) {
            return 0.0;
        }
        long present = records.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
        return (present * 100.0) / records.size();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class StudentStats {
        private double avgMarks;
        private double attendancePct;
        private double currentWeekAttendance;
        private double previousWeekAttendance;
        private double trendDelta;
        private boolean consistentDecline;
        private boolean irregularSpike;
        private boolean unusualActivity;
        private double performanceScore;
    }
}
