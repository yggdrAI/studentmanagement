package com.sms.service;

import com.sms.dto.ai.AiAssistantCommandRequest;
import com.sms.dto.ai.AiAssistantCommandResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiAssistantCommandService {

    public AiAssistantCommandResponse interpret(AiAssistantCommandRequest request) {
        String prompt = request.prompt() == null ? "" : request.prompt().trim();
        String normalized = prompt.toLowerCase(Locale.ROOT);
        Map<String, Object> parameters = extractParameters(normalized, request.scope(), request.tenantId());

        if (containsAny(normalized, "low attendance", "absent", "absentee", "attendance below")) {
            return response(
                    "FIND_LOW_ATTENDANCE",
                    "Find students whose attendance needs intervention.",
                    "/api/analytics/summary",
                    "GET",
                    parameters,
                    List.of("VIEW_ANALYTICS"),
                    false,
                    0.9,
                    List.of("Review at-risk student tags", "Open student summary", "Notify parents after admin confirmation"));
        }

        if (containsAny(normalized, "topper", "rank", "top performer", "leaderboard")) {
            return response(
                    "GENERATE_TOPPER_ANALYSIS",
                    "Generate rank and top-performer analysis for the selected academic scope.",
                    "/api/analytics/summary",
                    "GET",
                    parameters,
                    List.of("VIEW_ANALYTICS"),
                    false,
                    0.86,
                    List.of("Compare marks distribution", "Export report", "Share with leadership"));
        }

        if (containsAny(normalized, "risk", "failure", "fail", "weak subject", "intervention")) {
            return response(
                    "FIND_ACADEMIC_RISK",
                    "Identify academic-risk students using marks and attendance signals.",
                    "/api/analytics/summary",
                    "GET",
                    parameters,
                    List.of("VIEW_ANALYTICS"),
                    false,
                    0.88,
                    List.of("Inspect risk drivers", "Generate student action plans", "Schedule mentor review"));
        }

        if (containsAny(normalized, "compare", "across batches", "department performance", "class comparison")) {
            return response(
                    "COMPARE_COHORTS",
                    "Compare performance across classes, batches, departments, or subjects.",
                    "/api/analytics/summary",
                    "GET",
                    parameters,
                    List.of("VIEW_ANALYTICS"),
                    false,
                    0.82,
                    List.of("Choose comparison dimension", "Review trend chart", "Export leadership digest"));
        }

        if (containsAny(normalized, "parent meeting", "parent summary", "feedback", "report card")) {
            return response(
                    "GENERATE_PARENT_SUMMARY",
                    "Draft parent-ready academic feedback from student performance signals.",
                    "/api/analytics/student-summary/{studentId}",
                    "GET",
                    parameters,
                    List.of("VIEW_ANALYTICS"),
                    true,
                    0.8,
                    List.of("Select student", "Generate draft", "Review before sending"));
        }

        if (containsAny(normalized, "timetable", "schedule", "teacher availability", "classroom")) {
            return response(
                    "DRAFT_TIMETABLE",
                    "Create or improve a timetable draft with conflict checks.",
                    "/api/timetables/auto-schedule",
                    "POST",
                    parameters,
                    List.of("MANAGE_TIMETABLE"),
                    true,
                    0.78,
                    List.of("Load teacher availability", "Run conflict detector", "Preview draft before publishing"));
        }

        if (containsAny(normalized, "upload", "import", "excel", "csv", "pdf", "marksheet", "attendance sheet")) {
            return response(
                    "START_SMART_INGESTION",
                    "Analyze uploaded academic files and suggest schema mappings.",
                    "/api/admin/ai-ingestion/analyze",
                    "POST",
                    parameters,
                    List.of("MANAGE_IMPORTS"),
                    true,
                    0.84,
                    List.of("Upload source files", "Review AI mappings", "Confirm transactional import"));
        }

        return response(
                "ANSWER_WITH_CONTEXT",
                "Route the request to the AI assistant with current academic context.",
                "/api/analytics/summary",
                "GET",
                parameters,
                List.of("VIEW_ANALYTICS"),
                false,
                0.55,
                List.of("Clarify academic scope", "Show relevant dashboard", "Offer export or follow-up actions"));
    }

    private Map<String, Object> extractParameters(String normalizedPrompt, String scope, String tenantId) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (scope != null && !scope.isBlank()) {
            parameters.put("scope", scope.trim());
        }
        if (tenantId != null && !tenantId.isBlank()) {
            parameters.put("tenantId", tenantId.trim());
        }
        Integer semester = extractNumberAfter(normalizedPrompt, "semester");
        if (semester != null) {
            parameters.put("semester", semester);
        }
        Integer clazz = extractNumberAfter(normalizedPrompt, "class");
        if (clazz != null) {
            parameters.put("classNumber", clazz);
        }
        if (normalizedPrompt.contains("physics")) {
            parameters.put("subject", "Physics");
        } else if (normalizedPrompt.contains("math")) {
            parameters.put("subject", "Mathematics");
        } else if (normalizedPrompt.contains("chemistry")) {
            parameters.put("subject", "Chemistry");
        }
        return parameters;
    }

    private Integer extractNumberAfter(String text, String marker) {
        int index = text.indexOf(marker);
        if (index < 0) {
            return null;
        }
        String suffix = text.substring(index + marker.length()).trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d{1,2})").matcher(suffix);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private AiAssistantCommandResponse response(String intent,
                                                String summary,
                                                String endpoint,
                                                String method,
                                                Map<String, Object> parameters,
                                                List<String> permissions,
                                                boolean requiresConfirmation,
                                                double confidence,
                                                List<String> nextSteps) {
        return new AiAssistantCommandResponse(
                intent,
                summary,
                endpoint,
                method,
                parameters,
                permissions,
                requiresConfirmation,
                confidence,
                nextSteps);
    }
}
