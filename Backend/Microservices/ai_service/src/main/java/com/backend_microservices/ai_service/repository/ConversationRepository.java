package com.backend_microservices.ai_service.repository;

import com.backend_microservices.ai_service.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    // Find all chats for a specific user
    List<Conversation> findByUserId(UUID userId);
    @Query("SELECT c.userId, COUNT(c) FROM Conversation c GROUP BY c.userId")
    List<Object[]> countConversationsByUser();
}
