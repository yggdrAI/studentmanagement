package com.sms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class AnalyticsRedisBridge implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsRedisBridge.class);

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public AnalyticsRedisBridge(ObjectMapper objectMapper,
                                SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(@NonNull Message message, @Nullable byte[] pattern) {
        if (message == null || message.getBody() == null) {
            return;
        }

        String channel = message.getChannel() == null ? "" : new String(message.getChannel(), StandardCharsets.UTF_8);
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<LinkedHashMap<String, Object>>() {});
            if (channel.contains("live")) {
                messagingTemplate.convertAndSend("/topic/analytics/live", event);
            } else {
                messagingTemplate.convertAndSend("/topic/analytics/feed", event);
            }
        } catch (java.io.IOException ex) {
            log.warn("Failed to relay analytics redis message", ex);
        }
    }
}
