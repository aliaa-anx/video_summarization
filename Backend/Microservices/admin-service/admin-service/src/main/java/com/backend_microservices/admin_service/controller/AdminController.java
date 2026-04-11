package com.backend_microservices.admin_service.controller;



import com.backend_microservices.admin_service.dto.AdminDashboardStatsDto;
import com.backend_microservices.admin_service.dto.AdminUserDto;
import com.backend_microservices.admin_service.dto.RecentJobDto;
import com.backend_microservices.admin_service.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin") // Ensures entry points start with /admin/
@RequiredArgsConstructor
public class AdminController {




    private final AdminService adminService;


    @GetMapping("/dashboard/snapshot")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AdminDashboardStatsDto> getDashboardSnapshot() {
        // Calls the service to aggregate data from AI and Auth services
        return ResponseEntity.ok(adminService.getDashboardSnapshot());
    }

    /**
     * Requirement: Search for summaries by User or Week.
     * Handles specific filters for the dashboard table.
     */
    /**
    @GetMapping("/summaries/search")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<RecentJobDto>> searchSummaries(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String week) {

        // If a userId is provided, filter by that specific user
        if (userId != null) {
            return ResponseEntity.ok(adminService.searchSummariesByUser(userId));
        }

        // If a week is provided (e.g., "2026-04-10"), filter for that 7-day range
        if (week != null) {
            return ResponseEntity.ok(adminService.searchSummariesByWeek(week));
        }

        return ResponseEntity.badRequest().build();
    }**/

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