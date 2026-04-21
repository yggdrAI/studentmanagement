package com.sms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.AcademicClass;
import com.sms.model.AcademicProgram;

@Repository
public interface AcademicClassRepository extends JpaRepository<AcademicClass, Long> {
    Optional<AcademicClass> findByClassNumber(Integer classNumber);

    Optional<AcademicClass> findByAcademicProgram_IdAndLocalClassNumber(Long programId, Integer localClassNumber);

    Optional<AcademicClass> findByAcademicProgramAndLocalClassNumber(AcademicProgram program, Integer localClassNumber);

    List<AcademicClass> findAllByOrderByClassNumberAsc();

    List<AcademicClass> findByAcademicProgram_IdOrderByLocalClassNumberAsc(Long programId);
}