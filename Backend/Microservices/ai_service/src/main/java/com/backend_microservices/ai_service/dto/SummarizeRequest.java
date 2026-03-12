package com.backend_microservices.ai_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class SummarizeRequest {

    private String meetingId;
    private String transcript;

    public SummarizeRequest(String transcript) {
        this.meetingId = meetingId;
        this.transcript = transcript;
    }
    public SummarizeRequest(String meetingId, String transcript) {
        this.meetingId = meetingId;
        this.transcript = transcript;
    }
}