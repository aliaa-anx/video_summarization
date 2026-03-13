package com.backend_microservices.ai_service.repository;
import com.backend_microservices.ai_service.dto.MessageDTO;
import com.backend_microservices.ai_service.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public interface MessageRepository extends JpaRepository<Message,UUID>{
    // This allows you to load the chat history in order for the UI
    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
    // This allows ChatService to fetch the history in the correct DTO format
    @Query("SELECT new com.backend_microservices.ai_service.dto.MessageDTO(m.role, m.content) " +
            "FROM Message m WHERE m.conversation.id = :chatId ORDER BY m.createdAt ASC")
    List<MessageDTO> findRecentByChatId(UUID chatId);
}
