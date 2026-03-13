package com.backend_microservices.ai_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Data
@AllArgsConstructor
@Getter
@Setter
public class UploadResponse {

    private SummarizeResponse summarizeResponse;
    private UUID meetingId;
}
