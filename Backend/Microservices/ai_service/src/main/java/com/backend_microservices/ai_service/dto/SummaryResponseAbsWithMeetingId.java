package com.backend_microservices.ai_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryResponseAbsWithMeetingId {

    UUID meetingId;
    private String summary;
    private String language;

}
