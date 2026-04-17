package com.sms.controller;

import com.sms.dto.campus.LocationDTO;
import com.sms.service.CampusTrackingService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class CampusLocationWebSocketController {

    private final CampusTrackingService campusTrackingService;

    public CampusLocationWebSocketController(CampusTrackingService campusTrackingService) {
        this.campusTrackingService = campusTrackingService;
    }

    @MessageMapping("/location")
    public LocationDTO receive(LocationDTO loc) {
        LocationDTO payload = loc == null ? new LocationDTO() : loc;
        if (payload.getRecordedAt() == null) {
            payload.setRecordedAt(LocalDateTime.now());
        }
        if (payload.getStudentId() != null && payload.getSubjectId() != null && payload.getLat() != null && payload.getLng() != null) {
            campusTrackingService.recordLocation(
                payload.getStudentId(),
                payload.getSubjectId(),
                payload.getSessionId(),
                null,
                payload.getLat(),
                payload.getLng(),
                !Boolean.TRUE.equals(payload.getFlag()),
                Boolean.TRUE.equals(payload.getFlag()),
                null,
                null
            );
        }
        return payload;
    }
}