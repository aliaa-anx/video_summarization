package com.backend_microservices.ai_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "meeting_transcripts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingTranscript {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID userId;

    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(columnDefinition = "TEXT")
    private String correctedTranscript;

    private String source;

    @Column(columnDefinition = "TEXT")
    private String segmentsJson;

    private LocalDateTime createdAt;

    private String videoPath;
}