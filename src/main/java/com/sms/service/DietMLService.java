package com.sms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
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
            Map<String, Object> requestBody = Map.of(
                "calories", calories,
                "junk_ratio", junkRatio
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                .uri("/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return fallback(calories, junkRatio, "local-heuristic");
            }

            double score = parseDouble(response.get("health_score"), parseDouble(response.get("score"), 0.0));
            String prediction = String.valueOf(response.getOrDefault("prediction", "moderate"));
            String recommendation = String.valueOf(response.getOrDefault("recommendation", "Balanced Meal"));
            String recommendationReason = String.valueOf(response.getOrDefault("recommendation_reason", "Balanced nutrition profile"));
            String futureRisk = String.valueOf(response.getOrDefault("future_risk", "Calorie intake likely stable tomorrow"));
            List<Map<String, Object>> recommendations = extractMapList(response.get("recommendations"));
            List<Map<String, Object>> explanationRows = extractMapList(response.get("explanation"));
            return new DietMLResult(score, prediction, "python-ml", recommendation, recommendationReason, futureRisk, explanationRows, recommendations);
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
        String recommendation = switch (prediction) {
            case "unhealthy" -> "Replace Vada Pao with Veg Oats";
            case "moderate" -> "Choose Paneer Salad Bowl";
            default -> "Continue your current balanced meal";
        };
        String recommendationReason = switch (prediction) {
            case "unhealthy" -> "Higher protein, lower fat, matches calorie target";
            case "moderate" -> "Improves protein quality and keeps fat controlled";
            default -> "Current meal pattern is balanced";
        };
        String futureRisk = calories > 2200 ? "High calorie intake tomorrow" : "Calorie intake likely stable tomorrow";

        return new DietMLResult(
                Math.round(score * 10.0) / 10.0,
                prediction,
                source,
                recommendation,
                recommendationReason,
                futureRisk,
                List.of(),
                List.of()
        );
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractMapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    public record DietMLResult(double score,
                               String prediction,
                               String source,
                               String recommendation,
                               String recommendationReason,
                               String futureRisk,
                               List<Map<String, Object>> explanation,
                               List<Map<String, Object>> recommendations) {
    }
}
