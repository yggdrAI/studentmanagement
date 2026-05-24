package com.sms.controller;

import com.sms.dto.ai.AiIngestionAnalyzeResponse;
import com.sms.service.SmartIngestionAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/ai-ingestion")
@PreAuthorize("hasRole('ADMIN')")
public class AiIngestionController {

    private final SmartIngestionAnalysisService smartIngestionAnalysisService;

    public AiIngestionController(SmartIngestionAnalysisService smartIngestionAnalysisService) {
        this.smartIngestionAnalysisService = smartIngestionAnalysisService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AiIngestionAnalyzeResponse analyze(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "targetEntity", required = false) String targetEntity) {

        List<MultipartFile> uploadFiles = new ArrayList<>();
        if (files != null) {
            uploadFiles.addAll(files);
        }
        if (file != null) {
            uploadFiles.add(file);
        }
        return smartIngestionAnalysisService.analyze(uploadFiles, targetEntity);
    }
}
