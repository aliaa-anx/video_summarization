package com.backend_microservices.ai_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiResponse {
    //private String reply;
    private String reply;
    private List<EmbeddingPayload> payload; // This list name must match line 40

    @Data
    public static class EmbeddingPayload { // Must be 'public static'
        @JsonProperty("content")
        private String content;

        @JsonProperty("embedding")
        private float[] embedding;

        @JsonProperty("chunkIndex")
        private Integer chunkIndex;
    }
}
