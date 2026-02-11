package com.backend.text_summarizer.service;
import com.backend.text_summarizer.entity.RoleName;
import com.backend.text_summarizer.entity.User;
import com.backend.text_summarizer.entity.Role;
import com.backend.text_summarizer.dto.RegisterRequest;
import com.backend.text_summarizer.repository.RoleRepository;
import com.backend.text_summarizer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*; // Standard JUnit assertions
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    //fake mocks
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    //create a request object before wach test

    @InjectMocks
    private AuthService authService;

    private RegisterRequest request;
    @BeforeEach
    void setup() {
        request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("StrongP@ss1");
    }
    @Test
    @DisplayName("Should register user successfully when data is valid")
    void shouldRegisterUser_WhenValid() {
        // A. Arrange (Train the mocks)
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty()); // User doesn't exist
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);     // Email unused
        when(passwordEncoder.encode(any())).thenReturn("hashed_secret");

        Role dummyRole = new Role();
        dummyRole.setRoleName("ROLE_USER");
        when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(Optional.of(dummyRole));

        // B. Act (Run the real method)
        String result = authService.Register(request);

        // C. Assert (Check the result)
        assertEquals("User registered successfully!", result);

        // D. Verify (Check side effects)
        verify(userRepository, times(1)).save(any(User.class)); // Must save exactly once
    }

    // 6. Failure Scenario (Username Taken)
    @Test
    @DisplayName("Should throw exception when username is already taken")
    void shouldThrow_WhenUsernameExists() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(new User())); // User ALREADY exists

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.Register(request);
        });

        assertEquals("Error: Username is already taken!", exception.getMessage());

        // Verify we NEVER saved to the DB
        verify(userRepository, never()).save(any());
    }

    // 7. Failure Scenario (Invalid Email)
    @Test
    @DisplayName("Should throw exception when email format is invalid")
    void shouldThrow_WhenEmailInvalid() {
        // Arrange
        request.setEmail("bad-email-format"); // Broken email

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.Register(request);
        });

        assertTrue(exception.getMessage().contains("Invalid email"));
        verify(userRepository, never()).save(any());
    }


    }




