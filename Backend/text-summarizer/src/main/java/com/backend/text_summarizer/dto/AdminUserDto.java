package com.backend.text_summarizer.dto;

import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class AdminUserDto {
    private UUID id;
    private String username;
    private String email;
    private Set<String> roles;

}
