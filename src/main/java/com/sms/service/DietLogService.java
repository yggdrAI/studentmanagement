package com.sms.service;

import com.sms.dto.diet.DailyCaloriePoint;
import com.sms.dto.diet.DietLogBatchRequest;
import com.sms.model.DietLog;
import com.sms.repository.DietLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

    private static final Logger logger = LoggerFactory.getLogger(DietLogService.class);

    private static final Path DATASET_PATH = Paths.get("ml-api", "diet_dataset.csv");
    private static final String DATASET_HEADER = "calories,junk_ratio,protein,carbs,fat,meal_time,activity_level,health_score,label,sleep_hours,water_intake,steps,bmi";

    private static final Set<String> JUNK_KEYWORDS = Set.of(
            "fried", "chole", "poori", "pizza", "burger", "chips", "sweet", "fries", "soda"
    );

    private final DietLogRepository dietLogRepository;
    private final DietMLService dietMLService;

    public DietLogService(DietLogRepository dietLogRepository, DietMLService dietMLService) {
        this.dietLogRepository = dietLogRepository;
        this.dietMLService = dietMLService;
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
            appendToDataset(log);
        }
    }

    @Transactional(readOnly = true)
    public int exportTodayLogsToDataset(String studentId) {
        List<DietLog> logs = dietLogRepository.findByStudentIdAndDate(studentId, LocalDate.now());
        if (logs.isEmpty()) {
            return 0;
        }

        try {
            Files.createDirectories(DATASET_PATH.getParent());
            if (Files.notExists(DATASET_PATH) || Files.size(DATASET_PATH) == 0L) {
                Files.writeString(DATASET_PATH, DATASET_HEADER + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            List<String> rows = new ArrayList<>();
            for (DietLog log : logs) {
                rows.add(toDatasetRow(log, evaluateForDataset(log)));
            }
            Files.write(DATASET_PATH, rows, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return rows.size();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to export diet logs to dataset", ex);
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

    private void appendToDataset(DietLog log) {
        try {
            Files.createDirectories(DATASET_PATH.getParent());
            if (Files.notExists(DATASET_PATH) || Files.size(DATASET_PATH) == 0L) {
                Files.writeString(DATASET_PATH, DATASET_HEADER + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            String row = toDatasetRow(log, evaluateForDataset(log));
            Files.writeString(DATASET_PATH, row, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ex) {
            logger.warn("Failed to append diet log to dataset: {}", ex.getMessage());
        }
    }

    private DietMLService.DietMLResult evaluateForDataset(DietLog log) {
        double junkRatio = isJunk(log.getMealName()) ? 1.0 : 0.0;
        return dietMLService.evaluate(log.getCalories(), junkRatio);
    }

    private String toDatasetRow(DietLog log, DietMLService.DietMLResult mlResult) {
        int calories = log.getCalories();
        int mealHour = switch (log.getMealType() == null ? "" : log.getMealType().toLowerCase(Locale.ENGLISH)) {
            case "breakfast" -> 8;
            case "lunch" -> 13;
            case "snack" -> 17;
            case "dinner" -> 20;
            default -> LocalTime.now().getHour();
        };

        double junkRatio = isJunk(log.getMealName()) ? 1.0 : 0.0;
        int protein = estimateProtein(log.getMealName(), log.getMealType(), calories);
        int carbs = Math.max(8, Math.round(calories * 0.52f));
        int fat = Math.max(3, Math.round(calories * 0.24f));
        double activityLevel = 2.0;
        double sleepHours = 7.0;
        double waterIntake = 2.0;
        double steps = 5000.0;
        double bmi = 23.0;
        double healthScore = mlResult.score();
        String label = mlResult.prediction();

        return String.join(",",
                String.valueOf(calories),
                String.valueOf(junkRatio),
                String.valueOf(protein),
                String.valueOf(carbs),
                String.valueOf(fat),
                String.valueOf(mealHour),
                String.valueOf(activityLevel),
                String.valueOf(Math.round(healthScore * 100.0) / 100.0),
                label,
                String.valueOf(sleepHours),
                String.valueOf(waterIntake),
                String.valueOf(steps),
            String.valueOf(bmi)
        ) + System.lineSeparator();
    }

    private int estimateProtein(String mealName, String mealType, int calories) {
        String value = ((mealName == null ? "" : mealName) + " " + (mealType == null ? "" : mealType)).toLowerCase(Locale.ENGLISH);
        if (value.contains("paneer") || value.contains("dal") || value.contains("chana")) {
            return Math.max(12, Math.round(calories * 0.16f));
        }
        if (value.contains("oats") || value.contains("salad") || value.contains("yogurt")) {
            return Math.max(10, Math.round(calories * 0.14f));
        }
        if (isJunk(mealName)) {
            return Math.max(4, Math.round(calories * 0.08f));
        }
        return Math.max(8, Math.round(calories * 0.12f));
    }
}
