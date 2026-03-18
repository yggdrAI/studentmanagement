package service;

import model.Student;
import util.StudentComparators;

import java.util.*;

public class StudentService {

    private Map<String, Student> studentMap = new HashMap<>();

    public void addStudent(Student student) {
        studentMap.put(student.getId(), student);
    }

    public void deleteStudent(String id) {
        studentMap.remove(id);
    }

    public Student searchById(String id) {
        return studentMap.get(id);
    }

    public List<Student> getAllStudents() {
        return new ArrayList<>(studentMap.values());
    }

    public Map<String, Student> getStudentMap() {
        return studentMap;
    }

    public void setStudentMap(Map<String, Student> map) {
        this.studentMap = map;
    }

    public void sortByName() {
        List<Student> sorted = getAllStudents();
        Collections.sort(sorted);
        studentMap.clear();
        for (Student s : sorted) studentMap.put(s.getId(), s);
    }

    public void sortById() {
        List<Student> sorted = getAllStudents();
        sorted.sort(StudentComparators.sortById());
        studentMap.clear();
        for (Student s : sorted) studentMap.put(s.getId(), s);
    }

    public void sortByMarks() {
        List<Student> sorted = getAllStudents();
        sorted.sort(StudentComparators.sortByMarks());
        studentMap.clear();
        for (Student s : sorted) studentMap.put(s.getId(), s);
    }
}