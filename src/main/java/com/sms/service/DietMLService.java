package com.sms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class DietMLService {

    private final RestClient restClient;

    public DietMLService(@Value("${app.ml.api.base-url:http://localhost:8000}") String mlApiBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(mlApiBaseUrl)
                .build();
    }

    public DietMLResult evaluate(double calories, double junkRatio) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "calories", calories,
                            "junk_ratio", junkRatio
                    ))
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return fallback(calories, junkRatio, "local-heuristic");
            }

            double score = parseDouble(response.get("score"), 0.0);
            String prediction = String.valueOf(response.getOrDefault("prediction", "moderate"));
            return new DietMLResult(score, prediction, "python-ml");
        } catch (Exception ex) {
            return fallback(calories, junkRatio, "local-heuristic");
        }
    }

    private DietMLResult fallback(double calories, double junkRatio, String source) {
        double score = Math.max(0.0, 100.0 - (calories * 0.02 + junkRatio * 50.0));
        String prediction;
        if (score < 50.0) {
            prediction = "unhealthy";
        } else if (score < 75.0) {
            prediction = "moderate";
        } else {
            prediction = "healthy";
        }
        return new DietMLResult(Math.round(score * 10.0) / 10.0, prediction, source);
    }

    private double parseDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public record DietMLResult(double score, String prediction, String source) {
    }
}
