package com.sms.service;

import com.sms.dto.diet.DailyCaloriePoint;
import com.sms.dto.diet.DietLogBatchRequest;
import com.sms.model.DietLog;
import com.sms.repository.DietLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DietLogService {

    private static final Set<String> JUNK_KEYWORDS = Set.of(
            "fried", "chole", "poori", "pizza", "burger", "chips", "sweet", "fries", "soda"
    );

    private final DietLogRepository dietLogRepository;

    public DietLogService(DietLogRepository dietLogRepository) {
        this.dietLogRepository = dietLogRepository;
    }

    @Transactional
    public void logMeals(String studentId, List<DietLogBatchRequest.MealEntry> meals) {
        LocalDate today = LocalDate.now();
        dietLogRepository.deleteByStudentIdAndDate(studentId, today);

        if (meals == null || meals.isEmpty()) {
            return;
        }

        for (DietLogBatchRequest.MealEntry meal : meals) {
            if (meal == null || meal.getCalories() == null || meal.getCalories() <= 0) {
                continue;
            }

            DietLog log = new DietLog();
            log.setStudentId(studentId);
            log.setMealName(meal.getName() != null ? meal.getName().trim() : "Meal");
            log.setMealType(meal.getMealType() != null ? meal.getMealType().trim() : "General");
            log.setCalories(meal.getCalories());
            log.setDate(today);
            dietLogRepository.save(log);
        }
    }

    @Transactional(readOnly = true)
    public int calculateTodayCalories(String studentId) {
        return dietLogRepository.findByStudentIdAndDate(studentId, LocalDate.now())
                .stream()
                .mapToInt(DietLog::getCalories)
                .sum();
    }

    @Transactional(readOnly = true)
    public double calculateWeeklyAverage(String studentId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(Math.max(days - 1L, 0L));

        List<DietLog> logs = dietLogRepository.findByStudentIdAndDateBetween(studentId, startDate, endDate);
        if (logs.isEmpty()) {
            return 0.0;
        }

        int total = logs.stream().mapToInt(DietLog::getCalories).sum();
        return total / (double) Math.max(days, 1);
    }

    @Transactional(readOnly = true)
    public long countHighCalorieDays(String studentId, int days, int threshold) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(Math.max(days - 1L, 0L));

        List<DietLog> logs = dietLogRepository.findByStudentIdAndDateBetween(studentId, startDate, endDate);
        if (logs.isEmpty()) {
            return 0;
        }

        return logs.stream()
                .collect(java.util.stream.Collectors.groupingBy(DietLog::getDate,
                        java.util.stream.Collectors.summingInt(DietLog::getCalories)))
                .values()
                .stream()
                .filter(totalCalories -> totalCalories >= threshold)
                .count();
    }

    @Transactional(readOnly = true)
    public double calculateJunkRatioToday(String studentId) {
        List<DietLog> logs = dietLogRepository.findByStudentIdAndDate(studentId, LocalDate.now());
        if (logs.isEmpty()) {
            return 0.0;
        }

        int totalCalories = logs.stream().mapToInt(DietLog::getCalories).sum();
        if (totalCalories <= 0) {
            return 0.0;
        }

        int junkCalories = logs.stream()
                .filter(log -> isJunk(log.getMealName()))
                .mapToInt(DietLog::getCalories)
                .sum();

        return junkCalories / (double) totalCalories;
    }

    @Transactional(readOnly = true)
    public List<DailyCaloriePoint> getWeeklyTrend(String studentId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(Math.max(days - 1L, 0L));

        List<DietLog> logs = dietLogRepository.findByStudentIdAndDateBetween(studentId, startDate, endDate);
        Map<LocalDate, Integer> grouped = logs.stream()
                .collect(Collectors.groupingBy(
                        DietLog::getDate,
                        Collectors.summingInt(DietLog::getCalories)
                ));

        List<DailyCaloriePoint> points = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            points.add(new DailyCaloriePoint(
                    cursor.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    grouped.getOrDefault(cursor, 0)
            ));
            cursor = cursor.plusDays(1);
        }

        return points;
    }

    private boolean isJunk(String mealName) {
        if (mealName == null || mealName.isBlank()) {
            return false;
        }

        String value = mealName.toLowerCase(Locale.ENGLISH);
        return JUNK_KEYWORDS.stream().anyMatch(value::contains);
    }

    public String detectNextMealByTime() {
        LocalTime now = LocalTime.now();

        if (now.isBefore(LocalTime.of(10, 30))) {
            return "Lunch";
        }
        if (now.isBefore(LocalTime.of(16, 30))) {
            return "Snack";
        }
        if (now.isBefore(LocalTime.of(21, 0))) {
            return "Dinner";
        }
        return "Breakfast";
    }
}
