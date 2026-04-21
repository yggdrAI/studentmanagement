package com.sms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.AcademicProgram;

@Repository
public interface AcademicProgramRepository extends JpaRepository<AcademicProgram, Long> {
    Optional<AcademicProgram> findByCodeAndProgramTypeAndAdmissionYear(String code, String programType, String admissionYear);

    Optional<AcademicProgram> findByCode(String code);

    List<AcademicProgram> findAllByOrderByNameAsc();
}
