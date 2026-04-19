package com.sms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CollaborationRealtimeService {

    private static final long DEFAULT_TTL_SECONDS = 120L;

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private final Map<String, LockState> locks = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> presenceByScope = new ConcurrentHashMap<>();

    public CollaborationRealtimeService(SimpMessagingTemplate messagingTemplate,
                                        ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider,
                                        ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> joinScope(String scope, String username) {
        String normalizedScope = normalizeScope(scope);
        String normalizedUser = normalizeUser(username);

        presenceByScope.compute(normalizedScope, (key, existing) -> {
            Set<String> users = existing == null ? Collections.newSetFromMap(new ConcurrentHashMap<>()) : existing;
            users.add(normalizedUser);
            return users;
        });

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "presence.joined");
        event.put("scope", normalizedScope);
        event.put("user", normalizedUser);
        event.put("activeUsers", listPresence(normalizedScope));
        event.put("timestamp", Instant.now().toString());

        publish("collab:presence", "/topic/collab/updates", event);
        return event;
    }

    public Map<String, Object> leaveScope(String scope, String username) {
        String normalizedScope = normalizeScope(scope);
        String normalizedUser = normalizeUser(username);

        presenceByScope.computeIfPresent(normalizedScope, (key, users) -> {
            users.remove(normalizedUser);
            return users.isEmpty() ? null : users;
        });

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "presence.left");
        event.put("scope", normalizedScope);
        event.put("user", normalizedUser);
        event.put("activeUsers", listPresence(normalizedScope));
        event.put("timestamp", Instant.now().toString());

        publish("collab:presence", "/topic/collab/updates", event);
        return event;
    }

    public Map<String, Object> acquireLock(String entityType, String entityId, String username, Long ttlSeconds) {
        String key = lockKey(entityType, entityId);
        long now = Instant.now().getEpochSecond();
        long ttl = ttlSeconds == null || ttlSeconds <= 0 ? DEFAULT_TTL_SECONDS : ttlSeconds;
        long expiresAt = now + ttl;
        String normalizedUser = normalizeUser(username);

        LockState existing = locks.get(key);
        if (existing != null && existing.expiresAtEpochSecond() > now && !existing.username().equalsIgnoreCase(normalizedUser)) {
            Map<String, Object> blocked = new LinkedHashMap<>();
            blocked.put("type", "lock.blocked");
            blocked.put("entityType", entityType);
            blocked.put("entityId", entityId);
            blocked.put("lockedBy", existing.username());
            blocked.put("expiresAt", Instant.ofEpochSecond(existing.expiresAtEpochSecond()).toString());
            blocked.put("readOnly", true);
            return blocked;
        }

        LockState updated = new LockState(entityType, entityId, normalizedUser, expiresAt);
        locks.put(key, updated);

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "lock.acquired");
        event.put("entityType", entityType);
        event.put("entityId", entityId);
        event.put("lockedBy", normalizedUser);
        event.put("expiresAt", Instant.ofEpochSecond(expiresAt).toString());
        event.put("readOnly", false);
        event.put("timestamp", Instant.now().toString());

        publish("collab:locks", "/topic/collab/locks", event);
        return event;
    }

    public Map<String, Object> releaseLock(String entityType, String entityId, String username) {
        String key = lockKey(entityType, entityId);
        String normalizedUser = normalizeUser(username);
        LockState existing = locks.get(key);

        if (existing != null && !existing.username().equalsIgnoreCase(normalizedUser)) {
            Map<String, Object> denied = new LinkedHashMap<>();
            denied.put("type", "lock.release.denied");
            denied.put("entityType", entityType);
            denied.put("entityId", entityId);
            denied.put("lockedBy", existing.username());
            return denied;
        }

        locks.remove(key);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "lock.released");
        event.put("entityType", entityType);
        event.put("entityId", entityId);
        event.put("releasedBy", normalizedUser);
        event.put("timestamp", Instant.now().toString());

        publish("collab:locks", "/topic/collab/locks", event);
        return event;
    }

    public Map<String, Object> getLock(String entityType, String entityId) {
        String key = lockKey(entityType, entityId);
        long now = Instant.now().getEpochSecond();
        LockState state = locks.get(key);

        Map<String, Object> payload = new HashMap<>();
        payload.put("entityType", entityType);
        payload.put("entityId", entityId);

        if (state == null || state.expiresAtEpochSecond() <= now) {
            if (state != null) {
                locks.remove(key);
            }
            payload.put("locked", false);
            return payload;
        }

        payload.put("locked", true);
        payload.put("lockedBy", state.username());
        payload.put("expiresAt", Instant.ofEpochSecond(state.expiresAtEpochSecond()).toString());
        return payload;
    }

    public List<String> listPresence(String scope) {
        String normalizedScope = normalizeScope(scope);
        Collection<String> users = presenceByScope.getOrDefault(normalizedScope, Set.of());
        List<String> list = new ArrayList<>(users);
        list.sort(String::compareToIgnoreCase);
        return list;
    }

    public void publishDomainUpdate(String eventType, Map<String, Object> payload) {
        Map<String, Object> event = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        event.put("type", eventType);
        event.put("timestamp", Instant.now().toString());
        publish("collab:domain", "/topic/collab/updates", event);
    }

    @Scheduled(fixedDelay = 10000)
    public void evictExpiredLocks() {
        long now = Instant.now().getEpochSecond();
        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, LockState> entry : locks.entrySet()) {
            if (entry.getValue().expiresAtEpochSecond() <= now) {
                expired.add(entry.getKey());
            }
        }

        for (String key : expired) {
            LockState removed = locks.remove(key);
            if (removed == null) {
                continue;
            }
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "lock.expired");
            event.put("entityType", removed.entityType());
            event.put("entityId", removed.entityId());
            event.put("lockedBy", removed.username());
            event.put("timestamp", Instant.now().toString());
            publish("collab:locks", "/topic/collab/locks", event);
        }
    }

    private void publish(String channel, String destination, Map<String, Object> event) {
        messagingTemplate.convertAndSend(destination, event);
        if (stringRedisTemplate == null) {
            return;
        }

        try {
            stringRedisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(event));
        } catch (Exception ignored) {
            // Local websocket delivery already succeeded.
        }
    }

    private String lockKey(String entityType, String entityId) {
        return String.format("%s:%s", sanitize(entityType, "entity"), sanitize(entityId, "unknown"));
    }

    private String normalizeScope(String scope) {
        return sanitize(scope, "global");
    }

    private String normalizeUser(String username) {
        return sanitize(username, "anonymous");
    }

    private String sanitize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private record LockState(String entityType, String entityId, String username, long expiresAtEpochSecond) {}
}
