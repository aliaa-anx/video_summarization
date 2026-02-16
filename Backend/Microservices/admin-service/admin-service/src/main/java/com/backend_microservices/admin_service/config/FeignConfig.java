package com.backend_microservices.admin_service.config;

import feign.RequestInterceptor;
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
    }
}
