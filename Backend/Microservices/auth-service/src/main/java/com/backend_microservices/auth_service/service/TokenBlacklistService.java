package com.backend_microservices.auth_service.service;

import com.backend_microservices.auth_service.entity.BlacklistedToken;
import com.backend_microservices.auth_service.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
//  This service handles JWT invalidation (logout)
public class TokenBlacklistService {

    private final BlacklistedTokenRepository repository;

    public void blacklistToken(String token) {

        repository.save(
                BlacklistedToken.builder()
                        .token(token)
                        .blacklistedAt(Instant.now())
                        .build()
        );
    }

    public boolean isBlacklisted(String token) {
        return repository.existsByToken(token);
    }
}
