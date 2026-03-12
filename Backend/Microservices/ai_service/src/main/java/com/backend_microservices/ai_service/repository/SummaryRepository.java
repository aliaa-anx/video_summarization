package com.backend_microservices.ai_service.repository;

import com.backend_microservices.ai_service.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SummaryRepository
        extends JpaRepository<Summary, UUID> {
}