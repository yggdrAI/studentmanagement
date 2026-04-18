package com.sms.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DietMealOptimizerService {

    public List<Map<String, Object>> optimize(String nextMeal,
                                              int caloriesToday,
                                              double junkRatio,
                                              double weeklyAverage,
                                              String riskLevel) {
        String mealSlot = nextMeal != null && !nextMeal.isBlank() ? nextMeal.trim() : "Meal";

        List<Map<String, Object>> candidates = new ArrayList<>();
        candidates.add(option(mealSlot, "Paneer Salad Bowl", 430, 38, 24, 4, "LOW"));
        candidates.add(option(mealSlot, "Dal + Brown Rice", 520, 62, 20, 8, "LOW"));
        candidates.add(option(mealSlot, "Grilled Chicken Wrap", 560, 48, 34, 10, "MEDIUM"));
        candidates.add(option(mealSlot, "Veggie Oats Upma", 360, 50, 10, 5, "LOW"));
        candidates.add(option(mealSlot, "Fruit Yogurt Combo", 300, 42, 14, 3, "LOW"));
        candidates.add(option(mealSlot, "Fried Combo Meal", 780, 64, 18, 29, "HIGH"));

        double dailyPressure = Math.max(0, caloriesToday - 1400) / 12.0;
        double weeklyPressure = Math.max(0, weeklyAverage - 1500) / 10.0;
        double junkPressure = Math.max(0, junkRatio - 0.20) * 100.0;

        for (Map<String, Object> candidate : candidates) {
            int cals = ((Number) candidate.get("calories")).intValue();
            int protein = ((Number) candidate.get("protein")).intValue();
            int fiber = ((Number) candidate.get("fiber")).intValue();
            int oil = ((Number) candidate.get("oil")).intValue();

            double qualityScore = (protein * 1.8) + (fiber * 1.4) - (oil * 1.6) - (cals / 16.0);
            double penalty = dailyPressure + weeklyPressure + junkPressure;

            if ("HIGH".equalsIgnoreCase(riskLevel)) {
                penalty += cals > 520 ? 12 : 0;
            }

            double score = Math.max(0.0, Math.min(100.0, 65.0 + qualityScore - penalty));
            candidate.put("score", Math.round(score * 10.0) / 10.0);
        }

        candidates.sort(Comparator.comparingDouble(this::scoreValue).reversed());
        return candidates.stream().limit(3).toList();
    }

    private Map<String, Object> option(String mealSlot,
                                       String mealName,
                                       int calories,
                                       int carbs,
                                       int protein,
                                       int oil,
                                       String intensity) {
        Map<String, Object> item = new HashMap<>();
        item.put("mealSlot", mealSlot);
        item.put("mealName", mealName);
        item.put("calories", calories);
        item.put("carbs", carbs);
        item.put("protein", protein);
        item.put("oil", oil);
        item.put("intensity", intensity);
        return item;
    }

    private double scoreValue(Map<String, Object> row) {
        Object value = row.get("score");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }
}
