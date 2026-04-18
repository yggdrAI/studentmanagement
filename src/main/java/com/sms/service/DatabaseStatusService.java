package com.sms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseStatusService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final String sourceUrl;
    private final String persistentUrl;

    public DatabaseStatusService(DataSource dataSource,
                                 JdbcTemplate jdbcTemplate,
                                 @Value("${spring.datasource.url}") String sourceUrl,
                                 @Value("${app.database.persistent-url:jdbc:h2:file:./data/studentmanagement;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE}") String persistentUrl) {
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
            "user",
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
        return jdbcUrl != null && jdbcUrl.toLowerCase().startsWith("jdbc:h2:mem:");
    }

    public String getMode(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "unknown";
        }
        String normalized = jdbcUrl.toLowerCase();
        if (normalized.startsWith("jdbc:h2:mem:")) {
            return "h2-memory";
        }
        if (normalized.startsWith("jdbc:h2:file:")) {
            return "h2-file";
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

        String lower = jdbcUrl.toLowerCase();
        if (lower.startsWith("jdbc:h2:file:")) {
            String remainder = jdbcUrl.substring("jdbc:h2:file:".length());
            int optionsIndex = remainder.indexOf(';');
            return optionsIndex >= 0 ? remainder.substring(0, optionsIndex) : remainder;
        }

        if (lower.startsWith("jdbc:h2:mem:")) {
            return "<in-memory>";
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
