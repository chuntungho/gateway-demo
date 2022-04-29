package com.example.demo.config;

import com.example.demo.filter.CustomCircuitBreakerGatewayFilterFactory;
import com.example.demo.filter.CustomRetryGatewayFilterFactory;
import com.example.demo.filter.CustomTraceFilter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.sleuth.CurrentTraceContext;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GatewayCustomConfig {

    @Bean
    CustomRetryGatewayFilterFactory customRetryGatewayFilterFactory() {
        return new CustomRetryGatewayFilterFactory();
    }

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
    CustomTraceFilter customTraceFilter(CustomTraceFilter.SleuthBaggageProperties sleuthBaggageProperties,
                                        Tracer tracer, CurrentTraceContext currentTraceContext) {
        return new CustomTraceFilter(sleuthBaggageProperties, tracer, currentTraceContext);
    }
}
