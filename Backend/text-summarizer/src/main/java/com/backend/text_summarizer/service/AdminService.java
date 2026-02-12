package com.backend.text_summarizer.service;

import com.backend.text_summarizer.dto.AdminUserDto;
import com.backend.text_summarizer.repository.RoleRepository;
import com.backend.text_summarizer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.backend.text_summarizer.entity.User;
import com.backend.text_summarizer.entity.Role;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final RoleRepository roleRepository;

    public List<AdminUserDto>getAllUsers(){
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    public void DeleteUser(UUID id){
        if(!userRepository.existsById(id)){
            throw new RuntimeException("user not found");
        }
        userRepository.deleteById(id);

    }
    //helper function
    private AdminUserDto mapToDto(User user){
        AdminUserDto dto = new AdminUserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());

        dto.setRoles(user.getRoles().stream()
                .map(role -> role.getRoleName().toString())
                .collect(Collectors.toSet()));
        return dto;

    }
    //ban or unban user
    public void toggleUserBan(UUID userId){
        User user=userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        //change status
        boolean newStatus = !user.isEnabled(); //if true->false, if false->true
        user.setEnabled(newStatus);
        userRepository.save(user);
        try {
            // "newStatus == false" means they are now BANNED
            boolean isBanned = !newStatus;
            emailService.sendBanNotification(user.getEmail(), user.getUsername(), isBanned);
        } catch (Exception e) {
            // Log the error but DO NOT stop the ban
            System.err.println("Could not send ban email: " + e.getMessage());

    }
}

//promote user to admin
    public void promoteToAdmin(UUID id){
        User user= userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        user.getRoles().add(adminRole);
        userRepository.save(user);

    }

    public List<AdminUserDto> searchUsers(String keyword) {
        // Search in both Email and Username
        List<User> users = userRepository.findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(keyword, keyword);

        // Convert to DTOs
        return users.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

}
