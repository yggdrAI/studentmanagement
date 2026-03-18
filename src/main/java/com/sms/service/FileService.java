package com.sms.service;

import com.sms.model.Student;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FileService {

    private static final String FILE_NAME = "students.ser";

    public void save(Map<String, Student> map) {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(map);
        } catch (IOException e) {
            System.out.println("Error saving data.");
        }
    }

    public Map<String, Student> load() {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            return (Map<String, Student>) ois.readObject();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}