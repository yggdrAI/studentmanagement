package com.sms.controller;

import com.sms.dto.diet.DietLogBatchRequest;
import com.sms.dto.diet.DietSuggestionResponse;
import com.sms.model.Student;
import com.sms.service.DashboardService;
import com.sms.service.DietAIService;
import com.sms.service.DietLogService;
import com.sms.service.DietMLService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/diet")
@PreAuthorize("hasRole('STUDENT')")
public class DietPlannerController {

    private final DashboardService dashboardService;
    private final DietLogService dietLogService;
    private final DietAIService dietAIService;
    private final DietMLService dietMLService;

    public DietPlannerController(DashboardService dashboardService,
                                 DietLogService dietLogService,
                                 DietAIService dietAIService,
                                 DietMLService dietMLService) {
        this.dashboardService = dashboardService;
        this.dietLogService = dietLogService;
        this.dietAIService = dietAIService;
        this.dietMLService = dietMLService;
    }

    @GetMapping("/suggestion")
    public ResponseEntity<DietSuggestionResponse> getDietSuggestion(Authentication auth) {
        Student student = dashboardService.resolveStudentByUsername(auth.getName());
        return ResponseEntity.ok(buildSuggestion(student.getId()));
    }

    @PostMapping("/log-batch")
    public ResponseEntity<DietSuggestionResponse> logDietBatch(@RequestBody DietLogBatchRequest request,
                                                               Authentication auth) {
        Student student = dashboardService.resolveStudentByUsername(auth.getName());
        dietLogService.logMeals(student.getId(), request != null ? request.getMeals() : null);
        return ResponseEntity.ok(buildSuggestion(student.getId()));
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
                weeklyTrend
        );
    }
}
