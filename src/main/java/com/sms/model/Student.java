package com.sms.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Student extends Person implements Serializable, Comparable<Student> {
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Course> courses = new ArrayList<>();

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