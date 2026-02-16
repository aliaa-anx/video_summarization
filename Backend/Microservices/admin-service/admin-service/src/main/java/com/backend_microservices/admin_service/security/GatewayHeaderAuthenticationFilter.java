package com.backend_microservices.admin_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String username = request.getHeader("X-User-Username");
        String rolesHeader = request.getHeader("X-User-Roles");

        System.out.println("=== ADMIN SERVICE FILTER ===");

        System.out.println("USERNAME HEADER: " + username);
        System.out.println("ROLES HEADER: " + rolesHeader);

        if (username != null && rolesHeader != null) {


            List<SimpleGrantedAuthority> authorities =
                    Arrays.stream(rolesHeader.split(","))
                            .map(String::trim)   // ⭐ VERY IMPORTANT
                            .map(SimpleGrantedAuthority::new)
                            .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}

