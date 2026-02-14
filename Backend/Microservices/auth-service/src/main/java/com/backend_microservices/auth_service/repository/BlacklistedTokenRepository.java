package com.backend_microservices.auth_service.repository;

import com.backend_microservices.auth_service.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BlacklistedTokenRepository
        extends JpaRepository<BlacklistedToken, UUID> {

    boolean existsByToken(String token);
}
