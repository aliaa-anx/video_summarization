package com.backend_microservices.admin_service.controller;



import com.backend_microservices.admin_service.dto.AdminUserDto;
import com.backend_microservices.admin_service.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin") // Ensures entry points start with /admin/
@RequiredArgsConstructor
public class AdminController {


    private final AdminService adminService;

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable UUID id) {
        adminService.DeleteUser(id);
        return ResponseEntity.ok("User deleted successfully!");
    }


    @PutMapping("/users/{id}/ban")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> toggleBan(@PathVariable UUID id) {
        adminService.toggleUserBan(id);
        return ResponseEntity.ok("User status toggled successfully.");
    }
    @PutMapping("/users/{id}/promote")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> promoteToAdmin(@PathVariable UUID id) {
        adminService.promoteToAdmin(id);
        return ResponseEntity.ok("User promoted to Admin successfully.");
    }

    @GetMapping("/users/search")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AdminUserDto>> searchUsers(@RequestParam String keyword) {
        return ResponseEntity.ok(adminService.searchUsers(keyword));
    }
}