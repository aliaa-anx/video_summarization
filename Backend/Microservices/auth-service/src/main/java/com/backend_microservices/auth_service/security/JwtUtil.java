package com.backend_microservices.auth_service.security;

import com.backend_microservices.auth_service.entity.Role;
import com.backend_microservices.auth_service.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    // reads value from application.properties to be more secure than putting them in the code here
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    // this Key is used to sign and verify token
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // JWT token's payload contains: sub = username, iat = issued at, exp = expiration time
    public String generateToken(UUID userId, String username, Set<Role> roles) {

        String rolesString = roles.stream()
                .map(Role::getRoleName)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId.toString()) // JWT stores it as String
                .claim("roles", rolesString)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // extracts username from a token
    public String extractUsername(String token) {
        return parseClaims(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        String id = parseClaims(token).getBody().get("userId", String.class);
        return UUID.fromString(id);
    }

    // claims are the information stored inside the token
    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // we need the signing key to check if this token created by our server
                .build() // it automatically checks: if the token expired? if the token broken? if the token modified?
                // if expired → exception, If invalid → exception
                // if token is valid, it returns: Claims = { sub=username, iat, exp }
                .parseClaimsJws(token);
    }
}