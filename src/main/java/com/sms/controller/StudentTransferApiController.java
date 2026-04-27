package com.sms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sms.dto.student.TransferStudentRequest;
import com.sms.dto.student.TransferStudentResponse;
import com.sms.service.StudentTransferService;

@RestController
@RequestMapping("/api/students")
@PreAuthorize("hasRole('ADMIN')")
public class StudentTransferApiController {

    private final StudentTransferService studentTransferService;

    @Autowired
    public StudentTransferApiController(StudentTransferService studentTransferService) {
        this.studentTransferService = studentTransferService;
    }

    @PostMapping("/{studentId}/transfer")
    public ResponseEntity<TransferStudentResponse> transferStudent(
            @PathVariable String studentId,
            @RequestBody TransferStudentRequest request) {
        TransferStudentResponse response = studentTransferService.transferStudent(studentId, request);
        return ResponseEntity.ok(response);
    }
}
