package com.backend.text_summarizer.repository;

import com.backend.text_summarizer.entity.PasswordResetTokens;
import lombok.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import com.backend.text_summarizer.entity.User;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokens,Long> {

    Optional<PasswordResetTokens> findByToken(String token);

    void deleteByUser(User user);
}