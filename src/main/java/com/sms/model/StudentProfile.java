package com.sms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_profile")
public class StudentProfile {

    @Id
    @Column(name = "student_id", nullable = false, unique = true, length = 64)
    private String studentId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "enrollment_number")
    private String enrollmentNumber;

    @Lob
    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "gender")
    private String gender;

    @Column(name = "religion")
    private String religion;

    @Column(name = "caste")
    private String caste;

    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "university_email")
    private String universityEmail;

    @Column(name = "personal_email")
    private String personalEmail;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "place_of_origin")
    private String placeOfOrigin;

    @Column(name = "guardian_name")
    private String guardianName;

    @Column(name = "guardian_phone")
    private String guardianPhone;

    @Column(name = "college")
    private String college;

    @Column(name = "course")
    private String course;

    @Column(name = "department")
    private String department;

    @Column(name = "semester")
    private String semester;

    @Column(name = "section")
    private String section;

    @Column(name = "foundation_classroom")
    private String foundationClassroom;

    @Column(name = "team_number")
    private Integer teamNumber;

    @Column(name = "member_number")
    private Integer memberNumber;

    @Column(name = "admission_year")
    private Integer admissionYear;

    @Column(name = "passing_year")
    private Integer passingYear;

    @Column(name = "valid_upto")
    private LocalDate validUpto;

    @Column(name = "id_card_number")
    private String idCardNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

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
    public String getCaste() { return caste; }
    public void setCaste(String caste) { this.caste = caste; }
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
    public String getPlaceOfOrigin() { return placeOfOrigin; }
    public void setPlaceOfOrigin(String placeOfOrigin) { this.placeOfOrigin = placeOfOrigin; }
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
}
