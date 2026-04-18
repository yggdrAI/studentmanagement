-- Sample Data for Timetable Management System
-- This script populates the database with sample data for testing and demonstration

-- Insert Timetable
INSERT INTO timetable (
    timetable_code, course_id, course_name, semester, section, 
    academic_year, effective_from, effective_to, status, tenant_id, created_by
) VALUES (
    'TT-CSE-B.TECH-SEM2-20252026', 
    'CSE-B.TECH', 
    'B.Tech Computer Science Engineering', 
    2, 
    'CSE-2A', 
    '2025-2026', 
    '2025-01-19', 
    '2025-05-30', 
    'DRAFT', 
    1, 
    'admin@bennett.edu.in'
);

-- Get the inserted timetable ID (assuming it's 1)
SET @timetable_id = 1;

-- Insert Monday Schedule Entries
INSERT INTO schedule_entry (
    timetable_id, class_code, subject_id, subject_name, subject_code,
    faculty_id, faculty_name, room_id, room_number, day_of_week,
    start_time, end_time, class_type, attendance_status, tenant_id
) VALUES
-- Monday Classes
(@timetable_id, 'CLASS-MON-001', 'SUBJ-001', 'Object Oriented Programming using Java', '2025CSET152',
 'FAC-001', 'Dr. Rajesh Kumar Sharma', 'ROOM-001', 'P-LH-101', 'MONDAY',
 '08:20', '09:20', 'LECTURE', 'PENDING', 1),

(@timetable_id, 'CLASS-MON-002', 'SUBJ-002', 'Discrete Mathematical Structures', '2025CSEM151',
 'FAC-002', 'Prof. Anjali Singh', 'ROOM-002', 'P-LH-102', 'MONDAY',
 '09:30', '10:30', 'LECTURE', 'PENDING', 1),

(@timetable_id, 'CLASS-MON-003', 'SUBJ-003', 'Linear Algebra and Ordinary Differential Equations', '2025CSEM152',
 'FAC-003', 'Dr. Vikram Patel', 'ROOM-003', 'P-LH-103', 'MONDAY',
 '10:40', '11:40', 'LECTURE', 'PENDING', 1),

(@timetable_id, 'CLASS-MON-004', 'SUBJ-004', 'Digital Design', '2025CSET153',
 'FAC-004', 'Dr. Neha Gupta', 'ROOM-008', 'P-CC-018', 'MONDAY',
 '12:30', '13:30', 'LECTURE', 'PENDING', 1),

(@timetable_id, 'CLASS-MON-005', 'SUBJ-001', 'Object Oriented Programming using Java', '2025CSET152',
 'FAC-001', 'Dr. Rajesh Kumar Sharma', 'ROOM-013', 'P-LA-302', 'MONDAY',
 '13:40', '14:40', 'TUTORIAL', 'PENDING', 1),

(@timetable_id, 'CLASS-MON-006', 'SUBJ-006', 'Soft Skills and Personality Development', '2025CSHS151',
 'FAC-006', 'Ms. Priya Desai', 'ROOM-007', 'P-CC-012', 'MONDAY',
 '14:50', '15:50', 'LECTURE', 'PENDING', 1),

-- Tuesday Classes
(@timetable_id, 'CLASS-TUE-001', 'SUBJ-007', 'Introduction to Electrical and Electronics Engineering', '2025CSED151',
 'FAC-007', 'Prof. Arun Mishra', 'ROOM-004', 'P-LH-104', 'TUESDAY',
 '08:20', '09:20', 'LECTURE', 'PENDING', 1),

(@timetable_id, 'CLASS-TUE-002', 'SUBJ-002', 'Discrete Mathematical Structures', '2025CSEM151',
 'FAC-002', 'Prof. Anjali Singh', 'ROOM-002', 'P-LH-102', 'TUESDAY',
 '09:30', '10:30', 'TUTORIAL', 'PENDING', 1),

(@timetable_id, 'CLASS-TUE-003', 'SUBJ-005', 'Environment and Sustainability', '2025CSHS152',
 'FAC-005', 'Dr. Meera Joshi', 'ROOM-005', 'P-LH-105', 'TUESDAY',
 '10:40', '11:40', 'LECTURE', 'PENDING', 1),

(@timetable_id, 'CLASS-TUE-004', 'SUBJ-004', 'Digital Design', '2025CSET153',
 'FAC-004', 'Dr. Neha Gupta', 'ROOM-009', 'P-CC-019', 'TUESDAY',
 '12:30', '13:30', 'TUTORIAL', 'PENDING', 1),

(@timetable_id, 'CLASS-TUE-005', 'SUBJ-001', 'Object Oriented Programming using Java', '2025CSET152',
 'FAC-001', 'Dr. Rajesh Kumar Sharma', 'ROOM-012', 'P-LA-301', 'TUESDAY',
 '13:40', '14:40', 'PRACTICAL', 'PENDING', 1),

(@timetable_id, 'CLASS-TUE-006', 'SUBJ-001', 'Object Oriented Programming using Java', '2025CSET152',
 'FAC-001', 'Dr. Rajesh Kumar Sharma', 'ROOM-012', 'P-LA-301', 'TUESDAY',
 '14:50', '15:50', 'PRACTICAL', 'PENDING', 1),

-- Wednesday Classes
(@timetable_id, 'CLASS-WED-001', 'SUBJ-003', 'Linear Algebra and Ordinary Differential Equations', '2025CSEM152',
 'FAC-003', 'Dr. Vikram Patel', 'ROOM-003', 'P-LH-103', 'WEDNESDAY',
 '08:20', '09:20', 'TUTORIAL', 'PENDING', 1),

(@timetable_id, 'CLASS-WED-002', 'SUBJ-002', 'Discrete Mathematical Structures', '2025CSEM151',
 'FAC-002', 'Prof. Anjali Singh', 'ROOM-002', 'P-LH-102', 'WEDNESDAY',
 '09:30', '10:30', 'LECTURE', 'PENDING', 1),

(@timetable_id, 'CLASS-WED-003', 'SUBJ-004', 'Digital Design', '2025CSET153',
 'FAC-004', 'Dr. Neha Gupta', 'ROOM-014', 'P-LA-401', 'WEDNESDAY',
 '10:40', '11:40', 'PRACTICAL', 'PENDING', 1),

(@timetable_id, 'CLASS-WED-004', 'SUBJ-004', 'Digital Design', '2025CSET153',
 'FAC-004', 'Dr. Neha Gupta', 'ROOM-014', 'P-LA-401', 'WEDNESDAY',
 '12:30', '13:30', 'PRACTICAL', 'PENDING', 1),

(@timetable_id, 'CLASS-WED-005', 'SUBJ-007', 'Introduction to Electrical and Electronics Engineering', '2025CSED151',
 'FAC-007', 'Prof. Arun Mishra', 'ROOM-008', 'P-CC-020', 'WEDNESDAY',
 '13:40', '14:40', 'TUTORIAL', 'PENDING', 1),

(@timetable_id, 'CLASS-WED-006', 'SUBJ-005', 'Environment and Sustainability', '2025CSHS152',
 'FAC-005', 'Dr. Meera Joshi', 'ROOM-005', 'P-LH-105', 'WEDNESDAY',
 '14:50', '15:50', 'TUTORIAL', 'PENDING', 1),

-- Thursday Classes
(@timetable_id, 'CLASS-THU-001', 'SUBJ-001', 'Object Oriented Programming using Java', '2025CSET152',
 'FAC-001', 'Dr. Rajesh Kumar Sharma', 'ROOM-001', 'P-LH-101', 'THURSDAY',
 '08:20', '09:20', 'LECTURE', 'PENDING', 1),

(@timetable_id, 'CLASS-THU-002', 'SUBJ-003', 'Linear Algebra and Ordinary Differential Equations', '2025CSEM152',
 'FAC-003', 'Dr. Vikram Patel', 'ROOM-003', 'P-LH-103', 'THURSDAY',
 '09:30', '10:30', 'LECTURE', 'PENDING', 1),

(@timetable_id, 'CLASS-THU-003', 'SUBJ-007', 'Introduction to Electrical and Electronics Engineering', '2025CSED151',
 'FAC-007', 'Prof. Arun Mishra', 'ROOM-015', 'P-LA-501', 'THURSDAY',
 '10:40', '11:40', 'PRACTICAL', 'PENDING', 1),

(@timetable_id, 'CLASS-THU-004', 'SUBJ-007', 'Introduction to Electrical and Electronics Engineering', '2025CSED151',
 'FAC-007', 'Prof. Arun Mishra', 'ROOM-015', 'P-LA-501', 'THURSDAY',
 '12:30', '13:30', 'PRACTICAL', 'PENDING', 1),

(@timetable_id, 'CLASS-THU-005', 'SUBJ-002', 'Discrete Mathematical Structures', '2025CSEM151',
 'FAC-002', 'Prof. Anjali Singh', 'ROOM-008', 'P-CC-018', 'THURSDAY',
 '13:40', '14:40', 'LECTURE', 'PENDING', 1),

(@timetable_id, 'CLASS-THU-006', 'SUBJ-006', 'Soft Skills and Personality Development', '2025CSHS151',
 'FAC-006', 'Ms. Priya Desai', 'ROOM-007', 'P-CC-012', 'THURSDAY',
 '14:50', '15:50', 'LECTURE', 'PENDING', 1),

-- Friday Classes
(@timetable_id, 'CLASS-FRI-001', 'SUBJ-004', 'Digital Design', '2025CSET153',
 'FAC-004', 'Dr. Neha Gupta', 'ROOM-004', 'P-LH-104', 'FRIDAY',
 '08:20', '09:20', 'LECTURE', 'PENDING', 1),

(@timetable_id, 'CLASS-FRI-002', 'SUBJ-003', 'Linear Algebra and Ordinary Differential Equations', '2025CSEM152',
 'FAC-003', 'Dr. Vikram Patel', 'ROOM-003', 'P-LH-103', 'FRIDAY',
 '09:30', '10:30', 'LECTURE', 'PENDING', 1),

(@timetable_id, 'CLASS-FRI-003', 'SUBJ-005', 'Environment and Sustainability', '2025CSHS152',
 'FAC-005', 'Dr. Meera Joshi', 'ROOM-010', 'P-CC-021', 'FRIDAY',
 '10:40', '11:40', 'LECTURE', 'PENDING', 1),

(@timetable_id, 'CLASS-FRI-004', 'SUBJ-006', 'Soft Skills and Personality Development', '2025CSHS151',
 'FAC-006', 'Ms. Priya Desai', 'ROOM-005', 'P-LH-105', 'FRIDAY',
 '12:30', '13:30', 'TUTORIAL', 'PENDING', 1),

(@timetable_id, 'CLASS-FRI-005', 'SUBJ-007', 'Introduction to Electrical and Electronics Engineering', '2025CSED151',
 'FAC-007', 'Prof. Arun Mishra', 'ROOM-006', 'P-LH-106', 'FRIDAY',
 '13:40', '14:40', 'LECTURE', 'PENDING', 1);

-- Insert Holidays
INSERT INTO timetable_holiday (
    timetable_id, holiday_date, holiday_type, reason, tenant_id
) VALUES
(@timetable_id, '2025-01-26', 'NATIONAL_HOLIDAY', 'Republic Day', 1),
(@timetable_id, '2025-02-15', 'INSTITUTIONAL_HOLIDAY', 'Foundation Day', 1),
(@timetable_id, '2025-03-08', 'NATIONAL_HOLIDAY', 'Maha Shivaratri', 1),
(@timetable_id, '2025-04-14', 'NATIONAL_HOLIDAY', 'Ambedkar Jayanti', 1),
(@timetable_id, '2025-05-01', 'EXAM_DAY', 'Mid-Term Examinations Start', 1);

-- Insert Initial Version
INSERT INTO timetable_version (
    timetable_id, version_number, snapshot, change_description, change_type, created_by, tenant_id
) VALUES
(@timetable_id, 1, '{}', 'Timetable created with sample data', 'CREATED', 'admin@bennett.edu.in', 1);

-- Verification Queries
-- SELECT COUNT(*) as total_entries FROM schedule_entry WHERE timetable_id = @timetable_id;
-- SELECT * FROM timetable WHERE id = @timetable_id;
-- SELECT day_of_week, COUNT(*) as class_count FROM schedule_entry WHERE timetable_id = @timetable_id GROUP BY day_of_week;
