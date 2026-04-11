package com.backend_microservices.ai_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecentJobDto {
    private String fileName;
    private String status;    // Done, Failed, Processing
    private String createdAt; // Formatted date string
}