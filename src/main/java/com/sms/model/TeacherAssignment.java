package com.sms.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "teacher_assignments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"teacher_id", "class_id", "batch_id", "subject"})
}, indexes = {
        @Index(name = "idx_teacher_class_batch", columnList = "teacher_id, class_id, batch_id")
})
public class TeacherAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
        @SuppressWarnings("unused")
        private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
        @SuppressWarnings("unused")
        private Teacher teacher;

    @Column(name = "class_id", nullable = false)
        @SuppressWarnings("unused")
        private Long classId;

    @Column(name = "batch_id", nullable = false)
        @SuppressWarnings("unused")
        private Long batchId;

    @Column(nullable = false)
        @SuppressWarnings("unused")
        private String subject;

    @Column(nullable = false)
        @SuppressWarnings("unused")
        private Boolean isClassTeacher = false;

    @Column(nullable = false)
        @SuppressWarnings("unused")
        private LocalDateTime assignedAt = LocalDateTime.now();

        // Getters and setters for JPA
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Teacher getTeacher() { return teacher; }
        public void setTeacher(Teacher teacher) { this.teacher = teacher; }
        public Long getClassId() { return classId; }
        public void setClassId(Long classId) { this.classId = classId; }
        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public Boolean getIsClassTeacher() { return isClassTeacher; }
        public void setIsClassTeacher(Boolean isClassTeacher) { this.isClassTeacher = isClassTeacher; }
        public LocalDateTime getAssignedAt() { return assignedAt; }
        public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    // Getters and setters omitted for brevity
}
