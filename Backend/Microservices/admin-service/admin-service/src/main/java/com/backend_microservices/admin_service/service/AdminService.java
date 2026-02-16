package com.backend_microservices.admin_service.service;

import com.backend_microservices.admin_service.client.AuthClient;
import com.backend_microservices.admin_service.client.NotificationClient;
import com.backend_microservices.admin_service.dto.AdminUserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final AuthClient authClient;
    private final NotificationClient notificationClient;

    public List<AdminUserDto> getAllUsers() {
        return authClient.getAllUsers();
    }

    public void DeleteUser(UUID id) {
        authClient.deleteUser(id);
    }

    public void toggleUserBan(UUID userId) {
        // 1. Update status in Auth Service
        authClient.toggleBan(userId);

        // 2. Fetch fresh user data to see new status and get email info
        AdminUserDto user = authClient.getUserById(userId);

        try {
            // If user has no 'enabled' field in DTO yet, you'll need to add it to AdminUserDto
            boolean isBanned = !user.isEnabled();
            notificationClient.sendBanEmail(user.getEmail(), user.getUsername(), isBanned);
        } catch (Exception e) {
            log.error("Notification failed for user {}: {}", userId, e.getMessage());
        }
    }

    public void promoteToAdmin(UUID id) {

        authClient.promoteToAdmin(id);
    }

    public List<AdminUserDto> searchUsers(String keyword) {
        return authClient.searchUsers(keyword);
    }
}