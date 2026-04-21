package com.sms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sms.model.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, String>, JpaSpecificationExecutor<Student> {
	Optional<Student> findByUserUsername(String username);

	@EntityGraph(attributePaths = {"academicClass", "academicBatch"})
	@Query("select s from Student s")
	List<Student> findAllWithHierarchy();

	@EntityGraph(attributePaths = {"academicProgram", "academicClass", "academicBatch"})
	@Query("select s from Student s")
	List<Student> findAllWithFullHierarchy();

	/**
	 * Bulk-clears all four hierarchy FK columns across every student in a single
	 * SQL UPDATE statement. Because this is a {@code @Modifying} JPQL statement it
	 * bypasses Hibernate's first-level cache and writes immediately to the
	 * database, so subsequent {@code DELETE} calls on batches / classes / programs
	 * will not hit FK constraint violations.
	 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE Student s SET s.academicProgram = null, s.academicClass = null, " +
	       "s.academicBatch = null, s.classGroup = null, s.batchGroup = null")
	int clearAllHierarchyAssignments();
}
