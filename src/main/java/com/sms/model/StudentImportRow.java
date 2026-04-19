package com.sms.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_import_row")
public class StudentImportRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private StudentImportJob job;

    @Column(name = "row_index", nullable = false)
    private int rowIndex;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "enrollment_number")
    private String enrollmentNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "course")
    private String course;

    @Column(name = "semester")
    private String semester;

    @Column(name = "department")
    private String department;

    @Column(name = "section")
    private String section;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(name = "guardian_name")
    private String guardianName;

    @Column(name = "roll_number")
    private String rollNumber;

    @Column(name = "program")
    private String program;

    @Column(name = "school")
    private String school;

    @Column(name = "house")
    private String house;

    @Column(name = "joining_year")
    private String joiningYear;

    @Column(name = "leaving_year")
    private String leavingYear;

    @Column(name = "class_name")
    private String className;

    @Column(name = "source_file_name", length = 255)
    private String sourceFileName;

    @Column(name = "identity_key", length = 256)
    private String identityKey;

    @Column(name = "merge_group_key", length = 256)
    private String mergeGroupKey;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "status", length = 32)
    private String status = "PENDING";

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "normalized_enrollment", length = 128)
    private String normalizedEnrollment;

    @Column(name = "created_student_id", length = 64)
    private String createdStudentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public StudentImportJob getJob() { return job; }
    public void setJob(StudentImportJob job) { this.job = job; }
    public int getRowIndex() { return rowIndex; }
    public void setRowIndex(int rowIndex) { this.rowIndex = rowIndex; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEnrollmentNumber() { return enrollmentNumber; }
    public void setEnrollmentNumber(String enrollmentNumber) { this.enrollmentNumber = enrollmentNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }
    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }
    public String getHouse() { return house; }
    public void setHouse(String house) { this.house = house; }
    public String getJoiningYear() { return joiningYear; }
    public void setJoiningYear(String joiningYear) { this.joiningYear = joiningYear; }
    public String getLeavingYear() { return leavingYear; }
    public void setLeavingYear(String leavingYear) { this.leavingYear = leavingYear; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }
    public String getIdentityKey() { return identityKey; }
    public void setIdentityKey(String identityKey) { this.identityKey = identityKey; }
    public String getMergeGroupKey() { return mergeGroupKey; }
    public void setMergeGroupKey(String mergeGroupKey) { this.mergeGroupKey = mergeGroupKey; }
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getNormalizedEnrollment() { return normalizedEnrollment; }
    public void setNormalizedEnrollment(String normalizedEnrollment) { this.normalizedEnrollment = normalizedEnrollment; }
    public String getCreatedStudentId() { return createdStudentId; }
    public void setCreatedStudentId(String createdStudentId) { this.createdStudentId = createdStudentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
