-- Identity upgrade for multi-identifier login and OTP password reset

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS email VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS phone VARCHAR(32) NULL,
    ADD COLUMN IF NOT EXISTS is_verified_email BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_verified_phone BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS failed_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0;

UPDATE app_user
SET failed_login_attempts = COALESCE(failed_login_attempts, failed_attempts, 0);

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_username ON app_user(username);
CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_email ON app_user(email);
CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_phone ON app_user(phone);

CREATE TABLE IF NOT EXISTS otp_verification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    reset_token VARCHAR(128) NULL,
    reset_token_expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_otp_verification_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    INDEX idx_otp_user_created (user_id, created_at),
    INDEX idx_otp_user_code (user_id, otp_code),
    INDEX idx_otp_reset_token (reset_token)
);
