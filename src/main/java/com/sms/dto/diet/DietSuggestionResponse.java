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
        List<DailyCaloriePoint> weeklyCalories
) {
}
