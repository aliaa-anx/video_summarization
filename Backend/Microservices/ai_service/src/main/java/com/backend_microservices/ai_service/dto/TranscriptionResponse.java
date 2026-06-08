package com.backend_microservices.ai_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranscriptionResponse {

    private String transcript;

    private String corrected_text;   // EXACT JSON name

    private String source;

    private List<SegmentDto> segments;

    public String getCorrectedText() {
        return corrected_text;
    }

}