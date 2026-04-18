package com.sms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FaceEmbeddingClientService {

    private final RestClient restClient;

    public FaceEmbeddingClientService(@Value("${app.face.embedding-service-url:http://localhost:8001}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<Double> generateEmbedding(byte[] imageBytes, String filename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename == null || filename.isBlank() ? "face-upload.jpg" : filename;
            }
        });

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
            .uri("/embedding")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(Map.class);

        if (response == null || !response.containsKey("embedding")) {
            throw new IllegalStateException("Embedding service returned an invalid response");
        }

        Object embeddingObj = response.get("embedding");
        if (!(embeddingObj instanceof List<?> rawList) || rawList.isEmpty()) {
            throw new IllegalStateException("Embedding vector is missing from embedding service response");
        }

        List<Double> embedding = new ArrayList<>(rawList.size());
        for (Object value : rawList) {
            if (value instanceof Number number) {
                embedding.add(number.doubleValue());
            }
        }

        if (embedding.isEmpty()) {
            throw new IllegalStateException("Embedding vector contains no numeric values");
        }

        return embedding;
    }
}
