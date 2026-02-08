package com.backend.text_summarizer.config;


import com.backend.text_summarizer.security.JwtAuthFilter;
import com.backend.text_summarizer.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// this marks the class as a Spring configuration class
@Configuration
@RequiredArgsConstructor
/*
    this his class is the central configuration for Spring Security, it defines:
        - How authentication works
        - Which endpoints are public or protected
        - How passwords are encoded
        - Which filters run and in what order
        - Whether sessions are used
*/
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    // Spring Security never stores raw passwords, instead Raw password → hashed → stored in DB
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCryptPasswordEncoder Uses salting automatically, Resistant to brute-force attack so Even if two users
        // use the same password Hashes will be different
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // disable csrf (Cross-Site Request Forgery), since we are using JWT + Stateless API so we don't need it
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // All endpoints under /api/auth/ are public, no authentication required
                        .anyRequest().authenticated()   // Every other endpoint requires authentication
                )
                // since we are using JWT then we don't need to store sessions
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                /*
                    we want to insert our custom filter BEFORE the built-in UsernamePasswordAuthenticationFilter
                    in the filter chain since we are using JWT authentication instead of the ordinary authentication
                */
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
