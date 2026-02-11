package com.backend.text_summarizer.controller;

import com.backend.text_summarizer.dto.ForgotPasswordRequest;
import com.backend.text_summarizer.dto.ResetPasswordRequest;
import com.backend.text_summarizer.entity.User;
import com.backend.text_summarizer.repository.UserRepository;
import com.backend.text_summarizer.service.EmailService;
import com.backend.text_summarizer.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Create Token
        String token = passwordResetService.createTokenForUser(user);

        // 2. Create Link (This would be your React/Angular URL)
        String resetLink = "http://localhost:3000/reset-password?token=" + token;

        // 3. Send Email (Check your Console!)
        emailService.sendEmail(
                user.getEmail(),
                "Password Reset Request",
                "Click this link to reset your password: " + resetLink
        );
        return ResponseEntity.ok("Reset link sent to console/email.");
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


