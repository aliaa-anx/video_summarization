package com.backend_microservices.admin_service.dto;

import lombok.*;

@Data
@AllArgsConstructor

public class RoleDto {
    private Long id;
    private String roleName;
}
