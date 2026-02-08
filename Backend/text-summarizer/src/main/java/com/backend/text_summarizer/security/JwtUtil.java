package com.backend.text_summarizer.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

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
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)    // This prevents fake and modified token
                .compact(); // Turns it into string
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

    // claims are the information stored inside the token
    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // we need the signing key to check if this token created by our server
                .build()
                // it automatically checks: if the token expired? if the token broken? if the token modified?
                // if expired → exception, If invalid → exception
                // if token is valid, it returns: Claims = { sub=username, iat, exp }
                .parseClaimsJws(token);
    }
}