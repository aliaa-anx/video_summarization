package com.backend_microservices.ai_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private MeetingTranscript meeting;

    private LocalDateTime createdAt;
}