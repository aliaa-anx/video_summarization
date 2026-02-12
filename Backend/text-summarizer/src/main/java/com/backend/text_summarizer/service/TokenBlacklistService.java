package com.backend.text_summarizer.service;

import com.backend.text_summarizer.entity.BlacklistedToken;
import com.backend.text_summarizer.repository.BlacklistedTokenRepository;
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
