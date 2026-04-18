package com.sms.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationService {

    private final SimpMessagingTemplate template;

    public NotificationService(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void notifyTopic(String topic, Map<String, Object> payload) {
        template.convertAndSend(topic, payload);
    }

    public void notifyUser(String username, Map<String, Object> payload) {
        if (username == null || username.isBlank()) {
            return;
        }

        template.convertAndSendToUser(username, "/queue/notifications", payload);
    }

    public void notifyUpcomingClass(String message, Long sessionId, String courseCode, String startsAt) {
        notifyTopic("/topic/notifications", Map.of(
                "type", "CLASS_REMINDER",
                "message", message,
                "sessionId", sessionId,
                "courseCode", courseCode != null ? courseCode : "COURSE",
                "startsAt", startsAt != null ? startsAt : ""
        ));
    }

    public void notifyUpcomingClassToUser(String username,
                                          String message,
                                          Long sessionId,
                                          String courseCode,
                                          String startsAt) {
        notifyUser(username, Map.of(
                "type", "CLASS_REMINDER",
                "message", message,
                "sessionId", sessionId,
                "courseCode", courseCode != null ? courseCode : "COURSE",
                "startsAt", startsAt != null ? startsAt : ""
        ));
    }
    public void notifyBulkImport(String username, Long jobId, int successCount, String status) {
        notifyUser(username, Map.of(
            "type", "BULK_IMPORT_COMPLETE",
            "jobId", jobId,
            "successCount", successCount,
            "status", status != null ? status : "FINISHED",
            "message", "Bulk import completed: " + successCount + " students created"
        ));
    }
}
