# gateway-demo

Spring cloud gateway demo with customized features.

## Customized features

1. `CustomRetryGatewayFilterFactory` supports:
    - Retry following exceptions regardless of request method.
        - java.io.IOException
        - org.springframework.cloud.gateway.support.NotFoundException
        - io.github.resilience4j.circuitbreaker.CallNotPermittedException
    - Retry `org.springframework.cloud.gateway.support.TimeoutException` only for GET method.

2. `CustomCircuitBreakerGatewayFilterFactory` supports instance level (host + port) circuit breaker.
   > Note: There is a problem when instance restart frequently, more memory will be occupied.

3. `CustomTraceFilter` pass user IP/Locale to downstream service via sleuth `BaggageField`.