package com.backend_microservices.audit_service.service;

import com.backend_microservices.audit_service.model.AuditLog;
import com.backend_microservices.audit_service.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    // doesn't even need explanation...
    public void saveLog(AuditLog log) {
        log.setTimestamp(Instant.now());
        repository.save(log);
    }
}