-- Flyway Migration: Add extended student profile fields for dual email + team metadata
-- Version: 1.4

ALTER TABLE IF EXISTS student_profile
    ADD COLUMN IF NOT EXISTS university_email VARCHAR(255) NULL;

ALTER TABLE IF EXISTS student_profile
    ADD COLUMN IF NOT EXISTS personal_email VARCHAR(255) NULL;

ALTER TABLE IF EXISTS student_profile
    ADD COLUMN IF NOT EXISTS foundation_classroom VARCHAR(128) NULL;

ALTER TABLE IF EXISTS student_profile
    ADD COLUMN IF NOT EXISTS team_number INT NULL;

ALTER TABLE IF EXISTS student_profile
    ADD COLUMN IF NOT EXISTS member_number INT NULL;
