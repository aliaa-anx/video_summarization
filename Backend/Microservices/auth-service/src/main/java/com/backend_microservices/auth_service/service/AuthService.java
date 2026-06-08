package com.backend_microservices.auth_service.service;

//import com.backend_microservices.auth_service.dto.AuthResponse;
//import com.backend_microservices.auth_service.dto.LoginRequest;
//import com.backend_microservices.auth_service.dto.RefreshTokenRequest;
//import com.backend_microservices.auth_service.dto.RegisterRequest;
//import com.backend_microservices.auth_service.entity.RefreshToken;
//import com.backend_microservices.auth_service.entity.Role;
//import com.backend_microservices.auth_service.entity.User;
//import com.backend_microservices.auth_service.repository.RefreshTokenRepository;
//import com.backend_microservices.auth_service.repository.RoleRepository;
//import com.backend_microservices.auth_service.repository.UserRepository;
//import com.backend_microservices.auth_service.security.EmailValidator;
//import com.backend_microservices.auth_service.security.JwtUtil;
//import com.backend_microservices.auth_service.security.PasswordValidator;
import com.backend_microservices.auth_service.dto.*;
import com.backend_microservices.auth_service.entity.RefreshToken;
import com.backend_microservices.auth_service.entity.Role;
import com.backend_microservices.auth_service.entity.User;
import com.backend_microservices.auth_service.repository.RefreshTokenRepository;
import com.backend_microservices.auth_service.repository.RoleRepository;
import com.backend_microservices.auth_service.repository.UserRepository;
import com.backend_microservices.auth_service.security.EmailValidator;
import com.backend_microservices.auth_service.security.JwtUtil;
import com.backend_microservices.auth_service.security.PasswordValidator;
import jakarta.transaction.Transactional;
import lombok.Data;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@Data
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private User User;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;

    public List<AdminUserDto> getUsersByIds(List<UUID> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        return users.stream()
                .map(user -> new AdminUserDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.isEnabled()
                ))
                .collect(Collectors.toList());
    }
    public String Register(RegisterRequest request) {
        //check email
        boolean isEmailValid = EmailValidator.isValid(request.getEmail());
        if (!isEmailValid) {
            throw new IllegalArgumentException("Error: Invalid email format!");
        }

        //1.check if username already exists
        String normalizedUsername = request.getUsername().toLowerCase();
        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        //check email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        //2.check the password matches the pattern
        if (!PasswordValidator.isValid(request.getPassword())) {
            throw new IllegalArgumentException("Password too weak! Needs: 8+ chars, " +
                    "1 Upper, 1 Number, 1 Special Char.");
        }

        //3. create new user object
        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        //4.assign userRole
        // 1. Get the actual Role object (No Optional wrapping needed!)
        Role userRole = roleRepository.findByRoleName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        // 2. Create a Set that holds ROLES (not Optionals)
        Set<Role> roles = new HashSet<>();

        // 3. Add the role to the box
        roles.add(userRole);

        // 4. Give the box to the user
        user.setRoles(roles);

        // 5. Save to Database
        userRepository.save(user);

        return "User registered successfully!";
    }

    public AuthResponse Login(LoginRequest request) {
        //1. check username&password
        String normalizedUsername = request.getUsername().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizedUsername,
                        request.getPassword()
                )
        );

        //2.if we reach this part that means user is valid now we will get the userobject
        var user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // to delete old refresh tokens so only one refresh token active
        refreshTokenRepository.deleteByUser(user);

        String accessToken =
                jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoles());
        var refreshToken = refreshTokenService.createRefreshToken(user); // to generate the Long-lived JWT then Stored in DB

        return new AuthResponse(
                accessToken,
                refreshToken.getToken()
        );
    }

    public void logout(String token) {
        // now the JWT (access token) can’t be used again
        tokenBlacklistService.blacklistToken(token);

        String username = jwtUtil.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow();

        // now the JWT (refresh token) can’t be used again
        refreshTokenRepository.deleteByUser(user);
    }

    // this method is called when the client’s access token has expired and the client wants a new access
    // token without logging in again, so we use the refresh token to be able to do that
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestToken = request.getRefreshToken();

        // check if the refresh token given in the request exists or not
        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        // check if the refresh token given in the request expired or not (it will be invalid if its expired)
        refreshTokenService.verifyExpiration(refreshToken);

        // we implemented the refresh tokens to be rotated, means if we used our refresh token once this
        // token can't be used anymore to refresh again, so this refresh token is said to be 'revoked'
        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Refresh token already used.");
        }

        // as I just said, if we used our refresh token once this token can't be used anymore to refresh
        // again so it is marked to be 'revoked' :)
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // create new refresh token
        var newRefreshToken = refreshTokenService.createRefreshToken(refreshToken.getUser());

        // create new access token
        String newAccessToken =
                jwtUtil.generateToken(
                        refreshToken.getUser().getId(),
                        refreshToken.getUser().getUsername(),
                        refreshToken.getUser().getRoles()
                );

        return new AuthResponse(
                newAccessToken,
                newRefreshToken.getToken()
        );
    }
    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistService.isBlacklisted(token);
    }

}