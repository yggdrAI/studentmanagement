package com.sms.service;

import com.sms.model.FaceData;
import com.sms.repository.FaceDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores and verifies face embeddings only.
 */
@Service
public class FaceVerificationService {

    private static final double FACE_MATCH_THRESHOLD = 0.75;

    private final FaceDataRepository faceDataRepository;
    private final ConcurrentHashMap<String, float[]> faceCache = new ConcurrentHashMap<>();

    public FaceVerificationService(FaceDataRepository faceDataRepository) {
        this.faceDataRepository = faceDataRepository;
    }

    @Transactional
    public FaceVerificationResult registerFace(String studentId, List<Double> embedding, Boolean livenessVerified, String livenessPrompt) {
        validateEmbedding(embedding);
        if (!Boolean.TRUE.equals(livenessVerified)) {
            throw new IllegalArgumentException("Liveness verification is required to register a face");
        }

        float[] vector = toFloatArray(embedding);
        FaceData faceData = faceDataRepository.findByStudentId(studentId).orElseGet(FaceData::new);
        faceData.setStudentId(studentId);
        faceData.setEmbeddingVector(serialize(vector));
        faceData.setEmbeddingDimension(vector.length);
        faceData.setFaceModel("face-api.js");
        faceData.setIsActive(true);

        faceDataRepository.save(faceData);
        faceCache.put(studentId, vector);

        return new FaceVerificationResult(true, 1.0, true, "Face enrolled successfully");
    }

    public FaceVerificationResult verifyFace(String studentId, List<Double> embedding, Boolean livenessVerified, String livenessPrompt) {
        validateEmbedding(embedding);
        if (!Boolean.TRUE.equals(livenessVerified)) {
            throw new IllegalArgumentException("Liveness verification failed");
        }

        float[] candidate = toFloatArray(embedding);
        float[] enrolled = loadFaceVector(studentId)
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

    public boolean hasRegisteredFace(String studentId) {
        return faceDataRepository.existsByStudentId(studentId);
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

    private Optional<float[]> loadFaceVector(String studentId) {
        float[] cached = faceCache.get(studentId);
        if (cached != null) {
            return Optional.of(cached);
        }

        return faceDataRepository.findByStudentId(studentId).map(data -> {
            float[] vector = deserialize(data.getEmbeddingVector());
            faceCache.put(studentId, vector);
            return vector;
        });
    }

    private byte[] serialize(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES);
        for (float value : vector) {
            buffer.putFloat(value);
        }
        return buffer.array();
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
    }

    private float[] toFloatArray(List<Double> embedding) {
        float[] vector = new float[embedding.size()];
        for (int index = 0; index < embedding.size(); index++) {
            vector[index] = embedding.get(index).floatValue();
        }
        return vector;
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