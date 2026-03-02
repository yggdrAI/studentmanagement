package service;

import model.Student;
import util.*;

import java.util.*;
import java.util.stream.Collectors;

public class StudentService {

    private Map<String, Student> studentMap = new HashMap<>();

    public void addStudent(Student student) {
        studentMap.put(student.getId(), student);
        LoggerUtil.log("Added student " + student.getId());
    }

    public void deleteStudent(String id) {
        studentMap.remove(id);
        LoggerUtil.log("Deleted student " + id);
    }

    public Student searchById(String id) {
        return studentMap.get(id);
    }

    public List<Student> searchByName(String name) {
        return studentMap.values()
                .stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
    }

    public List<Student> getAllStudents() {
        return new ArrayList<>(studentMap.values());
    }

    public void sortByName() {
        List<Student> list = getAllStudents();
        Collections.sort(list);
        list.forEach(System.out::println);
    }

    public void sortById() {
        getAllStudents().stream()
                .sorted(StudentComparators.sortById())
                .forEach(System.out::println);
    }

    public void sortByMarks() {
        getAllStudents().stream()
                .sorted(StudentComparators.sortByMarks())
                .forEach(System.out::println);
    }

    public Map<String, Student> getStudentMap() {
        return studentMap;
    }

    public void setStudentMap(Map<String, Student> map) {
        this.studentMap = map;
    }
}