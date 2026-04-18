# Timetable Management System - Complete Documentation

## Overview

The **Timetable Management System** is a comprehensive Spring Boot-based solution for managing academic timetables. It provides features for creating, publishing, and managing class schedules with automatic conflict detection.

## Table of Contents

1. [Features](#features)
2. [Architecture](#architecture)
3. [Database Schema](#database-schema)
4. [API Endpoints](#api-endpoints)
5. [Usage Examples](#usage-examples)
6. [Configuration](#configuration)
7. [Deployment](#deployment)

---

## Features

### Core Features
- ✅ **Timetable Creation & Management** - Create and manage timetables for different courses/semesters
- ✅ **Schedule Entry Management** - Add, update, and remove individual class schedules
- ✅ **Conflict Detection** - Automatic detection of faculty and room clashes
- ✅ **Holiday Management** - Mark holidays and special days
- ✅ **Version Control** - Track all changes with version history
- ✅ **Publication Workflow** - Draft → Review → Publish workflow
- ✅ **Multi-tenant Support** - Support for multiple institutions

### Advanced Features
- 🔍 **Smart Conflict Resolution** - Intelligent conflict detection engine
- 📊 **Analytics & Reporting** - Timetable statistics and reports
- 🔐 **Audit Trail** - Complete audit history of changes
- 🔔 **Notifications** - Alerts for conflicts and changes
- 🌐 **Multi-format Export** - Export to PDF, Excel, ICS
- 📱 **Mobile-Responsive** - Access from any device

---

## Architecture

### Three-Tier Architecture

```
┌─────────────────────────────────────────────┐
│           REST API Layer                    │
│    (TimetableController, etc.)              │
├─────────────────────────────────────────────┤
│         Service Layer                       │
│    (TimetableService, etc.)                 │
├─────────────────────────────────────────────┤
│     Repository/DAO Layer                    │
│    (TimetableRepository, etc.)              │
├─────────────────────────────────────────────┤
│         Database Layer                      │
│       (MySQL/PostgreSQL)                    │
└─────────────────────────────────────────────┘
```

### Entity Relationships

```
Timetable (1) ──→ (Many) ScheduleEntry
    ↓
    ├─→ (Many) TimetableHoliday
    ├─→ (Many) TimetableVersion
    └─→ (Many) TimetableConflict
```

---

## Database Schema

### Tables

#### 1. `timetable`
Stores the main timetable information.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary Key |
| timetable_code | VARCHAR(64) | Unique timetable identifier |
| course_id | VARCHAR(64) | Course identifier |
| course_name | VARCHAR(255) | Full course name |
| semester | INT | Semester number |
| section | VARCHAR(32) | Section/batch code |
| academic_year | VARCHAR(16) | Academic year (e.g., 2025-26) |
| effective_from | DATE | Start date of timetable |
| effective_to | DATE | End date of timetable |
| status | VARCHAR(32) | DRAFT, PUBLISHED, ARCHIVED, CANCELLED |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |
| created_by | VARCHAR(128) | Creator user ID |
| updated_by | VARCHAR(128) | Last updater user ID |
| tenant_id | BIGINT | Tenant/Institution ID |

#### 2. `schedule_entry`
Individual class schedule entries.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary Key |
| timetable_id | BIGINT | Foreign Key to timetable |
| class_code | VARCHAR(64) | Unique class identifier |
| subject_id | VARCHAR(64) | Subject identifier |
| subject_name | VARCHAR(255) | Subject name |
| subject_code | VARCHAR(32) | Subject code |
| faculty_id | VARCHAR(64) | Faculty/Instructor ID |
| faculty_name | VARCHAR(255) | Faculty name |
| room_id | VARCHAR(64) | Room/Classroom ID |
| room_number | VARCHAR(32) | Room number |
| day_of_week | VARCHAR(32) | Day (MONDAY, TUESDAY, etc.) |
| schedule_date | DATE | Specific date (for exceptions) |
| start_time | TIME | Class start time |
| end_time | TIME | Class end time |
| class_type | VARCHAR(32) | LECTURE, TUTORIAL, PRACTICAL, SEMINAR, PROJECT |
| attendance_status | VARCHAR(32) | PENDING, ATTENDANCE_RECORDED, CANCELLED, RESCHEDULED |
| is_exception | BOOLEAN | Whether this is an exception |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |
| tenant_id | BIGINT | Tenant ID |

#### 3. `timetable_holiday`
Holiday and special day management.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary Key |
| timetable_id | BIGINT | Foreign Key to timetable |
| holiday_date | DATE | Holiday date |
| holiday_type | VARCHAR(32) | NATIONAL_HOLIDAY, INSTITUTIONAL_HOLIDAY, EXAM_DAY, etc. |
| reason | VARCHAR(255) | Holiday reason |
| description | TEXT | Detailed description |
| created_at | TIMESTAMP | Creation timestamp |
| tenant_id | BIGINT | Tenant ID |

#### 4. `timetable_version`
Version control and change tracking.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary Key |
| timetable_id | BIGINT | Foreign Key to timetable |
| version_number | INT | Version number |
| snapshot | LONGTEXT | JSON snapshot of timetable |
| change_description | VARCHAR(500) | Description of changes |
| change_type | VARCHAR(32) | CREATED, UPDATED, PUBLISHED, etc. |
| created_at | TIMESTAMP | Creation timestamp |
| created_by | VARCHAR(128) | User who made the change |
| tenant_id | BIGINT | Tenant ID |

#### 5. `timetable_conflict`
Conflict detection and tracking.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary Key |
| timetable_id | BIGINT | Foreign Key to timetable |
| schedule_entry_id_1 | BIGINT | First conflicting entry ID |
| schedule_entry_id_2 | BIGINT | Second conflicting entry ID |
| conflict_type | VARCHAR(32) | FACULTY_CLASH, ROOM_CLASH, STUDENT_OVERLAP, etc. |
| description | TEXT | Conflict description |
| resource_1 | VARCHAR(255) | First resource involved |
| resource_2 | VARCHAR(255) | Second resource involved |
| severity | VARCHAR(32) | LOW, MEDIUM, HIGH, CRITICAL |
| status | VARCHAR(32) | PENDING, REVIEWED, RESOLVED, IGNORED |
| resolution_suggestion | TEXT | Suggested resolution |
| resolved_at | TIMESTAMP | Resolution timestamp |
| created_at | TIMESTAMP | Detection timestamp |
| tenant_id | BIGINT | Tenant ID |

---

## API Endpoints

### Base URL
```
/api/v1/timetables
```

### 1. Create Timetable
**POST** `/api/v1/timetables`

**Request Body:**
```json
{
  "courseId": "CSE-B.TECH",
  "courseName": "B.Tech Computer Science Engineering",
  "semester": 2,
  "section": "CSE-2A",
  "academicYear": "2025-2026",
  "effectiveFrom": "2025-01-19",
  "effectiveTo": "2025-05-30"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "timetableCode": "TT-CSE-B.TECH-SEM2-20252026",
  "courseId": "CSE-B.TECH",
  "courseName": "B.Tech Computer Science Engineering",
  "semester": 2,
  "section": "CSE-2A",
  "academicYear": "2025-2026",
  "status": "DRAFT",
  "createdAt": "2025-01-15T10:30:00Z",
  "updatedAt": "2025-01-15T10:30:00Z"
}
```

### 2. Get Timetable
**GET** `/api/v1/timetables/{id}`

**Response (200 OK):**
```json
{
  "id": 1,
  "timetableCode": "TT-CSE-B.TECH-SEM2-20252026",
  "courseId": "CSE-B.TECH",
  "courseName": "B.Tech Computer Science Engineering",
  "semester": 2,
  "section": "CSE-2A",
  "academicYear": "2025-2026",
  "status": "DRAFT",
  "scheduleEntries": [
    {
      "id": 101,
      "classCode": "CLASS-001",
      "subjectName": "Object Oriented Programming using Java",
      "facultyName": "Dr. Rajesh Kumar Sharma",
      "roomNumber": "P-LH-101",
      "dayOfWeek": "MONDAY",
      "startTime": "08:20",
      "endTime": "09:20",
      "classType": "LECTURE",
      "attendanceStatus": "PENDING"
    }
  ]
}
```

### 3. Add Schedule Entries
**POST** `/api/v1/timetables/{id}/schedule-entries`

**Request Body:**
```json
[
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
  }
]
```

**Response (201 Created):**
```
HTTP/1.1 201 Created
```

### 4. Get Timetable for Specific Day
**GET** `/api/v1/timetables/{id}/day?date=2025-01-20`

**Response (200 OK):**
```json
[
  {
    "id": 101,
    "subjectName": "Object Oriented Programming using Java",
    "facultyName": "Dr. Rajesh Kumar Sharma",
    "roomNumber": "P-LH-101",
    "startTime": "08:20",
    "endTime": "09:20",
    "classType": "LECTURE"
  },
  {
    "id": 102,
    "subjectName": "Discrete Mathematical Structures",
    "facultyName": "Prof. Anjali Singh",
    "roomNumber": "P-LH-102",
    "startTime": "09:30",
    "endTime": "10:30",
    "classType": "LECTURE"
  }
]
```

### 5. Detect Conflicts
**POST** `/api/v1/timetables/{id}/detect-conflicts`

**Response (200 OK):**
```
HTTP/1.1 200 OK
```

### 6. Publish Timetable
**POST** `/api/v1/timetables/{id}/publish?publishedBy=admin123`

**Response (200 OK):**
```
HTTP/1.1 200 OK
```

### 7. Add Holiday
**POST** `/api/v1/timetables/{id}/holidays?date=2025-01-26&type=NATIONAL_HOLIDAY&reason=Republic%20Day`

**Response (201 Created):**
```
HTTP/1.1 201 Created
```

### 8. Get Active Timetables
**GET** `/api/v1/timetables/active?date=2025-01-20`

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "timetableCode": "TT-CSE-B.TECH-SEM2-20252026",
    "courseName": "B.Tech Computer Science Engineering",
    "status": "PUBLISHED"
  }
]
```

---

## Usage Examples

### Using cURL

#### Create a Timetable
```bash
curl -X POST http://localhost:8080/api/v1/timetables \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "CSE-B.TECH",
    "courseName": "B.Tech Computer Science Engineering",
    "semester": 2,
    "section": "CSE-2A",
    "academicYear": "2025-2026",
    "effectiveFrom": "2025-01-19"
  }'
```

#### Add Schedule Entries
```bash
curl -X POST http://localhost:8080/api/v1/timetables/1/schedule-entries \
  -H "Content-Type: application/json" \
  -d '[
    {
      "subjectId": "SUBJ-001",
      "subjectName": "Object Oriented Programming",
      "facultyId": "FAC-001",
      "facultyName": "Dr. Rajesh Kumar Sharma",
      "roomId": "ROOM-001",
      "roomNumber": "P-LH-101",
      "dayOfWeek": "MONDAY",
      "startTime": "08:20",
      "endTime": "09:20",
      "classType": "LECTURE"
    }
  ]'
```

#### Detect Conflicts
```bash
curl -X POST http://localhost:8080/api/v1/timetables/1/detect-conflicts
```

#### Publish Timetable
```bash
curl -X POST "http://localhost:8080/api/v1/timetables/1/publish?publishedBy=admin"
```

### Using Postman

1. Import the following collection into Postman
2. Set up environment variable: `base_url = http://localhost:8080`
3. Execute endpoints in sequence

---

## Configuration

### application.properties
```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/sms

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/student_management
spring.datasource.username=root
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=validate

# Flyway Configuration
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# Logging
logging.level.com.sms=INFO
logging.level.org.hibernate.SQL=DEBUG
```

### application-postgres.properties
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/student_management
spring.datasource.username=postgres
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL10Dialect
```

---

## Deployment

### Docker Deployment

#### Dockerfile
```dockerfile
FROM openjdk:21-slim
WORKDIR /app
COPY target/studentmanagement-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Build and Run
```bash
# Build
docker build -t studentmanagement:1.0 .

# Run
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/student_management \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=password \
  studentmanagement:1.0
```

### Kubernetes Deployment

#### deployment.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: studentmanagement
spec:
  replicas: 3
  selector:
    matchLabels:
      app: studentmanagement
  template:
    metadata:
      labels:
        app: studentmanagement
    spec:
      containers:
      - name: studentmanagement
        image: studentmanagement:1.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: db_url
```

---

## Best Practices

1. **Always Detect Conflicts** - Run conflict detection before publishing
2. **Version Control** - Keep track of all versions
3. **Regular Backups** - Backup timetable data regularly
4. **Audit Trail** - Review audit logs for compliance
5. **Multi-tenant Isolation** - Ensure proper tenant isolation

---

## Troubleshooting

### Common Issues

1. **Conflict Detection Not Working**
   - Ensure schedule entries have proper time ranges
   - Check faculty and room IDs are correctly set

2. **Publication Failed**
   - Check for unresolved conflicts
   - Verify all required fields are populated

3. **Database Connection Error**
   - Verify database credentials
   - Check database is running and accessible

---

## Support & Contact

For issues, questions, or feature requests, please contact:
- Email: support@bennett.edu.in
- Documentation: [https://docs.example.com](https://docs.example.com)

---

**Last Updated:** January 2025
**Version:** 1.0
