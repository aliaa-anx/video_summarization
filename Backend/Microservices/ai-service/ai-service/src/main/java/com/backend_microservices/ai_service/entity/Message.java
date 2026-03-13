package com.backend_microservices.ai_service.entity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "messages", schema = "chat")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // This links the message to the conversation table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "role")
    private String role; // 'user' or 'assistant'

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
