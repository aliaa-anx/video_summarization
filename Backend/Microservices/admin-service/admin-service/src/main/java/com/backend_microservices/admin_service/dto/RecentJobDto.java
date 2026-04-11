package com.backend_microservices.admin_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecentJobDto {
    private String fileName;
    private String status; // "Done", "Processing", "Failed", "Queued"
    private LocalDateTime timestamp;
}