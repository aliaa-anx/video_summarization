package com.backend_microservices.ai_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(name="meeting_id")
    private UUID meetingId;

    private String title;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

}
