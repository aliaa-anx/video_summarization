package com.backend_microservices.admin_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRankingDto {
    //private String name;      // From Auth Service
    //private long activityCount; // From AI Service (Meetings + Chats)
    private String name;
    private long uploadCount; // Count from meeting_transcripts
    private long chatCount;   // Count from conversations

    // Optional: Keep a total for easy sorting
    public long getTotal() {
        return uploadCount + chatCount;
    }
}