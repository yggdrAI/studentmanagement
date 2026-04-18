package com.sms.dto.diet;

import java.util.List;

public class DietLogBatchRequest {

    private String day;
    private Integer totalCalories;
    private List<MealEntry> meals;

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public Integer getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(Integer totalCalories) {
        this.totalCalories = totalCalories;
    }

    public List<MealEntry> getMeals() {
        return meals;
    }

    public void setMeals(List<MealEntry> meals) {
        this.meals = meals;
    }

    public static class MealEntry {
        private String name;
        private String mealType;
        private Integer calories;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMealType() {
            return mealType;
        }

        public void setMealType(String mealType) {
            this.mealType = mealType;
        }

        public Integer getCalories() {
            return calories;
        }

        public void setCalories(Integer calories) {
            this.calories = calories;
        }
    }
}
