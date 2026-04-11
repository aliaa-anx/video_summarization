package com.backend_microservices.admin_service.client;

import com.backend_microservices.admin_service.config.FeignConfig;
import com.backend_microservices.admin_service.dto.AdminUserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "auth-service",
        configuration = FeignConfig.class
)
public interface AuthClient {

    @GetMapping("/api/internal/users")
    List<AdminUserDto> getAllUsers();

    @GetMapping("/api/internal/users/count-all")
    long getTotalUserCount();

    @GetMapping("/api/internal/users/{id}")
    AdminUserDto getUserById(@PathVariable("id") UUID id);

    @PutMapping("/api/users/{id}/ban")
    void updateBanStatus(@PathVariable UUID id, @RequestParam boolean status);

    @DeleteMapping("/api/internal/users/{id}")
    void deleteUser(@PathVariable("id") UUID id);

    @PutMapping("/api/internal/users/{id}/toggle-ban")
    void toggleBan(@PathVariable("id") UUID id);

    @PutMapping("/api/internal/users/{id}/promote")
    void promoteToAdmin(@PathVariable("id") UUID id);

    @GetMapping("/api/internal/users/search")
    List<AdminUserDto> searchUsers(@RequestParam("keyword") String keyword);

}