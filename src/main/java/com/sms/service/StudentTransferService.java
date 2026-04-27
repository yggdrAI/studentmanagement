package com.sms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sms.dto.student.TransferStudentRequest;
import com.sms.dto.student.TransferStudentResponse;
import com.sms.model.AcademicBatch;
import com.sms.model.AcademicClass;
import com.sms.model.Student;
import com.sms.repository.AcademicBatchRepository;
import com.sms.repository.AcademicClassRepository;
import com.sms.repository.StudentRepository;

@Service
public class StudentTransferService {
    private final StudentRepository studentRepository;
    private final AcademicBatchRepository academicBatchRepository;
    private final AcademicClassRepository academicClassRepository;

    @Autowired
    public StudentTransferService(StudentRepository studentRepository,
                                 AcademicBatchRepository academicBatchRepository,
                                 AcademicClassRepository academicClassRepository) {
        this.studentRepository = studentRepository;
        this.academicBatchRepository = academicBatchRepository;
        this.academicClassRepository = academicClassRepository;
    }

    @Transactional
    public TransferStudentResponse transferStudent(String studentId, TransferStudentRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElse(null);
        if (student == null) {
            return new TransferStudentResponse(false, "Student not found");
        }
        AcademicBatch batch = academicBatchRepository.findById(request.getTargetBatchId()).orElse(null);
        AcademicClass clazz = academicClassRepository.findById(request.getTargetClassId()).orElse(null);
        if (batch == null || clazz == null) {
            return new TransferStudentResponse(false, "Target batch or class not found");
        }
        student.setAcademicBatch(batch);
        student.setAcademicClass(clazz);
        studentRepository.save(student);
        return new TransferStudentResponse(true, "Student transferred successfully");
    }
}
