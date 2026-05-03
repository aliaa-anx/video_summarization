package com.backend_microservices.ai_service.dto;
import com.backend_microservices.ai_service.entity.ChatMessage;
import com.backend_microservices.ai_service.entity.Message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data                  // Generates Getters, Setters, toString, equals, hashCode
@AllArgsConstructor    // Generates constructor with all fields
@NoArgsConstructor
public class ChatHistoryDto {
    private UUID chatId;
    private List<Message> messages;
}
