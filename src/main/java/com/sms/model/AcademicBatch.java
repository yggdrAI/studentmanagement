package com.sms.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "batches")
public class AcademicBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_number", nullable = false)
    private Integer batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private AcademicProgram academicProgram;

    @Column(name = "local_batch_number")
    private Integer localBatchNumber;

    @Column(name = "total_students")
    private Integer totalStudents = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private AcademicClass academicClass;

    @OneToMany(mappedBy = "academicBatch")
    private final List<Student> students = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(Integer batchNumber) {
        this.batchNumber = batchNumber;
    }

    public AcademicProgram getAcademicProgram() {
        return academicProgram;
    }

    public void setAcademicProgram(AcademicProgram academicProgram) {
        this.academicProgram = academicProgram;
    }

    public Integer getLocalBatchNumber() {
        return localBatchNumber;
    }

    public void setLocalBatchNumber(Integer localBatchNumber) {
        this.localBatchNumber = localBatchNumber;
    }

    public Integer getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Integer totalStudents) {
        this.totalStudents = totalStudents;
    }

    public AcademicClass getAcademicClass() {
        return academicClass;
    }

    public void setAcademicClass(AcademicClass academicClass) {
        this.academicClass = academicClass;
    }

    public List<Student> getStudents() {
        return students;
    }
}