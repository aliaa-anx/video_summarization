package com.backend_microservices.ai_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used to initialize a chat session with video content.
 * This carries the full transcript to the Python AI service.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitRequest {
    private String transcript;
}
