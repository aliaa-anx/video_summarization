package com.backend_microservices.auth_service.repository;

import com.backend_microservices.auth_service.entity.RefreshToken;
import com.backend_microservices.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}