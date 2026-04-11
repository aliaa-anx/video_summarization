package com.backend_microservices.admin_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminDashboardStatsDto {
    private long totalUsers;
    // Total registered users
    private long totalDocuments;
    //private long summariesToday;    // Count of summaries created today
    private double successRate;     // Based on total vs successful jobs
    private int avgProcessTime;
    private List<UserRankingDto> topUsers;
    private List<RecentJobDto> recentJobs;

    }

