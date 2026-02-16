package com.backend_microservices.test_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class TestController {

    @GetMapping("/test/private")
    public String privateEndpoint() {

        return "🔐 Private endpoint accessed with JWT!";
    }
    @GetMapping("/test/hello")
    public String hello(
            @RequestHeader("X-User-Username") String username,
            @RequestHeader("X-User-Roles") String roles
    ) {

        // Convert roles string to list
        List<String> roleList = Arrays.asList(roles.split(","));

        if (roleList.contains("ROLE_ADMIN")) {
            return "Hello ADMIN " + username;
        }

        return "Hello USER " + username;
    }

    @GetMapping("/test/roles")
    public String getRoles(
            @RequestHeader("X-User-Roles") String roles
    ) {
        return roles;
    }
}
