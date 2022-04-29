package com.example.demo.filter;

import com.example.demo.util.ContextUtil;
import com.example.demo.util.WebUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.sleuth.CurrentTraceContext;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.web.WebFluxSleuthOperators;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Trace filter that initialize user ip/locale.
 *
 * @author Chuntung Ho
 */
public class CustomTraceFilter implements GlobalFilter, Ordered {
    public static class SleuthBaggageProperties {
        List<String> remoteFields;

        public List<String> getRemoteFields() {
            return remoteFields;
        }

        public void setRemoteFields(List<String> remoteFields) {
            this.remoteFields = remoteFields;
        }
    }

    private SleuthBaggageProperties properties;
    private Tracer tracer;
    private CurrentTraceContext currentTraceContext;

    public CustomTraceFilter(SleuthBaggageProperties properties, Tracer tracer, CurrentTraceContext currentTraceContext) {
        this.properties = properties;
        this.tracer = tracer;
        this.currentTraceContext = currentTraceContext;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // put ip/locale into trace context
        WebFluxSleuthOperators.withSpanInScope(tracer, currentTraceContext, exchange, () ->
                ContextUtil.initIpAndLocale(WebUtil.findIp(exchange.getRequest()),
                        exchange.getLocaleContext().getLocale(), tracer.currentSpan().context())
        );

        // prevent fake request from client
        if (properties.getRemoteFields() != null) {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            for (String field : properties.remoteFields) {
                if (headers.containsKey(field)) {
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Illegal header detected"));
                }
            }
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }
}
