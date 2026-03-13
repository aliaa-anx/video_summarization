package com.backend_microservices.ai_service.entity;
import jakarta.persistence.*; // Uses the new Jakarta standard
import lombok.*;

import java.util.UUID;
import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "conversations", schema = "chat")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // Or GenerationType.UUID
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    private String title;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

}
