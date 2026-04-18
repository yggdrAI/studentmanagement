package com.sms.dto.diet;

import java.util.List;

public record DietSuggestionResponse(
        String suggestion,
        String riskLevel,
        int caloriesToday,
        String nextMeal,
        double weeklyAverage,
        long highCalorieDays,
        double mlScore,
        String mlPrediction,
        String mlSource,
        String recommendation,
        String recommendationReason,
        String futureRisk,
        List<DailyCaloriePoint> weeklyCalories,
        List<java.util.Map<String, Object>> explanation,
        List<java.util.Map<String, Object>> recommendations
) {
}
