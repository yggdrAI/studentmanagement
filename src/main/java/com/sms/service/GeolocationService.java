package com.sms.service;

import com.sms.model.CampusLocation;
import org.springframework.stereotype.Service;

/**
 * Geolocation Service
 * Handles distance calculations and location verification
 */
@Service
public class GeolocationService {

    // Earth's radius in kilometers
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculate distance between two coordinates using Haversine formula
     * 
     * Formula:
     * d = 2R * arcsin(sqrt(sin²((φ₂-φ₁)/2) + cosφ₁ * cosφ₂ * sin²((λ₂-λ₁)/2)))
     * 
     * @param lat1 Starting latitude
     * @param lon1 Starting longitude
     * @param lat2 Ending latitude
     * @param lon2 Ending longitude
     * @return Distance in kilometers
     */
    public double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Differences
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        // Haversine formula
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;

        return distance;
    }

    /**
     * Calculate distance in meters
     */
    public double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        return calculateDistanceKm(lat1, lon1, lat2, lon2) * 1000;
    }

    /**
     * Check if student location is inside campus location
     * 
     * @param studentLat Student's latitude
     * @param studentLon Student's longitude
     * @param campusLocation Campus location to verify
     * @return true if student is within allowed radius
     */
    public boolean isInsideGeofence(double studentLat, double studentLon, CampusLocation campusLocation) {
        double distance = calculateDistanceMeters(
            studentLat, 
            studentLon,
            campusLocation.getLatitude(),
            campusLocation.getLongitude()
        );

        boolean isInside = distance <= campusLocation.getRadiusMeters();
        
        System.out.println(String.format(
            "[GEOLOCATION] Student: (%.6f, %.6f) | Location: %s | Distance: %.2fm | Allowed: %.0fm | Inside: %s",
            studentLat, studentLon,
            campusLocation.getName(),
            distance,
            campusLocation.getRadiusMeters(),
            isInside
        ));

        return isInside;
    }

    /**
     * Check if student is inside ANY active location
     */
    public boolean isInsideAnyCampusLocation(double studentLat, double studentLon, java.util.List<CampusLocation> locations) {
        for (CampusLocation location : locations) {
            if (isInsideGeofence(studentLat, studentLon, location)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find the closest location to student
     */
    public CampusLocation findClosestLocation(double studentLat, double studentLon, java.util.List<CampusLocation> locations) {
        CampusLocation closest = null;
        double minDistance = Double.MAX_VALUE;

        for (CampusLocation location : locations) {
            double distance = calculateDistanceMeters(
                studentLat,
                studentLon,
                location.getLatitude(),
                location.getLongitude()
            );

            if (distance < minDistance) {
                minDistance = distance;
                closest = location;
            }
        }

        if (closest != null) {
            System.out.println(String.format(
                "[GEOLOCATION] Closest location: %s (%.2fm away)",
                closest.getName(),
                minDistance
            ));
        }

        return closest;
    }

    /**
     * Get distance confidence score (0-100)
     * 100 = dead center, 0 = at boundary
     */
    public int getConfidenceScore(double studentLat, double studentLon, CampusLocation location) {
        double distance = calculateDistanceMeters(
            studentLat,
            studentLon,
            location.getLatitude(),
            location.getLongitude()
        );

        if (distance > location.getRadiusMeters()) {
            return 0;
        }

        // Confidence decreases as student gets closer to boundary
        double confidence = (1 - (distance / location.getRadiusMeters())) * 100;
        return (int) Math.max(0, Math.min(100, confidence));
    }
}
