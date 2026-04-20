-- Harden OTP flow: add retry tracking and indexes for password reset lookup

ALTER TABLE otp_verification
    ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_otp_user_used_created ON otp_verification(user_id, is_used, created_at);
CREATE INDEX IF NOT EXISTS idx_otp_user_reset_token ON otp_verification(user_id, reset_token);
