package com.backend_microservices.ai_service.dto;

import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class UserDto {
    private UUID id;
    private String username;
    private String email;
    private Set<RoleDto> roles;
    private boolean isEnabled;
}
