-- Flyway Migration: Create Timetable Management Tables
-- Version: 1.0

-- Create timetable table
CREATE TABLE IF NOT EXISTS timetable (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    timetable_code VARCHAR(64) NOT NULL UNIQUE,
    course_id VARCHAR(64) NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    semester INT NOT NULL,
    section VARCHAR(32),
    academic_year VARCHAR(16) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(128),
    updated_by VARCHAR(128),
    tenant_id BIGINT NOT NULL,
    INDEX idx_timetable_course_sem (course_id, semester),
    INDEX idx_timetable_academic_year (academic_year),
    INDEX idx_timetable_effective (effective_from)
);

-- Create schedule_entry table
CREATE TABLE IF NOT EXISTS schedule_entry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    timetable_id BIGINT NOT NULL,
    class_code VARCHAR(64) UNIQUE,
    subject_id VARCHAR(64) NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    subject_code VARCHAR(32) NOT NULL,
    faculty_id VARCHAR(64) NOT NULL,
    faculty_name VARCHAR(255) NOT NULL,
    room_id VARCHAR(64) NOT NULL,
    room_number VARCHAR(32) NOT NULL,
    day_of_week VARCHAR(32) NOT NULL,
    schedule_date DATE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    class_type VARCHAR(32) NOT NULL,
    attendance_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    is_exception BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL,
    FOREIGN KEY (timetable_id) REFERENCES timetable(id) ON DELETE CASCADE,
    INDEX idx_schedule_timetable (timetable_id),
    INDEX idx_schedule_subject (subject_id),
    INDEX idx_schedule_faculty (faculty_id),
    INDEX idx_schedule_room (room_id),
    INDEX idx_schedule_date (schedule_date)
);

-- Create timetable_holiday table
CREATE TABLE IF NOT EXISTS timetable_holiday (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    timetable_id BIGINT NOT NULL,
    holiday_date DATE NOT NULL,
    holiday_type VARCHAR(32) NOT NULL,
    reason VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL,
    FOREIGN KEY (timetable_id) REFERENCES timetable(id) ON DELETE CASCADE,
    INDEX idx_holiday_timetable (timetable_id),
    INDEX idx_holiday_date (holiday_date)
);

-- Create timetable_version table
CREATE TABLE IF NOT EXISTS timetable_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    timetable_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    snapshot LONGTEXT NOT NULL,
    change_description VARCHAR(500),
    change_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(128),
    tenant_id BIGINT NOT NULL,
    FOREIGN KEY (timetable_id) REFERENCES timetable(id) ON DELETE CASCADE,
    UNIQUE KEY unique_version (timetable_id, version_number),
    INDEX idx_version_timetable (timetable_id),
    INDEX idx_version_number (version_number)
);

-- Create timetable_conflict table
CREATE TABLE IF NOT EXISTS timetable_conflict (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    timetable_id BIGINT NOT NULL,
    schedule_entry_id_1 BIGINT,
    schedule_entry_id_2 BIGINT,
    conflict_type VARCHAR(32) NOT NULL,
    description TEXT,
    resource_1 VARCHAR(255),
    resource_2 VARCHAR(255),
    severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    resolution_suggestion TEXT,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT NOT NULL,
    FOREIGN KEY (timetable_id) REFERENCES timetable(id) ON DELETE CASCADE,
    INDEX idx_conflict_timetable (timetable_id),
    INDEX idx_conflict_status (status)
);

-- Create triggers for audit
DELIMITER //

CREATE TRIGGER timetable_audit_update BEFORE UPDATE ON timetable
FOR EACH ROW
BEGIN
    SET NEW.updated_at = CURRENT_TIMESTAMP;
END//

CREATE TRIGGER schedule_entry_audit_update BEFORE UPDATE ON schedule_entry
FOR EACH ROW
BEGIN
    SET NEW.updated_at = CURRENT_TIMESTAMP;
END//

DELIMITER ;
