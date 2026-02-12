package com.backend.text_summarizer.controller;

import com.backend.text_summarizer.dto.AdminUserDto;
import com.backend.text_summarizer.repository.RoleRepository;
import com.backend.text_summarizer.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final RoleRepository roleRepository;
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AdminUserDto>> getAllUsers(){
        return ResponseEntity.ok(adminService.getAllUsers());

    }
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable UUID id){
      adminService.DeleteUser(id);
      return ResponseEntity.ok("user deleted successfully!");
    }
    @PutMapping("/users/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> banUser(@PathVariable UUID id) {
        adminService.toggleUserBan(id);
        return ResponseEntity.ok("User status changed successfully.");
    }

    @PutMapping("/users/{id}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> promoteUser(@PathVariable UUID id) {
        adminService.promoteToAdmin(id);
        return ResponseEntity.ok("User promoted to Admin successfully.");
    }

    @GetMapping("/users/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminUserDto>> searchUsers(@RequestParam String keyword) {
        return ResponseEntity.ok(adminService.searchUsers(keyword));
    }


}
