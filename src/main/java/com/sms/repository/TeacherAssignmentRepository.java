package com.sms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.TeacherAssignment;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {
    List<TeacherAssignment> findByClassIdAndBatchId(Long classId, Long batchId);
    List<TeacherAssignment> findByTeacherId(Long teacherId);
    boolean existsByTeacherIdAndClassIdAndBatchIdAndSubject(Long teacherId, Long classId, Long batchId, String subject);
}
