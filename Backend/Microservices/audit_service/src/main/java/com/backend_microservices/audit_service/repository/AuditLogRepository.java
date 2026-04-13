package com.backend_microservices.audit_service.repository;

import com.backend_microservices.audit_service.model.AuditLog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface AuditLogRepository
        extends ElasticsearchRepository<AuditLog, String> {
}