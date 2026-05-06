package com.backend_microservices.ai_service.dto;

import jakarta.persistence.Column;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingDto {
    private String status;
    private String transcript;

    private String corrected_text;

    private String source;

    @Column(columnDefinition = "TEXT")
    private String segmentsJson;
}
