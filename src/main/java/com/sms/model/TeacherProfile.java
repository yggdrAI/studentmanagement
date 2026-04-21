package com.sms.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "teacher_profile")
public class TeacherProfile implements Serializable {
    @Id
    @Column(name = "teacher_id", nullable = false, unique = true)
    private Long teacherId;

    @Lob
    @Column(name = "profile_image")
    private String profileImage; // base64 or URL

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    public TeacherProfile() {}

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
}
