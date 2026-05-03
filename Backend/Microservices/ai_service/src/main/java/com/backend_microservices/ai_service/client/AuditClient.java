package com.backend_microservices.ai_service.client;

import com.backend_microservices.ai_service.dto.AuditLog;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

// this class sends HTTP request to our audit-service to save the logs
@Service
public class AuditClient {

    private final RestTemplate restTemplate = new RestTemplate();

    private final String AUDIT_URL = "http://localhost:8085/audit";

    public void sendLog(AuditLog log) {
        try {
            restTemplate.postForObject(AUDIT_URL, log, String.class); // it builds the request to be like: POST http://localhost:8085/audit
        } catch (Exception e) {     // if the audit-service is down our application continuous normally without logging :)
            System.out.println("Audit service is down, you forgot to open it TwT");
        }
    }
}
