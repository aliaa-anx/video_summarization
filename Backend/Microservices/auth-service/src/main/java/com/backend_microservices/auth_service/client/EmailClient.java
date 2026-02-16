package com.backend_microservices.auth_service.client;

import com.backend_microservices.auth_service.dto.EmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "notification-service")
public interface EmailClient {

    @PostMapping("/api/email/send")
    void sendEmail(@RequestBody EmailRequest request);
}