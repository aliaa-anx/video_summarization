package com.backend_microservices.ai_service.repository;

import com.backend_microservices.ai_service.dto.MessageDTO;
import com.backend_microservices.ai_service.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message,UUID> {
    // This allows you to load the chat history in order for the UI
    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    // This allows ChatService to fetch the history in the correct DTO format
    @Query(value = """
    SELECT role, content 
    FROM chat.messages 
    WHERE conversation_id = :chatId 
    AND content IS NOT NULL
    ORDER BY created_at DESC 
    LIMIT 10
    """, nativeQuery = true)
    List<Object[]> findRecentByChatId(@Param("chatId") UUID chatId);

    @Query("SELECT m FROM Message m WHERE m.conversation.userId = :userId ORDER BY m.createdAt ASC")
    List<Message> findAllByUserId(@Param("userId") UUID userId);
}
