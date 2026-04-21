package com.sms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.model.AnalyticsSnapshot;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class AnalyticsReportService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsReportService.class);

    private final AnalyticsSnapshotService analyticsSnapshotService;
    private final AiAnalyticsService aiAnalyticsService;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @Value("${app.analytics.reports.enabled:false}")
    private boolean reportsEnabled;

    @Value("${app.analytics.reports.sender:no-reply@bennett.edu.in}")
    private String sender;

    @Value("${app.analytics.reports.recipients:}")
    private String recipients;

    @Value("${app.analytics.reports.subject:Daily AI Analytics Digest}")
    private String subject;

    public AnalyticsReportService(AnalyticsSnapshotService analyticsSnapshotService,
                                  AiAnalyticsService aiAnalyticsService,
                                  Optional<JavaMailSender> mailSender,
                                  ObjectMapper objectMapper) {
        this.analyticsSnapshotService = analyticsSnapshotService;
        this.aiAnalyticsService = aiAnalyticsService;
        this.mailSender = mailSender.orElse(null);
        this.objectMapper = objectMapper;
    }

    public byte[] generateCsv(Map<String, Object> summary) {
        StringBuilder builder = new StringBuilder();
        builder.append("Section,Key,Value\n");
        appendMap(builder, "Metrics", asMap(summary.get("metrics")));
        appendList(builder, "Recommendations", asList(summary.get("recommendations")));
        appendList(builder, "SmartCards", asList(summary.get("smartCards")));
        appendList(builder, "ActivityFeed", asList(summary.get("activityFeed")));
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generatePdf(Map<String, Object> summary, String title) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float y = 770;
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.beginText();
                contentStream.newLineAtOffset(40, y);
                contentStream.showText(truncate(title, 95));
                contentStream.endText();

                y -= 28;
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                for (String line : buildReportLines(summary)) {
                    if (y < 60) {
                        break;
                    }
                    contentStream.beginText();
                    contentStream.newLineAtOffset(40, y);
                    contentStream.showText(truncate(line, 105));
                    contentStream.endText();
                    y -= 14;
                }
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate analytics PDF", ex);
        }
    }

    public Map<String, Object> currentLeadershipSummary() {
        Optional<AnalyticsSnapshot> snapshot = analyticsSnapshotService.findLatestSnapshot("ADMIN", "GLOBAL");
        if (snapshot.isPresent()) {
            return analyticsSnapshotService.buildSnapshotView(snapshot.get());
        }
        return aiAnalyticsService.buildDashboard("ADMIN", "system", null, null, null, null, null);
    }

    public Map<String, byte[]> buildExportBundle() {
        Map<String, Object> summary = currentLeadershipSummary();
        Map<String, byte[]> bundle = new LinkedHashMap<>();
        bundle.put("csv", generateCsv(summary));
        bundle.put("pdf", generatePdf(summary, "AI Analytics Digest"));
        return bundle;
    }

    public void sendLeadershipDigest() {
        if (!reportsEnabled || recipients == null || recipients.isBlank()) {
            log.info("Analytics report digest skipped because reporting is disabled or recipients are not configured");
            return;
        }

        if (mailSender == null) {
            log.info("Analytics report digest skipped because JavaMailSender is not configured");
            return;
        }

        try {
            Map<String, byte[]> bundle = buildExportBundle();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(sender);
            helper.setTo(splitRecipients());
            helper.setSubject(subject + " - " + LocalDateTime.now().toLocalDate());
            helper.setText("Attached is the latest AI analytics digest for leadership review.", false);
            helper.addAttachment("ai-analytics-digest.csv", new ByteArrayResource(bundle.get("csv")));
            helper.addAttachment("ai-analytics-digest.pdf", new ByteArrayResource(bundle.get("pdf")));
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Unable to send analytics digest", ex);
        }
    }

    private void appendMap(StringBuilder builder, String section, Map<String, Object> map) {
        map.forEach((key, value) -> builder.append(section).append(',').append(escape(key)).append(',').append(escape(String.valueOf(value))).append('\n'));
    }

    private void appendList(StringBuilder builder, String section, List<?> list) {
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Object titleValue = map.containsKey("title") ? map.get("title") : (map.containsKey("message") ? map.get("message") : "item");
                builder.append(section).append(',').append(escape(String.valueOf(titleValue))).append(',').append(escape(objectToString(map))).append('\n');
            } else {
                builder.append(section).append(',').append(escape(String.valueOf(item))).append(',').append(escape(String.valueOf(item))).append('\n');
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private List<?> asList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        return List.of();
    }

    private List<String> buildReportLines(Map<String, Object> summary) {
        List<String> lines = new ArrayList<>();
        lines.add("Scope: " + summary.getOrDefault("scope", "ADMIN"));
        lines.add("Time range: " + summary.getOrDefault("timeRange", Map.of()));
        lines.add("Metrics: " + summary.getOrDefault("metrics", Map.of()));
        lines.add("Recommendations:");
        for (Object item : asList(summary.get("recommendations"))) {
            lines.add("- " + String.valueOf(item));
        }
        lines.add("Smart cards:");
        for (Object item : asList(summary.get("smartCards"))) {
            lines.add("- " + objectToString(item));
        }
        lines.add("Activity feed:");
        for (Object item : asList(summary.get("activityFeed"))) {
            lines.add("- " + objectToString(item));
        }
        return lines;
    }

    private String objectToString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private String[] splitRecipients() {
        return List.of(recipients.split(","))
                .stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\n", " ").replace(",", " ");
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }
}
