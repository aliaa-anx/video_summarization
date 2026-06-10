package com.backend_microservices.ai_service.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.stream.Collectors;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {


            // 1. Get the current incoming request
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // 2. Pull the User-Id directly from the incoming header
                // Note: Ensure this name matches exactly what the Gateway or your logs showed
                String userId = request.getHeader("X-User-Id");
                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                }

                Authentication authentication =
                        SecurityContextHolder.getContext().getAuthentication();

                if (authentication != null) {

                    requestTemplate.header(
                            "X-User-Username",
                            authentication.getName()
                    );

                    String roles = authentication.getAuthorities()
                            .stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(","));

                    requestTemplate.header("X-User-Roles", roles);

                }
            };
        };
    }}
