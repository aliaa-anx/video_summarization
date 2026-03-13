package com.backend_microservices.ai_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data                  // Generates Getters, Setters, toString, equals, hashCode
@AllArgsConstructor    // Generates constructor with all fields
@NoArgsConstructor
public class ChatRequest {

    //private String message;
    private String message;
    private List<MessageDTO> history; // The last few messages
    private List<String> context;

}
