package com.sms.service;

import com.sms.model.FaceData;
import com.sms.repository.FaceDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores and verifies face embeddings only.
 */
@Service
public class FaceVerificationService {

    private static final double FACE_MATCH_THRESHOLD = 0.80;
    private static final String SECURE_MODEL_NAME = "Facenet512";

    private final FaceDataRepository faceDataRepository;
    private final EmbeddingCryptoService embeddingCryptoService;
    private final FaceEmbeddingClientService faceEmbeddingClientService;
    private final ConcurrentHashMap<String, float[]> faceCache = new ConcurrentHashMap<>();

    public FaceVerificationService(FaceDataRepository faceDataRepository,
                                   EmbeddingCryptoService embeddingCryptoService,
                                   FaceEmbeddingClientService faceEmbeddingClientService) {
        this.faceDataRepository = faceDataRepository;
        this.embeddingCryptoService = embeddingCryptoService;
        this.faceEmbeddingClientService = faceEmbeddingClientService;
    }

    @Transactional
    public FaceVerificationResult registerFace(String studentId, List<Double> embedding, Boolean livenessVerified, String livenessPrompt) {
        return registerFace(studentId, 1L, embedding, livenessVerified, livenessPrompt, null, null, null);
    }

    @Transactional
    public FaceVerificationResult registerFace(String studentId,
                                               Long tenantId,
                                               List<Double> embedding,
                                               Boolean livenessVerified,
                                               String livenessPrompt) {
        return registerFace(studentId, tenantId, embedding, livenessVerified, livenessPrompt, null, null, null);
    }

    @Transactional
    public FaceVerificationResult registerFace(String studentId,
                                               Long tenantId,
                                               List<Double> embedding,
                                               Boolean livenessVerified,
                                               String livenessPrompt,
                                               Boolean blinkDetected,
                                               Boolean headMovementDetected,
                                               Integer frameCount) {
        validateEmbedding(embedding);
        if (!isAntiSpoofSignalValid(livenessVerified, livenessPrompt, blinkDetected, headMovementDetected, frameCount)) {
            throw new IllegalArgumentException("Liveness verification is required to register a face");
        }

        float[] vector = toFloatArray(embedding);
        FaceData faceData = faceDataRepository.findByStudentIdAndTenantId(studentId, normalizeTenantId(tenantId)).orElseGet(FaceData::new);
        String encrypted = embeddingCryptoService.encryptEmbedding(vector);
        faceData.setStudentId(studentId);
        faceData.setTenantId(normalizeTenantId(tenantId));
        faceData.setEncryptedEmbedding(encrypted);
        faceData.setEmbeddingVector(encrypted.getBytes(StandardCharsets.UTF_8));
        faceData.setEmbeddingDimension(vector.length);
        faceData.setFaceModel(SECURE_MODEL_NAME);
        faceData.setIsActive(true);

        faceDataRepository.save(faceData);
        faceCache.put(cacheKey(studentId, tenantId), vector);

        return new FaceVerificationResult(true, 1.0, true, "Face enrolled successfully");
    }

    @Transactional
    public FaceVerificationResult registerFaceFromImageUpload(String studentId,
                                                              Long tenantId,
                                                              byte[] imageBytes,
                                                              String filename,
                                                              Boolean livenessVerified,
                                                              String livenessPrompt) {
        return registerFaceFromImageUpload(studentId, tenantId, imageBytes, filename, livenessVerified, livenessPrompt, true, true, 3);
    }

    @Transactional
    public FaceVerificationResult registerFaceFromImageUpload(String studentId,
                                                              Long tenantId,
                                                              byte[] imageBytes,
                                                              String filename,
                                                              Boolean livenessVerified,
                                                              String livenessPrompt,
                                                              Boolean blinkDetected,
                                                              Boolean headMovementDetected,
                                                              Integer frameCount) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Face image is required");
        }

        List<Double> embedding = faceEmbeddingClientService.generateEmbedding(imageBytes, filename);
        return registerFace(studentId, tenantId, embedding, livenessVerified, livenessPrompt, blinkDetected, headMovementDetected, frameCount);
    }

    public FaceVerificationResult verifyFace(String studentId, List<Double> embedding, Boolean livenessVerified, String livenessPrompt) {
        return verifyFace(studentId, 1L, embedding, livenessVerified, livenessPrompt, null, null, null);
    }

    public FaceVerificationResult verifyFace(String studentId,
                                             Long tenantId,
                                             List<Double> embedding,
                                             Boolean livenessVerified,
                                             String livenessPrompt) {
        return verifyFace(studentId, tenantId, embedding, livenessVerified, livenessPrompt, null, null, null);
    }

    public FaceVerificationResult verifyFace(String studentId,
                                             Long tenantId,
                                             List<Double> embedding,
                                             Boolean livenessVerified,
                                             String livenessPrompt,
                                             Boolean blinkDetected,
                                             Boolean headMovementDetected,
                                             Integer frameCount) {
        validateEmbedding(embedding);
        if (!isAntiSpoofSignalValid(livenessVerified, livenessPrompt, blinkDetected, headMovementDetected, frameCount)) {
            throw new IllegalArgumentException("Liveness verification failed");
        }

        float[] candidate = toFloatArray(embedding);
        float[] enrolled = loadFaceVector(studentId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("No registered face found for this student"));

        double similarity = cosineSimilarity(candidate, enrolled);
        if (similarity < FACE_MATCH_THRESHOLD) {
            throw new IllegalArgumentException("Face mismatch");
        }

        return new FaceVerificationResult(true, similarity, true, "Face verified");
    }

    public Optional<FaceData> getFaceData(String studentId) {
        return faceDataRepository.findByStudentId(studentId);
    }

    public Optional<FaceData> getFaceData(String studentId, Long tenantId) {
        return faceDataRepository.findByStudentIdAndTenantId(studentId, normalizeTenantId(tenantId));
    }

    public boolean hasRegisteredFace(String studentId) {
        return faceDataRepository.existsByStudentId(studentId);
    }

    public boolean hasRegisteredFace(String studentId, Long tenantId) {
        return faceDataRepository.existsByStudentIdAndTenantId(studentId, normalizeTenantId(tenantId));
    }

    public double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length || a.length == 0) {
            throw new IllegalArgumentException("Embedding length mismatch");
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int index = 0; index < a.length; index++) {
            dot += a[index] * b[index];
            normA += Math.pow(a[index], 2);
            normB += Math.pow(b[index], 2);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private Optional<float[]> loadFaceVector(String studentId, Long tenantId) {
        String key = cacheKey(studentId, tenantId);
        float[] cached = faceCache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }

        return faceDataRepository.findByStudentIdAndTenantId(studentId, normalizeTenantId(tenantId)).map(data -> {
            float[] vector;
            if (data.getEncryptedEmbedding() != null && !data.getEncryptedEmbedding().isBlank()) {
                vector = embeddingCryptoService.decryptEmbedding(data.getEncryptedEmbedding());
            } else {
                byte[] stored = data.getEmbeddingVector();
                String maybeEncrypted = stored == null ? "" : new String(stored, StandardCharsets.UTF_8);
                if (maybeEncrypted.startsWith("v1:")) {
                    vector = embeddingCryptoService.decryptEmbedding(maybeEncrypted);
                } else {
                    vector = deserialize(data.getEmbeddingVector());
                }
            }

            faceCache.put(key, vector);
            return vector;
        });
    }

    private float[] deserialize(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] vector = new float[bytes.length / Float.BYTES];
        for (int index = 0; index < vector.length; index++) {
            vector[index] = buffer.getFloat();
        }
        return vector;
    }

    private void validateEmbedding(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalArgumentException("Face embedding is required");
        }

        if (embedding.size() < 128) {
            throw new IllegalArgumentException("Embedding vector is too small for secure verification");
        }
    }

    private float[] toFloatArray(List<Double> embedding) {
        float[] vector = new float[embedding.size()];
        for (int index = 0; index < embedding.size(); index++) {
            vector[index] = embedding.get(index).floatValue();
        }
        return vector;
    }

    private boolean isAntiSpoofSignalValid(Boolean livenessVerified,
                                           String livenessPrompt,
                                           Boolean blinkDetected,
                                           Boolean headMovementDetected,
                                           Integer frameCount) {
        if (!Boolean.TRUE.equals(livenessVerified)) {
            return false;
        }

        boolean telemetryProvided = blinkDetected != null || headMovementDetected != null || frameCount != null;

        if (telemetryProvided) {
            if (!Boolean.TRUE.equals(blinkDetected) || !Boolean.TRUE.equals(headMovementDetected)) {
                return false;
            }

            if (frameCount == null || frameCount < 3) {
                return false;
            }
        }

        if (livenessPrompt == null || livenessPrompt.isBlank()) {
            return false;
        }

        String normalizedPrompt = livenessPrompt.toLowerCase();
        return normalizedPrompt.contains("blink") ||
            normalizedPrompt.contains("turn") ||
            normalizedPrompt.contains("move");
    }

    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null || tenantId <= 0 ? 1L : tenantId;
    }

    private String cacheKey(String studentId, Long tenantId) {
        return normalizeTenantId(tenantId) + ":" + studentId;
    }

    public static class FaceVerificationResult {
        private final boolean verified;
        private final double similarity;
        private final boolean livenessVerified;
        private final String message;

        public FaceVerificationResult(boolean verified, double similarity, boolean livenessVerified, String message) {
            this.verified = verified;
            this.similarity = similarity;
            this.livenessVerified = livenessVerified;
            this.message = message;
        }

        public boolean isVerified() { return verified; }
        public double getSimilarity() { return similarity; }
        public boolean isLivenessVerified() { return livenessVerified; }
        public String getMessage() { return message; }
    }
}