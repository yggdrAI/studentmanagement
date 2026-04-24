# Bugfix Requirements Document

## Introduction

When a teacher logs in and navigates to the attendance QR generation screen, the subject dropdown is empty because no subjects (courses) are assigned to the demo teacher. This prevents the teacher from selecting a subject and generating an attendance QR code. The fix seeds a demo subject called "Java" and assigns it to the demo teacher so the dropdown is populated on a fresh install.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a teacher opens the attendance QR screen on a fresh install THEN the system displays an empty subject list with no selectable options
1.2 WHEN a teacher attempts to generate an attendance QR code on a fresh install THEN the system cannot proceed because no subject is available to select

### Expected Behavior (Correct)

2.1 WHEN a teacher opens the attendance QR screen on a fresh install THEN the system SHALL display at least one subject ("Java") in the subject dropdown
2.2 WHEN a teacher selects the "Java" subject and generates an attendance QR code THEN the system SHALL successfully generate and display the QR code

### Unchanged Behavior (Regression Prevention)

3.1 WHEN subjects already exist in the database at startup THEN the system SHALL CONTINUE TO load without seeding duplicate subjects
3.2 WHEN a teacher has existing course assignments THEN the system SHALL CONTINUE TO display those courses correctly in the subject dropdown
3.3 WHEN a student scans an attendance QR code for an existing subject THEN the system SHALL CONTINUE TO record attendance correctly
