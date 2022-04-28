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

## Filter chain with customized filters

```
0 = {OrderedGatewayFilter@11482} "[GatewayFilterAdapter{delegate=com.example.demo.filter.CustomTraceFilter@3c97f5e9}, order = -2147483648]"
1 = {OrderedGatewayFilter@11483} "[GatewayFilterAdapter{delegate=org.springframework.cloud.gateway.filter.RemoveCachedBodyFilter@211da640}, order = -2147483648]"
2 = {OrderedGatewayFilter@11484} "[GatewayFilterAdapter{delegate=org.springframework.cloud.gateway.filter.AdaptCachedBodyGlobalFilter@3dec79f8}, order = -2147482648]"
3 = {OrderedGatewayFilter@11485} "[GatewayFilterAdapter{delegate=org.springframework.cloud.gateway.filter.NettyWriteResponseFilter@22a63740}, order = -1]"
4 = {OrderedGatewayFilter@11486} "[GatewayFilterAdapter{delegate=org.springframework.cloud.gateway.filter.ForwardPathFilter@65ad2b42}, order = 0]"
5 = {OrderedGatewayFilter@11487} "[[CustomRetry routeId = 'test', retries = 3, series = list[SERVER_ERROR], statuses = list[[empty]], methods = list[GET], exceptions = list[IOException, NotFoundException, CallNotPermittedException]], order = 1]"
6 = {OrderedGatewayFilter@11488} "[[StripPrefix parts = 1], order = 1]"
7 = {OrderedGatewayFilter@11489} "[GatewayFilterAdapter{delegate=org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter@2ec92631}, order = 10000]"
8 = {OrderedGatewayFilter@11490} "[GatewayFilterAdapter{delegate=org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter@32e7b78d}, order = 10150]"
9 = {OrderedGatewayFilter@11491} "[com.example.demo.filter.CustomCircuitBreakerGatewayFilterFactory$$Lambda$1055/0x0000000800703840@523a0947, order = 10151]"
10 = {OrderedGatewayFilter@11492} "[GatewayFilterAdapter{delegate=org.springframework.cloud.gateway.filter.WebsocketRoutingFilter@294ebe11}, order = 2147483646]"
11 = {OrderedGatewayFilter@11493} "[GatewayFilterAdapter{delegate=org.springframework.cloud.gateway.filter.NettyRoutingFilter@4583b617}, order = 2147483647]"
12 = {OrderedGatewayFilter@11494} "[GatewayFilterAdapter{delegate=org.springframework.cloud.gateway.filter.ForwardRoutingFilter@76220ef1}, order = 2147483647]"
```