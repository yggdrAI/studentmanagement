-- Smart Attendance Verification telemetry tables.
-- These tables support high-volume biometric attendance sessions without storing raw face images.

CREATE TABLE IF NOT EXISTS attendance_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(96) NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    classroom_id VARCHAR(96),
    campus_location_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    qr_rotation_seconds INT NOT NULL DEFAULT 10,
    max_distance_meters INT NOT NULL DEFAULT 150,
    face_verification_required BOOLEAN NOT NULL DEFAULT TRUE,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_attendance_session_token (tenant_id, session_id),
    INDEX idx_attendance_session_teacher (tenant_id, teacher_id, started_at),
    INDEX idx_attendance_session_subject (tenant_id, subject_id, started_at)
);

CREATE TABLE IF NOT EXISTS qr_token_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(96) NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    token_hash VARCHAR(96) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    teacher_latitude DOUBLE,
    teacher_longitude DOUBLE,
    nonce_hash VARCHAR(96),
    used_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_qr_token_event_hash (tenant_id, token_hash),
    INDEX idx_qr_token_event_session (tenant_id, session_id, issued_at)
);

CREATE TABLE IF NOT EXISTS attendance_scan_attempt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    session_id VARCHAR(96),
    student_id VARCHAR(64) NOT NULL,
    subject_id BIGINT,
    attendance_id BIGINT,
    token_hash VARCHAR(96),
    status VARCHAR(32) NOT NULL,
    final_trust_score DOUBLE,
    face_score DOUBLE,
    liveness_score DOUBLE,
    qr_score DOUBLE,
    location_score DOUBLE,
    device_score DOUBLE,
    behavioral_score DOUBLE,
    fraud_score DOUBLE,
    risk_level VARCHAR(32),
    rejection_reason TEXT,
    ip_address VARCHAR(96),
    device_fingerprint VARCHAR(128),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_scan_attempt_session (tenant_id, session_id, created_at),
    INDEX idx_scan_attempt_student (tenant_id, student_id, created_at),
    INDEX idx_scan_attempt_status (tenant_id, status, created_at)
);

CREATE TABLE IF NOT EXISTS liveness_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    scan_attempt_id BIGINT,
    student_id VARCHAR(64) NOT NULL,
    model_name VARCHAR(128),
    liveness_score DOUBLE,
    blink_detected BOOLEAN,
    head_movement_detected BOOLEAN,
    frame_count INT,
    motion_parallax_score DOUBLE,
    brightness_variance DOUBLE,
    passed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_liveness_student (tenant_id, student_id, created_at),
    INDEX idx_liveness_attempt (scan_attempt_id)
);

CREATE TABLE IF NOT EXISTS gps_verification_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    scan_attempt_id BIGINT,
    student_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(96),
    latitude DOUBLE,
    longitude DOUBLE,
    accuracy DOUBLE,
    campus_location_id BIGINT,
    distance_meters DOUBLE,
    location_verified BOOLEAN NOT NULL DEFAULT FALSE,
    mock_location_detected BOOLEAN,
    ip_gps_distance_km DOUBLE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_gps_session (tenant_id, session_id, created_at),
    INDEX idx_gps_student (tenant_id, student_id, created_at)
);

CREATE TABLE IF NOT EXISTS device_fingerprint_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    student_id VARCHAR(64) NOT NULL,
    device_fingerprint VARCHAR(128) NOT NULL,
    client_device_id VARCHAR(128),
    wifi_ssid_hash VARCHAR(128),
    wifi_bssid_hash VARCHAR(128),
    bluetooth_beacon_summary TEXT,
    emulator_detected BOOLEAN,
    rooted_or_jailbroken BOOLEAN,
    vpn_detected_by_client BOOLEAN,
    ip_address VARCHAR(96),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_fingerprint (tenant_id, device_fingerprint, created_at),
    INDEX idx_device_student (tenant_id, student_id, created_at)
);

CREATE TABLE IF NOT EXISTS attendance_anomaly_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    student_id VARCHAR(64),
    subject_id BIGINT,
    session_id VARCHAR(96),
    anomaly_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    risk_score DOUBLE NOT NULL DEFAULT 0,
    summary TEXT NOT NULL,
    evidence TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    INDEX idx_anomaly_session (tenant_id, session_id, created_at),
    INDEX idx_anomaly_student (tenant_id, student_id, created_at),
    INDEX idx_anomaly_status (tenant_id, status, created_at)
);
