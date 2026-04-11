package com.backend_microservices.ai_service.controller;

import com.backend_microservices.ai_service.dto.RecentJobDto;
import com.backend_microservices.ai_service.repository.DocumentRepository;
import com.backend_microservices.ai_service.repository.MeetingTranscriptRepository;
import com.backend_microservices.ai_service.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/api/internal/ai/analytics")
@RequiredArgsConstructor
public class InternalAiController {
    private final AnalyticsService analyticsService;
    private final DocumentRepository documentRepo;
    private final MeetingTranscriptRepository meetingRepo;
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/success-rate")
    public double getSuccessRate() {
        return analyticsService.calculateSuccessRate();
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/documents/count")
    public long getDocCount() {
        return meetingRepo.count();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/avg-time")
    public int getAverageProcessingTime() {
        return analyticsService.getAverageProcessingTime();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/top-users-breakdown") // Renamed to match the new "Breakdown" logic
    public ResponseEntity<Map<UUID, Map<String, Long>>> getTopUserActivity() {
        // Calling the new method that differentiates between Uploads and Chats
        return ResponseEntity.ok(analyticsService.getTopUserActivityBreakdown());
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/recent-jobs")
    public ResponseEntity<List<RecentJobDto>> getRecentJobs() {
        return ResponseEntity.ok(analyticsService.getRecentJobs());
    }

}
