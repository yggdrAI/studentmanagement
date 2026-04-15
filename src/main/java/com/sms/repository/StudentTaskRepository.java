package com.sms.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.StudentTask;

@Repository
public interface StudentTaskRepository extends JpaRepository<StudentTask, Long> {

    Optional<StudentTask> findByStudentIdAndTaskId(String studentId, Long taskId);

    long countByStudentIdAndTaskCourseIdAndCompletedTrue(String studentId, Long courseId);

    long countByStudentIdAndTaskCourseIdInAndCompletedTrue(String studentId, Collection<Long> courseIds);

    List<StudentTask> findByStudentIdAndTaskCourseIdInAndCompletedTrue(String studentId, Collection<Long> courseIds);
}
