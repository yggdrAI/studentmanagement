package com.sms.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ImportArtifactService {

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
}