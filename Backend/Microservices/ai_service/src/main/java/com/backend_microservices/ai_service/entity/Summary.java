package com.backend_microservices.ai_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Summary {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private MeetingTranscript meeting;

    @Column(columnDefinition = "TEXT")
    private String summaryJson;

    private String title;

    private String language;

    private LocalDateTime createdAt;
}