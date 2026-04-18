package com.sms.service;

import com.sms.model.StudentLocation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
public class BehaviorAIService {

    private final RestClient restClient;

    public BehaviorAIService(@Value("${app.ml.api.base-url:http://localhost:8000}") String mlApiBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(mlApiBaseUrl)
                .build();
    }

    public Map<String, Object> analyzeLocations(List<StudentLocation> locations) {
        try {
            List<Map<String, Object>> points = locations.stream()
                    .filter(location -> location.getLatitude() != null && location.getLongitude() != null && location.getRecordedAt() != null)
                    .map(location -> Map.<String, Object>of(
                            "lat", location.getLatitude(),
                            "lng", location.getLongitude(),
                            "timestamp", location.getRecordedAt().toInstant(ZoneOffset.UTC).toEpochMilli()
                    ))
                    .toList();

            Map<String, Object> payload = Map.of(
                    "points", points,
                    "speed_threshold_kmh", 50.0
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/analyze-behavior")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response != null) {
                return response;
            }
        } catch (Exception ignored) {
            // Intentionally fallback when ML API is unavailable.
        }

        return Map.of(
                "suspicious", false,
                "reason", "Behavior AI unavailable",
                "max_speed_kmh", 0.0,
                "avg_speed_kmh", 0.0,
                "anomaly_score", 0.0,
                "source", "fallback"
        );
    }
}
