package com.backend_microservices.ai_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TranscriptionResponse {

    private String transcript;

    @JsonProperty("corrected_text")
    private String correctedText;

}