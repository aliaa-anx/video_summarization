package com.backend_microservices.auth_service.repository;

import com.backend_microservices.auth_service.entity.PasswordResetTokens;
import com.backend_microservices.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokens,Long> {

    Optional<PasswordResetTokens> findByToken(String token);

    void deleteByUser(User user);

    @Modifying
    @Query("DELETE FROM PasswordResetTokens p WHERE p.user.id = :userId")
    void deleteByUserId(UUID userId);
    @Modifying
    void deleteByToken(String token);
}