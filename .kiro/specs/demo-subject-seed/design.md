# Demo Subject Seed Bugfix Design

## Overview

On a fresh install, `DemoDataLoader.seedInitialAcademicData()` creates a demo teacher ("Dr. Rahul Sharma") and seeds two courses (CS301 "Algorithms", CS305 "Distributed Databases") assigned to that teacher. However, the teacher's attendance QR screen populates its subject dropdown via `GET /api/teacher/attendance/subjects`, which calls `courseRepository.findByTeacherId(teacher.getId())`. Because no "Java" subject is seeded and assigned to the demo teacher, the dropdown is empty and QR generation cannot proceed.

The fix adds a `seedDemoSubjectIfAbsent()` method to `DemoDataLoader` that creates a "Java" course and assigns it to the demo teacher, guarded by a `findByCourseNameIgnoreCase("Java")` existence check to prevent duplicates on restart.

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug — no course named "Java" is assigned to the demo teacher at application startup
- **Property (P)**: The desired behavior — after startup, `courseRepository.findByTeacherId(demoTeacher.getId())` returns a list containing a course named "Java"
- **Preservation**: The existing seeding behavior (CS301, CS305, student data, enrollments) and all runtime behavior that must remain unchanged by the fix
- **DemoDataLoader**: The `CommandLineRunner` in `src/main/java/com/sms/config/DemoDataLoader.java` that seeds initial demo data on every application startup
- **seedInitialAcademicData()**: The method that runs once when both `studentRepository.count() == 0` and `teacherRepository.count() == 0`, creating the demo teacher and two courses
- **findByTeacherId**: The `CourseRepository` query used by `TeacherAttendanceController.getTeacherSubjects()` to populate the subject dropdown

## Bug Details

### Bug Condition

The bug manifests when the application starts on a fresh install (or after a database wipe). The `seedInitialAcademicData()` method seeds CS301 and CS305 but does not seed a "Java" subject. The attendance QR subject dropdown is populated exclusively from courses assigned to the authenticated teacher, so if no "Java" course exists and is assigned to the demo teacher, the dropdown is empty.

**Formal Specification:**
```
FUNCTION isBugCondition(startupState)
  INPUT: startupState — the database state after DemoDataLoader.run() completes
  OUTPUT: boolean

  demoTeacher := teacherRepository.findAll()
                   .filter(t -> "Dr. Rahul Sharma".equals(t.getName()))
                   .findFirst()
  IF demoTeacher is absent THEN RETURN false

  javaCourse := courseRepository.findByCourseNameIgnoreCase("Java")
  IF javaCourse is absent THEN RETURN true

  RETURN javaCourse.getTeacher() IS NULL
      OR NOT javaCourse.getTeacher().getId().equals(demoTeacher.getId())
END FUNCTION
```

### Examples

- **Fresh install**: No courses exist, `seedInitialAcademicData()` runs, CS301 and CS305 are created but no "Java", `GET /subjects` returns `[]`, teacher cannot generate QR (bug)
- **After fix, fresh install**: `seedDemoSubjectIfAbsent()` runs, "Java" course created and assigned to demo teacher, `GET /subjects` returns `[{subjectId, "Java", ...}]`, QR generation succeeds (fixed)
- **After fix, restart with data**: "Java" already exists, existence check skips creation, no duplicate, `GET /subjects` still returns "Java" (preserved)
- **Edge case — no teacher**: Demo teacher does not exist yet; `seedDemoSubjectIfAbsent()` silently no-ops

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- The existing CS301 "Algorithms" and CS305 "Distributed Databases" courses and their enrollments must continue to be seeded exactly as before
- Student data, academic records, task items, class sessions, and documents seeded by `seedInitialAcademicData()` must remain unchanged
- The `studentRepository.count() == 0 && teacherRepository.count() == 0` guard on `seedInitialAcademicData()` must continue to function correctly
- All runtime endpoints (attendance recording, QR scanning, dashboard) must continue to work correctly for existing data

**Scope:**
All inputs that do NOT involve the "Java" subject existence check should be completely unaffected by this fix. This includes:
- Any existing courses (CS301, CS305, or user-created courses)
- Student enrollment and attendance records
- Teacher login and authentication flows
- Any other `CommandLineRunner` beans (e.g., `CampusLocationSeeder`)

## Hypothesized Root Cause

Based on the bug description and code analysis:

1. **Missing seed entry**: `seedInitialAcademicData()` seeds CS301 and CS305 but the "Java" subject was never added. The attendance QR feature was likely added after the initial seed data was written, and the seed was not updated to include a subject relevant to that feature.

2. **No fallback in the controller**: `TeacherAttendanceController.getTeacherSubjects()` returns whatever `courseRepository.findByTeacherId()` returns with no default — if the list is empty, the UI has nothing to show.

3. **Guard condition scope**: The `count() == 0` guard means `seedInitialAcademicData()` only runs once. Any new seed data added to that method will not be applied to existing installations. The fix must therefore be idempotent and run outside that guard.

## Correctness Properties

Property 1: Bug Condition - Java Subject Assigned to Demo Teacher After Startup

_For any_ application startup state where no course named "Java" is assigned to the demo teacher (isBugCondition returns true), the fixed `DemoDataLoader.run()` SHALL ensure that after completion, `courseRepository.findByTeacherId(demoTeacher.getId())` contains exactly one course with `courseName = "Java"`, making the attendance QR subject dropdown non-empty.

**Validates: Requirements 2.1, 2.2**

Property 2: Preservation - Idempotent Seeding on Restart

_For any_ application startup state where a course named "Java" already exists and is assigned to the demo teacher (isBugCondition returns false), the fixed `DemoDataLoader.run()` SHALL NOT create a duplicate "Java" course — the count of courses with `courseNameIgnoreCase = "Java"` SHALL remain exactly 1 after each run.

**Validates: Requirements 3.1, 3.2**

## Fix Implementation

### Changes Required

**File**: `src/main/java/com/sms/config/DemoDataLoader.java`

**Approach**: Add a new private method `seedDemoSubjectIfAbsent()` and call it from `run()` after the existing `seedInitialAcademicData()` guard block. This keeps the fix separate from the one-time guard so it is idempotent on restarts.

**Specific Changes**:

1. **Add `CourseRepository` dependency**: Inject `CourseRepository` into `DemoDataLoader` (it is not currently injected there).

2. **Add `seedDemoSubjectIfAbsent()` method**:
```
private void seedDemoSubjectIfAbsent() {
  if (courseRepository.findByCourseNameIgnoreCase("Java").isPresent()) return;
  teacherRepository.findAll().stream()
    .filter(t -> "Dr. Rahul Sharma".equals(t.getName()))
    .findFirst()
    .ifPresent(teacher -> {
      Course java = new Course();
      java.setCode("CS101");
      java.setCourseName("Java");
      java.setCredits(3);
      java.setTeacher(teacher);
      courseRepository.save(java);
    });
}
```

3. **Call from `run()`**: Add `seedDemoSubjectIfAbsent();` in `run()` after the `seedInitialAcademicData()` block.

4. **No changes to `seedInitialAcademicData()`**: The existing one-time seeding logic is untouched.

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on unfixed code, then verify the fix works correctly and preserves existing behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm or refute the root cause analysis.

**Test Plan**: Write a test that invokes `DemoDataLoader.run()` against a clean in-memory H2 database and then calls `courseRepository.findByTeacherId(demoTeacher.getId())`. Assert that the result contains a course named "Java". Run this test on the UNFIXED code to observe the failure.

**Test Cases**:
1. **Fresh install subjects test**: After `DemoDataLoader.run()` on empty DB, assert `findByTeacherId(demoTeacher.getId())` contains a course named "Java" (will fail on unfixed code)
2. **Subjects endpoint test**: After seeding, call `GET /api/teacher/attendance/subjects` as the demo teacher and assert the response is non-empty (will fail on unfixed code)
3. **QR generation test**: After seeding, attempt to generate a QR for the "Java" subject and assert HTTP 200 (will fail on unfixed code)
4. **Edge case — no teacher**: If the demo teacher does not exist, `seedDemoSubjectIfAbsent()` should silently no-op without throwing

**Expected Counterexamples**:
- `courseRepository.findByTeacherId(demoTeacher.getId())` returns a list that does not contain any course named "Java"
- Root cause confirmed: `seedInitialAcademicData()` never creates a "Java" course

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed `DemoDataLoader.run()` produces the expected behavior.

**Pseudocode:**
```
FOR ALL startupState WHERE isBugCondition(startupState) DO
  demoDataLoader.run()
  result := courseRepository.findByTeacherId(demoTeacher.getId())
  ASSERT result.stream().anyMatch(c -> "Java".equalsIgnoreCase(c.getCourseName()))
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, running `DemoDataLoader.run()` again does not create duplicate "Java" courses.

**Pseudocode:**
```
FOR ALL startupState WHERE NOT isBugCondition(startupState) DO
  countBefore := courseRepository.countByCourseNameIgnoreCase("Java")
  demoDataLoader.run()
  countAfter  := courseRepository.countByCourseNameIgnoreCase("Java")
  ASSERT countBefore == countAfter
END FOR
```

**Testing Approach**: Property-based testing is well-suited for preservation checking because:
- It can generate many restart scenarios (varying numbers of pre-existing courses) automatically
- It catches off-by-one errors in the existence check
- It provides strong guarantees that the idempotency guard works across all states

**Test Plan**: Observe that CS301 and CS305 are unaffected after the fix is applied, then write property-based tests that seed varying numbers of pre-existing "Java" courses and assert the count never increases beyond 1.

**Test Cases**:
1. **Idempotency test**: Run `DemoDataLoader.run()` twice; assert `findByCourseNameIgnoreCase("Java")` returns exactly 1 result
2. **Existing courses unaffected**: After fix, assert CS301 and CS305 still exist with correct teacher assignment
3. **Enrollment preservation**: After fix, assert existing student enrollments are unchanged

### Unit Tests

- Test `seedDemoSubjectIfAbsent()` on empty DB: assert "Java" course is created and assigned to demo teacher
- Test `seedDemoSubjectIfAbsent()` when "Java" already exists: assert no new course is created
- Test `seedDemoSubjectIfAbsent()` when demo teacher does not exist: assert no exception is thrown

### Property-Based Tests

- Generate random counts of pre-existing "Java" courses (0 or 1) and verify that after `run()`, the count is always exactly 1
- Generate random sets of non-"Java" courses and verify they are unaffected by `seedDemoSubjectIfAbsent()`
- Verify that running `run()` N times always results in exactly 1 "Java" course

### Integration Tests

- Full startup test: boot application against empty H2 DB, call `GET /api/teacher/attendance/subjects`, assert "Java" is in the response
- Restart test: boot twice, assert no duplicate "Java" entries in the subjects endpoint response
- QR generation flow: boot, select "Java" subject, call `POST /api/teacher/attendance/generate-qr`, assert HTTP 200
