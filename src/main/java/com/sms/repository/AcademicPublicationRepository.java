package com.sms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sms.model.AcademicPublication;

@Repository
public interface AcademicPublicationRepository extends JpaRepository<AcademicPublication, Long> {
    List<AcademicPublication> findByPublishedTrueOrderByPublishedAtDescIdDesc();

    List<AcademicPublication> findAllByOrderByPublishedAtDescIdDesc();
}
