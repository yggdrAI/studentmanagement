-- Flyway Migration: Add import enrichment columns and normalize profile data
-- Version: 1.7

ALTER TABLE IF EXISTS student_import_row
    ADD COLUMN IF NOT EXISTS personal_email VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS foundation_classroom VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS team_number VARCHAR(32) NULL,
    ADD COLUMN IF NOT EXISTS member_number VARCHAR(32) NULL;

UPDATE student_profile
SET college = 'Bennett University'
WHERE college IS NULL
   OR TRIM(college) = ''
   OR LOWER(TRIM(college)) <> 'bennett university';

UPDATE student_profile
SET foundation_classroom = NULL
WHERE foundation_classroom IS NOT NULL
  AND house IS NOT NULL
  AND LOWER(TRIM(foundation_classroom)) = LOWER(TRIM(house));
