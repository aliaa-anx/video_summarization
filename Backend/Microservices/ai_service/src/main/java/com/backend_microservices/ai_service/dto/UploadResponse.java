package com.backend_microservices.ai_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
@Getter
@Setter
public class UploadResponse {

    private String transcript;

    private String conversationId;
}
