# Advanced Questions — Spring Cloud and Microservices Infrastructure

## Important Corrections

- Configuration refresh uses `/actuator/refresh`, not `/refresh`.
- Cluster-wide refresh through Spring Cloud Bus uses `/actuator/busrefresh`.
- Modern Spring applications use **Micrometer Tracing** rather than Spring Cloud Sleuth.
- Spring Cloud Gateway is no longer only reactive; current versions provide both **WebFlux** and **Web MVC** server variants.
- `RestTemplate` is now deprecated in favor of `RestClient` for new synchronous code.
- `@EnableDiscoveryClient` is no longer required when a supported discovery implementation is on the classpath. ([Home][1])

---

# Q13: How does Spring Cloud Config Server work? How do you refresh configuration without restarting?

Spring Cloud Config provides centralized, externalized configuration for distributed applications. A Config Server reads properties from an `EnvironmentRepository`, commonly backed by Git, and exposes configuration based on the application name, profile, and optional label. Clients retrieve this configuration during startup and add it to their Spring `Environment`. ([Home][2])

## High-level architecture

```text
Git repository or another configuration source
                    ↓
          Spring Cloud Config Server
                    ↓
       /{application}/{profile}/{label}
                    ↓
      Spring Boot configuration clients
```

For example:

```text
Application: order-service
Profile:     production
Label:       main
```

may resolve configuration from files such as:

```text
application.yml
application-production.yml
order-service.yml
order-service-production.yml
```

## Config Server setup

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                ConfigServerApplication.class,
                args
        );
    }
}
```

```yaml
server:
  port: 8888

spring:
  cloud:
    config:
      server:
        git:
          uri: https://git.example.com/platform/configuration.git
          default-label: main
```

The Config Server should be secured because configuration may contain internal endpoints, infrastructure details, and other operationally sensitive values.

## Config Client setup

```yaml
spring:
  application:
    name: order-service

  profiles:
    active: production

  config:
    import: optional:configserver:http://config-server:8888
```

The client may make more than one request because Spring Boot first resolves default configuration and then retrieves configuration for any profiles activated during that process. ([Home][2])

---

## Refreshing one application instance

Assume this configuration changes:

```yaml
order:
  maximum-items: 50
```

A refreshable bean can be declared with `@RefreshScope`:

```java
@Component
@RefreshScope
public class OrderPolicy {

    private final int maximumItems;

    public OrderPolicy(
            @Value("${order.maximum-items}")
            int maximumItems
    ) {
        this.maximumItems = maximumItems;
    }

    public int getMaximumItems() {
        return maximumItems;
    }
}
```

Expose the refresh endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - refresh
```

After changing and committing the configuration, call:

```bash
curl -X POST http://order-service:8080/actuator/refresh
```

Refreshing clears refresh-scoped bean instances so that they can be recreated with the new configuration. Spring Cloud Context also supports rebinding configuration-property beans as part of refresh processing. ([Home][3])

### Important limitation

The refresh applies only to the instance receiving the request:

```text
Order service instance 1 → refreshed
Order service instance 2 → unchanged
Order service instance 3 → unchanged
```

---

## Refreshing all service instances with Spring Cloud Bus

Spring Cloud Bus connects application instances through a message broker such as RabbitMQ or Kafka.

```text
POST /actuator/busrefresh
              ↓
Spring Cloud Bus event
              ↓
RabbitMQ or Kafka
       ├── Instance 1 refreshes
       ├── Instance 2 refreshes
       └── Instance 3 refreshes
```

Expose the Bus endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - busrefresh
```

Trigger the broadcast:

```bash
curl -X POST \
  http://order-service:8080/actuator/busrefresh
```

The Bus refresh operation clears `RefreshScope` caches and rebinds configuration properties across the destination applications. ([Home][1])

A Config Server can also receive repository webhook notifications through its monitor integration and publish refresh events through Spring Cloud Bus. ([Home][4])

---

## Production considerations

Do not assume every property can be refreshed safely. Consider what happens when changing:

- Database URLs
- Connection-pool sizes
- Executor sizes
- Kafka consumer settings
- Security configuration
- Encryption keys
- Feature flags during active requests

For critical changes, a controlled rolling restart may be safer than runtime refresh.

The refresh endpoints should also be authenticated and restricted to administrative networks. Otherwise, an attacker could repeatedly recreate beans or change application behavior.

### Interview-ready answer

> Spring Cloud Config Server reads centralized configuration from an environment repository such as Git and exposes it by application, profile, and label. Clients import that configuration into their Spring environment. For one running instance, I use `@RefreshScope` or refreshable configuration properties and call `/actuator/refresh`. For multiple instances, I use Spring Cloud Bus with RabbitMQ or Kafka and call `/actuator/busrefresh`, while securing the management endpoints and validating that each property is safe to change dynamically.

---

# Q14: Spring Cloud Gateway vs Zuul 1

Historically, Spring Cloud Netflix Zuul 1 used the Servlet model and blocking I/O, normally assigning a request-processing thread to each active request. The original Spring Cloud Gateway was built on Spring WebFlux, Project Reactor, and commonly Reactor Netty, allowing non-blocking request processing. The Spring Cloud Zuul integration has been out of support for years, and Spring maintainers recommend migrating to Spring Cloud Gateway. ([GitHub][5])

However, saying that Spring Cloud Gateway is now **reactive only** is outdated. Current Spring Cloud Gateway provides full gateway server variants for both WebFlux and Spring Web MVC. ([Home][6])

## Comparison

| Zuul 1                                       | Spring Cloud Gateway                                      |
| -------------------------------------------- | --------------------------------------------------------- |
| Servlet-based                                | WebFlux or Web MVC variants                               |
| Blocking request model                       | Reactive non-blocking or Servlet model                    |
| Uses Zuul filters                            | Uses route predicates and gateway filters                 |
| Legacy Spring Cloud integration              | Current Spring Cloud gateway project                      |
| Limited alignment with modern reactive stack | Integrates with Reactor, LoadBalancer and Circuit Breaker |
| Spring integration is out of support         | Actively supported Spring project                         |

Netflix’s separate Zuul project still exists and has newer versions. It is specifically the old **Spring Cloud Netflix Zuul 1 integration** that should not be selected for a new Spring application. ([GitHub][7])

---

## Core Gateway concepts

### Route

A route defines where a matching request should be sent.

### Predicate

A predicate determines whether a request matches the route.

Examples:

- Path
- HTTP method
- Header
- Host
- Query parameter
- Date and time

### Filter

A filter modifies or controls the request and response.

Examples:

- Add or remove headers
- Strip path prefixes
- Authenticate requests
- Apply rate limiting
- Retry requests
- Apply a circuit breaker
- Rewrite paths

## Example route

The current WebFlux property structure is:

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: inventory-service
              uri: lb://inventory-service
              predicates:
                - Path=/inventory/**
              filters:
                - StripPrefix=1
                - name: CircuitBreaker
                  args:
                    name: inventoryCircuitBreaker
                    fallbackUri: forward:/fallback/inventory
```

Here:

```text
Path=/inventory/**
```

is the predicate, while `StripPrefix` and `CircuitBreaker` are filters.

Using:

```text
lb://inventory-service
```

allows Spring Cloud LoadBalancer to resolve a service instance through the configured discovery client.

## Rate limiting

Spring Cloud Gateway provides a `RequestRateLimiter` filter. When the request is rejected, the default response is `429 Too Many Requests`. ([Home][8])

```yaml
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 20
      redis-rate-limiter.burstCapacity: 40
```

## When to use which Gateway variant

Use the WebFlux gateway when:

- The gateway handles high concurrent I/O.
- Filters and integrations are non-blocking.
- Reactive networking fits the operational model.

Use the Web MVC gateway when:

- The team uses the Servlet stack.
- Existing filters or libraries are blocking.
- Reactive programming would add unnecessary complexity.

### Interview-ready answer

> Zuul 1 used a blocking Servlet request model, while the original Spring Cloud Gateway used WebFlux, Reactor, and non-blocking I/O. Spring Cloud’s Zuul integration is no longer supported. Current Spring Cloud Gateway supports both WebFlux and Web MVC variants and uses routes, predicates, and filters for routing, authentication, rate limiting, resilience, and request transformation.

---

# Q15: How do you implement a circuit breaker with Resilience4j?

A circuit breaker prevents an application from repeatedly calling a dependency that is already failing or responding too slowly.

```text
Application
    ↓
Circuit Breaker
    ├── Dependency healthy → call allowed
    └── Dependency unhealthy → fail fast
```

Resilience4j supports Spring Boot annotations and also integrates through the Spring Cloud Circuit Breaker abstraction for blocking and reactive applications. ([Home][9])

## Circuit breaker states

### `CLOSED`

Calls are allowed and their outcomes are recorded.

```text
Successful and failed calls
        ↓
Failure rate calculated
```

### `OPEN`

Calls are rejected immediately without invoking the dependency.

```text
Request
   ↓
Circuit OPEN
   ↓
CallNotPermittedException
```

### `HALF_OPEN`

After the open-state wait period, a limited number of test requests are allowed.

```text
Test calls succeed
→ CLOSED

Test calls fail
→ OPEN
```

---

## Annotation-based example

```java
@Service
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("http://inventory-service")
                .build();
    }

    @CircuitBreaker(
        name = "inventoryService",
        fallbackMethod = "inventoryFallback"
    )
    public InventoryResponse getInventory(String productId) {
        return restClient.get()
                .uri("/inventory/{productId}", productId)
                .retrieve()
                .body(InventoryResponse.class);
    }

    private InventoryResponse inventoryFallback(
            String productId,
            Exception exception
    ) {
        return InventoryResponse.temporarilyUnavailable(
                productId
        );
    }
}
```

A fallback method must have a compatible return type and the original method parameters, followed by an exception parameter. Resilience4j selects the closest matching fallback based on the exception type. ([resilience4j][10])

## Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      inventoryService:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 20
        minimum-number-of-calls: 10
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 2s
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 5
        automatic-transition-from-open-to-half-open-enabled: true
```

### Property meanings

| Property                                       | Meaning                                            |
| ---------------------------------------------- | -------------------------------------------------- |
| `sliding-window-size`                          | Number of recent calls considered                  |
| `minimum-number-of-calls`                      | Calls required before calculating the failure rate |
| `failure-rate-threshold`                       | Percentage at which the circuit opens              |
| `slow-call-duration-threshold`                 | Duration after which a call is considered slow     |
| `slow-call-rate-threshold`                     | Slow-call percentage that can open the circuit     |
| `wait-duration-in-open-state`                  | Time before trial calls are permitted              |
| `permitted-number-of-calls-in-half-open-state` | Number of test calls                               |

Spring Cloud Circuit Breaker also supports configuration through properties and factory customizers. ([Home][11])

---

## Circuit breaker is not a timeout

A circuit breaker observes results and rejects calls when failure thresholds are exceeded. It does not stop an individual request from waiting indefinitely.

Use it together with:

- Connection timeout
- Response or read timeout
- `TimeLimiter`
- Bulkhead
- Carefully controlled retry
- Rate limiting

```text
Timeout:
Limits one request's duration.

Circuit breaker:
Stops repeated calls to an unhealthy dependency.

Bulkhead:
Limits how many calls may run concurrently.
```

## Fallback cautions

A fallback should be semantically valid.

Reasonable fallbacks:

- Return cached read-only data.
- Return a temporary-unavailability result.
- Queue non-urgent work.
- Use an approved secondary provider.

Dangerous fallback:

```text
Inventory service unavailable
→ assume stock is available
```

This hides failure and can corrupt business behavior.

## Metrics

Spring Cloud Circuit Breaker can publish Resilience4j metrics when Actuator and the Micrometer integration are present. ([Home][12])

Monitor:

- Circuit state
- Failure rate
- Slow-call rate
- Rejected calls
- Fallback count
- Dependency latency

### Interview-ready answer

> I configure a Resilience4j circuit breaker around a remote dependency, define failure and slow-call thresholds, and provide a fallback only when it is business-safe. The circuit moves from closed to open when the threshold is exceeded, rejects calls while open, and later permits trial calls in half-open state. I combine it with explicit timeouts and bulkheads because a circuit breaker does not itself limit the duration or concurrency of an individual call.

---

# Q17: How do you implement distributed tracing with Sleuth and Zipkin?

For older Spring Boot systems, this was commonly implemented using Spring Cloud Sleuth and Zipkin.

For modern Spring Boot applications, the correct answer is:

```text
Spring Cloud Sleuth
        ↓ migrated to
Micrometer Tracing
        ↓
Brave or OpenTelemetry
        ↓
Zipkin, OTLP backend, or another tracing system
```

The Spring Cloud Sleuth project states that its core moved to Micrometer Tracing and directs users to the migration guide. Spring Boot now auto-configures Micrometer Tracing and supports OpenTelemetry with OTLP as well as OpenZipkin Brave with Zipkin. ([Home][13])

---

## Trace and span

### Trace

Represents the complete distributed operation.

```text
Create order request
├── API gateway
├── Order service
├── Inventory service
└── Payment service
```

All spans normally share the same trace ID.

### Span

Represents one operation inside the trace.

```text
Trace ID:  abc123

Span 1: API Gateway
Span 2: Order database query
Span 3: Inventory HTTP call
Span 4: Payment authorization
```

Each span contains information such as:

- Trace ID
- Span ID
- Parent span ID
- Start and end time
- Operation name
- Status
- Low-cardinality attributes
- Events

---

## Modern Spring Boot setup

Spring Boot’s current tracing stack supports dedicated integrations for OpenTelemetry with OTLP and Brave with Zipkin. The exact dependencies should match the Spring Boot release line used by the project. ([Home][14])

A common configuration is:

```yaml
management:
  tracing:
    sampling:
      probability: 0.1
```

A probability of `0.1` samples approximately ten percent of root traces. Current Spring Boot documentation uses ten percent as its default to avoid overwhelming the tracing backend. ([Home][14])

For local debugging, a higher value may be used:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
```

Avoid collecting every trace in a high-volume production system without estimating the cost.

---

## Context propagation

When one service calls another, tracing headers carry the trace context.

```text
Order Service
Trace ID: abc123
Span ID:  111
        ↓ HTTP tracing headers
Inventory Service
Trace ID: abc123
Span ID:  222
Parent:   111
```

Common propagation formats include W3C Trace Context and B3. The selected format depends on the tracing implementation and configuration.

For automatic HTTP propagation, construct clients using Spring Boot’s auto-configured builders:

```java
@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("http://inventory-service")
                .build();
    }
}
```

Spring Boot documents automatic propagation for clients created with the auto-configured `RestTemplateBuilder`, `RestClient.Builder`, and `WebClient.Builder`. Creating these clients manually can bypass the tracing customizers. ([Home][14])

---

## Log correlation

Micrometer Tracing adds trace and span identifiers to logging context so that application logs can be correlated with a distributed trace.

```text
[order-service,abc123,111] Creating order
[inventory-service,abc123,222] Reserving stock
[payment-service,abc123,333] Authorizing payment
```

Spring Boot includes correlation IDs based on `traceId` and `spanId` in logging output when Micrometer Tracing is configured. ([Home][14])

## Async context propagation

Trace context must be propagated when work crosses:

- Executor threads
- `CompletableFuture`
- Reactive boundaries
- Message brokers
- Scheduled jobs

Framework-managed integrations often propagate context automatically. Raw executors or manually created clients may require explicit context propagation.

Do not place sensitive or high-cardinality information into span attributes or baggage, such as:

- Full tokens
- Passwords
- Complete request bodies
- Card numbers
- Arbitrary user IDs as metric dimensions

### Interview-ready answer

> In older systems I used Spring Cloud Sleuth with Zipkin. In modern Spring Boot I use Micrometer Tracing with either Brave and Zipkin or OpenTelemetry and OTLP. Each request receives a trace ID, each operation gets a span ID, and context is propagated through HTTP or messaging headers. I build HTTP clients from Spring Boot’s auto-configured builders, configure an appropriate sampling rate, correlate logs with trace and span IDs, and verify context propagation across asynchronous boundaries.

---

# Q18: Feign Client vs `RestTemplate` vs `WebClient`

A modern comparison should also include `RestClient`, because current Spring Framework documentation deprecates `RestTemplate` in favor of `RestClient` for new synchronous code. ([Home][15])

| Client         | Programming model           | Best use                          |
| -------------- | --------------------------- | --------------------------------- |
| OpenFeign      | Declarative interface       | Simple service-to-service clients |
| `RestClient`   | Synchronous, fluent         | New blocking applications         |
| `RestTemplate` | Synchronous, template-based | Existing legacy code              |
| `WebClient`    | Reactive and non-blocking   | Reactive flows and streaming      |

---

## OpenFeign

Feign defines the HTTP client as an interface.

```java
@FeignClient(
    name = "inventory-service",
    path = "/api/inventory"
)
public interface InventoryClient {

    @GetMapping("/{productId}")
    InventoryResponse getInventory(
            @PathVariable String productId
    );
}
```

Enable scanning:

```java
@SpringBootApplication
@EnableFeignClients
public class OrderApplication {
}
```

Spring Cloud OpenFeign integrates Spring MVC annotations, message conversion, service discovery, Spring Cloud LoadBalancer, and optionally Spring Cloud Circuit Breaker. ([Home][16])

### Advantages

- Minimal boilerplate
- Easy-to-read client contracts
- Discovery and load-balancing integration
- Good for conventional JSON APIs

### Risks

- Network calls can look like ordinary method calls.
- Developers may forget latency and failure handling.
- Large interfaces can hide coupling.
- Streaming and highly customized protocols may be less natural.

---

## `RestClient`

`RestClient` is the modern synchronous Spring HTTP client.

```java
InventoryResponse response = restClient.get()
        .uri("/inventory/{id}", productId)
        .retrieve()
        .body(InventoryResponse.class);
```

Use it when:

- The application uses Spring MVC or another blocking model.
- The service requires direct control over requests.
- A declarative Feign interface is unnecessary.
- The call is synchronous by design.

Current Spring documentation identifies `RestClient` as the fluent synchronous client and `RestTemplate` as deprecated in its favor. ([Home][15])

---

## `RestTemplate`

```java
InventoryResponse response =
        restTemplate.getForObject(
                "/inventory/{id}",
                InventoryResponse.class,
                productId
        );
```

It remains common in older applications, but new synchronous implementations should generally use `RestClient` rather than introducing additional `RestTemplate` usage. ([Home][15])

---

## `WebClient`

```java
Mono<InventoryResponse> response =
        webClient.get()
                .uri("/inventory/{id}", productId)
                .retrieve()
                .bodyToMono(InventoryResponse.class);
```

Use it when:

- The complete request path is reactive.
- Non-blocking I/O is required.
- Streaming responses are involved.
- High concurrent I/O is expected.
- Reactive composition and backpressure are useful.

Calling:

```java
response.block();
```

turns that part of the workflow into a blocking call. Using `WebClient` does not make blocking database drivers or blocking business logic non-blocking.

---

## Decision guide

```text
Simple declarative internal API
→ OpenFeign

New synchronous Spring MVC client
→ RestClient

Existing legacy client
→ RestTemplate

Reactive or streaming workflow
→ WebClient
```

Regardless of the client, configure:

- Connection timeout
- Response timeout
- Connection pool
- Maximum response size
- Authentication
- Tracing
- Circuit breaker
- Safe retry policy
- Idempotency for retried writes

### Interview-ready answer

> Feign is a declarative client where I define the remote contract as an interface. `RestClient` is the preferred modern synchronous client. `RestTemplate` is the older blocking client and is now deprecated for new use. `WebClient` is reactive and non-blocking, so I use it when the complete flow benefits from reactive I/O or streaming. The choice depends on the application’s thread model, not only on syntax.

---

# Q20: What is `@EnableDiscoveryClient`, and how does Eureka registration work internally?

`@EnableDiscoveryClient` historically enabled service-discovery integration. In modern Spring Cloud, it is no longer required. Adding a supported `DiscoveryClient` implementation to the classpath is enough for auto-registration by default. ([Home][17])

## Client configuration

```yaml
spring:
  application:
    name: order-service

eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka/
```

A modern application generally does not need:

```java
@EnableDiscoveryClient
```

The annotation can still be used when explicit configuration such as disabling auto-registration is required.

---

## Registration process

### 1. Application starts

The Eureka client builds instance metadata such as:

- Application name
- Instance ID
- Hostname or IP
- Port
- Status
- Health and status URLs
- Metadata

### 2. Client registers

```text
Order service instance
        ↓ registration request
Eureka Server registry
```

The server records the instance under the application name.

### 3. Client renews its lease

The client periodically sends heartbeat requests to tell Eureka that it remains alive.

```text
Order service
    ↓ heartbeat
Eureka Server
    ↓ renew lease
```

If renewals stop for long enough, the server may evict the instance. Eureka’s client/server documentation describes registration, lease renewal, periodic registry fetching, and cancellation during graceful shutdown. ([GitHub][18])

### 4. Clients fetch the registry

A Eureka client periodically downloads registry information and caches it locally.

```text
Eureka Server
      ↓ registry
Order service local cache
      ↓
inventory-service instances
```

This allows service lookup without contacting Eureka for every business request.

### 5. Load balancer selects an instance

```text
inventory-service
    ├── 10.0.1.12:8080
    ├── 10.0.1.13:8080
    └── 10.0.1.14:8080
```

Spring Cloud LoadBalancer selects one available instance for the request.

### 6. Graceful shutdown deregisters

During a normal shutdown, the client sends a cancellation request so the server can remove it promptly. This is best effort; a crashed process cannot deregister gracefully. ([GitHub][18])

---

## Self-preservation mode

During a network partition, many healthy services might suddenly stop sending heartbeats even though they are still running.

Without protection:

```text
Network problem
→ heartbeats disappear
→ Eureka evicts many healthy instances
→ registry becomes empty
```

Self-preservation mode reduces mass eviction when Eureka detects an unexpectedly large drop in renewals. This favors availability and may temporarily retain stale registrations. ([GitHub][19])

Consequently, Eureka discovery is eventually consistent:

- New registrations may take time to appear.
- Stopped instances may remain visible briefly.
- Clients should use connection timeouts and retry another instance where safe.
- Discovery should not be treated as proof that a dependency is currently healthy.

### Interview-ready answer

> `@EnableDiscoveryClient` historically enabled service registration, but modern Spring Cloud auto-configures discovery when an implementation such as Eureka is on the classpath. A Eureka client registers its instance metadata, periodically renews its lease, fetches and caches the registry, and deregisters during graceful shutdown. Eureka’s self-preservation mode avoids evicting large numbers of instances during network failures, so discovery favors availability and can temporarily contain stale entries.

---

# Quick Interview Cheat Sheet

```text
Spring Cloud Config:
Central configuration by application, profile, and label.

Single-instance refresh:
POST /actuator/refresh

Cluster refresh:
POST /actuator/busrefresh
through RabbitMQ or Kafka.

Gateway:
Routes + predicates + filters.
Current versions support WebFlux and Web MVC.

Zuul 1:
Legacy blocking Servlet gateway integration.
No longer supported by Spring Cloud.

Circuit breaker:
CLOSED → OPEN → HALF_OPEN.
Fail fast when a dependency is unhealthy.

Tracing:
Modern stack is Micrometer Tracing.
Sleuth is the older stack.

Feign:
Declarative HTTP client.

RestClient:
Modern synchronous HTTP client.

RestTemplate:
Legacy synchronous client.

WebClient:
Reactive and non-blocking client.

Eureka:
Register → heartbeat → fetch registry → deregister.

@EnableDiscoveryClient:
No longer required in modern Spring Cloud.

Self-preservation:
Avoids mass eviction during network failures.
```

[1]: https://docs.spring.io/spring-cloud-bus/docs/current/reference/html/?utm_source=chatgpt.com "Spring Cloud Bus"
[2]: https://docs.spring.io/spring-cloud-config/reference/client.html?utm_source=chatgpt.com "Spring Cloud Config Client"
[3]: https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/application-context-services.html?utm_source=chatgpt.com "Application Context Services :: Spring Cloud Commons"
[4]: https://docs.spring.io/spring-cloud-config/reference/server/push-notifications-and-bus.html?utm_source=chatgpt.com "Push Notifications and Spring Cloud Bus"
[5]: https://github.com/spring-cloud/spring-cloud-netflix/issues/4158?utm_source=chatgpt.com "spring-cloud-starter-netflix-zuul not work with spring-boot ..."
[6]: https://docs.spring.io/spring-cloud-gateway/reference/index.html?utm_source=chatgpt.com "Spring Cloud Gateway"
[7]: https://github.com/netflix/zuul?utm_source=chatgpt.com "Netflix/zuul: Zuul is a gateway service that ..."
[8]: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html?utm_source=chatgpt.com "RequestRateLimiter GatewayFilter Factory"
[9]: https://docs.spring.io/spring-cloud-circuitbreaker/reference/spring-cloud-circuitbreaker-resilience4j.html?utm_source=chatgpt.com "Configuring Resilience4J Circuit Breakers"
[10]: https://resilience4j.readme.io/docs/getting-started-3?utm_source=chatgpt.com "Getting Started - resilience4j - ReadMe"
[11]: https://docs.spring.io/spring-cloud-circuitbreaker/reference/spring-cloud-circuitbreaker-resilience4j/circuit-breaker-properties-configuration.html?utm_source=chatgpt.com "Circuit Breaker Properties Configuration"
[12]: https://docs.spring.io/spring-cloud-circuitbreaker/reference/spring-cloud-circuitbreaker-resilience4j/collecting-metrics.html?utm_source=chatgpt.com "Collecting Metrics :: Spring Cloud Circuitbreaker"
[13]: https://docs.spring.io/spring-cloud-sleuth/docs/current/reference/html/index.html?utm_source=chatgpt.com "Spring Cloud Sleuth Reference Documentation"
[14]: https://docs.spring.io/spring-boot/reference/actuator/tracing.html "Tracing :: Spring Boot"
[15]: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html "REST Clients :: Spring Framework"
[16]: https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com "Spring Cloud OpenFeign Features"
[17]: https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/common-abstractions.html?utm_source=chatgpt.com "Spring Cloud Commons: Common Abstractions"
[18]: https://github.com/Netflix/eureka/wiki/UQBNOrnQlzo3ftqm0Jj5Sf9zEHlPApapd-rWsAHREzkweiTw "Understanding eureka client server communication · Netflix/eureka Wiki · GitHub"
[19]: https://github.com/Netflix/eureka/wiki/Server-Self-Preservation-Mode "Server Self Preservation Mode · Netflix/eureka Wiki · GitHub"
