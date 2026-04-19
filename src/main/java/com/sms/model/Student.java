package com.sms.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

@Entity
public class Student extends Person implements Comparable<Student> {

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String email;
    private String phone;

    private String gender;
    private LocalDate dob;
    private String address;

    private String course;
    private String department;
    private String semester;
    private String section;
    @Column(name = "class_group", length = 32)
    private String classGroup;
    @Column(name = "batch_group", length = 32)
    private String batchGroup;
    private String rollNumber;
    private String enrollmentYear;

    @Lob
    private String profileImageUrl;

    private Long tenantId = 1L;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final List<Course> courses = new ArrayList<>();

    public Student() {}

    public Student(String id, String name) {
        super(id, name);
    }

    public void addCourse(Course course) {
        courses.add(course);
    }

    public List<Course> getCourses() {
        return courses;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getClassGroup() {
        return classGroup;
    }

    public void setClassGroup(String classGroup) {
        this.classGroup = classGroup;
    }

    public String getBatchGroup() {
        return batchGroup;
    }

    public void setBatchGroup(String batchGroup) {
        this.batchGroup = batchGroup;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getEnrollmentYear() {
        return enrollmentYear;
    }

    public void setEnrollmentYear(String enrollmentYear) {
        this.enrollmentYear = enrollmentYear;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public double calculateAverage() {
        return courses.stream()
                .mapToDouble(Course::getMarks)
                .average()
                .orElse(0.0);
    }

    @Override
    public String getRole() {
        return "Student";
    }

    @Override
    public int compareTo(Student other) {
        return this.getName().compareToIgnoreCase(other.getName());
    }

    @Override
    public String toString() {
        return String.format("ID: %s, Name: %s, Avg: %.2f", 
                getId(), getName(), calculateAverage());
    }
}