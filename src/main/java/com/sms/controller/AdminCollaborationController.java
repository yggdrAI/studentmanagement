package com.sms.controller;

import com.sms.service.CollaborationRealtimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/collab")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCollaborationController {

    private final CollaborationRealtimeService collaborationRealtimeService;

    public AdminCollaborationController(CollaborationRealtimeService collaborationRealtimeService) {
        this.collaborationRealtimeService = collaborationRealtimeService;
    }

    @GetMapping("/presence")
    public ResponseEntity<Map<String, Object>> presence(@RequestParam(defaultValue = "global") String scope) {
        List<String> users = collaborationRealtimeService.listPresence(scope);
        return ResponseEntity.ok(Map.of("scope", scope, "users", users, "count", users.size()));
    }

    @PostMapping("/presence/join")
    public ResponseEntity<Map<String, Object>> joinPresence(@RequestBody(required = false) PresenceRequest request,
                                                            Authentication authentication) {
        String scope = request == null ? "global" : request.getScope();
        String user = authentication == null ? "anonymous" : authentication.getName();
        return ResponseEntity.ok(collaborationRealtimeService.joinScope(scope, user));
    }

    @PostMapping("/presence/leave")
    public ResponseEntity<Map<String, Object>> leavePresence(@RequestBody(required = false) PresenceRequest request,
                                                             Authentication authentication) {
        String scope = request == null ? "global" : request.getScope();
        String user = authentication == null ? "anonymous" : authentication.getName();
        return ResponseEntity.ok(collaborationRealtimeService.leaveScope(scope, user));
    }

    @PostMapping("/locks/acquire")
    public ResponseEntity<Map<String, Object>> acquireLock(@RequestBody LockRequest request,
                                                           Authentication authentication) {
        String user = authentication == null ? "anonymous" : authentication.getName();
        return ResponseEntity.ok(collaborationRealtimeService.acquireLock(
                request.getEntityType(),
                request.getEntityId(),
                user,
                request.getTtlSeconds()
        ));
    }

    @PostMapping("/locks/release")
    public ResponseEntity<Map<String, Object>> releaseLock(@RequestBody LockRequest request,
                                                           Authentication authentication) {
        String user = authentication == null ? "anonymous" : authentication.getName();
        return ResponseEntity.ok(collaborationRealtimeService.releaseLock(
                request.getEntityType(),
                request.getEntityId(),
                user
        ));
    }

    @GetMapping("/locks/{entityType}/{entityId}")
    public ResponseEntity<Map<String, Object>> getLock(@PathVariable String entityType,
                                                       @PathVariable String entityId) {
        return ResponseEntity.ok(collaborationRealtimeService.getLock(entityType, entityId));
    }

    public static class PresenceRequest {
        private String scope;

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }

    public static class LockRequest {
        private String entityType;
        private String entityId;
        private Long ttlSeconds;

        public String getEntityType() {
            return entityType;
        }

        public void setEntityType(String entityType) {
            this.entityType = entityType;
        }

        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        public Long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(Long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }
}
