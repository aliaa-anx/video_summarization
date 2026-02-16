package com.backend_microservices.auth_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
    this class protects downstream services by allowing only requests from the API Gateway, It doesn’t
    validate JWTs, it simply checks for gateway-added headers.
*/
@Component
public class GatewayHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {

        String path = request.getServletPath();

        if (path.startsWith("/api/auth") || path.startsWith("/password")) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = request.getHeader("X-User-Username");
        String rolesHeader = request.getHeader("X-User-Roles");

        if (username == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

}
