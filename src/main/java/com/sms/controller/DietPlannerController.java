package com.sms.controller;

import com.sms.dto.diet.DietLogBatchRequest;
import com.sms.dto.diet.DietSuggestionResponse;
import com.sms.model.Student;
import com.sms.service.DashboardService;
import com.sms.service.DietAIService;
import com.sms.service.DietLogService;
import com.sms.service.DietMealOptimizerService;
import com.sms.service.DietMLService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/student/diet")
@PreAuthorize("hasRole('STUDENT')")
public class DietPlannerController {

    private final DashboardService dashboardService;
    private final DietLogService dietLogService;
    private final DietAIService dietAIService;
    private final DietMLService dietMLService;
    private final DietMealOptimizerService dietMealOptimizerService;

    public DietPlannerController(DashboardService dashboardService,
                                 DietLogService dietLogService,
                                 DietAIService dietAIService,
                                 DietMLService dietMLService,
                                 DietMealOptimizerService dietMealOptimizerService) {
        this.dashboardService = dashboardService;
        this.dietLogService = dietLogService;
        this.dietAIService = dietAIService;
        this.dietMLService = dietMLService;
        this.dietMealOptimizerService = dietMealOptimizerService;
    }

    @GetMapping("/suggestion")
    public ResponseEntity<DietSuggestionResponse> getDietSuggestion(Authentication auth) {
        Student student = dashboardService.resolveStudentByUsername(auth.getName());
        return ResponseEntity.ok(buildSuggestion(student.getId()));
    }

    @PostMapping("/export-dataset")
    public ResponseEntity<Map<String, Object>> exportDataset(Authentication auth) {
        Student student = dashboardService.resolveStudentByUsername(auth.getName());
        int rowsWritten = dietLogService.exportTodayLogsToDataset(student.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("studentId", student.getId());
        response.put("rowsWritten", rowsWritten);
        response.put("datasetPath", "ml-api/diet_dataset.csv");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/log-batch")
    public ResponseEntity<DietSuggestionResponse> logDietBatch(@RequestBody DietLogBatchRequest request,
                                                               Authentication auth) {
        Student student = dashboardService.resolveStudentByUsername(auth.getName());
        dietLogService.logMeals(student.getId(), request != null ? request.getMeals() : null);
        return ResponseEntity.ok(buildSuggestion(student.getId()));
    }

    @GetMapping("/optimize")
    public ResponseEntity<Map<String, Object>> optimizeDietPlan(Authentication auth) {
        Student student = dashboardService.resolveStudentByUsername(auth.getName());

        int caloriesToday = dietLogService.calculateTodayCalories(student.getId());
        double junkRatioToday = dietLogService.calculateJunkRatioToday(student.getId());
        String nextMeal = dietLogService.detectNextMealByTime();
        double weeklyAverage = dietLogService.calculateWeeklyAverage(student.getId(), 7);
        long highCalorieDays = dietLogService.countHighCalorieDays(student.getId(), 7, 1800);

        DietSuggestionResponse base = dietAIService.getSuggestion(caloriesToday, nextMeal, weeklyAverage, highCalorieDays);

        Map<String, Object> response = new HashMap<>();
        response.put("studentId", student.getId());
        response.put("nextMeal", nextMeal);
        response.put("caloriesToday", caloriesToday);
        response.put("junkRatioToday", Math.round(junkRatioToday * 1000.0) / 1000.0);
        response.put("riskLevel", base.riskLevel());
        response.put("engine", "meal-optimizer-v1");
        response.put("recommendations", dietMealOptimizerService.optimize(
                nextMeal,
                caloriesToday,
                junkRatioToday,
                weeklyAverage,
                base.riskLevel()
        ));

        return ResponseEntity.ok(response);
    }

    private DietSuggestionResponse buildSuggestion(String studentId) {
        int caloriesToday = dietLogService.calculateTodayCalories(studentId);
        double junkRatioToday = dietLogService.calculateJunkRatioToday(studentId);
        String nextMeal = dietLogService.detectNextMealByTime();
        double weeklyAverage = dietLogService.calculateWeeklyAverage(studentId, 7);
        long highCalorieDays = dietLogService.countHighCalorieDays(studentId, 7, 1800);
        var weeklyTrend = dietLogService.getWeeklyTrend(studentId, 7);

        DietSuggestionResponse base = dietAIService.getSuggestion(caloriesToday, nextMeal, weeklyAverage, highCalorieDays);
        DietMLService.DietMLResult mlResult = dietMLService.evaluate(caloriesToday, junkRatioToday);

        return new DietSuggestionResponse(
                base.suggestion(),
                base.riskLevel(),
                base.caloriesToday(),
                base.nextMeal(),
                base.weeklyAverage(),
                base.highCalorieDays(),
                mlResult.score(),
                mlResult.prediction(),
                mlResult.source(),
                mlResult.recommendation(),
                mlResult.recommendationReason(),
                mlResult.futureRisk(),
                weeklyTrend,
                mlResult.explanation(),
                mlResult.recommendations()
        );
    }
}
