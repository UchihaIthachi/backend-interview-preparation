# API Gateway

## 1. Definition

An **API Gateway** is a centralized entry point that receives client requests and routes them to the appropriate backend service.

```text
Web application
Mobile application
Partner system
        ↓
    API Gateway
        ├── Order Service
        ├── Inventory Service
        ├── Payment Service
        └── Customer Service
```

Instead of requiring clients to know the location and contract of every microservice, they communicate through one external interface.

An API Gateway can apply cross-cutting concerns such as:

- Routing
- Authentication
- Rate limiting
- Load balancing
- Request transformation
- Resilience
- Monitoring
- Traffic management

Spring Cloud Gateway provides full gateway server variants for both **Spring WebFlux** and **Spring Web MVC** applications. ([Home][1])

---

## 2. Why an API Gateway Exists

Without a gateway, a client may need to call several services directly:

```text
Mobile Client
    ├── calls Order Service
    ├── calls Payment Service
    ├── calls Inventory Service
    └── calls Customer Service
```

This creates several problems:

- Clients must know every service location.
- Internal service topology becomes externally visible.
- Authentication logic may be duplicated.
- Rate limiting becomes inconsistent.
- API changes affect many clients.
- Mobile clients may make excessive network requests.
- Traffic migration becomes difficult.

With a gateway:

```text
Mobile Client
       ↓
Single public API
       ↓
Internal services
```

The gateway hides internal routing details and gives the platform one place to apply edge-level policies.

---

# 3. Core Gateway Concepts

Spring Cloud Gateway is built around three main concepts:

## Route

A route defines:

- A unique ID
- A destination URI
- One or more predicates
- Zero or more filters

## Predicate

A predicate determines whether a request matches a route.

Predicates can examine:

- Path
- HTTP method
- Host
- Header
- Query parameter
- Cookie
- Date or time

## Filter

A filter modifies or controls the request and response before or after the downstream call.

Examples include:

- Removing or adding headers
- Rewriting paths
- Applying rate limits
- Retrying requests
- Applying circuit breakers
- Adding request metadata

Spring Cloud Gateway defines a route as a destination URI combined with predicates and filters. ([Home][2])

---

# 4. How an API Gateway Processes a Request

```text
1. Client sends an HTTP request.

2. Gateway authenticates or validates the request.

3. Route predicates are evaluated.

4. The first suitable route is selected.

5. Pre-filters process the request.

6. Load balancer selects a service instance.

7. Request is forwarded to the service.

8. Service returns a response.

9. Post-filters process the response.

10. Gateway returns the response to the client.
```

Example:

```text
GET /api/orders/O-100
        ↓
Authenticate access token
        ↓
Match /api/orders/**
        ↓
Remove /api prefix
        ↓
Resolve order-service instance
        ↓
Forward GET /orders/O-100
        ↓
Return response
```

---

# 5. Main Responsibilities

## 5.1 Routing

The gateway routes requests according to URL, method, host, headers, or other request attributes.

```text
/api/orders/**     → Order Service
/api/payments/**   → Payment Service
/api/inventory/**  → Inventory Service
```

Example:

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: order-service
              uri: lb://order-service
              predicates:
                - Path=/api/orders/**
              filters:
                - StripPrefix=1
```

The virtual URI:

```text
lb://order-service
```

allows Spring Cloud LoadBalancer to resolve an actual service instance. Spring Cloud LoadBalancer provides client-side load-balancing abstractions, including round-robin and random implementations. ([Home][3])

---

## 5.2 Authentication and Authorization

The gateway can validate an OAuth 2.0 access token before forwarding a request.

For a reactive gateway:

```java
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http
    ) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(
                                "/public/**",
                                "/actuator/health"
                        ).permitAll()
                        .pathMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .anyExchange()
                        .authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                )
                .build();
    }
}
```

Configuration:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://identity.example.com/realms/platform
```

Spring Security’s resource-server support processes bearer tokens and can validate JWT signatures and claims using the authorization server configuration. ([Home][4])

### The gateway must not be the only authorization layer

The gateway can perform coarse checks such as:

```text
Does this token contain orders.read?
```

The Order Service must still check:

```text
Does this user own this order?
Does the order belong to the user's tenant?
Is this order currently editable?
```

```text
Gateway:
Authentication and coarse authorization

Backend service:
Resource-level and business authorization
```

Internal access, configuration errors, or alternate network paths may bypass gateway policies. Security-sensitive services should therefore enforce their own authorization rules.

---

## 5.3 Rate Limiting and Throttling

Rate limiting protects the system from:

- Abuse
- Accidental request loops
- Traffic spikes
- Excessive client usage
- Resource starvation

Example:

```yaml
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 20
      redis-rate-limiter.burstCapacity: 40
      redis-rate-limiter.requestedTokens: 1
      key-resolver: "#{@userKeyResolver}"
```

Key resolver:

```java
@Bean
KeyResolver userKeyResolver() {
    return exchange -> {
        Principal principal =
                exchange.getPrincipal()
                        .blockOptional()
                        .orElse(null);

        if (principal == null) {
            return Mono.just("anonymous");
        }

        return Mono.just(principal.getName());
    };
}
```

In reactive code, avoid blocking to obtain the principal. A better implementation is:

```java
@Bean
KeyResolver userKeyResolver() {
    return exchange -> exchange
            .getPrincipal()
            .map(Principal::getName)
            .defaultIfEmpty("anonymous");
}
```

Spring Cloud Gateway’s WebFlux `RequestRateLimiter` returns `429 Too Many Requests` by default when the configured limiter rejects a request. ([Home][5])

Possible rate-limit keys include:

- User ID
- OAuth client ID
- Tenant ID
- API key
- Source IP
- Endpoint group

IP-only rate limiting may be inaccurate when many users share a proxy or when forwarding headers are not handled securely.

---

## 5.4 Load Balancing

The gateway can distribute requests across multiple service instances.

```text
Gateway
   ↓
order-service
   ├── Instance 1
   ├── Instance 2
   └── Instance 3
```

```yaml
uri: lb://order-service
```

The service name can be resolved using:

- Kubernetes service discovery
- Eureka
- Consul
- Another Spring `DiscoveryClient`
- A custom service-instance supplier

Load balancing does not eliminate the need for:

- Connection timeouts
- Health checks
- Circuit breakers
- Safe retries
- Idempotency

An instance may fail after it was selected or discovery data may temporarily be stale.

---

## 5.5 Request and Response Transformation

A gateway can transform requests before forwarding them.

Common transformations include:

- Adding headers
- Removing sensitive headers
- Rewriting paths
- Rewriting host names
- Normalizing legacy APIs
- Adding correlation IDs

Example:

```yaml
filters:
  - StripPrefix=1
  - AddRequestHeader=X-Gateway-Source, public-api
  - RemoveResponseHeader=Server
```

Spring Cloud Gateway supports global and route-specific response-header removal, including removal of sensitive headers before responses reach clients. ([Home][6])

### Avoid excessive transformation

Complex transformations can make debugging difficult:

```text
Client contract
    ↓ transformed by gateway
Internal contract
    ↓ transformed by service
Database model
```

For substantial domain translation, use an Adapter, Backend for Frontend, or dedicated integration service rather than turning the gateway into a business-logic layer.

---

## 5.6 Circuit Breaker

A gateway can wrap a route with a circuit breaker.

```yaml
filters:
  - name: CircuitBreaker
    args:
      name: inventoryCircuitBreaker
      fallbackUri: forward:/fallback/inventory
```

```text
CLOSED
→ requests reach Inventory Service

OPEN
→ requests fail fast or use fallback

HALF_OPEN
→ limited trial requests are allowed
```

Spring Cloud Gateway integrates with Spring Cloud Circuit Breaker and supports Resilience4j out of the box. ([Home][7])

### Fallback must be truthful

Reasonable:

```text
Recommendation service unavailable
→ return cached recommendations
```

Dangerous:

```text
Inventory service unavailable
→ report that stock is available
```

A fallback should not hide a failure in a way that violates business correctness.

---

## 5.7 Retry

Retries can help with short-lived failures, but they must be applied carefully.

A safe retry policy should include:

- Small maximum-attempt count
- Exponential backoff
- Jitter
- Retryable error classification
- Overall timeout budget
- Idempotency for write operations

Do not automatically retry:

- Authentication failures
- Authorization failures
- Invalid requests
- Permanent business failures
- Non-idempotent writes without an idempotency key

```text
POST /payments
without idempotency
+
gateway retry
=
possible duplicate charge
```

Retries should be implemented with the downstream service’s capacity and the complete call chain in mind.

---

## 5.8 Caching

Gateway caching can reduce repeated backend calls for suitable read-only resources.

Examples:

- Public catalog metadata
- Static reference information
- Versioned assets
- Responses with clearly defined freshness rules

Spring Cloud Gateway’s WebFlux `LocalResponseCache` supports cacheable bodiless `GET` requests and respects HTTP cache-control restrictions. It caches only selected response statuses such as `200`, `206`, and `301`. ([Home][8])

### Important limitation

A local gateway cache is local to one gateway instance:

```text
Gateway instance 1 → Cache A
Gateway instance 2 → Cache B
Gateway instance 3 → Cache C
```

It is not automatically a distributed cache.

Avoid caching:

- User-specific sensitive responses
- Rapidly changing inventory
- Authorization decisions
- Responses without clear invalidation rules
- Financial transaction results without a deliberate design

---

## 5.9 WebSocket Routing

Spring Cloud Gateway WebFlux can route `ws://` and `wss://` destinations and can combine WebSocket routing with load balancing. ([Home][9])

Example:

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: notification-websocket
              uri: lb:ws://notification-service
              predicates:
                - Path=/ws/notifications/**
```

### Protocol translation nuance

A gateway can route or adapt supported protocols, but arbitrary translation such as:

```text
HTTP request → custom gRPC call
```

is not automatically provided merely by adding a route. Complex protocol translation normally requires:

- A custom gateway filter
- An adapter service
- A Backend for Frontend
- A specialized proxy

---

# 6. Request Aggregation

A client may need data from several services:

```text
Product page
├── Catalog details
├── Price
├── Inventory
└── Reviews
```

Without aggregation:

```text
Client → Catalog
Client → Pricing
Client → Inventory
Client → Reviews
```

With aggregation:

```text
Client
  ↓
Aggregation endpoint
  ├── Catalog
  ├── Pricing
  ├── Inventory
  └── Reviews
```

Aggregation reduces client round trips, but it introduces:

- Partial failures
- Timeout budgeting
- Response-composition logic
- Different data freshness
- Increased gateway workload

For simple client-facing composition, a gateway or BFF may aggregate calls. For complex domain workflows, use a dedicated composition or orchestration service rather than placing significant business logic in the gateway.

---

# 7. Observability

An API Gateway is an important observation point because most external traffic passes through it.

## Metrics

Monitor:

- Requests per route
- P50, P95 and P99 latency
- HTTP status distribution
- Rate-limit rejections
- Circuit-breaker state
- Retry count
- Downstream connection failures
- Active connections
- Gateway CPU and memory

Spring Cloud Gateway is designed to support cross-cutting concerns including security, resiliency, monitoring, and metrics. ([Home][10])

## Logs

Use structured fields:

```text
timestamp
traceId
spanId
routeId
requestMethod
requestPath
responseStatus
durationMs
clientId
tenantId
gatewayInstance
```

Do not log:

- Complete JWTs
- Passwords
- API keys
- Session cookies
- Sensitive request bodies
- Payment information

## Distributed tracing

```text
Trace ID: abc-123

Gateway
└── Order Service
    ├── Inventory Service
    └── Payment Service
```

The gateway should propagate trace context rather than generating unrelated identifiers for each downstream request.

---

# 8. Production Architecture

```text
                         Internet
                            ↓
                   CDN / WAF / DDoS layer
                            ↓
                  External Load Balancer
                            ↓
              ┌─────────────┴─────────────┐
              ↓                           ↓
        Gateway Instance 1          Gateway Instance 2
              ↓                           ↓
              └─────────────┬─────────────┘
                            ↓
                 Service discovery or DNS
              ┌─────────────┼─────────────┐
              ↓             ↓             ↓
          Orders        Inventory      Payments
```

The gateway should normally be:

- Stateless
- Horizontally scalable
- Deployed with multiple replicas
- Protected by readiness and health checks
- Configured with bounded timeouts
- Monitored independently
- Deployed using gradual rollout strategies

A gateway centralizes entry, so an unhealthy or overloaded gateway can affect every backend service. It must not become an unprotected single point of failure.

---

# 9. What Should Not Be Placed in the Gateway?

Avoid placing the following in the API Gateway:

- Core business rules
- Database access
- Order-processing workflows
- Payment calculations
- Complex Saga coordination
- Large data transformations
- Service-specific domain validation
- Long-running operations

Bad design:

```text
Gateway
├── calculates discounts
├── updates orders
├── reserves inventory
├── processes payments
└── writes audit records
```

Better:

```text
Gateway
├── validates edge authentication
├── routes requests
├── limits traffic
└── applies cross-cutting policies

Business services
└── implement domain behavior
```

Otherwise, the gateway becomes a new monolith through which every team must coordinate changes.

---

# 10. Common Mistakes

## Mistake 1: Relying only on gateway authentication

Backend services should still validate the identity and permissions relevant to their resources.

## Mistake 2: Adding retries to every request

Retries on non-idempotent writes can duplicate business effects.

## Mistake 3: Performing complex orchestration in filters

Filters should remain focused on request and response concerns.

## Mistake 4: Logging complete requests and tokens

The gateway sees sensitive traffic and can become a major data-leak point.

## Mistake 5: Using one global timeout

Different stages require separate limits:

```text
Connection acquisition timeout
Connection timeout
TLS timeout
Response timeout
Overall request deadline
```

## Mistake 6: Treating CORS as authentication

CORS controls browser cross-origin behavior. It does not authenticate users or protect an API from non-browser clients.

## Mistake 7: Using the gateway as a permanent compatibility dumping ground

Temporary legacy transformations should have an owner and removal plan.

## Mistake 8: Forgetting gateway capacity

Expensive logging, body transformation, aggregation, and authentication can consume CPU, memory, and network capacity before requests reach the services.

---

# 11. Advantages and Trade-Offs

| Advantages                        | Trade-offs                                            |
| --------------------------------- | ----------------------------------------------------- |
| Single client entry point         | Can become a critical failure point                   |
| Centralized routing               | Adds another network hop                              |
| Consistent edge policies          | Risk of excessive centralization                      |
| Simplified clients                | Gateway configuration becomes complex                 |
| Rate limiting and traffic control | Gateway must scale with total traffic                 |
| Hides internal topology           | Can hide downstream coupling                          |
| Enables gradual migrations        | Incorrect routing can affect many services            |
| Supports aggregation              | Aggregation can increase latency and failure handling |

---

# 12. Interview Questions

## Q1: What is an API Gateway?

> An API Gateway is a centralized entry point that routes client requests to backend services and applies cross-cutting concerns such as authentication, rate limiting, load balancing, request transformation, resilience, and observability.

## Q2: How does Spring Cloud Gateway work?

> Spring Cloud Gateway represents each route using a destination, predicates, and filters. Predicates decide whether the request matches, while filters modify or control the request and response. The gateway can route through service discovery using `lb://service-name`.

## Q3: Should the gateway handle authorization?

> It can perform coarse authorization, such as checking token scopes, but every backend service should still enforce resource ownership, tenant boundaries, and business rules.

## Q4: What is the difference between a predicate and a filter?

> A predicate determines whether a request matches a route. A filter processes the matched request or response, for example by rewriting a path, applying a rate limit, or invoking a circuit breaker.

## Q5: How do you prevent the gateway from becoming a bottleneck?

> I keep it stateless, deploy multiple replicas, minimize blocking and expensive transformations, configure bounded timeouts, monitor saturation, and scale it independently.

## Q6: Should retry be configured at the gateway?

> Only selectively. The failure must be transient, the operation must be safe to retry, attempts must be bounded, and the retry must fit the total request deadline. Non-idempotent writes require an idempotency mechanism.

## Q7: Gateway vs service mesh?

> An API Gateway primarily manages north-south traffic between external clients and the platform. A service mesh primarily manages east-west communication between internal workloads. They may overlap in areas such as TLS, routing, retries, and observability, but they protect different traffic boundaries.

---

# Short Interview Answer

> An API Gateway is the controlled entry point to a microservices platform. It routes requests using predicates and filters, resolves service instances through discovery and load balancing, validates access tokens, applies rate limits and circuit breakers, and records traffic metrics and traces. I keep the gateway stateless and focused on cross-cutting edge concerns. Business logic, object-level authorization, data access, and workflow orchestration remain inside the relevant backend services.

[1]: https://docs.spring.io/spring-cloud-gateway/reference/index.html?utm_source=chatgpt.com "Spring Cloud Gateway"
[2]: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/glossary.html?utm_source=chatgpt.com "Glossary :: Spring Cloud Gateway"
[3]: https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html?utm_source=chatgpt.com "Spring Cloud LoadBalancer"
[4]: https://docs.spring.io/spring-security/reference/reactive/oauth2/resource-server/jwt.html?utm_source=chatgpt.com "OAuth 2.0 Resource Server JWT"
[5]: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html?utm_source=chatgpt.com "RequestRateLimiter GatewayFilter Factory"
[6]: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/filters/removeresponseheader.html?utm_source=chatgpt.com "RemoveResponseHeader Filter :: Spring Cloud Gateway"
[7]: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/filters/circuitbreaker-filter.html?utm_source=chatgpt.com "CircuitBreaker Filter :: Spring Cloud Gateway"
[8]: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/local-cache-response-filter.html?utm_source=chatgpt.com "LocalResponseCache GatewayFilter Factory"
[9]: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/global-filters.html?utm_source=chatgpt.com "Global Filters :: Spring Cloud Gateway"
[10]: https://spring.io/projects/spring-cloud-gateway?utm_source=chatgpt.com "Spring Cloud Gateway"
