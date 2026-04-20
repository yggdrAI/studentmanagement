package com.sms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
	Optional<Course> findByCode(String code);
	Optional<Course> findByCourseNameIgnoreCase(String courseName);

	java.util.List<Course> findByTeacherId(Long teacherId);
}