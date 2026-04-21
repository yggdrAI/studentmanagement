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
import jakarta.persistence.Table;

@Entity
@Table(name = "teacher_credentials")
public class TeacherCredentials {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SuppressWarnings("unused")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    @SuppressWarnings("unused")
    private Teacher teacher;

    @Column(nullable = false, unique = true)
    @SuppressWarnings("unused")
    private String username;

    @Column(nullable = false)
    @SuppressWarnings("unused")
    private String passwordHash;

    @Column
    @SuppressWarnings("unused")
    private LocalDateTime lastLogin;

    @Column(nullable = false)
    @SuppressWarnings("unused")
    private Boolean passwordResetRequired = false;

    // Getters and setters for JPA
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    public Boolean getPasswordResetRequired() { return passwordResetRequired; }
    public void setPasswordResetRequired(Boolean passwordResetRequired) { this.passwordResetRequired = passwordResetRequired; }

    // Getters and setters omitted for brevity
}
