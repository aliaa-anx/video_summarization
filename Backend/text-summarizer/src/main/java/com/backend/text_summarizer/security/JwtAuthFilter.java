package com.backend.text_summarizer.security;

import com.backend.text_summarizer.service.TokenBlacklistService;
import com.backend.text_summarizer.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
/*
    - intercepts every incoming HTTP request
    - checks if the request contains a JWT token
    - extracts the username from the token
    - validates the token
    - if valid:
        - authenticates the user
        - stores authentication in Spring Security context
        - so after this filter runs, Spring Security “knows” who the user is
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    // this method runs for every incoming request
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if (path.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        /*
            HTTP request example:
            GET /api/meetings
            Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
        */
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        // let's check if header contains a JWT
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token != null) {

            // If token was logged out → reject request immediately
            if (tokenBlacklistService.isBlacklisted(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Token has been revoked");
                return;
            }

            try {
                username = jwtUtil.extractUsername(token);
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid or expired token");
                return;
            }
        }

        // now let's check if user not already authenticated, if Spring security doesn't know if this user
        // is authenticated or not, SecurityContextHolder stores the current authenticated user for the request
        if (username != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            // loads the user from database into userDetails => (username, password, roles)
            var userDetails =
                    userDetailsService.loadUserByUsername(username);
            // if the token is invalid then no authentication is set, request continues as unauthenticated
            if (jwtUtil.validateToken(token)) {
                // UsernamePasswordAuthenticationToken is Spring Security’s standard authentication object, it means that the user is authenticated
                var authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,   // its null because we already authenticated via JWT
                                userDetails.getAuthorities()
                        );
                // this adds request metadata used for logging/auditing and security checks
                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                // now this finally stores the authentication in security context, so Spring Security knows
                // that this request is from user: "username" with roles: ROLE_USER, etc.
                SecurityContextHolder.getContext()
                        .setAuthentication(authToken);
            }
        }

        // this passes the request to the next filter, Controller, Endpoint so the program will continue execution
        filterChain.doFilter(request, response);
    }
}
