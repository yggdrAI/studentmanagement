package com.sms.dto.profile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StudentProfileResponseDTO {
    private String studentId;
    private String fullName;
    private String enrollmentNumber;
    private String profileImage;

    private LocalDate dob;
    private String gender;
    private String religion;
    private String bloodGroup;

    private String phone;
    private String email;
    private String universityEmail;
    private String personalEmail;
    private String address;

    private String guardianName;
    private String guardianPhone;

    private String college;
    private String course;
    private String department;
    private String semester;
    private String section;
    private String foundationClassroom;
    private Integer teamNumber;
    private Integer memberNumber;
    private Integer admissionYear;
    private Integer passingYear;

    private LocalDate validUpto;
    private String idCardNumber;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;

    private int completionPercentage;
    private String profileQrUrl;
    private boolean adminEditable;
    private String viewerRole;

    private List<StudentDocumentDTO> documents = new ArrayList<>();
    private List<AcademicRecordDTO> academicRecords = new ArrayList<>();

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEnrollmentNumber() { return enrollmentNumber; }
    public void setEnrollmentNumber(String enrollmentNumber) { this.enrollmentNumber = enrollmentNumber; }
    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUniversityEmail() { return universityEmail; }
    public void setUniversityEmail(String universityEmail) { this.universityEmail = universityEmail; }
    public String getPersonalEmail() { return personalEmail; }
    public void setPersonalEmail(String personalEmail) { this.personalEmail = personalEmail; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }
    public String getGuardianPhone() { return guardianPhone; }
    public void setGuardianPhone(String guardianPhone) { this.guardianPhone = guardianPhone; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getFoundationClassroom() { return foundationClassroom; }
    public void setFoundationClassroom(String foundationClassroom) { this.foundationClassroom = foundationClassroom; }
    public Integer getTeamNumber() { return teamNumber; }
    public void setTeamNumber(Integer teamNumber) { this.teamNumber = teamNumber; }
    public Integer getMemberNumber() { return memberNumber; }
    public void setMemberNumber(Integer memberNumber) { this.memberNumber = memberNumber; }
    public Integer getAdmissionYear() { return admissionYear; }
    public void setAdmissionYear(Integer admissionYear) { this.admissionYear = admissionYear; }
    public Integer getPassingYear() { return passingYear; }
    public void setPassingYear(Integer passingYear) { this.passingYear = passingYear; }
    public LocalDate getValidUpto() { return validUpto; }
    public void setValidUpto(LocalDate validUpto) { this.validUpto = validUpto; }
    public String getIdCardNumber() { return idCardNumber; }
    public void setIdCardNumber(String idCardNumber) { this.idCardNumber = idCardNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public int getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(int completionPercentage) { this.completionPercentage = completionPercentage; }
    public String getProfileQrUrl() { return profileQrUrl; }
    public void setProfileQrUrl(String profileQrUrl) { this.profileQrUrl = profileQrUrl; }
    public boolean isAdminEditable() { return adminEditable; }
    public void setAdminEditable(boolean adminEditable) { this.adminEditable = adminEditable; }
    public String getViewerRole() { return viewerRole; }
    public void setViewerRole(String viewerRole) { this.viewerRole = viewerRole; }
    public List<StudentDocumentDTO> getDocuments() { return documents; }
    public void setDocuments(List<StudentDocumentDTO> documents) { this.documents = documents; }
    public List<AcademicRecordDTO> getAcademicRecords() { return academicRecords; }
    public void setAcademicRecords(List<AcademicRecordDTO> academicRecords) { this.academicRecords = academicRecords; }
}
