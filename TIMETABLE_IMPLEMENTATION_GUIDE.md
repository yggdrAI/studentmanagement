# Timetable Management System - Implementation Guide

## Quick Start Guide

### Prerequisites
- Java 21+
- Maven 3.9+
- MySQL 8.0 or PostgreSQL 12+
- Spring Boot 3.2+
- Lombok library

### Step 1: Setup Database

#### MySQL Setup
```sql
-- Create database
CREATE DATABASE student_management;
CREATE USER 'sms_user'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON student_management.* TO 'sms_user'@'localhost';
FLUSH PRIVILEGES;
```

#### PostgreSQL Setup
```sql
-- Create database and user
CREATE DATABASE student_management;
CREATE USER sms_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE student_management TO sms_user;
```

### Step 2: Configure Application

Add to `application.properties`:
```properties
# MySQL Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/student_management
spring.datasource.username=sms_user
spring.datasource.password=secure_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# Logging
logging.level.com.sms=INFO
logging.level.org.springframework.web=INFO
```

### Step 3: Add Dependencies

Add to `pom.xml`:
```xml
<!-- Spring Boot Starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>

<!-- Flyway for migrations -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Jakarta EE -->
<dependency>
    <groupId>jakarta.persistence</groupId>
    <artifactId>jakarta.persistence-api</artifactId>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Step 4: Start Application

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Or run as JAR
java -jar target/studentmanagement-1.0.jar
```

---

## Workflow Example: Creating a Complete Timetable

### Scenario
Create a timetable for B.Tech CSE Semester 2, Section A for academic year 2025-2026.

### Step-by-Step Implementation

#### Step 1: Create Timetable
```bash
curl -X POST http://localhost:8080/api/v1/timetables \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "CSE-B.TECH",
    "courseName": "B.Tech Computer Science Engineering",
    "semester": 2,
    "section": "CSE-2A",
    "academicYear": "2025-2026",
    "effectiveFrom": "2025-01-19",
    "effectiveTo": "2025-05-30"
  }'
```

**Response:**
```json
{
  "id": 1,
  "timetableCode": "TT-CSE-B.TECH-SEM2-20252026",
  "courseId": "CSE-B.TECH",
  "courseName": "B.Tech Computer Science Engineering",
  "semester": 2,
  "section": "CSE-2A",
  "academicYear": "2025-2026",
  "status": "DRAFT"
}
```

#### Step 2: Add Monday Classes
```bash
curl -X POST http://localhost:8080/api/v1/timetables/1/schedule-entries \
  -H "Content-Type: application/json" \
  -d '[
    {
      "subjectId": "SUBJ-001",
      "subjectName": "Object Oriented Programming using Java",
      "subjectCode": "2025CSET152",
      "facultyId": "FAC-001",
      "facultyName": "Dr. Rajesh Kumar Sharma",
      "roomId": "ROOM-001",
      "roomNumber": "P-LH-101",
      "dayOfWeek": "MONDAY",
      "startTime": "08:20",
      "endTime": "09:20",
      "classType": "LECTURE"
    },
    {
      "subjectId": "SUBJ-002",
      "subjectName": "Discrete Mathematical Structures",
      "subjectCode": "2025CSEM151",
      "facultyId": "FAC-002",
      "facultyName": "Prof. Anjali Singh",
      "roomId": "ROOM-002",
      "roomNumber": "P-LH-102",
      "dayOfWeek": "MONDAY",
      "startTime": "09:30",
      "endTime": "10:30",
      "classType": "LECTURE"
    }
  ]'
```

#### Step 3: Add More Days' Classes
```bash
curl -X POST http://localhost:8080/api/v1/timetables/1/schedule-entries \
  -H "Content-Type: application/json" \
  -d '[
    {
      "subjectId": "SUBJ-001",
      "subjectName": "Object Oriented Programming using Java",
      "subjectCode": "2025CSET152",
      "facultyId": "FAC-001",
      "facultyName": "Dr. Rajesh Kumar Sharma",
      "roomId": "ROOM-012",
      "roomNumber": "P-LA-301",
      "dayOfWeek": "TUESDAY",
      "startTime": "13:40",
      "endTime": "14:40",
      "classType": "PRACTICAL"
    },
    {
      "subjectId": "SUBJ-003",
      "subjectName": "Linear Algebra and ODE",
      "subjectCode": "2025CSEM152",
      "facultyId": "FAC-003",
      "facultyName": "Dr. Vikram Patel",
      "roomId": "ROOM-003",
      "roomNumber": "P-LH-103",
      "dayOfWeek": "WEDNESDAY",
      "startTime": "08:20",
      "endTime": "09:20",
      "classType": "TUTORIAL"
    }
  ]'
```

#### Step 4: Add Holidays
```bash
# Republic Day
curl -X POST "http://localhost:8080/api/v1/timetables/1/holidays?date=2025-01-26&type=NATIONAL_HOLIDAY&reason=Republic%20Day"

# Foundation Day
curl -X POST "http://localhost:8080/api/v1/timetables/1/holidays?date=2025-02-15&type=INSTITUTIONAL_HOLIDAY&reason=Foundation%20Day"

# Exam Period
curl -X POST "http://localhost:8080/api/v1/timetables/1/holidays?date=2025-05-01&type=EXAM_DAY&reason=Mid%20Term%20Exams%20Start"
```

#### Step 5: Detect Conflicts
```bash
curl -X POST http://localhost:8080/api/v1/timetables/1/detect-conflicts
```

#### Step 6: Review and Get Timetable
```bash
curl -X GET http://localhost:8080/api/v1/timetables/1
```

#### Step 7: Get Specific Day Timetable
```bash
curl -X GET "http://localhost:8080/api/v1/timetables/1/day?date=2025-01-20"
```

#### Step 8: Publish Timetable
```bash
curl -X POST "http://localhost:8080/api/v1/timetables/1/publish?publishedBy=admin@bennett.edu.in"
```

---

## Java Integration Example

### Service Usage
```java
@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentTimetableController {
    
    private final TimetableService timetableService;
    
    @GetMapping("/{studentId}/timetable")
    public ResponseEntity<TimetableDTO> getStudentTimetable(
            @PathVariable String studentId,
            @RequestParam String courseId,
            @RequestParam Integer semester) {
        
        // Get active timetable for the course/semester
        List<Timetable> timetables = timetableService.
            getTimetablesForCourse(courseId, semester);
        
        if (timetables.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Timetable timetable = timetables.get(0);
        return ResponseEntity.ok(convertToDTO(timetable));
    }
    
    @GetMapping("/{studentId}/daily-schedule")
    public ResponseEntity<List<ScheduleEntryDTO>> getDailySchedule(
            @PathVariable String studentId,
            @RequestParam LocalDate date) {
        
        // Get timetable for specific day
        List<ScheduleEntry> entries = timetableService.
            getTimetableForDay(1L, date);
        
        List<ScheduleEntryDTO> dtos = entries.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
}
```

---

## Testing the System

### Unit Test Example
```java
@SpringBootTest
class TimetableServiceTest {
    
    @Autowired
    private TimetableService timetableService;
    
    @Autowired
    private TimetableRepository timetableRepository;
    
    @Test
    void testCreateTimetable() {
        TimetableDTO dto = new TimetableDTO();
        dto.setCourseId("CSE-B.TECH");
        dto.setCourseName("B.Tech Computer Science");
        dto.setSemester(2);
        dto.setAcademicYear("2025-2026");
        
        Timetable created = timetableService.createTimetable(dto, 1L);
        
        assertNotNull(created.getId());
        assertEquals("CSE-B.TECH", created.getCourseId());
    }
    
    @Test
    void testDetectConflicts() {
        Timetable timetable = createTestTimetable();
        addConflictingEntries(timetable);
        
        timetableService.detectConflicts(timetable.getId());
        
        // Verify conflicts are detected
        List<TimetableConflict> conflicts = 
            conflictRepository.findByTimetableId(timetable.getId());
        
        assertFalse(conflicts.isEmpty());
    }
}
```

---

## Advanced Features

### Exporting Timetable
```java
@GetMapping("/{id}/export/pdf")
public ResponseEntity<byte[]> exportToPdf(@PathVariable Long id) {
    Timetable timetable = timetableService.getTimetableWithDetails(id);
    byte[] pdfContent = generatePDF(timetable);
    
    return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=timetable.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfContent);
}

@GetMapping("/{id}/export/ics")
public ResponseEntity<String> exportToICS(@PathVariable Long id) {
    Timetable timetable = timetableService.getTimetableWithDetails(id);
    String icsContent = generateICS(timetable);
    
    return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=timetable.ics")
            .contentType(MediaType.parseMediaType("text/calendar"))
            .body(icsContent);
}
```

### Push Notifications
```java
@Service
public class TimetableNotificationService {
    
    private final NotificationService notificationService;
    
    public void notifyConflict(TimetableConflict conflict) {
        String message = String.format(
            "Conflict detected: %s - %s",
            conflict.getResource1(),
            conflict.getResource2()
        );
        
        notificationService.sendToAdmins(
            "Timetable Conflict",
            message,
            NotificationType.WARNING
        );
    }
    
    public void notifyPublication(Timetable timetable) {
        String message = String.format(
            "Timetable for %s published successfully",
            timetable.getCourseName()
        );
        
        notificationService.broadcastToStudents(
            "Timetable Updated",
            message
        );
    }
}
```

---

## Monitoring & Analytics

### Key Metrics to Track
1. **Conflict Detection Rate** - Percentage of timetables with conflicts
2. **Publication Time** - Average time to publish timetable
3. **Change Frequency** - Number of changes per timetable
4. **User Load** - API request patterns

### Sample Dashboard Query
```sql
-- Timetable statistics
SELECT 
    tt.semester,
    COUNT(*) as total_timetables,
    COUNT(CASE WHEN tt.status = 'PUBLISHED' THEN 1 END) as published,
    COUNT(CASE WHEN tt.status = 'DRAFT' THEN 1 END) as draft,
    (SELECT COUNT(*) FROM timetable_conflict WHERE timetable_id = tt.id) as conflicts
FROM timetable tt
GROUP BY tt.semester;
```

---

## Troubleshooting Checklist

- [ ] Database is running and accessible
- [ ] Application.properties has correct database URL
- [ ] Flyway migrations have executed successfully
- [ ] All required fields are populated before operations
- [ ] Conflict detection runs before publishing
- [ ] Proper error handling and logging is in place
- [ ] API responses match expected DTOs

---

## Next Steps

1. Implement authentication/authorization
2. Add email notifications for faculty changes
3. Create frontend dashboard
4. Integrate with student portal
5. Add analytics and reporting features
6. Implement schedule optimization algorithm

