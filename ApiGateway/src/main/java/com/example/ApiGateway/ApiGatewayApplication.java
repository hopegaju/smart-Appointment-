package com.example.ApiGateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Patient Service Routes
                .route("patient-service", r -> r
                        .path("/api/patients/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("patientCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/patient"))
                                .requestRateLimiter(rl -> rl
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setStatuses(org.springframework.http.HttpStatus.BAD_GATEWAY)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true)))
                        .uri("lb://PATIENT-SERVICE"))

                // Doctor Service Routes
                .route("doctor-service", r -> r
                        .path("/api/doctors/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("doctorCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/doctor"))
                                .requestRateLimiter(rl -> rl
                                        .setRateLimiter(redisRateLimiter())))
                        .uri("lb://DOCTOR-SERVICE"))

                // Queue Service Routes
                .route("queue-service", r -> r
                        .path("/api/queue/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("queueCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/queue"))
                                .addRequestHeader("X-Service", "Queue"))
                        .uri("lb://QUEUE-SERVICE"))

                // Appointment Service Routes
                .route("appointment-service", r -> r
                        .path("/api/appointments/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("appointmentCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/appointment")))
                        .uri("lb://APPOINTMENT-SERVICE"))

                // Notification Service Routes (Internal only)
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f
                                .addRequestHeader("X-Internal-Request", "true"))
                        .uri("lb://NOTIFICATION-SERVICE"))

                .build();
    }

    @Bean
    public org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter redisRateLimiter() {
        return new org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter(
                10,  // replenishRate: tokens per second
                20   // burstCapacity: maximum tokens
        );
    }

    @Bean
    public org.springframework.cloud.gateway.filter.ratelimit.KeyResolver userKeyResolver() {
        return exchange -> reactor.core.publisher.Mono.just(
                exchange.getRequest().getHeaders().getFirst("X-User-Id") != null
                        ? exchange.getRequest().getHeaders().getFirst("X-User-Id")
                        : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://localhost:4200");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
