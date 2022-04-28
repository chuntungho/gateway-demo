package com.example.demo.filter;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreaker;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.HasRouteId;
import org.springframework.cloud.gateway.support.ServiceUnavailableException;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * Circuit breaker based on Resilience4J in instance level (host + port).<br>
 * The filter order should be after {@link ReactiveLoadBalancerClientFilter}.
 *
 * @author Chuntung Ho
 * @see org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory
 * @see org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerResilience4JFilterFactory
 */
public class CustomCircuitBreakerGatewayFilterFactory
        extends AbstractGatewayFilterFactory<CustomCircuitBreakerGatewayFilterFactory.Config> implements Ordered {

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private TimeLimiterRegistry timeLimiterRegistry;

    public CustomCircuitBreakerGatewayFilterFactory(CircuitBreakerRegistry circuitBreakerRegistry,
                                                    TimeLimiterRegistry timeLimiterRegistry) {
        super(Config.class);
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return singletonList(NAME_KEY);
    }

    @Override
    public GatewayFilter apply(Config config) {
        // get config by routeId or name argument
        String id = config.getId();
        Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration breakerConfig = new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(circuitBreakerRegistry.getConfiguration(id).orElse(circuitBreakerRegistry.getDefaultConfig()))
                .timeLimiterConfig(timeLimiterRegistry.getConfiguration(id).orElse(timeLimiterRegistry.getDefaultConfig())).build();

        return new OrderedGatewayFilter((exchange, chain) -> {
            URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
            String instanceId = url.getHost() + ":" + url.getPort();
            // break in instance level
            // TODO how to remove config from registry when instance is down?
            ReactiveCircuitBreaker circuitBreaker = new ReactiveResilience4JCircuitBreaker(instanceId, breakerConfig,
                    circuitBreakerRegistry, timeLimiterRegistry, Optional.empty());
            return circuitBreaker.run(chain.filter(exchange), Mono::error)
                    .onErrorResume(t -> handleErrorWithoutFallback(t, false));
        }, ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER + 1);
    }

    protected Mono<Void> handleErrorWithoutFallback(Throwable t, boolean resumeWithoutError) {
        if (java.util.concurrent.TimeoutException.class.isInstance(t)) {
            return Mono.error(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, t.getMessage(), t));
        }
        if (CallNotPermittedException.class.isInstance(t)) {
            return Mono.error(new ServiceUnavailableException());
        }
        if (resumeWithoutError) {
            return Mono.empty();
        }
        return Mono.error(t);
    }

    @Override
    public int getOrder() {
        return ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER + 1;
    }

    public static class Config implements HasRouteId {
        private String name;
        private String routeId;

        @Override
        public void setRouteId(String routeId) {
            this.routeId = routeId;
        }

        public String getRouteId() {
            return routeId;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getId() {
            if (!StringUtils.hasLength(name) && StringUtils.hasLength(routeId)) {
                return routeId;
            }
            return name;
        }
    }
}
