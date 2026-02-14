package com.backend_microservices.auth_service.controller;

import com.backend_microservices.auth_service.dto.AuthResponse;
import com.backend_microservices.auth_service.dto.LoginRequest;
import com.backend_microservices.auth_service.dto.RefreshTokenRequest;
import com.backend_microservices.auth_service.dto.RegisterRequest;
import com.backend_microservices.auth_service.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/auth")
@RestController
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.Register(request));
    }
     @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.Login(request));
     }
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody RefreshTokenRequest request
    ){
        return ResponseEntity.ok(authService.refreshToken(request));
    }
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);
            authService.logout(token);
        }

        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/check-blacklist")
    public ResponseEntity<Boolean> checkBlacklist(
            @RequestParam String token
    ) {
        boolean blacklisted = authService.isTokenBlacklisted(token);
        return ResponseEntity.ok(blacklisted);
    }


}
