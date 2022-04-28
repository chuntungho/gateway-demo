package com.example.demo.config;

import com.example.demo.filter.CustomCircuitBreakerGatewayFilterFactory;
import com.example.demo.filter.CustomTraceFilter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GatewayCustomConfig {

    @Bean
    CustomCircuitBreakerGatewayFilterFactory customCircuitBreakerGatewayFilterFactory(
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        return new CustomCircuitBreakerGatewayFilterFactory(circuitBreakerRegistry, timeLimiterRegistry);
    }

    @Bean
    @ConfigurationProperties("spring.sleuth.baggage")
    CustomTraceFilter.SleuthBaggageProperties sleuthBaggageProperties() {
        return new CustomTraceFilter.SleuthBaggageProperties();
    }

    @Bean
    CustomTraceFilter customTraceFilter(CustomTraceFilter.SleuthBaggageProperties sleuthBaggageProperties) {
        return new CustomTraceFilter(sleuthBaggageProperties);
    }
}
