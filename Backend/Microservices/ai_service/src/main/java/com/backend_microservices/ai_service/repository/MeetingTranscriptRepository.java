package com.backend_microservices.ai_service.repository;

import com.backend_microservices.ai_service.entity.MeetingTranscript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MeetingTranscriptRepository
        extends JpaRepository<MeetingTranscript, UUID> {
}
