package com.sms.dto.ai;

import jakarta.validation.constraints.NotBlank;

public record AiAssistantCommandRequest(
        @NotBlank(message = "prompt is required")
        String prompt,
        String scope,
        String tenantId) {
}
