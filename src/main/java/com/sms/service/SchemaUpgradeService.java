package com.sms.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(0)
public class SchemaUpgradeService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgradeService.class);

    private final DataSource dataSource;

    public SchemaUpgradeService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
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

                try {
                    statement.execute("ALTER TABLE course DROP FOREIGN KEY FKsybhlxoejr4j3teomm5u2bx1n");
                } catch (SQLException ignored) {
                    // The constraint name can differ across local databases; best-effort only.
                }

                try {
                    statement.execute("ALTER TABLE course ADD CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id)");
                } catch (SQLException ex) {
                    log.warn("Course foreign key already aligned or could not be updated: {}", ex.getMessage());
                }
            }

            log.info("Schema upgrade completed: image columns widened for MySQL and course.teacher_id aligned to teachers.");
        } catch (Exception ex) {
            // Best-effort: app should still start even if schema changes are already applied or permissions are limited.
            log.warn("Schema upgrade skipped/failed: {}", ex.getMessage());
        }
    }
}

