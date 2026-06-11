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
import java.security.SecureRandom;
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
//    @Transactional
//    public String createTokenForUser(User user) {
//        // 1. Clean up old tokens
//        tokenRepository.deleteByUserId(user.getId());
//
//        // 2. Create and Save the new token
//        String token = UUID.randomUUID().toString();
//        PasswordResetTokens myToken = new PasswordResetTokens();
//        myToken.setToken(token);
//        myToken.setUser(user);
//        myToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
//        myToken.setUsed(false);
//
//        tokenRepository.save(myToken);
//
//
//
//        return token;
//    }
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateToken() {
        StringBuilder token = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            token.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return token.toString();
    }

    @Transactional
    public String createTokenForUser(User user) {
        tokenRepository.deleteByUserId(user.getId());

        String token = generateToken();
        PasswordResetTokens myToken = new PasswordResetTokens();
        myToken.setToken(token);
        myToken.setUser(user);
        myToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        myToken.setUsed(false);

        tokenRepository.save(myToken);

        return token;
    }
    @Transactional
    public String validatePasswordResetToken(String token) {
        PasswordResetTokens passToken = tokenRepository.findByToken(token)
                .orElse(null);

        if (passToken == null) {
            return "invalidToken";
        }

        if (passToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            //tokenRepository.delete(passToken);
            return "expired";
        }

        if (passToken.isUsed()) {
            //tokenRepository.delete(passToken);
            return "used"; // Prevent replay attacks
        }

        return "valid";
    }
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetTokens storedToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Token expired");

        if (storedToken.isUsed())
            throw new RuntimeException("Token already used");

        User user = storedToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        storedToken.setUsed(true);
        tokenRepository.save(storedToken);
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

