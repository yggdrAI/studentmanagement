-- Flyway Migration: RBAC role-profile isolation tables
-- Version: 1.2

CREATE TABLE IF NOT EXISTS admin_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    display_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(32),
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_admin_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_admin_profiles_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    INDEX idx_admin_profiles_tenant (tenant_id)
);

CREATE TABLE IF NOT EXISTS teacher_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    employee_code VARCHAR(64),
    full_name VARCHAR(255),
    email VARCHAR(255),
    department VARCHAR(128),
    designation VARCHAR(128),
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_teacher_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_teacher_profiles_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    INDEX idx_teacher_profiles_tenant (tenant_id),
    INDEX idx_teacher_profiles_department (department)
);

CREATE TABLE IF NOT EXISTS student_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    student_code VARCHAR(64),
    full_name VARCHAR(255),
    email VARCHAR(255),
    program VARCHAR(128),
    section VARCHAR(64),
    semester VARCHAR(32),
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    INDEX idx_student_profiles_tenant (tenant_id),
    INDEX idx_student_profiles_program_section (program, section)
);
