package com.sms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FaceLivenessClientService {

    private final RestClient restClient;

    public FaceLivenessClientService(@Value("${app.face.liveness-service-url:http://localhost:8001}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public LivenessResult verifyLiveness(Boolean blinkDetected,
                                         Boolean headMovementDetected,
                                         Integer frameCount,
                                         Double motionParallaxScore,
                                         Double brightnessVariance,
                                         List<List<Double>> frameEmbeddings,
                                         List<String> frameSnapshots) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("blink_detected", Boolean.TRUE.equals(blinkDetected));
        payload.put("head_turn_detected", Boolean.TRUE.equals(headMovementDetected));
        payload.put("frame_count", frameCount == null ? 0 : frameCount);
        payload.put("motion_parallax_score", motionParallaxScore == null ? 0.0 : motionParallaxScore);
        payload.put("brightness_variance", brightnessVariance == null ? 0.0 : brightnessVariance);
        payload.put("frame_embeddings", frameEmbeddings == null ? List.of() : frameEmbeddings);
        payload.put("frame_snapshots", frameSnapshots == null ? List.of() : frameSnapshots);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
            .uri("/liveness/verify")
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body(Map.class);

        if (response == null) {
            return new LivenessResult(false, "Liveness service returned empty response");
        }

        boolean passed = Boolean.TRUE.equals(response.get("passed"));
        String reason = passed ? "liveness passed" : String.valueOf(response.getOrDefault("reasons", "liveness failed"));
        return new LivenessResult(passed, reason);
    }

    public static class LivenessResult {
        private final boolean passed;
        private final String reason;

        public LivenessResult(boolean passed, String reason) {
            this.passed = passed;
            this.reason = reason;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getReason() {
            return reason;
        }
    }
}
