//package com.backend_microservices.auth_service.controller;
//
//import com.backend_microservices.auth_service.client.EmailClient;
//import com.backend_microservices.auth_service.dto.EmailRequest;
//import com.backend_microservices.auth_service.dto.ForgotPasswordRequest;
//import com.backend_microservices.auth_service.dto.ResetPasswordRequest;
//import com.backend_microservices.auth_service.entity.User;
//import com.backend_microservices.auth_service.repository.UserRepository;
//import com.backend_microservices.auth_service.service.PasswordResetService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/password")
//@RequiredArgsConstructor
//public class PasswordResetController {
//
//    private final UserRepository userRepository;
//    private final PasswordResetService passwordResetService;
//    //private final EmailService emailService;
//    private final EmailClient emailClient;
//
//    @PostMapping("/forgot-password")
//    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        // 1. Create Token
//        String token = passwordResetService.createTokenForUser(user);
//
//        // 2. Create Link (This would be your React/Angular URL)
//        String resetLink = "http://localhost:3000/reset-password?token=" + token;
//
//        System.out.println("DEBUG: Database token saved. Now preparing Feign call...");
//
//        EmailRequest emailRequest = new EmailRequest(
//                user.getEmail(),
//                "Password Reset Request",
//                "Click this link to reset your password: " + resetLink
//        );
//        try {
//            System.out.println("DEBUG: Sending request to Notification Service...");
//            emailClient.sendEmail(emailRequest); // You must call the client here!
//            System.out.println("DEBUG: Feign call successful.");
//        } catch (Exception e) {
//            System.err.println("DEBUG: Feign call failed: " + e.getMessage());
//            return ResponseEntity.internalServerError().body("Failed to send email.");
//        }
//        Map<String,String> response =new HashMap<>();
//        response.put("message", "Reset link sent to email");
//        response.put("token", token);
//        return ResponseEntity.ok(response);
//
//    }
//    @PostMapping("/reset-password")
//    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
//        // 1. Validate Token
//        String validationResult = passwordResetService.validatePasswordResetToken(request.getToken());
//        if (!"valid".equals(validationResult)) {
//            return ResponseEntity.badRequest().body("Token is " + validationResult);
//        }
//
//
//        // 2. Get User & Change Password
//        User user = passwordResetService.getUserByToken(request.getToken());
//        if (user != null) {
//            passwordResetService.changePassword(user, request.getNewPassword());
//            passwordResetService.consumeToken(request.getToken()); // Mark token as used
//            return ResponseEntity.ok("Password successfully updated.");
//        }
//
//
//        return ResponseEntity.badRequest().body("Error processing request.");
//    }
//}
//
//
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
import org.springframework.web.bind.annotation.CrossOrigin;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    //private final EmailService emailService;
    private final EmailClient emailClient;

//    @PostMapping("/forgot-password")
//    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
////        User user = userRepository.findByEmail(request.getEmail())
////                .orElseThrow(() -> new RuntimeException("User not found"));
//        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
//        if (userOpt.isEmpty()) {
//            return ResponseEntity.ok(Map.of("message", "If that email exists, a reset code was sent."));
//        }
//        User user = userOpt.get();
//
//        // 1. Create Token
//        String token = passwordResetService.createTokenForUser(user);
//
//        // 2. Create Link (This would be your React/Angular URL)
//        String resetLink = "http://localhost:3000/reset-password?token=" + token;
//
//        System.out.println("DEBUG: Database token saved. Now preparing Feign call...");
//
//        EmailRequest emailRequest = new EmailRequest(
//                user.getEmail(),
//                "Password Reset Request",
//                "Click this link to reset your password: " + resetLink
//        );
//        try {
//            System.out.println("DEBUG: Sending request to Notification Service...");
//            emailClient.sendEmail(emailRequest); // You must call the client here!
//            System.out.println("DEBUG: Feign call successful.");
//        } catch (Exception e) {
//            System.err.println("DEBUG: Feign call failed: " + e.getMessage());
//            return ResponseEntity.internalServerError().body("Failed to send email.");
//        }
//        Map<String,String> response =new HashMap<>();
//        response.put("message", "Reset link sent to email");
//        response.put("Code:", token);
//        return ResponseEntity.ok(response);
//
//    }
//    @PostMapping("/reset-password")
//    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
//        // 1. Validate Token
//        String validationResult = passwordResetService.validatePasswordResetToken(request.getToken());
//        if (!"valid".equals(validationResult)) {
//            return ResponseEntity.badRequest().body("Token is " + validationResult);
//        }
//
//
//        // 2. Get User & Change Password
//        User user = passwordResetService.getUserByToken(request.getToken());
//        if (user != null) {
//            passwordResetService.changePassword(user, request.getNewPassword());
//            passwordResetService.consumeToken(request.getToken()); // Mark token as used
//            return ResponseEntity.ok("Password successfully updated.");
//        }
//
//
//        return ResponseEntity.badRequest().body("Error processing request.");
//    }
@PostMapping("/forgot-password")
public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
    Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
    if (userOpt.isEmpty()) {
        return ResponseEntity.ok(Map.of("message", "If that email exists, a reset code was sent."));
    }

    User user = userOpt.get();
    String token = passwordResetService.createTokenForUser(user);

    EmailRequest emailRequest = new EmailRequest(
            user.getEmail(),
            "Password Reset Request",
            "Your password reset code is: " + token + "\nThis code expires in 15 minutes."
    );

    try {
        emailClient.sendEmail(emailRequest);
    } catch (Exception e) {
        return ResponseEntity.internalServerError().body("Failed to send email.");
    }

    return ResponseEntity.ok(Map.of("message", "If that email exists, a reset code was sent."));
}
  @PostMapping("/reset-password")
  public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
    try {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
}



