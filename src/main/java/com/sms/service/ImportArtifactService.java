package com.sms.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ImportArtifactService {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public String saveArtifact(String fileName, byte[] content) {
        try {
            Path dir = Paths.get(uploadDir, "imports");
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            Files.write(target, content);
            return target.toString().replace('\\', '/');
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to save import artifact", ex);
        }
    }

    public String saveUploadedSource(String originalFileName, byte[] content) {
        try {
            Path dir = Paths.get(uploadDir, "imports", "source");
            Files.createDirectories(dir);

            String safeName = sanitizeFileName(originalFileName);
            String timestamp = LocalDateTime.now().format(TS_FORMAT);
            Path target = dir.resolve(timestamp + "-" + safeName);
            Files.write(target, content);

            Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path relative = uploadRoot.relativize(target.toAbsolutePath().normalize());
            return "/uploads/" + relative.toString().replace('\\', '/');
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to save uploaded source file", ex);
        }
    }

    private String sanitizeFileName(String fileName) {
        String fallback = "students-import.";
        if (fileName == null || fileName.isBlank()) {
            return fallback + "csv";
        }

        String lower = fileName.toLowerCase(Locale.ROOT);
        String ext = lower.endsWith(".xlsx") ? ".xlsx" : lower.endsWith(".xls") ? ".xls" : ".csv";
        String base = fileName.replaceAll("[\\\\/]+", "_").replaceAll("\n|\r|\t", "_");
        base = base.replaceAll("\\.[A-Za-z0-9]{1,8}$", "").trim();
        if (base.isBlank()) {
            base = "students-import";
        }
        return base + ext;
    }
}