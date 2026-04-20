package com.sms.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sms.dto.auth.ForgotPasswordResponse;
import com.sms.model.User;
import com.sms.repository.UserRepository;

@Service
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String OTP_KEY_PREFIX = "OTP:USER:";
    private static final String OTP_REQUEST_PREFIX = "OTP_REQ:USER:";
    private static final String OTP_ATTEMPT_PREFIX = "OTP_ATTEMPT:USER:";
    private static final String OTP_CONTEXT_PREFIX = "OTP_CTX:USER:";
    private static final String OTP_COOLDOWN_PREFIX = "OTP_COOLDOWN:USER:";
    private static final String OTP_RESET_PREFIX = "OTP_RESET:USER:";

    private final IdentityLookupService identityLookupService;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final OtpDeliveryService otpDeliveryService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.otp.expiry-minutes:5}")
    private long otpExpiryMinutes;

    @Value("${app.auth.otp.max-verify-attempts:5}")
    private int maxVerifyAttempts;

    @Value("${app.auth.otp.max-requests-per-window:3}")
    private int maxRequestsPerWindow;

    @Value("${app.auth.otp.request-window-minutes:5}")
    private long requestWindowMinutes;

    @Value("${app.auth.otp.resend-min-seconds:30}")
    private long resendMinSeconds;

    @Value("${app.auth.otp.reset-token-expiry-minutes:10}")
    private long resetTokenExpiryMinutes;

    @Value("${app.auth.otp.dev-return-otp-when-undelivered:false}")
    private boolean devReturnOtpWhenUndelivered;

    @Value("${app.auth.otp.bind-client-context:false}")
    private boolean bindClientContext;

    public PasswordResetService(IdentityLookupService identityLookupService,
                                StringRedisTemplate redisTemplate,
                                UserRepository userRepository,
                                OtpDeliveryService otpDeliveryService,
                                PasswordEncoder passwordEncoder) {
        this.identityLookupService = identityLookupService;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.otpDeliveryService = otpDeliveryService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ForgotPasswordResponse initiateForgotPassword(String identifier) {
        return initiateForgotPassword(identifier, null);
    }

    @Transactional
    public ForgotPasswordResponse initiateForgotPassword(String identifier, String clientContext) {
        String normalizedIdentifier = IdentityLookupService.normalizeIdentifier(identifier);
        User user = identityLookupService.findByEmailOrPhone(normalizedIdentifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for provided email/phone"));

        String userIdKey = String.valueOf(user.getId());
        if (!canRequestOtp(userIdKey)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many OTP requests. Please try again later.");
        }

        String cooldownKey = otpCooldownKey(userIdKey);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting another OTP.");
        }

        String otp = generateOtp();
        saveOtp(userIdKey, otp, clientContext);
        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(resendMinSeconds));

        boolean delivered;
        String channel;

        if (IdentityLookupService.isEmail(normalizedIdentifier)) {
            String emailTarget = user.getEmail();
            if (emailTarget == null || emailTarget.isBlank()) {
                emailTarget = normalizedIdentifier;
            }
            delivered = otpDeliveryService.sendEmailOtp(emailTarget, otp);
            channel = "email";
        } else {
            delivered = otpDeliveryService.sendSmsOtp(user.getPhone(), otp);
            channel = "sms";
        }

        if (delivered) {
            return new ForgotPasswordResponse(true, channel, null);
        }

        String otpPreview = devReturnOtpWhenUndelivered ? otp : null;
        return new ForgotPasswordResponse(false, channel, otpPreview);
    }

    @Transactional
    public String verifyOtp(String identifier, String otpCode) {
        return verifyOtp(identifier, otpCode, null);
    }

    @Transactional
    public String verifyOtp(String identifier, String otpCode, String clientContext) {
        String normalizedIdentifier = IdentityLookupService.normalizeIdentifier(identifier);
        User user = identityLookupService.findByEmailOrPhone(normalizedIdentifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for provided email/phone"));

        String userIdKey = String.valueOf(user.getId());
        String storedOtpHash = redisTemplate.opsForValue().get(otpKey(userIdKey));
        if (storedOtpHash == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP has expired or is invalid");
        }

        if (!matchesClientContext(userIdKey, clientContext)) {
            clearOtpData(userIdKey);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP context mismatch");
        }

        String incomingHash = hashOtp(otpCode);
        if (!MessageDigest.isEqual(storedOtpHash.getBytes(StandardCharsets.UTF_8), incomingHash.getBytes(StandardCharsets.UTF_8))) {
            int attempts = incrementVerifyAttempts(userIdKey);
            if (attempts >= maxVerifyAttempts) {
                clearOtpData(userIdKey);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP retry limit exceeded");
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP");
        }

        String resetToken = UUID.randomUUID().toString();
        clearOtpData(userIdKey);
        redisTemplate.opsForValue().set(otpResetKey(userIdKey), resetToken, Duration.ofMinutes(resetTokenExpiryMinutes));
        return resetToken;
    }

    @Transactional
    public void resetPassword(String identifier, String resetToken, String newPassword, String confirmPassword) {
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password and confirm password do not match");
        }

        String normalizedIdentifier = IdentityLookupService.normalizeIdentifier(identifier);
        User user = identityLookupService.findByEmailOrPhone(normalizedIdentifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for provided email/phone"));

        String userIdKey = String.valueOf(user.getId());
        String storedResetToken = redisTemplate.opsForValue().get(otpResetKey(userIdKey));
        if (storedResetToken == null || !storedResetToken.equals(resetToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired reset token");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setIsFirstLogin(false);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        redisTemplate.delete(otpResetKey(userIdKey));
    }

    private String generateOtp() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    private boolean canRequestOtp(String userId) {
        String key = otpRequestKey(userId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return false;
        }
        if (count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(requestWindowMinutes));
        }
        return count <= maxRequestsPerWindow;
    }

    private void saveOtp(String userId, String otp, String clientContext) {
        redisTemplate.opsForValue().set(otpKey(userId), hashOtp(otp), Duration.ofMinutes(otpExpiryMinutes));
        redisTemplate.delete(otpAttemptKey(userId));

        if (bindClientContext) {
            String context = normalizeContext(clientContext);
            if (context != null) {
                redisTemplate.opsForValue().set(otpContextKey(userId), context, Duration.ofMinutes(otpExpiryMinutes));
            } else {
                redisTemplate.delete(otpContextKey(userId));
            }
        } else {
            redisTemplate.delete(otpContextKey(userId));
        }
    }

    private int incrementVerifyAttempts(String userId) {
        String attemptsKey = otpAttemptKey(userId);
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts == null) {
            return maxVerifyAttempts;
        }

        Long otpTtl = redisTemplate.getExpire(otpKey(userId));
        if (attempts == 1L && otpTtl != null && otpTtl > 0) {
            redisTemplate.expire(attemptsKey, Duration.ofSeconds(otpTtl));
        }

        if (attempts == 1L && (otpTtl == null || otpTtl <= 0)) {
            redisTemplate.expire(attemptsKey, Duration.ofMinutes(otpExpiryMinutes));
        }

        return attempts.intValue();
    }

    private boolean matchesClientContext(String userId, String providedContext) {
        if (!bindClientContext) {
            return true;
        }

        String storedContext = redisTemplate.opsForValue().get(otpContextKey(userId));
        if (storedContext == null) {
            return true;
        }

        String normalizedProvided = normalizeContext(providedContext);
        return storedContext.equals(normalizedProvided);
    }

    private void clearOtpData(String userId) {
        redisTemplate.delete(otpKey(userId));
        redisTemplate.delete(otpAttemptKey(userId));
        redisTemplate.delete(otpContextKey(userId));
    }

    private String hashOtp(String otp) {
        if (otp == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String normalizeContext(String clientContext) {
        if (clientContext == null || clientContext.isBlank()) {
            return null;
        }
        return clientContext.trim();
    }

    private String otpKey(String userId) {
        return OTP_KEY_PREFIX + userId;
    }

    private String otpRequestKey(String userId) {
        return OTP_REQUEST_PREFIX + userId;
    }

    private String otpAttemptKey(String userId) {
        return OTP_ATTEMPT_PREFIX + userId;
    }

    private String otpContextKey(String userId) {
        return OTP_CONTEXT_PREFIX + userId;
    }

    private String otpCooldownKey(String userId) {
        return OTP_COOLDOWN_PREFIX + userId;
    }

    private String otpResetKey(String userId) {
        return OTP_RESET_PREFIX + userId;
    }
}
