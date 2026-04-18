package com.sms.controller;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sms.service.AiAnalyticsService;
import com.sms.service.AnalyticsReportService;

@RestController
@RequestMapping({"/api/analytics", "/api/admin/analytics", "/api/teacher/analytics", "/api/student/analytics"})
@PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
public class AnalyticsApiController {

    private final AiAnalyticsService aiAnalyticsService;
    private final AnalyticsReportService analyticsReportService;

    public AnalyticsApiController(AiAnalyticsService aiAnalyticsService,
                                  AnalyticsReportService analyticsReportService) {
        this.aiAnalyticsService = aiAnalyticsService;
        this.analyticsReportService = analyticsReportService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam(name = "course", required = false) String course,
                                       @RequestParam(name = "semester", required = false) String semester,
                                       @RequestParam(name = "section", required = false) String section,
                                       @RequestParam(name = "from", required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(name = "to", required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return aiAnalyticsService.buildDashboard(role, authentication.getName(), course, semester, section, from, to);
    }

    @GetMapping("/live")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Map<String, Object> live() {
        return aiAnalyticsService.buildLiveSnapshot();
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportCsv() {
        Map<String, byte[]> bundle = analyticsReportService.buildExportBundle();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ai-analytics-digest.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bundle.get("csv"));
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportPdf() {
        Map<String, byte[]> bundle = analyticsReportService.buildExportBundle();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ai-analytics-digest.pdf")
            .contentType(MediaType.parseMediaType("application/pdf"))
                .body(bundle.get("pdf"));
    }

    @PostMapping("/reports/digest")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> sendDigest() {
        analyticsReportService.sendLeadershipDigest();
        return Map.of("status", "queued");
    }

    @GetMapping("/student-summary/{studentId}")
    public Map<String, Object> studentSummary(@PathVariable String studentId,
                                              Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if ("ROLE_STUDENT".equals(role)) {
            Map<String, Object> own = aiAnalyticsService.buildDashboard("STUDENT", authentication.getName(), null, null, null, null, null);
            Object tagsObj = own.get("studentTags");
            if (tagsObj instanceof java.util.List<?> tags) {
                boolean owns = tags.stream().anyMatch(item -> item instanceof java.util.Map<?, ?> map && studentId.equals(String.valueOf(map.get("studentId"))));
                if (!owns) {
                    throw new IllegalArgumentException("Not allowed to access this profile analytics");
                }
            }
        } else if ("ROLE_TEACHER".equals(role)) {
            Map<String, Object> own = aiAnalyticsService.buildDashboard("TEACHER", authentication.getName(), null, null, null, null, null);
            Object tagsObj = own.get("studentTags");
            if (tagsObj instanceof java.util.List<?> tags) {
                boolean teachesStudent = tags.stream().anyMatch(item -> item instanceof java.util.Map<?, ?> map && Objects.equals(studentId, String.valueOf(map.get("studentId"))));
                if (!teachesStudent) {
                    throw new IllegalArgumentException("Not allowed to access non-assigned student analytics");
                }
            }
        }
        return aiAnalyticsService.buildStudentAiSummary(studentId);
    }
}
