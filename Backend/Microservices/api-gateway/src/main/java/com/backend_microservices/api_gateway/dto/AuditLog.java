package com.backend_microservices.api_gateway.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class AuditLog {

    private String id;
    private String userId;
    private String action;
    private String serviceName;
    private String endpoint;
    private String method;
    private String status;
    private String details;
    private Instant timestamp;
}
