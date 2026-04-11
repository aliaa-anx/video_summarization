package com.backend_microservices.ai_service.repository;

import com.backend_microservices.ai_service.entity.Summary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SummaryRepository
        extends JpaRepository<Summary, UUID> {

    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (s.created_at - m.created_at))), 0) " +
            "FROM summaries s " +
            "JOIN meeting_transcripts m ON s.meeting_id = m.id",
            nativeQuery = true)
    Integer getAverageProcessingTimeInSeconds();

    @Query("SELECT s FROM Summary s JOIN s.meeting m ORDER BY s.createdAt DESC")
    List<Summary> findTop5RecentJobs(Pageable pageable);
}