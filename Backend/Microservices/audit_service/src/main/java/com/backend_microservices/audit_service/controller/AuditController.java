package com.backend_microservices.audit_service.controller;

import com.backend_microservices.audit_service.model.AuditLog;
import com.backend_microservices.audit_service.service.AuditService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping
    public String receiveLog(@RequestBody AuditLog log) {
        auditService.saveLog(log);
        return "Log saved";
    }
}