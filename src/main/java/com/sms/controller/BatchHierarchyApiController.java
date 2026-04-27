package com.sms.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sms.dto.student.BatchHierarchyDTO;
import com.sms.repository.AcademicBatchRepository;
import com.sms.repository.AcademicClassRepository;

@RestController
@RequestMapping("/api/students")
@PreAuthorize("hasRole('ADMIN')")
public class BatchHierarchyApiController {
    private final AcademicClassRepository academicClassRepository;
    private final AcademicBatchRepository academicBatchRepository;

    @Autowired
    public BatchHierarchyApiController(AcademicClassRepository academicClassRepository,
                                       AcademicBatchRepository academicBatchRepository) {
        this.academicClassRepository = academicClassRepository;
        this.academicBatchRepository = academicBatchRepository;
    }

    @GetMapping("/batches-hierarchy")
    public ResponseEntity<List<BatchHierarchyDTO>> getBatchHierarchy() {
        List<BatchHierarchyDTO> result = academicClassRepository.findAllByOrderByClassNumberAsc().stream()
                .map(clazz -> {
                    BatchHierarchyDTO dto = new BatchHierarchyDTO();
                    dto.setClassId(clazz.getId());
                    dto.setClassName("Class " + clazz.getClassNumber());
                    List<BatchHierarchyDTO.BatchDTO> batches = academicBatchRepository.findByAcademicClass_IdOrderByLocalBatchNumberAsc(clazz.getId())
                            .stream()
                            .map(batch -> {
                                BatchHierarchyDTO.BatchDTO b = new BatchHierarchyDTO.BatchDTO();
                                b.setBatchId(batch.getId());
                                b.setBatchName("Batch " + batch.getLocalBatchNumber());
                                return b;
                            })
                            .collect(Collectors.toList());
                    dto.setBatches(batches);
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
