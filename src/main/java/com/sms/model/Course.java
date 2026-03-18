package model;
import java.io.Serializable;

public class Course implements Serializable {
    private String courseName;
    private double marks;

    public Course(String courseName, double marks) {
        this.courseName = courseName;
        this.marks = marks;
    }

    public String getCourseName() { return courseName; }
    public double getMarks() { return marks; }
}