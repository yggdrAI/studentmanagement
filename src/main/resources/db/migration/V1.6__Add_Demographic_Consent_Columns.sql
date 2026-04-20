-- Flyway Migration: Add consent/source demographic columns to student_profile
-- Version: 1.6

ALTER TABLE IF EXISTS student_profile
    ADD COLUMN IF NOT EXISTS house VARCHAR(128) NULL;

ALTER TABLE IF EXISTS student_profile
    ADD COLUMN IF NOT EXISTS caste_category VARCHAR(128) NULL;

ALTER TABLE IF EXISTS student_profile
    ADD COLUMN IF NOT EXISTS gender_source VARCHAR(64) NULL;

ALTER TABLE IF EXISTS student_profile
    ADD COLUMN IF NOT EXISTS religion_source VARCHAR(64) NULL;

ALTER TABLE IF EXISTS student_profile
    ADD COLUMN IF NOT EXISTS caste_source VARCHAR(64) NULL;

ALTER TABLE IF EXISTS student_profile
    ADD COLUMN IF NOT EXISTS demographic_consent_given BOOLEAN NULL DEFAULT FALSE;

ALTER TABLE IF EXISTS student_profile
    ADD COLUMN IF NOT EXISTS demographic_consent_at TIMESTAMP NULL;

ALTER TABLE IF EXISTS student_profile
    ADD COLUMN IF NOT EXISTS demographic_consent_version VARCHAR(32) NULL;