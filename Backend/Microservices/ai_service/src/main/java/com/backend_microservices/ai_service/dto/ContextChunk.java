package com.backend_microservices.ai_service.dto;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContextChunk {
    private String content;
    private Double startTime;
    private Double endTime;
}