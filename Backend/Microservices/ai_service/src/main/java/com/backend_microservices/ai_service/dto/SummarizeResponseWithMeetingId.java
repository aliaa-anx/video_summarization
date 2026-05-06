package com.backend_microservices.ai_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummarizeResponseWithMeetingId {

    private UUID meeting_id;
    private String language;
    private String json_type;
    private int num_sentences;
    private int num_keypoints;
    private List<KeyPointDto> keypoints;
}
