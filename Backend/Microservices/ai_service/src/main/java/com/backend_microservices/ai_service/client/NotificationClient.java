package com.backend_microservices.ai_service.client;

import com.backend_microservices.ai_service.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/email/summary-email")
    void sendSummaryEmail(@RequestParam String email,
                      @RequestParam String username,
                      @RequestParam String message
    );
}
