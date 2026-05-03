package com.backend_microservices.api_gateway.config;

import com.backend_microservices.api_gateway.dto.AuditLog;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
public class AuditLoggingFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;

    public AuditLoggingFilter(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("lb://audit-service")
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        long startTime = System.currentTimeMillis();

        return chain.filter(exchange)
                .doFinally(signalType -> {

                    long duration = System.currentTimeMillis() - startTime;

                    AuditLog log = new AuditLog();

                    log.setId(UUID.randomUUID().toString());
                    log.setTimestamp(Instant.now());

                    log.setEndpoint(exchange.getRequest().getURI().getPath());
                    log.setMethod(exchange.getRequest().getMethod().name());

                    var status = exchange.getResponse().getStatusCode();
                    log.setStatus(status != null ? status.toString() : "UNKNOWN");

                    log.setServiceName("api-gateway");
                    log.setAction("REQUEST");

                    log.setUserId(exchange.getRequest()
                            .getHeaders()
                            .getFirst("X-User-Id"));

                    log.setDetails("Duration: " + duration + "ms | " + signalType.name());

                    webClient.post()
                            .uri("/audit")
                            .bodyValue(log)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .subscribe();
                });
    }

    @Override
    public int getOrder() {
        return 1;
    }
}