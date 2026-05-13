package com.backend_microservices.ai_service.dto;



import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContextChunk {
    private String content;

    @JsonProperty("start_time")
    private Double startTime;

    @JsonProperty("end_time")
    private Double endTime;
}