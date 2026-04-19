package com.sms.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sms.model.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalseAndExpiresAtAfter(String tokenHash, LocalDateTime now);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<RefreshToken> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.id = :tokenId AND rt.user.id = :userId")
    int revokeByIdAndUserId(@Param("tokenId") Long tokenId, @Param("userId") Long userId);

        @Modifying
        @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.tokenHash <> :keepTokenHash")
        int revokeAllExceptTokenHash(@Param("userId") Long userId, @Param("keepTokenHash") String keepTokenHash);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff OR rt.revoked = true")
    int deleteExpiredAndRevoked(@Param("cutoff") LocalDateTime cutoff);
}
