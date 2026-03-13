package com.backend_microservices.ai_service.repository;

import com.backend_microservices.ai_service.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
}
