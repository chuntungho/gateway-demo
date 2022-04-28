package com.example.demo.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.retry.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;

/**
 * Retry filter with below points:
 * <ul>
 * <li>Retry specified exceptions regardless of request method.</li>
 * <li>Retry specified exceptions only for GET method.</li>
 * </ul>
 *
 * TODO Avoid retrying on same instance
 * https://github.com/spring-cloud/spring-cloud-commons/pull/834
 */
public class CustomRetryGatewayFilterFactory extends RetryGatewayFilterFactory {
    private List<Class<? extends Throwable>> getRetryExceptions = Arrays.asList(TimeoutException.class);

    // just override retry predicate
    @Override
    public GatewayFilter apply(RetryConfig retryConfig) {
        retryConfig.validate();

        Repeat<ServerWebExchange> statusCodeRepeat = null;
        if (!retryConfig.getStatuses().isEmpty() || !retryConfig.getSeries().isEmpty()) {
            Predicate<RepeatContext<ServerWebExchange>> repeatPredicate = context -> {
                ServerWebExchange exchange = context.applicationContext();
                if (exceedsMaxIterations(exchange, retryConfig)) {
                    return false;
                }

                HttpStatus statusCode = exchange.getResponse().getStatusCode();

                boolean retryableStatusCode = retryConfig.getStatuses().contains(statusCode);

                // null status code might mean a network exception?
                if (!retryableStatusCode && statusCode != null) {
                    // try the series
                    retryableStatusCode = false;
                    for (int i = 0; i < retryConfig.getSeries().size(); i++) {
                        if (statusCode.series().equals(retryConfig.getSeries().get(i))) {
                            retryableStatusCode = true;
                            break;
                        }
                    }
                }

                final boolean finalRetryableStatusCode = retryableStatusCode;

                HttpMethod httpMethod = exchange.getRequest().getMethod();
                boolean retryableMethod = retryConfig.getMethods().contains(httpMethod);

                return retryableMethod && finalRetryableStatusCode;
            };

            statusCodeRepeat = Repeat.onlyIf(repeatPredicate)
                    .doOnRepeat(context -> reset(context.applicationContext()));

            BackoffConfig backoff = retryConfig.getBackoff();
            if (backoff != null) {
                statusCodeRepeat = statusCodeRepeat.backoff(getBackoff(backoff));
            }
        }


        // NOTE: just override the retry predicate
        Retry<ServerWebExchange> exceptionRetry = null;
        Predicate<RetryContext<ServerWebExchange>> retryContextPredicate = context -> {
            ServerWebExchange exchange = context.applicationContext();
            if (exceedsMaxIterations(exchange, retryConfig)) {
                return false;
            }

            Throwable exception = context.exception();
            // retry exceptions regardless of operation
            if (!retryConfig.getExceptions().isEmpty()) {
                if (allowException(retryConfig.getExceptions(), exception)) {
                    return true;
                }
            }

            // other exceptions just for GET operation
            HttpMethod httpMethod = exchange.getRequest().getMethod();
            if (HttpMethod.GET.equals(httpMethod)) {
                if(allowException(getRetryExceptions, exception)) {
                    return true;
                }
            }

            return false;
        };

        exceptionRetry = Retry.onlyIf(retryContextPredicate)
                .doOnRetry(context -> reset(context.applicationContext())).retryMax(retryConfig.getRetries());
        BackoffConfig backoff = retryConfig.getBackoff();
        if (backoff != null) {
            exceptionRetry = exceptionRetry.backoff(getBackoff(backoff));
        }

        GatewayFilter gatewayFilter = apply(retryConfig.getRouteId(), statusCodeRepeat, exceptionRetry);
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                return gatewayFilter.filter(exchange, chain);
            }

            @Override
            public String toString() {
                return filterToStringCreator(CustomRetryGatewayFilterFactory.this).append("routeId", retryConfig.getRouteId())
                        .append("retries", retryConfig.getRetries()).append("series", retryConfig.getSeries())
                        .append("statuses", retryConfig.getStatuses()).append("methods", retryConfig.getMethods())
                        .append("exceptions", retryConfig.getExceptions()).toString();
            }
        };
    }

    private boolean allowException(List<Class<? extends Throwable>> exceptions, Throwable exception) {
        for (Class<? extends Throwable> retryableClass : exceptions) {
            if (retryableClass.isInstance(exception)
                    || (exception != null && retryableClass.isInstance(exception.getCause()))) {
                return true;
            }
        }
        return false;
    }

    private Backoff getBackoff(BackoffConfig backoff) {
        return Backoff.exponential(backoff.getFirstBackoff(),
                backoff.getMaxBackoff(), backoff.getFactor(),
                backoff.isBasedOnPreviousValue());
    }
}
