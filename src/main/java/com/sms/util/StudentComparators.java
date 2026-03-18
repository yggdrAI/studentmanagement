package com.sms.util;

import com.sms.model.Student;
import java.util.Comparator;

public class StudentComparators {

    public static Comparator<Student> sortById() {
        return Comparator.comparing(Student::getId);
    }

    public static Comparator<Student> sortByMarks() {
        return Comparator.comparing(Student::calculateAverage).reversed();
    }
}