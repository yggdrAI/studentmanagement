package com.sms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AnalyticsRealtimeNotifier {

    private final SimpMessagingTemplate messagingTemplate;
    private final AiAnalyticsService aiAnalyticsService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public AnalyticsRealtimeNotifier(SimpMessagingTemplate messagingTemplate,
                                     AiAnalyticsService aiAnalyticsService,
                                     ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider,
                                     ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.aiAnalyticsService = aiAnalyticsService;
        this.stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
        this.objectMapper = objectMapper;
    }

    public void notifyAttendanceEvent(String studentId, String status) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "ATTENDANCE");
        event.put("studentId", studentId);
        event.put("status", status);
        event.put("message", studentId + " marked " + (status == null ? "updated" : status.toLowerCase()));
        event.put("timestamp", LocalDateTime.now().toString());
        publish("analytics:feed", "/topic/analytics/feed", event);
    }

    public void notifyStudentAdded(String studentId, String studentName) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "STUDENT_CREATED");
        event.put("studentId", studentId);
        event.put("message", "New student added: " + studentName + " (" + studentId + ")");
        event.put("timestamp", LocalDateTime.now().toString());
        publish("analytics:feed", "/topic/analytics/feed", event);
    }

    public void notifyStudentBulkImport(Long jobId, int successCount) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "STUDENT_BULK_IMPORT");
        event.put("jobId", jobId);
        event.put("successCount", successCount);
        event.put("message", "Bulk import completed: " + successCount + " students created");
        event.put("timestamp", LocalDateTime.now().toString());
        publish("analytics:feed", "/topic/analytics/feed", event);
    }

    @Scheduled(fixedDelay = 6000)
    public void publishLiveSnapshot() {
        publish("analytics:live", "/topic/analytics/live", aiAnalyticsService.buildLiveSnapshot());
    }

    private void publish(String channel, String destination, Map<String, Object> event) {
        messagingTemplate.convertAndSend(destination, event);
        if (stringRedisTemplate == null) {
            return;
        }
        try {
            stringRedisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(event));
        } catch (Exception ex) {
            // Local delivery already happened; Redis fan-out is best-effort.
        }
    }
}
