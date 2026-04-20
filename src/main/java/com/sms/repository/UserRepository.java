package com.sms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sms.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByPhone(String phone);

    @Query(value = "SELECT * FROM app_user u WHERE REGEXP_REPLACE(COALESCE(u.phone, ''), '[^0-9]', '') = :normalizedPhone LIMIT 1", nativeQuery = true)
    Optional<User> findByNormalizedPhone(@Param("normalizedPhone") String normalizedPhone);

    @Query("select u from User u where lower(u.username) = lower(:identifier) or lower(u.email) = lower(:identifier)")
    Optional<User> findByUsernameOrEmailIgnoreCase(@Param("identifier") String identifier);

    void deleteByUsername(String username);
}
