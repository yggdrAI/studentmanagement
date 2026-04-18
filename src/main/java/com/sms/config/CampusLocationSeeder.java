package com.sms.config;

import com.sms.model.CampusLocation;
import com.sms.repository.CampusLocationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class CampusLocationSeeder implements CommandLineRunner {

    private final CampusLocationRepository campusLocationRepository;

    public CampusLocationSeeder(CampusLocationRepository campusLocationRepository) {
        this.campusLocationRepository = campusLocationRepository;
    }

    @Override
    public void run(String... args) {
        if (campusLocationRepository.count() > 0) {
            return;
        }

        campusLocationRepository.save(build("Bennett University Main Block", 28.450600, 77.584500, 200.0));
        campusLocationRepository.save(build("Bennett University Academic Block", 28.451200, 77.585100, 180.0));
        campusLocationRepository.save(build("Bennett University Library", 28.449900, 77.583800, 150.0));
    }

    private CampusLocation build(String name, double lat, double lng, double radiusMeters) {
        CampusLocation location = new CampusLocation();
        location.setName(name);
        location.setLatitude(lat);
        location.setLongitude(lng);
        location.setRadiusMeters(radiusMeters);
        location.setIsActive(true);
        return location;
    }
}
