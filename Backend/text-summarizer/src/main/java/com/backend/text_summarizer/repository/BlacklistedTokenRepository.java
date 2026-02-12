package com.backend.text_summarizer.repository;

import com.backend.text_summarizer.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BlacklistedTokenRepository
        extends JpaRepository<BlacklistedToken, UUID> {

    boolean existsByToken(String token);
}
