package com.backend_microservices.ai_service.service;

import com.backend_microservices.ai_service.dto.RecentJobDto;
import com.backend_microservices.ai_service.entity.MeetingTranscript;
import com.backend_microservices.ai_service.entity.Summary;
import com.backend_microservices.ai_service.repository.ConversationRepository;
import com.backend_microservices.ai_service.repository.DocumentRepository;
import com.backend_microservices.ai_service.repository.MeetingTranscriptRepository;
import com.backend_microservices.ai_service.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final SummaryRepository summaryRepo;
    private final DocumentRepository documentRepo;
    private final ConversationRepository conversationRepo;
    private final MeetingTranscriptRepository transcriptRepo;

    public double calculateSuccessRate() {
        long totalDocs = documentRepo.count();
        long totalSummaries = summaryRepo.count();

        if (totalDocs == 0) return 0.0;

        // Formula: (Successful Summaries / Total Documents) * 100
        return (double) totalSummaries / totalDocs * 100;
    }
    public int getAverageProcessingTime() {
        // This calls the native SQL to calculate the real time delta
        return summaryRepo.getAverageProcessingTimeInSeconds();
    }
   /**
    public Map<UUID, Long> getTopUserIds() {
        // 1. Get counts from meetings (Top 10 most active)
        List<Object[]> meetingCounts = transcriptRepo.getTopUsersByMeetings(PageRequest.of(0, 10));

        // 2. Get counts from chatbot instances
        List<Object[]> chatCounts = conversationRepo.countConversationsByUser();

        Map<UUID, Long> totalActivity = new HashMap<>();

        // Combine Meetings into the map
        if (meetingCounts != null) {
            for (Object[] row : meetingCounts) {
                UUID userId = (UUID) row[0];
                Long count = (Long) row[1];
                totalActivity.put(userId, count);
            }
        }
        // Add Chat Instances to the existing counts
        if (chatCounts != null) {
            for (Object[] row : chatCounts) {
                UUID userId = (UUID) row[0];
                Long count = (Long) row[1];
                totalActivity.put(userId, totalActivity.getOrDefault(userId, 0L) + count);
            }
        }

        return totalActivity;
    }**/
   public Map<UUID, Map<String, Long>> getTopUserActivityBreakdown() {
       // 1. Get raw counts from repositories
       List<Object[]> meetingCounts = transcriptRepo.getTopUsersByMeetings(PageRequest.of(0, 10));
       List<Object[]> chatCounts = conversationRepo.countConversationsByUser();

       // Map structure: userId -> { "uploads": X, "chats": Y }
       Map<UUID, Map<String, Long>> detailedActivity = new HashMap<>();

       // 2. Process Meetings (Uploads)
       if (meetingCounts != null) {
           for (Object[] row : meetingCounts) {
               UUID userId = (UUID) row[0];
               Long count = (Long) row[1];

               Map<String, Long> userStats = new HashMap<>();
               userStats.put("uploads", count);
               userStats.put("chats", 0L); // Initialize chats at 0
               detailedActivity.put(userId, userStats);
           }
       }

       // 3. Process Chatbot Instances
       if (chatCounts != null) {
           for (Object[] row : chatCounts) {
               UUID userId = (UUID) row[0];
               Long count = (Long) row[1];

               // Get existing map for user or create new one if they haven't uploaded anything
               Map<String, Long> userStats = detailedActivity.getOrDefault(userId, new HashMap<>());
               if (!userStats.containsKey("uploads")) {
                   userStats.put("uploads", 0L);
               }

               userStats.put("chats", count);
               detailedActivity.put(userId, userStats);
           }
       }

       return detailedActivity;
   }

    public List<RecentJobDto> getRecentJobs() {
        // Get the top 5 most recent summaries
        List<Summary> latestSummaries = summaryRepo.findTop5RecentJobs(PageRequest.of(0, 5));

        return latestSummaries.stream().map(s -> RecentJobDto.builder()
                        .fileName(s.getMeeting().getFileName())
                        .status("Done") // Since it's in the Summary table, it's completed
                        .createdAt(s.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());
    }
}
