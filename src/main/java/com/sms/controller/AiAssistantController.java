package com.sms.controller;

import com.sms.dto.ai.AiAssistantCommandRequest;
import com.sms.dto.ai.AiAssistantCommandResponse;
import com.sms.service.AiAssistantCommandService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/assistant")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class AiAssistantController {

    private final AiAssistantCommandService aiAssistantCommandService;

    public AiAssistantController(AiAssistantCommandService aiAssistantCommandService) {
        this.aiAssistantCommandService = aiAssistantCommandService;
    }

    @PostMapping("/command")
    public AiAssistantCommandResponse command(@Valid @RequestBody AiAssistantCommandRequest request) {
        return aiAssistantCommandService.interpret(request);
    }
}
