package com.sms.dto.ai;

import java.util.List;
import java.util.Map;

public record AiIngestionAnalyzeResponse(
        String jobType,
        int fileCount,
        List<FileAnalysis> files,
        List<FieldMapping> suggestedMappings,
        List<DataQualityWarning> warnings,
        List<String> nextActions) {

    public record FileAnalysis(
            String fileName,
            String contentType,
            String detectedDocumentType,
            List<String> extractedHeaders,
            double confidence) {
    }

    public record FieldMapping(
            String sourceField,
            String canonicalField,
            String targetEntity,
            double confidence,
            String reason) {
    }

    public record DataQualityWarning(
            String severity,
            String code,
            String message,
            Map<String, Object> context) {
    }
}
