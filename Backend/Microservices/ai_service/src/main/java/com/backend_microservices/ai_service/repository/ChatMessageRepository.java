package com.backend_microservices.ai_service.repository;

import com.backend_microservices.ai_service.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, UUID> {
}
