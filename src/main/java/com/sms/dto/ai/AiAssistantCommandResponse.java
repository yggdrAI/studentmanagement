package com.sms.dto.ai;

import java.util.List;
import java.util.Map;

public record AiAssistantCommandResponse(
        String intent,
        String summary,
        String suggestedEndpoint,
        String httpMethod,
        Map<String, Object> parameters,
        List<String> requiredPermissions,
        boolean requiresConfirmation,
        double confidence,
        List<String> nextSteps) {
}
