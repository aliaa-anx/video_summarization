package com.backend_microservices.admin_service.client;

import com.backend_microservices.admin_service.config.FeignConfig;
import com.backend_microservices.admin_service.dto.RecentJobDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(
        name = "ai-service",
        configuration = FeignConfig.class
)
public interface AiClient {

    @GetMapping("/api/internal/ai/analytics/documents/count")
    long getTotalDocumentCount();

    @GetMapping("/api/internal/ai/analytics/success-rate")
    double getSuccessRate();

    @GetMapping("/api/internal/ai/analytics/avg-time")
    int getAverageProcessingTime();


    @GetMapping("/api/internal/ai/analytics/top-users-ids")
    Map<UUID, Long> getTopUserIds();

    @GetMapping("/api/internal/ai/analytics/top-users-breakdown")
    Map<UUID, Map<String, Long>> getTopUserActivityBreakdown();

    @GetMapping("/api/internal/ai/analytics/recent-jobs")
    List<RecentJobDto> getRecentJobs();

}
