package com.backend_microservices.ai_service.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummarizeRequest {
    private String transcript;
    private String source;
    private List<SegmentDto> segments;
    private String correctedTranscript;
    private String status;

}