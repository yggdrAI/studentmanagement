package com.sms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sms.model.AnalyticsSnapshot;
import com.sms.repository.AnalyticsSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AnalyticsSnapshotService {

    private final AnalyticsSnapshotRepository analyticsSnapshotRepository;
    private final ObjectMapper objectMapper;

    public AnalyticsSnapshotService(AnalyticsSnapshotRepository analyticsSnapshotRepository,
                                    ObjectMapper objectMapper) {
        this.analyticsSnapshotRepository = analyticsSnapshotRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalyticsSnapshot saveSnapshot(String scopeRole, String scopeKey, String snapshotType, Map<String, Object> payload) {
        try {
            AnalyticsSnapshot snapshot = new AnalyticsSnapshot();
            snapshot.setScopeRole(scopeRole);
            snapshot.setScopeKey(scopeKey);
            snapshot.setSnapshotType(snapshotType);
            snapshot.setSnapshotDate(LocalDate.now());
            snapshot.setPayloadJson(objectMapper.writeValueAsString(payload));
            return analyticsSnapshotRepository.save(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to persist analytics snapshot", ex);
        }
    }

    public Optional<AnalyticsSnapshot> findLatestSnapshot(String scopeRole, String scopeKey) {
        return analyticsSnapshotRepository.findTopByScopeRoleAndScopeKeyOrderBySnapshotDateDescCreatedAtDesc(scopeRole, scopeKey);
    }

    public Map<String, Object> buildSnapshotView(AnalyticsSnapshot snapshot) {
        if (snapshot == null || snapshot.getPayloadJson() == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(snapshot.getPayloadJson(), LinkedHashMap.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse analytics snapshot", ex);
        }
    }

    public List<AnalyticsSnapshot> findRecentSnapshots(LocalDate date) {
        return analyticsSnapshotRepository.findTop10BySnapshotDateOrderByCreatedAtDesc(date);
    }
}
