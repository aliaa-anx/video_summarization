package com.backend_microservices.ai_service.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummarizeResponse {

    private String language;
    private String json_type;
    private int num_sentences;
    private int num_keypoints;
    private List<KeyPointDto> keypoints;
}