package com.backend_microservices.test_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test/private")
    public String privateEndpoint() {
        return "🔐 Private endpoint accessed with JWT!";
    }
}
