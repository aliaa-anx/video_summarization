package com.backend_microservices.admin_service.dto;
import lombok.Data;

import javax.management.relation.Role;
import java.util.Set;
import java.util.UUID;

@Data
public class AdminUserDto {
    private UUID id;
    private String username;
    private String email;
    private Set<RoleDto> roles;
    private boolean isEnabled;
}
