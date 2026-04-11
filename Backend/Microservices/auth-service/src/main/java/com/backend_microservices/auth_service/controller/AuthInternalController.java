package com.backend_microservices.auth_service.controller;
import com.backend_microservices.auth_service.dto.AdminUserDto;
import com.backend_microservices.auth_service.entity.User;
import com.backend_microservices.auth_service.repository.UserRepository;
import com.backend_microservices.auth_service.repository.RoleRepository;
import com.backend_microservices.auth_service.entity.Role;
import com.backend_microservices.auth_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal/users") // Matches the Feign Client path
@RequiredArgsConstructor
public class AuthInternalController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthService authService;


    @GetMapping("/count-all") // Must match the client!
    public long getTotalUserCount() {
        return userRepository.count();
    }
    @GetMapping("/{id}")
    public User getUserById(@PathVariable UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }



    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable UUID id) {
        if(!userRepository.existsById(id)){
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    @PutMapping("/{id}/toggle-ban")
    public AdminUserDto toggleBan(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return new AdminUserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled()
        );

    }

    @PutMapping("/{id}/promote")
    public void promoteToAdmin(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        user.getRoles().add(adminRole);
        userRepository.save(user);

    }

    @GetMapping("/search")
    public List<User> searchUsers(@RequestParam String keyword) {
        return userRepository.findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(keyword, keyword);
    }

}