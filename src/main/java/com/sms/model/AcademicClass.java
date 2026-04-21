package com.sms.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "classes")
public class AcademicClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_number", nullable = false)
    private Integer classNumber;

    @ManyToOne
    @JoinColumn(name = "program_id")
    private AcademicProgram academicProgram;

    @Column(name = "local_class_number")
    private Integer localClassNumber;

    @Column(name = "total_students")
    private Integer totalStudents = 0;

    @OneToMany(mappedBy = "academicClass")
    private final List<AcademicBatch> batches = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getClassNumber() {
        return classNumber;
    }

    public void setClassNumber(Integer classNumber) {
        this.classNumber = classNumber;
    }

    public AcademicProgram getAcademicProgram() {
        return academicProgram;
    }

    public void setAcademicProgram(AcademicProgram academicProgram) {
        this.academicProgram = academicProgram;
    }

    public Integer getLocalClassNumber() {
        return localClassNumber;
    }

    public void setLocalClassNumber(Integer localClassNumber) {
        this.localClassNumber = localClassNumber;
    }

    public Integer getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Integer totalStudents) {
        this.totalStudents = totalStudents;
    }

    public List<AcademicBatch> getBatches() {
        return batches;
    }
}