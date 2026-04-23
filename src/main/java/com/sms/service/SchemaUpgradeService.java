package com.sms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;

@Service
public class SchemaUpgradeService {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgradeService.class);

    private final DataSource dataSource;

    public SchemaUpgradeService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void upgradeSchemaIfNeeded() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            String product = meta.getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("mysql")) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                // Allow storing data-URI images / base64 safely.
                // Hibernate ddl-auto=update does not reliably widen existing columns.
                statement.execute("ALTER TABLE student_profile MODIFY COLUMN profile_image LONGTEXT");
                statement.execute("ALTER TABLE student_profile MODIFY COLUMN profile_photo_url VARCHAR(2048)");

                statement.execute("ALTER TABLE student MODIFY COLUMN profile_image_url LONGTEXT");

                statement.execute("ALTER TABLE teacher_profile MODIFY COLUMN profile_image LONGTEXT");
                statement.execute("ALTER TABLE teacher_profile MODIFY COLUMN profile_photo_url VARCHAR(2048)");
            }

            log.info("Schema upgrade completed: image columns widened for MySQL.");
        } catch (Exception ex) {
            // Best-effort: app should still start even if schema changes are already applied or permissions are limited.
            log.warn("Schema upgrade skipped/failed: {}", ex.getMessage());
        }
    }
}

