package com.sms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.Teacher;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
	Optional<Teacher> findByUserUsername(String username);
}
