package com.sms.service;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseStatusService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final String sourceUrl;
    private final String persistentUrl;

    public DatabaseStatusService(DataSource dataSource,
                                 JdbcTemplate jdbcTemplate,
                                 @Value("${spring.datasource.url}") String sourceUrl,
                                 @Value("${app.database.persistent-url:jdbc:mysql://localhost:3306/studentmanagement?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8}") String persistentUrl) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.sourceUrl = sourceUrl;
        this.persistentUrl = persistentUrl;
    }

    public DatabaseStatusSnapshot getSnapshot() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : getTrackedTables()) {
            counts.put(table, countTable(table));
        }

        return new DatabaseStatusSnapshot(
            sourceUrl,
            getMode(sourceUrl),
            extractPath(sourceUrl),
            persistentUrl,
            extractPath(persistentUrl),
            counts,
            isMemoryMode(sourceUrl),
            sourceUrl.equalsIgnoreCase(persistentUrl)
        );
    }

    public List<String> getTrackedTables() {
        return List.of(
            "student",
            "student_profile",
            "app_user",
            "teacher",
            "course",
            "enrollment",
            "attendance",
            "security_audit",
            "campus_location",
            "student_document",
            "academic_record",
            "class_session",
            "task_item",
            "student_task",
            "fraud_log"
        );
    }

    public long countTable(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return 0L;
        }

        try {
            String sql = "SELECT COUNT(*) FROM " + tableName;
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count == null ? 0L : count;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public boolean isMemoryMode(String jdbcUrl) {
        return false;
    }

    public String getMode(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "unknown";
        }
        String normalized = jdbcUrl.toLowerCase();
        if (normalized.startsWith("jdbc:mysql:")) {
            return "mysql";
        }
        if (normalized.startsWith("jdbc:postgresql:")) {
            return "postgresql";
        }
        return "jdbc";
    }

    public String extractPath(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "-";
        }

        try {
            return URI.create(jdbcUrl.replace("jdbc:", "")).toString();
        } catch (Exception ignored) {
            return jdbcUrl;
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getPersistentUrl() {
        return persistentUrl;
    }

    public record DatabaseStatusSnapshot(
        String sourceUrl,
        String mode,
        String sourcePath,
        String persistentUrl,
        String persistentPath,
        Map<String, Long> tableCounts,
        boolean migrationRequired,
        boolean alreadyPersistent
    ) {}
}
