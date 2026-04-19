package com.sms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DatabaseMigrationService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationService.class);
    private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DataSource dataSource;
    private final String sourceUrl;
    private final String sourceUsername;
    private final String sourcePassword;
    private final String persistentUrl;
    private final DatabaseStatusService databaseStatusService;
    private final AtomicBoolean migrationAttempted = new AtomicBoolean(false);
    private volatile String lastMigrationMessage = "Migration not attempted";
    private volatile boolean lastMigrationSuccess = false;

    public DatabaseMigrationService(DataSource dataSource,
                                    DatabaseStatusService databaseStatusService,
                                    @Value("${spring.datasource.url}") String sourceUrl,
                                    @Value("${spring.datasource.username:}") String sourceUsername,
                                    @Value("${spring.datasource.password:}") String sourcePassword,
                                    @Value("${app.database.persistent-url:jdbc:mysql://localhost:3306/studentmanagement?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8}") String persistentUrl) {
        this.dataSource = dataSource;
        this.databaseStatusService = databaseStatusService;
        this.sourceUrl = sourceUrl;
        this.sourceUsername = sourceUsername;
        this.sourcePassword = sourcePassword;
        this.persistentUrl = persistentUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!databaseStatusService.isMemoryMode(sourceUrl)) {
            lastMigrationMessage = "Source database is persistent; no export needed";
            lastMigrationSuccess = true;
            return;
        }
        lastMigrationMessage = "In-memory source detected. Export will run on shutdown.";
    }

    @EventListener(ContextClosedEvent.class)
    public void onContextClosed() {
        migrateMemoryDbIfNeeded();
    }

    public void migrateMemoryDbIfNeeded() {
        if (!databaseStatusService.isMemoryMode(sourceUrl)) {
            return;
        }

        if (!migrationAttempted.compareAndSet(false, true)) {
            return;
        }

        if (sourceUrl.equalsIgnoreCase(persistentUrl)) {
            lastMigrationMessage = "Source and persistent URLs are identical; skipping migration";
            lastMigrationSuccess = true;
            return;
        }

        Path tempScript = null;
        try (Connection sourceConnection = dataSource.getConnection()) {
            if (hasExistingUserDataInTarget()) {
                lastMigrationMessage = "Persistent database already has data; migration skipped to avoid overwrite";
                lastMigrationSuccess = true;
                return;
            }

            tempScript = Files.createTempFile("studentmanagement-db-migration", ".sql");
            String scriptPath = tempScript.toAbsolutePath().toString().replace("\\", "/");

            try (Statement statement = sourceConnection.createStatement()) {
                statement.execute("SCRIPT TO '" + scriptPath + "'");
            }

            try (Connection targetConnection = openTargetConnection(); Statement targetStatement = targetConnection.createStatement()) {
                targetStatement.execute("RUNSCRIPT FROM '" + scriptPath + "'");
            }

            lastMigrationMessage = "In-memory database exported successfully to persistent storage";
            lastMigrationSuccess = true;
            log.info(lastMigrationMessage);
        } catch (Exception ex) {
            lastMigrationMessage = "Migration failed: " + ex.getMessage();
            lastMigrationSuccess = false;
            log.warn("Database migration did not complete", ex);
        } finally {
            if (tempScript != null) {
                try {
                    Files.deleteIfExists(tempScript);
                } catch (Exception ignored) {
                    // Best effort cleanup only.
                }
            }
        }
    }

    public ExportedDatabaseBackup exportCurrentDatabase() throws Exception {
        Path tempScript = Files.createTempFile("studentmanagement-backup", ".sql");
        try (Connection sourceConnection = dataSource.getConnection(); Statement statement = sourceConnection.createStatement()) {
            String scriptPath = tempScript.toAbsolutePath().toString().replace("\\", "/");
            statement.execute("SCRIPT TO '" + scriptPath + "'");
            byte[] bytes = Files.readAllBytes(tempScript);
            String fileName = "studentmanagement-backup-" + LocalDateTime.now().format(BACKUP_FORMAT) + ".sql";
            return new ExportedDatabaseBackup(fileName, bytes);
        } finally {
            Files.deleteIfExists(tempScript);
        }
    }

    public void restoreDatabaseFromScript(byte[] scriptBytes) throws Exception {
        if (scriptBytes == null || scriptBytes.length == 0) {
            throw new IllegalArgumentException("Backup file is empty");
        }

        Path tempScript = Files.createTempFile("studentmanagement-restore", ".sql");
        try {
            Files.write(tempScript, scriptBytes);
            try (Connection connection = openTargetConnection(); Statement statement = connection.createStatement()) {
                statement.execute("DROP ALL OBJECTS DELETE FILES");
                statement.execute("RUNSCRIPT FROM '" + tempScript.toAbsolutePath().toString().replace("\\", "/") + "'");
            }
            lastMigrationMessage = "Database restored successfully from backup";
            lastMigrationSuccess = true;
        } finally {
            Files.deleteIfExists(tempScript);
        }
    }

    private boolean hasExistingUserDataInTarget() {
        try (Connection targetConnection = openTargetConnection()) {
            DatabaseMetaData metaData = targetConnection.getMetaData();
            try (var tables = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String schema = tables.getString("TABLE_SCHEM");
                    String tableName = tables.getString("TABLE_NAME");
                    if (schema != null && !schema.equalsIgnoreCase("PUBLIC") && !schema.equalsIgnoreCase("APP")) {
                        continue;
                    }
                    if (tableName != null && !tableName.startsWith("SYSTEM_")) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private Connection openTargetConnection() throws Exception {
        if (sourceUsername == null || sourceUsername.isBlank()) {
            return java.sql.DriverManager.getConnection(persistentUrl);
        }
        return java.sql.DriverManager.getConnection(persistentUrl, sourceUsername, sourcePassword == null ? "" : sourcePassword);
    }

    public boolean isLastMigrationSuccess() {
        return lastMigrationSuccess;
    }

    public String getLastMigrationMessage() {
        return lastMigrationMessage;
    }

    public record ExportedDatabaseBackup(String fileName, byte[] content) {}
}
