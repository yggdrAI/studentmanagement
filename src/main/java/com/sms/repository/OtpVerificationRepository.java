package com.sms.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sms.model.OtpVerification;
import com.sms.model.User;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(User user);

    Optional<OtpVerification> findTopByUserAndOtpCodeAndIsUsedFalseOrderByCreatedAtDesc(User user, String otpCode);

    Optional<OtpVerification> findTopByUserAndResetTokenAndIsUsedFalseOrderByCreatedAtDesc(User user, String resetToken);

    Optional<OtpVerification> findTopByUserAndResetTokenOrderByCreatedAtDesc(User user, String resetToken);

    long countByUserAndCreatedAtAfter(User user, LocalDateTime createdAt);
}
