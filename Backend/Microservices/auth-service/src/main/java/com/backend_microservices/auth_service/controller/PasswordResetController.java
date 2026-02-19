package com.backend_microservices.auth_service.controller;

import com.backend_microservices.auth_service.client.EmailClient;
import com.backend_microservices.auth_service.dto.EmailRequest;
import com.backend_microservices.auth_service.dto.ForgotPasswordRequest;
import com.backend_microservices.auth_service.dto.ResetPasswordRequest;
import com.backend_microservices.auth_service.entity.User;
import com.backend_microservices.auth_service.repository.UserRepository;
import com.backend_microservices.auth_service.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    //private final EmailService emailService;
    private final EmailClient emailClient;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Create Token
        String token = passwordResetService.createTokenForUser(user);

        // 2. Create Link (This would be your React/Angular URL)
        String resetLink = "http://localhost:3000/reset-password?token=" + token;

        System.out.println("DEBUG: Database token saved. Now preparing Feign call...");

        EmailRequest emailRequest = new EmailRequest(
                user.getEmail(),
                "Password Reset Request",
                "Click this link to reset your password: " + resetLink
        );
        try {
            System.out.println("DEBUG: Sending request to Notification Service...");
            emailClient.sendEmail(emailRequest); // You must call the client here!
            System.out.println("DEBUG: Feign call successful.");
        } catch (Exception e) {
            System.err.println("DEBUG: Feign call failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to send email.");
        }
        Map<String,String> response =new HashMap<>();
        response.put("message", "Reset link sent to email");
        response.put("token", token);
        return ResponseEntity.ok(response);

    }
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        // 1. Validate Token
        String validationResult = passwordResetService.validatePasswordResetToken(request.getToken());
        if (!"valid".equals(validationResult)) {
            return ResponseEntity.badRequest().body("Token is " + validationResult);
        }

        // 2. Get User & Change Password
        User user = passwordResetService.getUserByToken(request.getToken());
        if (user != null) {
            passwordResetService.changePassword(user, request.getNewPassword());
            passwordResetService.consumeToken(request.getToken()); // Mark token as used
            return ResponseEntity.ok("Password successfully updated.");
        }

        return ResponseEntity.badRequest().body("Error processing request.");
    }
}


