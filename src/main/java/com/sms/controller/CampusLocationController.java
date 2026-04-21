package com.sms.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sms.model.CampusLocation;
import com.sms.repository.CampusLocationRepository;
import com.sms.service.GeolocationService;

@RestController
@RequestMapping("/api/locations")
public class CampusLocationController {

    @Autowired
    private CampusLocationRepository campusLocationRepository;

    @Autowired
    private GeolocationService geolocationService;

    /**
     * Get all campus locations (for map, UI, etc)
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllLocations() {
        List<CampusLocation> locations = campusLocationRepository.findAllActive();
        List<Map<String, Object>> result = locations.stream().map(loc -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", loc.getId());
            map.put("name", loc.getName());
            map.put("latitude", loc.getLatitude());
            map.put("longitude", loc.getLongitude());
            map.put("radiusMeters", loc.getRadiusMeters());
            map.put("isActive", loc.getIsActive());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Get nearby campus locations for a given lat/lng
     */
    @GetMapping("/nearby")
    public ResponseEntity<Map<String, Object>> getNearbyLocation(
            @RequestParam double lat,
            @RequestParam double lng) {
        List<CampusLocation> locations = campusLocationRepository.findAllActive();
        CampusLocation closest = geolocationService.findClosestLocation(lat, lng, locations);
        double distance = closest != null ? geolocationService.calculateDistanceMeters(lat, lng, closest.getLatitude(), closest.getLongitude()) : -1;
        boolean inside = closest != null && geolocationService.isInsideGeofence(lat, lng, closest);
        return ResponseEntity.ok(Map.of(
                "closestLocation", closest,
                "distanceMeters", distance,
                "inside", inside
        ));
    }
}
