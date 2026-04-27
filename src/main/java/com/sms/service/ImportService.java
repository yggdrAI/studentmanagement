package com.sms.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    public void importFile(String type, MultipartFile file, String classId, String batchId) throws IOException {
        log.info("Import requested: type={} classId={} batchId={} filename={}", type, classId, batchId, file.getOriginalFilename());

        List<Map<String, String>> rows = parseCsv(file);

        // Minimal normalization placeholder - in future, expand by type
        for (Map<String, String> row : rows) {
            normalizeRow(type, row, classId, batchId);
        }

        // In a real implementation: persist to DB, enqueue for feature-extraction, update analytics store
        log.info("Imported {} rows for type={} from file={}", rows.size(), type, file.getOriginalFilename());
    }

    private List<Map<String, String>> parseCsv(MultipartFile file) throws IOException {
        List<Map<String, String>> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) return result;
            String[] headers = splitCsvLine(headerLine);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = splitCsvLine(line);
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length && i < cols.length; i++) {
                    row.put(headers[i].trim(), cols[i].trim());
                }
                result.add(row);
            }
        }
        return result;
    }

    private String[] splitCsvLine(String line) {
        // Very basic CSV splitter — does not handle quoted commas. Replace with a CSV library if needed.
        return line.split(",");
    }

    private void normalizeRow(String type, Map<String, String> row, String classId, String batchId) {
        // Basic normalization hooks
        if (classId != null && !classId.isEmpty()) row.putIfAbsent("classId", classId);
        if (batchId != null && !batchId.isEmpty()) row.putIfAbsent("batchId", batchId);

        // Normalize student id keys if present
        if (row.containsKey("student_id")) {
            row.put("studentId", row.remove("student_id"));
        }

        // Example: normalize date fields to ISO — placeholder
        if (row.containsKey("date")) {
            // leave as-is for now; production: parse and format
        }

        // For now just log one row sample for debugging
        log.debug("Normalized row sample: {}", row);
    }
}
