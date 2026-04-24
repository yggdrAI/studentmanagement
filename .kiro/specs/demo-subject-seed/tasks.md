# Implementation Plan

- [-] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Java Subject Missing After Fresh Install
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists
  - **Scoped PBT Approach**: Scope the property to the concrete failing case — fresh install (empty DB) with demo teacher seeded
  - In a Spring Boot test with an in-memory H2 database, invoke `DemoDataLoader.run()` on an empty DB
  - Assert that `courseRepository.findByTeacherId(demoTeacher.getId())` contains a course with `courseName = "Java"` (from Bug Condition in design)
  - Also assert `GET /api/teacher/attendance/subjects` as the demo teacher returns a non-empty list
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the bug exists; counterexample: `findByTeacherId` returns a list with no "Java" entry)
  - Document counterexamples found (e.g., "findByTeacherId returns [CS301, CS305] — no Java course present")
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2_

- [~] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Idempotent Seeding and Existing Data Unaffected
  - **IMPORTANT**: Follow observation-first methodology
  - Observe: after `DemoDataLoader.run()` on unfixed code, CS301 and CS305 exist and are assigned to the demo teacher
  - Observe: running `DemoDataLoader.run()` a second time does not duplicate CS301 or CS305
  - Write property-based test: for any startup state where a "Java" course already exists and is assigned to the demo teacher, running `DemoDataLoader.run()` again SHALL NOT increase the count of courses named "Java" beyond 1 (from Preservation Requirements in design)
  - Also write property-based test: for random sets of non-"Java" courses, assert they are unaffected after `run()`
  - Verify tests PASS on UNFIXED code (baseline behavior confirmed)
  - **EXPECTED OUTCOME**: Tests PASS (confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3_

- [ ] 3. Fix for missing "Java" subject seed in DemoDataLoader

  - [~] 3.1 Inject CourseRepository into DemoDataLoader
    - Add `@Autowired` (or constructor injection) for `CourseRepository` in `src/main/java/com/sms/config/DemoDataLoader.java`
    - Confirm `CourseRepository` exposes `findByCourseNameIgnoreCase(String name)` returning `Optional<Course>`
    - _Bug_Condition: isBugCondition(startupState) — no course named "Java" assigned to demo teacher after run()_
    - _Requirements: 2.1_

  - [~] 3.2 Add seedDemoSubjectIfAbsent() method to DemoDataLoader
    - Implement the private method as specified in the design:
      - Guard: `if (courseRepository.findByCourseNameIgnoreCase("Java").isPresent()) return;`
      - Find demo teacher by name "Dr. Rahul Sharma" via `teacherRepository.findAll().stream()`
      - If teacher found, create `Course` with code "CS101", name "Java", credits 3, assign teacher, save
      - If teacher not found, silently no-op (edge case from design)
    - _Bug_Condition: isBugCondition(startupState) where javaCourse is absent or unassigned_
    - _Expected_Behavior: courseRepository.findByTeacherId(demoTeacher.getId()) contains exactly one course with courseName = "Java"_
    - _Preservation: findByCourseNameIgnoreCase guard ensures count of "Java" courses never exceeds 1; seedInitialAcademicData() is untouched_
    - _Requirements: 2.1, 2.2, 3.1, 3.2_

  - [~] 3.3 Call seedDemoSubjectIfAbsent() from run()
    - In `DemoDataLoader.run()`, add `seedDemoSubjectIfAbsent();` after the `seedInitialAcademicData()` guard block
    - Do NOT modify `seedInitialAcademicData()` itself
    - _Requirements: 2.1, 2.2, 3.1_

  - [~] 3.4 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Java Subject Assigned to Demo Teacher After Startup
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior
    - When this test passes, it confirms `courseRepository.findByTeacherId(demoTeacher.getId())` contains "Java"
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: 2.1, 2.2_

  - [~] 3.5 Verify preservation tests still pass
    - **Property 2: Preservation** - Idempotent Seeding and Existing Data Unaffected
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions — CS301, CS305, enrollments, and all other seeded data unaffected)
    - Confirm all tests still pass after fix (no regressions)

- [~] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
