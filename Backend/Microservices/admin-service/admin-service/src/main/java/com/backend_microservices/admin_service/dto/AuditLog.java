package com.backend_microservices.admin_service.dto;

import lombok.Data;

@Data
public class AuditLog {

    private String userId;
    private String action;
    private String serviceName;
    private String endpoint;
    private String method;
    private String status;
    private String details;
}