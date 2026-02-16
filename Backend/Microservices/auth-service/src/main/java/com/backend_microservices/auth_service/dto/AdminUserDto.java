package com.backend_microservices.auth_service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.management.relation.Role;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserDto {
    private UUID id;
    private String username;
    private String email;

    private boolean isEnabled;
}
