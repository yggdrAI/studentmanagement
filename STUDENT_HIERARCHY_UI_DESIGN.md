# 🎓 Hierarchical Student Data Management UI Design

## 📋 Executive Summary

Transform the flat student table into a structured, aesthetic, 3-layer hierarchy:
```
CLASS (120 students) → BATCH (30 students) → STUDENTS
```

This design creates an intuitive academic hierarchy dashboard that feels like a structured management system, not a raw database table.

---

## 🏫 Section 1: Academic Structure & Assignment Logic

### 1.1 System Definition

```
Class Capacity: 120 students
Batch Capacity: 30 students (within a class)
Batches per Class: 4

Example Distribution:
├── Class 1
│   ├── Batch 1: S25CSEU0001 → S25CSEU0030
│   ├── Batch 2: S25CSEU0031 → S25CSEU0060
│   ├── Batch 3: S25CSEU0061 → S25CSEU0090
│   └── Batch 4: S25CSEU0091 → S25CSEU0120
├── Class 2
│   ├── Batch 1: S25CSEU0121 → S25CSEU0150
│   ├── Batch 2: S25CSEU0151 → S25CSEU0180
│   ├── Batch 3: S25CSEU0181 → S25CSEU0210
│   └── Batch 4: S25CSEU0211 → S25CSEU0240
└── Class N...
```

### 1.2 Auto-Assignment Algorithm

**Step 1: Extract Serial Number**
```javascript
// From enrollment number: S25CSEU0031
const enrollmentNumber = "S25CSEU0031";
const serial = parseInt(enrollmentNumber.match(/\d+$/)[0]); // 31
```

**Step 2: Compute Class & Batch**
```javascript
const classNumber = Math.ceil(serial / 120);        // ceil(31/120) = 1
const batchNumber = Math.ceil((serial % 120) / 30); // ceil((31%120)/30) = ceil(31/30) = 2

// Or using 0-indexed math:
const classNumber = Math.floor((serial - 1) / 120) + 1;
const batchNumber = Math.floor(((serial - 1) % 120) / 30) + 1;
```

**Step 3: Store in Database**
```
Student.classGroup = "Class 1"
Student.batchGroup = "Batch 2"
```

### 1.3 Java Backend Implementation

```java
public class HierarchyAssignmentService {
    
    /**
     * Extract serial number from enrollment (S25CSEU0031 → 31)
     */
    public static int extractSerialNumber(String enrollmentNumber) {
        String digits = enrollmentNumber.replaceAll("\\D", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }
    
    /**
     * Compute class from serial number
     */
    public static int computeClass(int serial) {
        return (serial - 1) / 120 + 1;
    }
    
    /**
     * Compute batch from serial number
     */
    public static int computeBatch(int serial) {
        return ((serial - 1) % 120) / 30 + 1;
    }
    
    /**
     * Get readable class label
     */
    public static String getClassLabel(int classNum) {
        return "Class " + classNum;
    }
    
    /**
     * Get readable batch label
     */
    public static String getBatchLabel(int batchNum) {
        return "Batch " + batchNum;
    }
}
```

---

## 🔌 Section 2: Enhanced API Response Format

### 2.1 New Endpoint: `/api/admin/students-hierarchy`

**Request:**
```
GET /api/admin/students-hierarchy?course=B.Tech&semester=5&searchClass=1
```

**Response:**
```json
{
  "structure": {
    "totalClasses": 3,
    "totalBatches": 12,
    "totalStudents": 360
  },
  "classes": [
    {
      "classId": "Class_1",
      "classNumber": 1,
      "classLabel": "Class 1",
      "totalStudents": 120,
      "batches": [
        {
          "batchId": "Batch_1_1",
          "classNumber": 1,
          "batchNumber": 1,
          "batchLabel": "Batch 1",
          "totalStudents": 30,
          "color": "#FF6B6B",
          "analytics": {
            "averageAttendance": 87.5,
            "averageMarks": 78.2,
            "topPerformer": { "id": "S25CSEU0001", "name": "Raj Kumar", "marks": 92 },
            "lowestPerformer": { "id": "S25CSEU0015", "name": "Priya Singh", "marks": 45 },
            "riskStudents": 3,
            "presentToday": 28
          },
          "students": [
            {
              "id": "S25CSEU0001",
              "studentId": "STU001",
              "name": "Raj Kumar",
              "enrollment": "S25CSEU0001",
              "email": "raj@bennett.edu.in",
              "phone": "9876543210",
              "gender": "M",
              "course": "B.Tech",
              "semester": 5,
              "section": "A",
              "classGroup": "Class 1",
              "batchGroup": "Batch 1",
              "profileImageUrl": "/uploads/profile-images/S25CSEU0001.jpg",
              "performance": {
                "averageMarks": 85.5,
                "attendancePercentage": 92.0,
                "status": "excellent"
              },
              "faceStatus": "verified",
              "lastUpdated": "2026-04-19T14:30:00Z"
            },
            // ... 29 more students
          ]
        },
        // ... 3 more batches
      ],
      "classAnalytics": {
        "totalAttendance": 87.2,
        "averageMarks": 76.8,
        "topPerformingBatch": "Batch 1",
        "lowestAttendanceBatch": "Batch 3",
        "totalRiskStudents": 12,
        "classStatus": "good"
      }
    }
    // ... more classes
  ]
}
```

### 2.2 Backend Controller Implementation

```java
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHierarchyController {
    
    private final StudentService studentService;
    private final EnrollmentRepository enrollmentRepository;
    private final AnalyticsService analyticsService;
    
    @GetMapping("/students-hierarchy")
    public ResponseEntity<Map<String, Object>> getStudentsHierarchy(
        @RequestParam(required = false) String course,
        @RequestParam(required = false) String semester,
        @RequestParam(required = false) String searchClass,
        @RequestParam(required = false) String searchBatch) {
        
        List<Student> students = studentService.findByCourseAndSemester(course, semester);
        
        // Group by class, then batch
        Map<Integer, Map<Integer, List<Student>>> hierarchy = students.stream()
            .collect(Collectors.groupingBy(
                s -> extractClassNumber(s),
                Collectors.groupingBy(s -> extractBatchNumber(s))
            ));
        
        List<Map<String, Object>> classes = new ArrayList<>();
        
        for (Integer classNum : hierarchy.keySet()) {
            Map<String, Object> classMap = buildClassNode(classNum, hierarchy.get(classNum));
            classes.add(classMap);
        }
        
        return ResponseEntity.ok(Map.of(
            "structure", Map.of(
                "totalClasses", hierarchy.size(),
                "totalBatches", hierarchy.values().stream().mapToInt(Map::size).sum(),
                "totalStudents", students.size()
            ),
            "classes", classes
        ));
    }
    
    private Map<String, Object> buildClassNode(Integer classNum, Map<Integer, List<Student>> batches) {
        List<Map<String, Object>> batchList = new ArrayList<>();
        List<String> classColors = getClassColors();
        
        for (Integer batchNum : batches.keySet()) {
            Map<String, Object> batchMap = buildBatchNode(classNum, batchNum, batches.get(batchNum));
            batchList.add(batchMap);
        }
        
        return Map.of(
            "classId", "Class_" + classNum,
            "classNumber", classNum,
            "classLabel", "Class " + classNum,
            "totalStudents", batches.values().stream().mapToInt(List::size).sum(),
            "batches", batchList,
            "classAnalytics", buildClassAnalytics(batchList)
        );
    }
    
    private Map<String, Object> buildBatchNode(Integer classNum, Integer batchNum, List<Student> students) {
        return Map.of(
            "batchId", "Batch_" + classNum + "_" + batchNum,
            "classNumber", classNum,
            "batchNumber", batchNum,
            "batchLabel", "Batch " + batchNum,
            "totalStudents", students.size(),
            "color", getBatchColor(batchNum),
            "analytics", buildBatchAnalytics(students),
            "students", students.stream().map(this::buildStudentNode).collect(Collectors.toList())
        );
    }
    
    private Map<String, Object> buildStudentNode(Student student) {
        StudentProfile profile = studentProfileRepository.findByStudentId(student.getId()).orElse(null);
        Enrollment enrollment = enrollmentRepository.findByStudentId(student.getId()).orElse(null);
        
        return Map.of(
            "id", student.getId(),
            "studentId", student.getId(),
            "name", student.getName(),
            "enrollment", profile != null ? profile.getEnrollmentNumber() : student.getId(),
            "email", student.getEmail(),
            "phone", student.getPhone(),
            "gender", student.getGender(),
            "course", student.getCourse(),
            "semester", student.getSemester(),
            "section", student.getSection(),
            "classGroup", student.getClassGroup(),
            "batchGroup", student.getBatchGroup(),
            "profileImageUrl", student.getProfileImageUrl(),
            "performance", buildPerformanceMetrics(student),
            "faceStatus", getFaceStatus(student.getId()),
            "lastUpdated", LocalDateTime.now()
        );
    }
    
    private Map<String, Object> buildPerformanceMetrics(Student student) {
        // Query marks, attendance from related entities
        Double averageMarks = analyticsService.getAverageMarks(student.getId());
        Double attendancePercentage = analyticsService.getAttendancePercentage(student.getId());
        
        String status = "excellent";
        if (averageMarks < 50) status = "poor";
        else if (averageMarks < 60) status = "average";
        else if (averageMarks < 75) status = "good";
        
        return Map.of(
            "averageMarks", averageMarks != null ? averageMarks : 0.0,
            "attendancePercentage", attendancePercentage != null ? attendancePercentage : 0.0,
            "status", status
        );
    }
    
    private Map<String, Object> buildBatchAnalytics(List<Student> students) {
        double avgAttendance = analyticsService.getAverageAttendance(students);
        double avgMarks = analyticsService.getAverageMarks(students);
        
        Student topPerformer = students.stream()
            .max(Comparator.comparingDouble(s -> analyticsService.getAverageMarks(s)))
            .orElse(null);
        
        long riskStudentCount = students.stream()
            .filter(s -> analyticsService.getAverageMarks(s) < 50)
            .count();
        
        return Map.of(
            "averageAttendance", avgAttendance,
            "averageMarks", avgMarks,
            "topPerformer", topPerformer != null ? Map.of(
                "id", topPerformer.getId(),
                "name", topPerformer.getName(),
                "marks", analyticsService.getAverageMarks(topPerformer)
            ) : null,
            "riskStudents", riskStudentCount,
            "presentToday", getPresentCount(students)
        );
    }
    
    private String getBatchColor(int batchNum) {
        String[] colors = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A"};
        return colors[(batchNum - 1) % colors.length];
    }
    
    private List<String> getClassColors() {
        return Arrays.asList("#1E88E5", "#43A047", "#FB8C00", "#6F42C1");
    }
}
```

---

## 🎨 Section 3: UI Implementation (HTML/CSS/JS)

### 3.1 HTML Structure - Hierarchical Accordion

```html
<!-- admin-students-hierarchy.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Student Hierarchy - Class & Batch Management</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" th:href="@{/css/dashboard.css}">
    <link rel="stylesheet" th:href="@{/css/hierarchy-students.css}">
</head>
<body>
<div th:replace="~{fragments/sidebar :: sidebar(activeTab='students')}"></div>

<main class="content-area" id="maincontent" tabindex="-1">
    <div class="content-wrapper hierarchy-shell">
        
        <!-- Top Bar -->
        <header class="unified-topbar hierarchy-topbar app-enter">
            <div>
                <h1 class="page-title">🏫 Student Hierarchy</h1>
                <p class="dashboard-subtitle">Organized by Class → Batch → Students with real-time analytics.</p>
            </div>
            <div class="top-actions">
                <input id="globalSearch" class="global-search" type="search" 
                       placeholder="Search students, class, or batch..." aria-label="Search">
                <button id="sidebarToggle" class="icon-btn" type="button" aria-label="Toggle sidebar">☰</button>
            </div>
        </header>

        <!-- Filters & Controls -->
        <section class="filters-section">
            <div class="filter-group">
                <label for="courseFilter">Course</label>
                <select id="courseFilter">
                    <option value="">All Courses</option>
                    <option value="B.Tech">B.Tech</option>
                    <option value="BBA">BBA</option>
                    <option value="MBA">MBA</option>
                </select>
            </div>
            <div class="filter-group">
                <label for="semesterFilter">Semester</label>
                <select id="semesterFilter">
                    <option value="">All Semesters</option>
                    <option value="1">Semester 1</option>
                    <option value="2">Semester 2</option>
                    <option value="3">Semester 3</option>
                    <option value="4">Semester 4</option>
                    <option value="5">Semester 5</option>
                    <option value="6">Semester 6</option>
                    <option value="7">Semester 7</option>
                    <option value="8">Semester 8</option>
                </select>
            </div>
            <div class="filter-group">
                <label for="performanceFilter">Performance</label>
                <select id="performanceFilter">
                    <option value="">All Students</option>
                    <option value="excellent">Excellent (75+)</option>
                    <option value="good">Good (60-74)</option>
                    <option value="average">Average (50-59)</option>
                    <option value="poor">Poor (<50)</option>
                </select>
            </div>
            <button id="refreshBtn" class="btn-secondary" aria-label="Refresh data">↻ Refresh</button>
            <button id="viewToggleBtn" class="btn-secondary" aria-label="Toggle view mode">📊 Expand All</button>
        </section>

        <!-- Structure Overview -->
        <section class="structure-overview">
            <div class="stat-card">
                <div class="stat-label">Total Classes</div>
                <div class="stat-value" id="totalClasses">0</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Total Batches</div>
                <div class="stat-value" id="totalBatches">0</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Total Students</div>
                <div class="stat-value" id="totalStudents">0</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Avg Attendance</div>
                <div class="stat-value" id="avgAttendance">--</div>
            </div>
        </section>

        <!-- Class Hierarchy -->
        <section class="classes-container" id="classesContainer">
            <!-- Dynamically populated -->
        </section>

        <!-- Loading State -->
        <div id="loadingSpinner" class="loading-spinner" hidden>
            <div class="spinner"></div>
            <p>Loading student hierarchy...</p>
        </div>

        <!-- No Data State -->
        <div id="noDataState" class="no-data-state" hidden>
            <p>No students found. Try adjusting your filters.</p>
        </div>

    </div>
</main>

<script th:src="@{/js/hierarchy-students.js}"></script>
</body>
</html>
```

### 3.2 CSS - Card-Based Hierarchy Styling

```css
/* hierarchy-students.css */

:root {
    --class-bg: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    --batch-1-color: #FF6B6B;
    --batch-2-color: #4ECDC4;
    --batch-3-color: #45B7D1;
    --batch-4-color: #FFA07A;
    --card-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    --card-hover-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    --transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ===== Structure Overview ===== */
.structure-overview {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 16px;
    margin: 24px 0;
    padding: 20px;
    background: #f8f9fa;
    border-radius: 12px;
}

.stat-card {
    background: white;
    padding: 16px;
    border-radius: 8px;
    text-align: center;
    box-shadow: var(--card-shadow);
    transition: var(--transition);
}

.stat-card:hover {
    transform: translateY(-2px);
    box-shadow: var(--card-hover-shadow);
}

.stat-label {
    font-size: 12px;
    color: #666;
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 8px;
}

.stat-value {
    font-size: 28px;
    font-weight: 700;
    color: #2c3e50;
}

/* ===== Class Cards (Top Level) ===== */
.class-card {
    background: white;
    border-radius: 12px;
    margin-bottom: 20px;
    overflow: hidden;
    box-shadow: var(--card-shadow);
    transition: var(--transition);
}

.class-card:hover {
    box-shadow: var(--card-hover-shadow);
}

.class-header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    cursor: pointer;
    user-select: none;
    transition: var(--transition);
}

.class-header:hover {
    background: linear-gradient(135deg, #5568d3 0%, #6a3f8f 100%);
}

.class-header.expanded {
    border-bottom: 3px solid #4ECDC4;
}

.class-info {
    flex: 1;
}

.class-title {
    font-size: 22px;
    font-weight: 700;
    margin-bottom: 8px;
}

.class-stats {
    display: flex;
    gap: 20px;
    font-size: 13px;
    opacity: 0.9;
}

.class-stats span {
    display: flex;
    align-items: center;
    gap: 4px;
}

.class-toggle {
    font-size: 24px;
    transition: transform 0.3s ease;
}

.class-toggle.rotated {
    transform: rotate(180deg);
}

/* Class Body - Batches Container */
.class-body {
    padding: 24px;
    background: #f8f9fa;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 16px;
    max-height: 600px;
    overflow-y: auto;
    transition: max-height 0.3s ease;
}

.class-body.collapsed {
    max-height: 0;
    padding: 0;
    overflow: hidden;
}

/* ===== Batch Cards (Second Level) ===== */
.batch-card {
    background: white;
    border-radius: 10px;
    border-left: 4px solid;
    box-shadow: var(--card-shadow);
    overflow: hidden;
    transition: var(--transition);
    cursor: pointer;
}

.batch-card:hover {
    box-shadow: var(--card-hover-shadow);
    transform: translateY(-4px);
}

.batch-card.batch-1 { border-left-color: var(--batch-1-color); }
.batch-card.batch-2 { border-left-color: var(--batch-2-color); }
.batch-card.batch-3 { border-left-color: var(--batch-3-color); }
.batch-card.batch-4 { border-left-color: var(--batch-4-color); }

.batch-header {
    padding: 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: linear-gradient(to right, rgba(0,0,0,0.02), rgba(0,0,0,0.05));
    border-bottom: 1px solid #e0e0e0;
}

.batch-title {
    font-size: 16px;
    font-weight: 600;
    color: #2c3e50;
}

.batch-count {
    font-size: 12px;
    color: #999;
    background: #f0f0f0;
    padding: 4px 8px;
    border-radius: 4px;
}

.batch-analytics {
    padding: 16px;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    font-size: 12px;
}

.analytics-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px;
    background: #f8f9fa;
    border-radius: 6px;
}

.analytics-label {
    color: #666;
    font-weight: 500;
}

.analytics-value {
    color: #2c3e50;
    font-weight: 600;
    font-size: 13px;
}

.batch-indicator {
    display: flex;
    gap: 8px;
    padding: 12px 16px;
    background: #f0f0f0;
    font-size: 11px;
    border-top: 1px solid #e0e0e0;
}

.indicator-item {
    display: flex;
    align-items: center;
    gap: 4px;
}

.indicator-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #FF6B6B;
}

.batch-toggle {
    font-size: 18px;
    cursor: pointer;
}

/* ===== Student List (Third Level) ===== */
.students-list {
    background: white;
    border-top: 1px solid #e0e0e0;
    max-height: 400px;
    overflow-y: auto;
    display: none;
}

.students-list.visible {
    display: block;
}

.student-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;
    hover-background: #f8f9fa;
    transition: var(--transition);
}

.student-row:hover {
    background: #f8f9fa;
}

.student-info {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;
}

.student-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    font-size: 14px;
}

.student-details {
    flex: 1;
}

.student-name {
    font-weight: 600;
    color: #2c3e50;
    font-size: 14px;
    margin-bottom: 4px;
}

.student-meta {
    font-size: 12px;
    color: #999;
}

.student-performance {
    display: flex;
    gap: 12px;
    align-items: center;
}

.performance-badge {
    padding: 4px 8px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 600;
}

.performance-badge.excellent {
    background: #d4edda;
    color: #155724;
}

.performance-badge.good {
    background: #cfe2ff;
    color: #0c5460;
}

.performance-badge.average {
    background: #fff3cd;
    color: #664d03;
}

.performance-badge.poor {
    background: #f8d7da;
    color: #842029;
}

.student-actions {
    display: flex;
    gap: 6px;
}

.action-btn {
    width: 28px;
    height: 28px;
    border: none;
    background: #f0f0f0;
    border-radius: 4px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    transition: var(--transition);
}

.action-btn:hover {
    background: #e0e0e0;
}

/* ===== Responsive Design ===== */
@media (max-width: 1024px) {
    .class-body {
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    }
}

@media (max-width: 768px) {
    .class-body {
        grid-template-columns: 1fr;
        max-height: none;
        overflow-y: visible;
    }
    
    .batch-analytics {
        grid-template-columns: 1fr;
    }
    
    .structure-overview {
        grid-template-columns: repeat(2, 1fr);
    }
}

/* ===== Animations ===== */
@keyframes slideDown {
    from {
        opacity: 0;
        transform: translateY(-10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.batch-card {
    animation: slideDown 0.3s ease;
}

.student-row {
    animation: slideDown 0.2s ease;
}
```

### 3.3 JavaScript - Interactive Hierarchy Logic

```javascript
// hierarchy-students.js

(function() {
    "use strict";
    
    const state = {
        hierarchy: null,
        expanded: new Set(),
        visibleBatches: new Set(),
        filters: {
            course: "",
            semester: "",
            performance: "",
            searchQuery: ""
        },
        allExpanded: false
    };
    
    const refs = {
        classesContainer: document.getElementById("classesContainer"),
        loadingSpinner: document.getElementById("loadingSpinner"),
        noDataState: document.getElementById("noDataState"),
        courseFilter: document.getElementById("courseFilter"),
        semesterFilter: document.getElementById("semesterFilter"),
        performanceFilter: document.getElementById("performanceFilter"),
        globalSearch: document.getElementById("globalSearch"),
        refreshBtn: document.getElementById("refreshBtn"),
        viewToggleBtn: document.getElementById("viewToggleBtn"),
        totalClasses: document.getElementById("totalClasses"),
        totalBatches: document.getElementById("totalBatches"),
        totalStudents: document.getElementById("totalStudents"),
        avgAttendance: document.getElementById("avgAttendance")
    };
    
    // ===== Initialization =====
    function init() {
        attachEventListeners();
        loadHierarchy();
    }
    
    function attachEventListeners() {
        refs.courseFilter?.addEventListener("change", handleFilterChange);
        refs.semesterFilter?.addEventListener("change", handleFilterChange);
        refs.performanceFilter?.addEventListener("change", handleFilterChange);
        refs.globalSearch?.addEventListener("input", debounce(handleSearch, 300));
        refs.refreshBtn?.addEventListener("click", loadHierarchy);
        refs.viewToggleBtn?.addEventListener("click", toggleExpandAll);
    }
    
    // ===== Data Loading =====
    function loadHierarchy() {
        refs.loadingSpinner.hidden = false;
        
        const params = new URLSearchParams();
        if (state.filters.course) params.append("course", state.filters.course);
        if (state.filters.semester) params.append("semester", state.filters.semester);
        
        fetch(`/api/admin/students-hierarchy?${params}`)
            .then(r => r.json())
            .then(data => {
                state.hierarchy = data;
                updateStatistics();
                renderHierarchy();
                refs.loadingSpinner.hidden = true;
            })
            .catch(err => {
                console.error("Failed to load hierarchy:", err);
                refs.loadingSpinner.hidden = true;
            });
    }
    
    function updateStatistics() {
        refs.totalClasses.textContent = state.hierarchy.structure.totalClasses;
        refs.totalBatches.textContent = state.hierarchy.structure.totalBatches;
        refs.totalStudents.textContent = state.hierarchy.structure.totalStudents;
        
        const avgAtt = calculateAverageAttendance();
        refs.avgAttendance.textContent = avgAtt.toFixed(1) + "%";
    }
    
    function calculateAverageAttendance() {
        let total = 0, count = 0;
        state.hierarchy.classes.forEach(cls => {
            cls.batches.forEach(batch => {
                total += batch.analytics.averageAttendance || 0;
                count++;
            });
        });
        return count > 0 ? total / count : 0;
    }
    
    // ===== Rendering =====
    function renderHierarchy() {
        if (!state.hierarchy || state.hierarchy.classes.length === 0) {
            refs.noDataState.hidden = false;
            refs.classesContainer.innerHTML = "";
            return;
        }
        
        refs.noDataState.hidden = true;
        refs.classesContainer.innerHTML = state.hierarchy.classes
            .map(cls => renderClassCard(cls))
            .join("");
        
        attachClassCardListeners();
    }
    
    function renderClassCard(classData) {
        const isExpanded = state.expanded.has(classData.classId);
        const totalStudents = classData.totalStudents;
        const totalBatches = classData.batches.length;
        
        return `
            <div class="class-card" data-class-id="${classData.classId}">
                <div class="class-header ${isExpanded ? 'expanded' : ''}">
                    <div class="class-info">
                        <div class="class-title">${classData.classLabel}</div>
                        <div class="class-stats">
                            <span>📚 ${totalStudents} Students</span>
                            <span>🎯 ${totalBatches} Batches</span>
                            <span>📊 Avg: ${(classData.classAnalytics?.averageMarks || 0).toFixed(1)}</span>
                        </div>
                    </div>
                    <div class="class-toggle ${isExpanded ? 'rotated' : ''}">⌄</div>
                </div>
                <div class="class-body ${isExpanded ? '' : 'collapsed'}">
                    ${classData.batches.map((batch, i) => renderBatchCard(batch, i + 1)).join("")}
                </div>
            </div>
        `;
    }
    
    function renderBatchCard(batch, batchIndex) {
        const isVisible = state.visibleBatches.has(batch.batchId);
        const batchClass = `batch-${batchIndex}`;
        
        return `
            <div class="batch-card ${batchClass}" data-batch-id="${batch.batchId}">
                <div class="batch-header">
                    <div>
                        <div class="batch-title">${batch.batchLabel}</div>
                        <div class="batch-count">${batch.totalStudents} students</div>
                    </div>
                    <div class="batch-toggle" data-batch-id="${batch.batchId}">
                        ${isVisible ? '▼' : '▶'}
                    </div>
                </div>
                <div class="batch-analytics">
                    <div class="analytics-item">
                        <span class="analytics-label">📊 Avg Marks</span>
                        <span class="analytics-value">${batch.analytics.averageMarks.toFixed(1)}</span>
                    </div>
                    <div class="analytics-item">
                        <span class="analytics-label">📅 Attendance</span>
                        <span class="analytics-value">${batch.analytics.averageAttendance.toFixed(1)}%</span>
                    </div>
                    <div class="analytics-item">
                        <span class="analytics-label">⭐ Top Performer</span>
                        <span class="analytics-value">${batch.analytics.topPerformer?.name || 'N/A'}</span>
                    </div>
                    <div class="analytics-item">
                        <span class="analytics-label">⚠️ At Risk</span>
                        <span class="analytics-value">${batch.analytics.riskStudents}</span>
                    </div>
                </div>
                <div class="batch-indicator">
                    <div class="indicator-item">
                        <div class="indicator-dot"></div>
                        <span>Present Today: ${batch.analytics.presentToday || 0}</span>
                    </div>
                </div>
                <div class="students-list ${isVisible ? 'visible' : ''}">
                    ${batch.students.map(student => renderStudentRow(student)).join("")}
                </div>
            </div>
        `;
    }
    
    function renderStudentRow(student) {
        const initials = student.name.split(' ').map(n => n[0]).join('').toUpperCase();
        const performance = student.performance?.status || 'average';
        
        return `
            <div class="student-row" data-student-id="${student.id}">
                <div class="student-info">
                    <div class="student-avatar">${initials}</div>
                    <div class="student-details">
                        <div class="student-name">${student.name}</div>
                        <div class="student-meta">${student.enrollment} • ${student.email}</div>
                    </div>
                </div>
                <div class="student-performance">
                    <div class="performance-badge ${performance}">
                        ${student.performance?.averageMarks.toFixed(1) || '--'}
                    </div>
                </div>
                <div class="student-actions">
                    <button class="action-btn" title="View Profile" onclick="viewStudentProfile('${student.id}')">👁</button>
                    <button class="action-btn" title="Edit" onclick="editStudent('${student.id}')">✏️</button>
                    <button class="action-btn" title="More" onclick="showStudentMenu('${student.id}')">⋯</button>
                </div>
            </div>
        `;
    }
    
    function attachClassCardListeners() {
        document.querySelectorAll(".class-header").forEach(header => {
            header.addEventListener("click", handleClassToggle);
        });
        
        document.querySelectorAll(".batch-toggle").forEach(toggle => {
            toggle.addEventListener("click", handleBatchToggle);
        });
    }
    
    // ===== Event Handlers =====
    function handleClassToggle(e) {
        const classCard = e.currentTarget.closest(".class-card");
        const classId = classCard.dataset.classId;
        const classBody = classCard.querySelector(".class-body");
        const header = e.currentTarget;
        const toggle = header.querySelector(".class-toggle");
        
        if (state.expanded.has(classId)) {
            state.expanded.delete(classId);
            classBody.classList.add("collapsed");
            header.classList.remove("expanded");
            toggle.classList.remove("rotated");
        } else {
            state.expanded.add(classId);
            classBody.classList.remove("collapsed");
            header.classList.add("expanded");
            toggle.classList.add("rotated");
        }
    }
    
    function handleBatchToggle(e) {
        e.stopPropagation();
        const batchId = e.currentTarget.dataset.batchId;
        const batchCard = document.querySelector(`[data-batch-id="${batchId}"]`);
        const studentsList = batchCard.querySelector(".students-list");
        const toggle = e.currentTarget;
        
        if (state.visibleBatches.has(batchId)) {
            state.visibleBatches.delete(batchId);
            studentsList.classList.remove("visible");
            toggle.textContent = "▶";
        } else {
            state.visibleBatches.add(batchId);
            studentsList.classList.add("visible");
            toggle.textContent = "▼";
        }
    }
    
    function handleFilterChange() {
        state.filters.course = refs.courseFilter.value;
        state.filters.semester = refs.semesterFilter.value;
        state.filters.performance = refs.performanceFilter.value;
        loadHierarchy();
    }
    
    function handleSearch(e) {
        state.filters.searchQuery = e.target.value.toLowerCase();
        filterAndRender();
    }
    
    function toggleExpandAll() {
        state.allExpanded = !state.allExpanded;
        
        if (state.allExpanded) {
            state.hierarchy.classes.forEach(cls => state.expanded.add(cls.classId));
            refs.viewToggleBtn.textContent = "📊 Collapse All";
        } else {
            state.expanded.clear();
            refs.viewToggleBtn.textContent = "📊 Expand All";
        }
        
        renderHierarchy();
    }
    
    function filterAndRender() {
        // Filter based on search query and performance
        // Implement filtering logic
        renderHierarchy();
    }
    
    // ===== Utility Functions =====
    function debounce(func, wait) {
        let timeout;
        return function(...args) {
            clearTimeout(timeout);
            timeout = setTimeout(() => func(...args), wait);
        };
    }
    
    // Exposed functions for buttons
    window.viewStudentProfile = (studentId) => {
        window.location.href = `/admin/students/${studentId}`;
    };
    
    window.editStudent = (studentId) => {
        // Open edit modal
        console.log("Edit student:", studentId);
    };
    
    window.showStudentMenu = (studentId) => {
        // Show context menu
        console.log("Menu for student:", studentId);
    };
    
    // ===== Initialize on Load =====
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
```

---

## 📊 Section 4: Example Data Mapping

### 4.1 Database Records

**Student Table Records:**
```
id      | name           | enrollment_number | class_group | batch_group | email
--------|----------------|-------------------|-------------|-------------|------------------
STU001  | Raj Kumar      | S25CSEU0001      | Class 1     | Batch 1     | raj@bennett.edu.in
STU002  | Priya Singh    | S25CSEU0002      | Class 1     | Batch 1     | priya@bennett.edu.in
...
STU031  | Aman Gupta     | S25CSEU0031      | Class 1     | Batch 2     | aman@bennett.edu.in
...
STU121  | Neha Sharma    | S25CSEU0121      | Class 2     | Batch 1     | neha@bennett.edu.in
```

### 4.2 Enrollment Number Pattern Analysis

```
Pattern: S[YY][DEPT][SECTION][SERIAL]

S25CSEU0001
├─ S = Enrollment prefix
├─ 25 = Batch year (2025)
├─ CS = Department (Computer Science)
├─ EU = Section/Stream
└─ 0001 = Serial number (extract this)

Assignment Logic:
├─ Class = ceil(serial / 120)        → ceil(0001 / 120) = 1
└─ Batch = ceil((serial % 120) / 30) → ceil((0001 % 120) / 30) = 1
Result: Class 1, Batch 1

---

S25CSEU0031
├─ Serial = 31
├─ Class = ceil(31 / 120) = 1
└─ Batch = ceil((31 % 120) / 30) = ceil(31 / 30) = 2
Result: Class 1, Batch 2

---

S25CSEU0121
├─ Serial = 121
├─ Class = ceil(121 / 120) = 2
└─ Batch = ceil((121 % 120) / 30) = ceil(1 / 30) = 1
Result: Class 2, Batch 1
```

---

## 🎨 Section 5: UI/UX Breakdown

### 5.1 Three-Layer Architecture

| Layer | Component | Purpose | Interaction |
|-------|-----------|---------|------------|
| **L1: Class** | Large purple card | Shows class overview & stats | Click to expand |
| **L2: Batch** | Medium colored cards (4 per class) | Shows batch analytics | Click to show students |
| **L3: Student** | Compact rows inside batch | Individual student details | Hover for actions |

### 5.2 Visual Hierarchy & Information Density

```
CLASS HEADER (30px)
├─ Title: "Class 1"
├─ Stats: "120 Students | 4 Batches | Avg 76.8"
└─ Toggle Arrow: "⌄" (rotates on expand)

CLASS BODY (collapsed/expanded)
├─ BATCH CARD 1 (colored left border)
│  ├─ Header: "Batch 1 | 30 students"
│  ├─ Analytics: "Avg Marks: 78.2 | Attendance: 87.5%"
│  ├─ Indicators: "Present: 28 | At Risk: 3"
│  └─ Students List (collapsible)
│     ├─ Row 1: Avatar | Name | Email | Performance | Actions
│     ├─ Row 2: ...
│     └─ Row N: ...
├─ BATCH CARD 2 ...
├─ BATCH CARD 3 ...
└─ BATCH CARD 4 ...
```

### 5.3 Color Coding

```css
Batch Colors (Distinct & Consistent):
├─ Batch 1: #FF6B6B (Red)     ← Dynamic, attention-grabbing
├─ Batch 2: #4ECDC4 (Teal)    ← Calm, professional
├─ Batch 3: #45B7D1 (Blue)    ← Cool, steady
└─ Batch 4: #FFA07A (Salmon)  ← Warm, approachable

Class Colors:
├─ Class Header: #667eea to #764ba2 (Purple gradient)
└─ Background: #f8f9fa (Soft gray)
```

### 5.4 Interactions & Animations

```
Click Class Header → Smooth expand/collapse (0.3s)
  └─ Arrow rotates 180°
  └─ Batches slide in with fade
  
Hover Class Card → Subtle lift effect (+4px)
  └─ Shadow intensifies

Hover Batch Card → Lift & shadow intensify
  └─ Performance badges highlight

Click Batch Toggle → Students list slides open
  └─ Individual rows animate in (staggered)

Hover Student Row → Background highlight + actions visible
  └─ View | Edit | More buttons appear
```

### 5.5 Responsive Behavior

**Desktop (1200px+)**
- 4 batches per row (grid layout)
- All analytics visible
- Full class headers

**Tablet (768px - 1199px)**
- 2 batches per row
- Condensed analytics
- Adjusted font sizes

**Mobile (< 768px)**
- 1 batch per row (full width)
- Minimized headers
- Touch-friendly tap targets
- Collapsible sections for space efficiency

---

## ⚡ Section 6: Advanced Features (Optional)

### 6.1 Batch Swipe Navigation (Mobile)

```javascript
// Swipe between batches on mobile
touchStartX = 0;
touchEndX = 0;

element.addEventListener("touchstart", e => {
    touchStartX = e.changedTouches[0].screenX;
});

element.addEventListener("touchend", e => {
    touchEndX = e.changedTouches[0].screenX;
    handleSwipe();
});
```

### 6.2 Bulk Edit Mode

```
Admin selects multiple students → Show bulk edit panel
├─ Update performance status
├─ Change batch assignment
├─ Bulk upload attendance
└─ Send bulk notifications
```

### 6.3 Quick Search & Filter

```
Global search: "Raj" → Highlights matching students across all batches
Performance filter: "At Risk" → Shows only students with <50 marks
```

### 6.4 Export to Report

```
Button: "📥 Export as PDF"
├─ Class-wise breakdown
├─ Performance analytics per batch
└─ Student roster with QR codes
```

---

## 📦 Section 7: Implementation Checklist

- [ ] Backend: Create `/api/admin/students-hierarchy` endpoint
- [ ] Backend: Implement `HierarchyAssignmentService`
- [ ] Backend: Add `AdminHierarchyController` with full logic
- [ ] Database: Ensure `classGroup` & `batchGroup` fields populated
- [ ] Frontend: Create `admin-students-hierarchy.html`
- [ ] Frontend: Add `hierarchy-students.css` with all styling
- [ ] Frontend: Implement `hierarchy-students.js` with all interactions
- [ ] Testing: Verify auto-assignment with 200+ students
- [ ] Testing: Check responsive design on mobile/tablet
- [ ] Testing: Load testing with large datasets (1000+ students)
- [ ] Accessibility: Add ARIA labels for screen readers
- [ ] Documentation: Document API response format for frontend team

---

## 🎯 Summary

This hierarchical UI transforms the flat student list into a **structured, visually appealing, and highly functional** dashboard that:

✅ Organizes students logically (Class → Batch → Students)
✅ Provides real-time analytics per batch & class
✅ Uses color-coding for quick visual recognition
✅ Supports responsive design for all devices
✅ Implements smooth animations & transitions
✅ Enables advanced filtering & searching
✅ Maintains accessibility standards

The system now feels like a **structured academic hierarchy dashboard**, not a raw database table! 🚀
