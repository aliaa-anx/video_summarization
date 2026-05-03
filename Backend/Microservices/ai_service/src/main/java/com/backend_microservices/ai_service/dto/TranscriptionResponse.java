package com.backend_microservices.ai_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.swing.text.Segment;
import java.util.List;

@Data
public class TranscriptionResponse {

    private String transcript;

    @JsonProperty("corrected_text")
    private String correctedText;
    private String source;

    private List<Segment> segments;

    @Data
    public static class Segment {
        private Double start;
        private Double end;
        private String text;
    }
}