package com.backend_microservices.admin_service.service;

import com.backend_microservices.admin_service.client.AiClient;
import com.backend_microservices.admin_service.client.AuthClient;
import com.backend_microservices.admin_service.client.NotificationClient;
import com.backend_microservices.admin_service.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final AuthClient authClient;
    private final NotificationClient notificationClient;

    private final AiClient aiClient;

    // In admin-service -> service -> AdminService.java

    public List<UserRankingDto> getTopUserRankings() {
        // 1. Get the raw activity breakdown from AI Service
        // AI Service returns: Map<UUID, Map<String, Long>>
        Map<UUID, Map<String, Long>> activityMap = aiClient.getTopUserActivityBreakdown();

        if (activityMap == null || activityMap.isEmpty()) {
            return List.of();
        }

        // 2. Map the data and attach usernames from Auth Service
        return activityMap.entrySet().stream()
                .map(entry -> {
                    UUID userId = entry.getKey();
                    Map<String, Long> stats = entry.getValue();

                    // Get the individual counts we sent from the AI service
                    long uploads = stats.getOrDefault("uploads", 0L);
                    long chats = stats.getOrDefault("chats", 0L);

                    try {
                        // Match the ID with a real name
                        AdminUserDto user = authClient.getUserById(userId);
                        return new UserRankingDto(user.getUsername(), uploads, chats);
                    } catch (Exception e) {
                        return new UserRankingDto("Unknown User", uploads, chats);
                    }
                })
                // 3. Sort by TOTAL activity (Uploads + Chats) so the most active is first
                .sorted((u1, u2) -> Long.compare(
                        (u2.getUploadCount() + u2.getChatCount()),
                        (u1.getUploadCount() + u1.getChatCount())
                ))
                .limit(5)
                .collect(Collectors.toList());
    }


    /**
     * Requirement: Start with Users and Summaries made today.
     * Fuels the 4 metric cards and initial charts in the dashboard.
     */
    public AdminDashboardStatsDto getDashboardSnapshot() {
        long totalUsers = authClient.getTotalUserCount();
        long totalDocs = aiClient.getTotalDocumentCount();

        double successRate = aiClient.getSuccessRate();
        int avgTime = aiClient.getAverageProcessingTime();

        return AdminDashboardStatsDto.builder()
                .totalUsers(totalUsers)
                .totalDocuments(totalDocs)
                .successRate(successRate)
                .topUsers(getTopUserRankings())
                .recentJobs(aiClient.getRecentJobs())
                .avgProcessTime(avgTime)
                .build();
    }



    public List<AdminUserDto> getAllUsers() {
        return authClient.getAllUsers();
    }

    public void DeleteUser(UUID id) {
        authClient.deleteUser(id);
    }

    public void toggleUserBan(UUID userId) {
        // 1. Update status in Auth Service
        authClient.toggleBan(userId);

        // 2. Fetch fresh user data to see new status and get email info
        AdminUserDto user = authClient.getUserById(userId);

        try {
            // If user has no 'enabled' field in DTO yet, you'll need to add it to AdminUserDto
            boolean isBanned = !user.isEnabled();
            notificationClient.sendBanEmail(user.getEmail(), user.getUsername(), isBanned);
        } catch (Exception e) {
            log.error("Notification failed for user {}: {}", userId, e.getMessage());
        }
    }

    public void promoteToAdmin(UUID id) {

        authClient.promoteToAdmin(id);
    }

    public List<AdminUserDto> searchUsers(String keyword) {
        return authClient.searchUsers(keyword);
    }
}