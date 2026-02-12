package com.backend.text_summarizer.repository;

import com.backend.text_summarizer.entity.RefreshToken;
import com.backend.text_summarizer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}