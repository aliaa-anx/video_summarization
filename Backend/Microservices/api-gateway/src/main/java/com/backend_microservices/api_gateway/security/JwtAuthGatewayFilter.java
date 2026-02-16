package com.backend_microservices.api_gateway.security;

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

@Component
//  implements GlobalFilter => this filter applies to ALL routes so centralized security enforcement
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final WebClient webClient;


    public JwtAuthGatewayFilter(JwtUtil jwtUtil,
                                WebClient.Builder webClientBuilder) {
        this.jwtUtil = jwtUtil;
        this.webClient = webClientBuilder
                .baseUrl("lb://auth-service")   // to resolve auth-service to be able to use its endpoints
                .build();
    }


    @Override
    /*
     this method intercepts every HTTP request and decides if to allow or reject or modify?
     Mono<Void> → async completion signal
    */
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

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
            return unauthorized(exchange);
        }

        //  this extracts the token
        String token = authHeader.substring(7);

        // this calls auth-service endpoint to check blacklist, used for logout so blacklisted tokens can't be used anymore
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/auth/check-blacklist")
                        .queryParam("token", token)
                        .build())   // till here it builds request like : GET /api/auth/check-blacklist?token=...
                .retrieve()
                .bodyToMono(Boolean.class)  // converts HTTP response to boolean if true = blacklisted, false = valid
                .flatMap(isBlacklisted -> {

                    // means the user logged out already
                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        return unauthorized(exchange);
                    }

                    //  if the JWT is not blacklisted then
                    try {
                        Claims claims = jwtUtil.extractClaims(token);

                        String username = claims.getSubject();
                        String roles = claims.get("roles", String.class);

                        ServerWebExchange mutatedExchange = exchange.mutate()
                                .request(builder -> builder
                                        .header("X-User-Username", username)
                                        .header("X-User-Roles", roles)
                                )
                                .build();

                        return chain.filter(mutatedExchange);   // this sends request to downstream service

                        //  if the JWT is expired or fake then
                    } catch (JwtException e) {
                        return unauthorized(exchange);
                    }
                });
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    //  Negative = very early execution, so this Ensures: JWT check runs before routing and before response writing
    @Override
    public int getOrder() {
        return -1;
    }
}