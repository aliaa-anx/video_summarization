package com.backend_microservices.admin_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/email/ban-email")
    void sendBanEmail(@RequestParam String email,
                      @RequestParam String username,
                      @RequestParam boolean isBanned);
}