package com.backend_microservices.api_gateway.security;

import com.backend_microservices.api_gateway.client.AuditClient;
import com.backend_microservices.api_gateway.dto.AuditLog;
import com.backend_microservices.api_gateway.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

//  implements GlobalFilter => this filter applies to ALL routes so centralized security enforcement
@Component
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final WebClient webClient;
    private final AuditClient auditClient;

    public JwtAuthGatewayFilter(JwtUtil jwtUtil,
                                WebClient.Builder webClientBuilder,
                                AuditClient auditClient) {
        this.jwtUtil = jwtUtil;
        this.auditClient = auditClient;
        this.webClient = webClientBuilder
                .baseUrl("lb://auth-service")       // to resolve auth-service to be able to use its endpoints
                .build();
    }

    @Override
    /*
     this method intercepts every HTTP request and decides if to allow or reject or modify?
     Mono<Void> → async completion signal
    */
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        if (exchange.getRequest().getMethod().name().equals("OPTIONS")) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();

        // to allow public endpoints to path without tokens
        if (path.startsWith("/api/auth") || path.startsWith("/password")) {
            return chain.filter(exchange);
        }

        //  this reads authorization header as "Authorization: Bearer eyJhbGciOiJIUzI1..."
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing token");
        }

        //  this extracts the token
        String token = authHeader.substring(7);

        // this calls auth-service endpoint to check blacklist, used for logout so blacklisted tokens can't be used anymore
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/auth/check-blacklist")
                        .queryParam("token", token)
                        .build())       // till here it builds request like : GET /api/auth/check-blacklist?token=...
                .retrieve()
                .bodyToMono(Boolean.class) // converts HTTP response to boolean if true = blacklisted, false = valid
                .flatMap(isBlacklisted -> {

                    // means the user logged out already
                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        return unauthorized(exchange, "Blacklisted token");
                    }

                    //  if the JWT is not blacklisted then
                    try {
                        Claims claims = jwtUtil.extractClaims(token);

                        ServerWebExchange mutated = exchange.mutate()
                                .request(builder -> builder
                                        .header("X-User-Username", claims.getSubject())
                                        .header("X-User-Roles", claims.get("roles", String.class))
                                        .header("X-User-Id", claims.get("userId", String.class))
                                )
                                .build();

                        return chain.filter(mutated);   // this sends request to downstream service

                        //  if the JWT is expired or fake then
                    } catch (JwtException e) {
                        return unauthorized(exchange, "Invalid JWT");
                    }
                });
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {

        AuditLog log = new AuditLog();

        log.setId(UUID.randomUUID().toString());
        log.setTimestamp(Instant.now());
        log.setEndpoint(exchange.getRequest().getURI().getPath());
        log.setMethod(exchange.getRequest().getMethod().name());

        log.setStatus("401");
        log.setServiceName("api-gateway");
        log.setAction("UNAUTHORIZED_ACCESS");
        log.setDetails(reason);

        log.setUserId(exchange.getRequest()
                .getHeaders()
                .getFirst("X-User-Id"));

        auditClient.sendLog(log);

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    //  Negative = very early execution, so this Ensures: JWT check runs before routing and before response writing
    @Override
    public int getOrder() {
        return -1;
    }
}