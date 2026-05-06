package com.backend_microservices.ai_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeyPointDto {

    private int index;
    private String text;

    private Double start;
    private Double end;

    private String timestamp;
}