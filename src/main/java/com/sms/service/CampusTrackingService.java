package com.sms.service;

import com.sms.dto.campus.CampusLocationUpdateDTO;
import com.sms.model.StudentLocation;
import com.sms.repository.StudentLocationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Persists and streams live campus location updates.
 */
@Service
public class CampusTrackingService {

    private final StudentLocationRepository studentLocationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public CampusTrackingService(StudentLocationRepository studentLocationRepository,
                                 SimpMessagingTemplate messagingTemplate) {
        this.studentLocationRepository = studentLocationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public StudentLocation recordLocation(String studentId,
                                          Long subjectId,
                                          String sessionId,
                                          Long attendanceId,
                                          double latitude,
                                          double longitude,
                                          boolean locationVerified,
                                          boolean suspicious,
                                          Double faceSimilarity,
                                          Integer locationConfidence) {
        StudentLocation location = new StudentLocation();
        location.setStudentId(studentId);
        location.setSubjectId(subjectId);
        location.setSessionId(sessionId);
        location.setAttendanceId(attendanceId);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setLocationVerified(locationVerified);
        location.setSuspicious(suspicious);
        location.setFaceSimilarity(faceSimilarity);
        location.setLocationConfidence(locationConfidence);
        location.setRecordedAt(LocalDateTime.now());

        StudentLocation savedLocation = studentLocationRepository.save(location);
        CampusLocationUpdateDTO payload = toPayload(savedLocation);
        messagingTemplate.convertAndSend("/topic/locations", Objects.requireNonNull(payload, "payload"));
        if (sessionId != null && !sessionId.isBlank()) {
            messagingTemplate.convertAndSend("/topic/locations/" + sessionId, Objects.requireNonNull(payload, "payload"));
        }
        return savedLocation;
    }

    public List<StudentLocation> getRecentLocations(Long subjectId, String sessionId, String status, int limit) {
        List<StudentLocation> locations;
        if (sessionId != null && !sessionId.isBlank()) {
            locations = studentLocationRepository.findTop100BySessionIdOrderByRecordedAtDesc(sessionId);
        } else if (subjectId != null) {
            locations = studentLocationRepository.findTop200BySubjectIdOrderByRecordedAtDesc(subjectId);
        } else {
            locations = studentLocationRepository.findAll();
        }

        return locations.stream()
            .filter(location -> status == null || status.isBlank() || matchesStatus(location, status))
            .limit(Math.max(1, limit))
            .toList();
    }

    private boolean matchesStatus(StudentLocation location, String status) {
        return switch (status.toLowerCase()) {
            case "verified" -> Boolean.TRUE.equals(location.getLocationVerified());
            case "suspicious" -> Boolean.TRUE.equals(location.getSuspicious());
            case "outside" -> Boolean.FALSE.equals(location.getLocationVerified());
            default -> true;
        };
    }

    private CampusLocationUpdateDTO toPayload(StudentLocation location) {
        CampusLocationUpdateDTO payload = new CampusLocationUpdateDTO();
        payload.setLocationId(location.getId());
        payload.setAttendanceId(location.getAttendanceId());
        payload.setStudentId(location.getStudentId());
        payload.setSubjectId(location.getSubjectId());
        payload.setSessionId(location.getSessionId());
        payload.setLatitude(location.getLatitude());
        payload.setLongitude(location.getLongitude());
        payload.setLocationVerified(location.getLocationVerified());
        payload.setSuspicious(location.getSuspicious());
        payload.setFaceSimilarity(location.getFaceSimilarity());
        payload.setLocationConfidence(location.getLocationConfidence());
        payload.setRecordedAt(location.getRecordedAt());
        return payload;
    }
}