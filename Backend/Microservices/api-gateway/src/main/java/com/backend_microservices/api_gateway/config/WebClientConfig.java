package com.backend_microservices.api_gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration  // Spring will: Instantiate this class at startup, scan it for @Bean methods, Register returned
                // objects in the Application Context
public class WebClientConfig {

    @Bean
    @LoadBalanced // because we use "lb://auth-service" and this is not a real url, so without @LoadBalanced Spring
                  // doesn't know how to resolve auth-service

    //  WebClient is Spring’s non-blocking HTTP client, for high concurrency
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
