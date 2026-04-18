package com.sms.repository;

import com.sms.model.TimetableVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableVersionRepository extends JpaRepository<TimetableVersion, Long> {
    
    List<TimetableVersion> findByTimetableIdOrderByVersionNumberDesc(Long timetableId);
    
    Optional<TimetableVersion> findByTimetableIdAndVersionNumber(Long timetableId, Integer versionNumber);
    
    @Query("SELECT MAX(t.versionNumber) FROM TimetableVersion t WHERE t.timetable.id = :timetableId")
    Integer findMaxVersionNumber(@Param("timetableId") Long timetableId);
}
