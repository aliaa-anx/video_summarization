package com.backend_microservices.auth_service.service;

import com.backend_microservices.auth_service.entity.PasswordResetTokens;
import com.backend_microservices.auth_service.entity.User;
import com.backend_microservices.auth_service.repository.PasswordResetTokenRepository;
import com.backend_microservices.auth_service.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Service
@RequiredArgsConstructor

public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    //1. CREATE TOKEN
    @Transactional
    public String createTokenForUser(User user){

        tokenRepository.deleteByUserId(user.getId());
        String token = UUID.randomUUID().toString();
        PasswordResetTokens myToken= new PasswordResetTokens();
        myToken.setToken(token);
        myToken.setUser(user);
        myToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        myToken.setUsed(false);
        tokenRepository.save(myToken);

        return token;

    }

    public String validatePasswordResetToken(String token) {
        PasswordResetTokens passToken = tokenRepository.findByToken(token)
                .orElse(null);

        if (passToken == null) {
            return "invalidToken";
        }

        if (passToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "expired";
        }

        if (passToken.isUsed()) {
            return "used"; // Prevent replay attacks
        }

        return "valid";
    }
    public User getUserByToken(String token) {
        return tokenRepository.findByToken(token)
                .map(PasswordResetTokens::getUser)
                .orElse(null);
    }

    public void changePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword)); // Hash the password
        userRepository.save(user);
    }

    public void consumeToken(String token) {
        PasswordResetTokens storedToken = tokenRepository.findByToken(token).orElse(null);
        if (storedToken != null) {
            storedToken.setUsed(true);
            tokenRepository.save(storedToken);
        }
    }
}

