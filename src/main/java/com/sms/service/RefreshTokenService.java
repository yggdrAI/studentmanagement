package com.sms.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sms.model.RefreshToken;
import com.sms.model.User;
import com.sms.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public IssuedRefreshToken issueRefreshToken(User user, String ipAddress, String userAgent) {
        String rawToken = generateOpaqueToken();
        String tokenHash = sha256(rawToken);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(LocalDateTime.now().plusDays(refreshExpirationDays));
        token.setIpAddress(ipAddress);
        token.setUserAgent(userAgent);
        refreshTokenRepository.save(token);

        return new IssuedRefreshToken(rawToken, token.getExpiresAt());
    }

    @Transactional
    public Optional<User> validateRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        return refreshTokenRepository
            .findByTokenHashAndRevokedFalseAndExpiresAtAfter(sha256(rawToken), LocalDateTime.now())
            .map(refreshToken -> {
                refreshToken.setLastUsedAt(LocalDateTime.now());
                return refreshToken.getUser();
            });
    }

    @Transactional
    public Optional<RotationResult> rotateRefreshToken(String rawToken, String ipAddress, String userAgent) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.of(RotationResult.invalid());
        }

        String tokenHash = sha256(rawToken);

        Optional<RefreshToken> activeToken = refreshTokenRepository
            .findByTokenHashAndRevokedFalseAndExpiresAtAfter(tokenHash, LocalDateTime.now());

        if (activeToken.isPresent()) {
            return activeToken.map(existing -> {
                existing.setRevoked(true);
                existing.setLastUsedAt(LocalDateTime.now());
                IssuedRefreshToken next = issueRefreshToken(existing.getUser(), ipAddress, userAgent);
                return RotationResult.rotated(existing.getUser(), next.rawToken(), next.expiresAt());
            });
        }

        Optional<RefreshToken> reused = refreshTokenRepository.findByTokenHash(tokenHash)
            .filter(RefreshToken::isRevoked);
        if (reused.isPresent()) {
            User user = reused.get().getUser();
            revokeAllForUser(user.getId());
            return Optional.of(RotationResult.reused(user));
        }

        return Optional.of(RotationResult.invalid());
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        if (userId != null) {
            refreshTokenRepository.revokeAllByUserId(userId);
        }
    }

    @Transactional(readOnly = true)
    public List<RefreshToken> listSessionsForUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return refreshTokenRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public boolean revokeSession(Long userId, Long sessionId) {
        if (userId == null || sessionId == null) {
            return false;
        }
        return refreshTokenRepository.revokeByIdAndUserId(sessionId, userId) > 0;
    }

    @Transactional
    public int revokeAllOtherSessions(Long userId, String currentRawToken) {
        if (userId == null || currentRawToken == null || currentRawToken.isBlank()) {
            return 0;
        }
        return refreshTokenRepository.revokeAllExceptTokenHash(userId, sha256(currentRawToken));
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now().minusDays(1));
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public record IssuedRefreshToken(String rawToken, LocalDateTime expiresAt) {}

    public record RotationResult(User user,
                                 String rawToken,
                                 LocalDateTime expiresAt,
                                 RotationStatus status,
                                 String message) {

        public static RotationResult rotated(User user, String rawToken, LocalDateTime expiresAt) {
            return new RotationResult(user, rawToken, expiresAt, RotationStatus.ROTATED, "Refresh token rotated");
        }

        public static RotationResult reused(User user) {
            return new RotationResult(user, null, null, RotationStatus.REUSE_DETECTED, "Refresh token reuse detected");
        }

        public static RotationResult invalid() {
            return new RotationResult(null, null, null, RotationStatus.INVALID, "Invalid refresh token");
        }
    }

    public enum RotationStatus {
        ROTATED,
        REUSE_DETECTED,
        INVALID
    }
}
