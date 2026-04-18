package com.sms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class EmbeddingCryptoService {

    private static final String PREFIX = "v1";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    private final ObjectMapper objectMapper;
    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmbeddingCryptoService(ObjectMapper objectMapper,
                                  @Value("${app.face.embedding-encryption-key:}") String configuredKey) {
        this.objectMapper = objectMapper;
        this.secretKey = buildKey(configuredKey);
    }

    public String encryptEmbedding(float[] vector) {
        try {
            List<Double> values = new ArrayList<>(vector.length);
            for (float value : vector) {
                values.add((double) value);
            }
            String json = objectMapper.writeValueAsString(values);
            return encryptText(json);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt embedding", ex);
        }
    }

    public float[] decryptEmbedding(String encrypted) {
        try {
            String json = decryptText(encrypted);
            List<Double> values = objectMapper.readValue(json, new TypeReference<List<Double>>() {});
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i).floatValue();
            }
            return vector;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt embedding", ex);
        }
    }

    public String encryptText(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return PREFIX + ":" +
                Base64.getEncoder().encodeToString(iv) + ":" +
                Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt payload", ex);
        }
    }

    public String decryptText(String payload) {
        try {
            String[] parts = payload.split(":", 3);
            if (parts.length != 3 || !PREFIX.equals(parts[0])) {
                throw new IllegalArgumentException("Unsupported encrypted payload format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] cipherText = Base64.getDecoder().decode(parts[2]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] decrypted = cipher.doFinal(cipherText);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt payload", ex);
        }
    }

    private SecretKey buildKey(String configuredKey) {
        try {
            if (configuredKey != null && !configuredKey.isBlank()) {
                byte[] keyBytes = Base64.getDecoder().decode(configuredKey);
                return new SecretKeySpec(keyBytes, "AES");
            }

            // Deterministic fallback for local environments.
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest("studentmanagement-face-embedding-default-key".getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hash, "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to initialize embedding encryption key", ex);
        }
    }
}
