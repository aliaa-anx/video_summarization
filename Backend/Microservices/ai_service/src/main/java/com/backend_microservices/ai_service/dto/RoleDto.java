package com.backend_microservices.ai_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class RoleDto {
    private Long id;
    private String roleName;
}

