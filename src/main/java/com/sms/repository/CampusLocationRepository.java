package com.sms.repository;

import com.sms.model.CampusLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for CampusLocation
 */
@Repository
public interface CampusLocationRepository extends JpaRepository<CampusLocation, Long> {
    
    /**
     * Get all active campus locations
     */
    @Query("SELECT c FROM CampusLocation c WHERE c.isActive = true")
    List<CampusLocation> findAllActive();

    /**
     * Find location by name
     */
    CampusLocation findByName(String name);
}
