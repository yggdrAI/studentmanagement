package com.sms.service;

import com.sms.dto.diet.DietSuggestionResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DietAIService {

    public DietSuggestionResponse getSuggestion(int caloriesToday,
                                                String nextMeal,
                                                double weeklyAverage,
                                                long highCalorieDays) {
        String suggestion;
        String riskLevel;

        if (caloriesToday > 1800) {
            suggestion = "High calorie intake today. Choose a light " + nextMeal + " with vegetables and hydration.";
            riskLevel = "HIGH";
        } else if (caloriesToday < 800 && "Lunch".equalsIgnoreCase(nextMeal)) {
            suggestion = "You are low on energy today. Choose a balanced lunch with protein + carbs.";
            riskLevel = "LOW";
        } else if ("Dinner".equalsIgnoreCase(nextMeal)) {
            suggestion = "Prefer a low-carb dinner for better digestion and sleep quality.";
            riskLevel = "MEDIUM";
        } else if (weeklyAverage > 1700 || highCalorieDays >= 4) {
            suggestion = "Your weekly trend is calorie-heavy. Add one lighter meal slot today.";
            riskLevel = "MEDIUM";
        } else {
            suggestion = "You are maintaining good meal balance. Continue the same pattern.";
            riskLevel = "LOW";
        }

        return new DietSuggestionResponse(
                suggestion,
                riskLevel,
                caloriesToday,
                nextMeal,
                Math.round(weeklyAverage * 10.0) / 10.0,
            highCalorieDays,
            0.0,
            "moderate",
            "heuristic",
            List.of()
        );
    }
}
