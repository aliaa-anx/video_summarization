package com.backend_microservices.ai_service.dto;

import lombok.*;
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SummarizeResponse {

    private String summary;
    private String language;

}