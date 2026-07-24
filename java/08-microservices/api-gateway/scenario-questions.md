# Scenario-Based Questions — API Gateway and Rate Limiting

The repeated questions are consolidated into two connected design problems:

1. How to design and scale an API rate limiter.
2. What responsibilities an API Gateway should handle.

---

# Scenario 1: Design a Rate Limiter for APIs

## The problem

A public API is receiving:

- Legitimate traffic spikes
- Accidental retry loops
- Aggressive integrations
- Automated abuse
- Expensive repeated requests
- Traffic from multiple gateway instances

The rate limiter must protect downstream services without unfairly blocking legitimate users.

```text
Client
   ↓
Edge protection
   ↓
API Gateway
   ↓
Rate-limit decision
   ├── Allowed → route to service
   └── Rejected → return HTTP 429
```

Spring Cloud Gateway’s `RequestRateLimiter` filter makes an allow-or-deny decision for a request and returns `429 Too Many Requests` by default when the request is rejected. ([Home][1])

---

# 1. Clarify the Requirements First

Before selecting an algorithm, clarify:

| Requirement       | Example                                          |
| ----------------- | ------------------------------------------------ |
| Limit subject     | User, tenant, API key, IP or client application  |
| Limit scope       | One endpoint, one service or the entire platform |
| Rate              | 100 requests per minute                          |
| Burst allowance   | Up to 20 requests immediately                    |
| Request cost      | Search costs more than health check              |
| Enforcement point | Gateway, service or both                         |
| Distribution      | One instance, cluster or multiple regions        |
| Failure policy    | Fail open or fail closed                         |
| Response contract | `429`, retry time and quota information          |

“Allow 100 requests per minute” is incomplete without identifying **100 requests for whom and for which operation**.

---

# 2. Which Rate-Limiting Algorithm Should You Use?

## Fixed Window Counter

Count requests in a fixed interval.

```text
Window:
10:00:00–10:00:59

Limit:
100 requests
```

Conceptual implementation:

```text
Key: rate:user-100:202607221000
Value: 74
TTL: 60 seconds
```

### Advantages

- Simple
- Low memory use
- Easy to implement with Redis `INCR` and expiry

### Disadvantage: boundary burst

A client can send:

```text
100 requests at 10:00:59
+
100 requests at 10:01:00
=
200 requests in approximately one second
```

Use it for simple quotas where boundary bursts are acceptable.

---

## Sliding Window Log

Store the timestamp of every accepted request.

```text
Current time: 10:01:30

Count requests since:
10:00:30
```

### Advantages

- Accurate rolling-window enforcement
- No large fixed-window boundary burst

### Disadvantages

- Stores many timestamps
- More expensive for high-volume APIs
- Requires old entries to be removed

It is useful where precise limits matter and traffic volume is manageable.

---

## Sliding Window Counter

Approximate a sliding window by combining the current and previous fixed-window counters.

```text
Estimated usage =
current window count
+
weighted previous window count
```

It is less expensive than storing every request timestamp while being smoother than a basic fixed window.

---

## Token Bucket

Tokens are added to a bucket at a fixed rate. Each request consumes one or more tokens.

```text
Bucket capacity:       20 tokens
Refill rate:           10 tokens/second
Normal request cost:    1 token
Expensive request cost: 5 tokens
```

Flow:

```text
Request arrives
    ↓
Enough tokens?
├── Yes → remove tokens and allow
└── No  → reject with 429
```

### Advantages

- Allows controlled bursts
- Enforces a sustainable long-term rate
- Supports different request costs
- Efficient for online APIs
- Easy to explain and configure

### Disadvantages

- Requires atomic token updates
- Distributed implementation needs shared state
- Incorrect time calculations can over-issue tokens

Spring Cloud Gateway’s Redis rate limiter implements a token-bucket algorithm with:

- `replenishRate` for the refill rate
- `burstCapacity` for bucket capacity
- `requestedTokens` for the cost of one request ([Home][1])

### Recommended default

For most public APIs, **Token Bucket is the strongest general-purpose choice** because it permits legitimate short bursts while still enforcing a stable long-term rate.

---

## Leaky Bucket

Requests enter a bounded queue and are processed at a controlled rate.

```text
Incoming requests
      ↓
Bounded queue
      ↓ fixed processing rate
Downstream service
```

### Advantages

- Produces a smooth output rate
- Protects a downstream dependency from bursts
- Useful for queued jobs and message processing

### Disadvantages

- Introduces queueing latency
- Requires a bounded queue
- Requests may wait before execution
- Queue overflow still needs rejection or shedding

### Token Bucket vs Leaky Bucket

| Token Bucket                                | Leaky Bucket                         |
| ------------------------------------------- | ------------------------------------ |
| Allows controlled bursts                    | Smooths traffic into a constant rate |
| Usually rejects when tokens are unavailable | May queue before processing          |
| Good for synchronous APIs                   | Good for queued work                 |
| Controls admission rate                     | Controls output rate                 |
| Lower queueing latency                      | Can introduce waiting                |

For a synchronous REST API, I would usually choose **Token Bucket**. For a background-processing pipeline where output must be smoothed, I would consider **Leaky Bucket or a bounded queue**.

---

## Concurrency Limiter

A request-rate limit does not fully protect expensive, long-running operations.

For example:

```text
Limit:
100 requests/second

Each request lasts:
30 seconds
```

The service could still accumulate thousands of active requests.

A concurrency limiter controls the number of simultaneous operations:

```text
Maximum active report generations: 10
```

Use rate and concurrency limits together for expensive endpoints:

```text
Rate limit
→ controls arrivals over time

Concurrency limit
→ controls active work
```

---

# 3. Where Should Rate Limiting Be Implemented?

## At the edge or WAF

Use edge-level controls for:

- Network-level floods
- Clearly malicious IPs
- Geographic restrictions
- Bot traffic
- Very coarse anonymous limits

The goal is to reject harmful traffic before it consumes gateway or application resources.

---

## At the API Gateway

The gateway is the best place for general external API quotas:

- Per authenticated user
- Per OAuth client
- Per API key
- Per tenant
- Per route
- Per pricing plan

```text
Internet
   ↓
Gateway
   ├── Tenant A: 1,000 requests/minute
   ├── Tenant B: 10,000 requests/minute
   └── Anonymous: 30 requests/minute
```

Advantages:

- Centralized policy
- Rejection before downstream processing
- Consistent response format
- Easier cross-service quota management
- One place for monitoring abuse

---

## Inside each service

Service-level limits are still necessary for:

- Internal traffic that bypasses the gateway
- Business-specific quotas
- Expensive operations
- Per-resource concurrency
- Third-party dependency limits
- Sensitive operations such as login or payment

Example:

```text
Gateway:
1,000 requests/minute per tenant

Report Service:
Maximum 5 concurrent reports per tenant
```

The gateway understands traffic policy, while the service understands the true cost and business meaning of the operation.

---

## Recommended layered design

| Layer             | Responsibility                                |
| ----------------- | --------------------------------------------- |
| CDN/WAF           | Network abuse and coarse IP controls          |
| API Gateway       | User, client, tenant and route quotas         |
| Service           | Business-specific rate and concurrency limits |
| Queue/consumer    | Backpressure and processing-rate control      |
| Downstream client | Protect third-party quotas                    |

> I would enforce broad external quotas at the gateway and retain service-level protection for expensive or bypassable operations.

---

# 4. Choosing the Rate-Limit Key

The limiter must identify whose quota is being consumed.

Good keys include:

```text
user:{userId}
client:{oauthClientId}
tenant:{tenantId}
api-key:{apiKeyId}
route:{routeId}:user:{userId}
```

## Prefer authenticated identity

```text
tenant:T-100:user:U-200:route:payment
```

This is usually more reliable than IP-based limiting.

## IP-based fallback

IP limits are useful for unauthenticated endpoints such as:

- Login
- Registration
- Password reset
- Public search

However:

- Many users may share one corporate IP.
- Mobile users may change IPs.
- Attackers may rotate addresses.
- Proxy headers can be forged.

Do not trust arbitrary `X-Forwarded-For` values. Use forwarded-address information only when it has been sanitized by a trusted load balancer or proxy.

---

# 5. Thread-Safe Token Bucket in Java

The following implementation works for one JVM instance:

```java
import java.time.Duration;

public final class TokenBucketRateLimiter {

    private final long capacity;
    private final double tokensPerNanosecond;

    private double availableTokens;
    private long lastRefillNanos;

    public TokenBucketRateLimiter(
            long capacity,
            double refillTokensPerSecond
    ) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "capacity must be greater than zero"
            );
        }

        if (refillTokensPerSecond <= 0) {
            throw new IllegalArgumentException(
                    "refillTokensPerSecond must be greater than zero"
            );
        }

        this.capacity = capacity;
        this.tokensPerNanosecond =
                refillTokensPerSecond / 1_000_000_000.0;

        this.availableTokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    public synchronized Decision tryAcquire(
            long requestedTokens
    ) {
        if (requestedTokens <= 0 ||
                requestedTokens > capacity) {

            throw new IllegalArgumentException(
                    "requestedTokens must be between 1 and capacity"
            );
        }

        refill();

        if (availableTokens >= requestedTokens) {
            availableTokens -= requestedTokens;

            return new Decision(
                    true,
                    (long) Math.floor(availableTokens),
                    Duration.ZERO
            );
        }

        double missingTokens =
                requestedTokens - availableTokens;

        long retryAfterNanos = (long) Math.ceil(
                missingTokens / tokensPerNanosecond
        );

        return new Decision(
                false,
                (long) Math.floor(availableTokens),
                Duration.ofNanos(retryAfterNanos)
        );
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillNanos;

        if (elapsedNanos <= 0) {
            return;
        }

        double generatedTokens =
                elapsedNanos * tokensPerNanosecond;

        availableTokens = Math.min(
                capacity,
                availableTokens + generatedTokens
        );

        lastRefillNanos = now;
    }

    public record Decision(
            boolean allowed,
            long remainingTokens,
            Duration retryAfter
    ) {
    }
}
```

Usage:

```java
TokenBucketRateLimiter limiter =
        new TokenBucketRateLimiter(
                20,
                10
        );

TokenBucketRateLimiter.Decision decision =
        limiter.tryAcquire(1);

if (!decision.allowed()) {
    throw new RateLimitExceededException(
            decision.retryAfter()
    );
}
```

## Why this implementation is thread-safe

The mutable token state is protected by:

```java
synchronized
```

`System.nanoTime()` is used to measure elapsed time rather than wall-clock time.

## Limitation

This limiter is local to one JVM:

```text
Gateway Instance 1 → Bucket A
Gateway Instance 2 → Bucket B
Gateway Instance 3 → Bucket C
```

A client could consume the limit independently on each instance. It is suitable for:

- A single-instance application
- Unit tests
- Local protection
- Per-instance concurrency protection

It does not enforce a globally consistent distributed quota.

---

# 6. Scaling the Rate Limiter Across Instances

## Shared Redis state

For multiple gateway or service instances, store limiter state in Redis:

```text
Gateway Instance 1 ─┐
Gateway Instance 2 ─┼──→ Redis rate-limit state
Gateway Instance 3 ─┘
```

Redis can enforce per-user, per-API, and per-tenant quotas consistently across distributed instances. Its Lua scripting support keeps the read–decide–update operation atomic, preventing concurrent requests from spending the same tokens. ([Redis][2])

## Do not perform separate read and write operations

Unsafe:

```text
GET current tokens
Calculate remaining tokens
SET new token count
```

Two gateway instances may read the same value before either writes it.

Use:

- An atomic Lua script
- A proven Redis-backed rate limiter
- Spring Cloud Gateway’s Redis rate limiter
- A distributed Bucket4j backend

---

# 7. Spring Cloud Gateway Implementation

Current Spring Cloud Gateway provides WebFlux and Web MVC gateway variants. The full gateway supports route predicates, filters, and cross-cutting concerns such as security, monitoring and resilience. ([Home][3])

The example below uses the WebFlux variant.

## Dependencies

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>
        spring-cloud-starter-gateway-server-webflux
    </artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>
        spring-boot-starter-data-redis-reactive
    </artifactId>
</dependency>
```

The current WebFlux starter is `spring-cloud-starter-gateway-server-webflux`, and the Redis limiter requires reactive Spring Data Redis. ([Home][4])

---

## Key resolver

```java
import java.net.InetSocketAddress;
import java.security.Principal;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfiguration {

    @Bean
    KeyResolver rateLimitKeyResolver() {
        return exchange -> exchange
                .getPrincipal()
                .map(Principal::getName)
                .map(name -> "principal:" + name)
                .switchIfEmpty(
                        Mono.fromSupplier(() -> {
                            InetSocketAddress address =
                                    exchange.getRequest()
                                            .getRemoteAddress();

                            if (address == null ||
                                    address.getAddress() == null) {
                                return "anonymous";
                            }

                            return "ip:" +
                                    address.getAddress()
                                           .getHostAddress();
                        })
                );
    }
}
```

For an authenticated system, a better production key may combine tenant and user identity:

```text
tenant:T-100:user:U-200
```

Spring Cloud Gateway obtains limiter keys through a `KeyResolver`; its default resolver uses the authenticated principal’s name. ([Home][1])

---

## Gateway route configuration

```yaml
spring:
  data:
    redis:
      host: redis
      port: 6379

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
                - name: RequestRateLimiter
                  args:
                    key-resolver: "#{@rateLimitKeyResolver}"
                    redis-rate-limiter.replenishRate: 10
                    redis-rate-limiter.burstCapacity: 20
                    redis-rate-limiter.requestedTokens: 1
```

This configuration means:

```text
Sustained rate:
10 tokens per second

Maximum accumulated capacity:
20 tokens

Cost of one request:
1 token
```

Therefore, a client can temporarily send a burst of 20 requests, but the bucket refills at only 10 tokens per second. These parameters match the current Spring Cloud Gateway Redis rate-limiter model. ([Home][1])

---

# 8. Weighted Requests

Not every endpoint has equal cost.

Example policy:

| Operation            | Token cost |
| -------------------- | ---------: |
| Health check         |          1 |
| Read order           |          1 |
| Search catalog       |          3 |
| Generate report      |         10 |
| Export large dataset |         20 |

```text
Client has 100 tokens/minute.

100 lightweight requests
or
10 report requests
```

This protects expensive operations more accurately than treating every request equally.

Spring Cloud Gateway supports `requestedTokens`, which determines how many tokens one request consumes. ([Home][1])

For route-specific costs, configure separate routes or a custom rate-limiter policy.

---

# 9. Rate-Limit Response Design

When a request exceeds its quota, return:

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/problem+json
Retry-After: 5
```

```json
{
  "type": "https://api.example.com/problems/rate-limit",
  "title": "Rate limit exceeded",
  "status": 429,
  "detail": "The request quota has been exceeded.",
  "retryAfterSeconds": 5,
  "traceId": "18e71c5a"
}
```

RFC 6585 defines `429 Too Many Requests` for rate limiting and allows the response to include `Retry-After` to indicate how long the client should wait. ([IETF Datatracker][5])

Clients should not retry immediately in a tight loop. They should respect the retry instruction and apply bounded backoff.

---

# 10. API Gateway Responsibilities

An API Gateway should mainly handle **cross-cutting edge concerns**.

## Appropriate responsibilities

### Routing

```text
/api/orders/**     → Order Service
/api/payments/**   → Payment Service
/api/inventory/**  → Inventory Service
```

### Authentication

- Validate OAuth access tokens
- Validate JWT issuer and audience
- Check token expiration
- Reject malformed credentials

### Coarse authorization

```text
/api/admin/** requires ADMIN
/api/orders/** requires orders.read
```

Backend services must still enforce:

- Resource ownership
- Tenant isolation
- Business-state rules
- Object-level permissions

### Rate limiting

- Per user
- Per tenant
- Per client
- Per API key
- Per route

### Load balancing

```text
lb://order-service
```

resolves and selects a service instance.

### Request protection

- Maximum body size
- Header-size limits
- Allowed methods
- CORS policy
- Content-type validation

### Request and response transformation

- Path rewriting
- Header normalization
- Removing sensitive response headers
- Temporary compatibility transformations

### Resilience

- Connection and response timeouts
- Circuit breakers
- Carefully controlled retries
- Safe fallback routing

### Observability

- Route ID
- Trace ID
- Latency
- Status code
- Rejection count
- Downstream failure count

Spring Cloud Gateway route filters can modify incoming requests and outgoing responses and are scoped to routes. ([Home][6])

---

## Responsibilities that should remain outside the gateway

Do not place the following in the gateway:

- Payment calculation
- Inventory reservation
- Database access
- Order state transitions
- Saga orchestration
- Detailed domain authorization
- Large business transformations
- Long-running workflows

Bad design:

```text
API Gateway
├── calculates price
├── reserves inventory
├── creates order
├── charges payment
└── writes database records
```

The gateway would become a new distributed monolith and a release bottleneck.

---

# 11. How Does the Gateway Help with Authentication and Routing?

## Authentication flow

```text
Client
   ↓ Authorization: Bearer <token>
Gateway
   ├── verifies signature
   ├── validates issuer
   ├── validates audience
   ├── validates expiry
   └── checks coarse permissions
   ↓
Backend service
   └── checks resource-level authorization
```

## Routing flow

```text
GET /api/orders/O-100
       ↓
Path predicate matches /api/orders/**
       ↓
Gateway selects order-service route
       ↓
Load balancer selects an instance
       ↓
Request forwarded to Order Service
```

This allows external clients to use one stable API even when internal service locations and replica counts change.

---

# 12. Should You Use Spring Cloud Gateway?

Spring Cloud Gateway is a suitable choice when:

- The platform already uses Spring.
- Custom Java filters are required.
- Routes use Spring service discovery.
- OAuth and Spring Security integration are important.
- The team needs route-specific resilience or transformations.
- A self-managed gateway is operationally acceptable.

A managed cloud gateway or infrastructure gateway may be preferable when:

- The platform primarily needs standard routing and quotas.
- The organization does not want to operate a gateway application.
- Global edge protection is required.
- Most controls already exist in the ingress platform.
- Non-Java platform ownership is preferred.

Do not introduce Spring Cloud Gateway merely because the architecture uses microservices. It should provide enough routing, security, traffic-management or compatibility value to justify another critical runtime component.

---

# 13. Handling an Active Abuse Incident

## Immediate containment

1. Identify the route and identity causing the traffic.
2. Reduce limits for the affected endpoint.
3. Block clearly malicious API keys or accounts.
4. Apply coarse WAF or edge controls.
5. Protect downstream connection pools and queues.
6. Disable expensive optional operations if required.
7. Monitor whether attackers rotate identities or addresses.

## Do not depend only on IP limits

Attackers may use:

- Distributed addresses
- Proxies
- Compromised accounts
- Multiple API keys

Use several dimensions:

```text
IP
+
account
+
client ID
+
tenant
+
route
+
global system quota
```

## Make rejection cheap

Under a severe attack, even generating a complete `429` response consumes resources. RFC 6585 notes that a server under attack may choose to drop connections or take other measures instead of responding to every excessive request. ([IETF Datatracker][5])

---

# 14. Distributed Scaling Concerns

## Stateless gateway instances

Gateway instances should be horizontally scalable:

```text
Load Balancer
   ├── Gateway 1
   ├── Gateway 2
   └── Gateway 3
```

Limiter state should not live only in gateway memory when one cluster-wide quota is required.

---

## Redis availability

The rate limiter becomes part of the synchronous request path.

You must define what happens if Redis is unavailable.

### Fail open

```text
Limiter unavailable
→ allow request
```

Advantages:

- Preserves API availability

Risk:

- Downstream systems lose protection

Suitable for lower-risk reads where availability is more important.

### Fail closed

```text
Limiter unavailable
→ reject request
```

Advantages:

- Prevents quota bypass
- Protects expensive or sensitive resources

Risk:

- Redis failure blocks legitimate traffic

Suitable for high-cost or abuse-sensitive operations.

Policies may differ by endpoint:

| Endpoint            | Possible policy                      |
| ------------------- | ------------------------------------ |
| Public catalog read | Fail open with emergency local limit |
| Login               | Fail closed or heavily restricted    |
| Payment submission  | Fail closed                          |
| Health check        | Bypass limiter                       |
| Report generation   | Fail closed                          |

---

## Hot keys

A global key such as:

```text
global:all-requests
```

may receive every limiter operation and become a hotspot.

Prefer hierarchical limits:

```text
region:{region}
tenant:{tenantId}
user:{userId}
route:{routeId}
```

Global capacity can also be protected through infrastructure-level admission controls rather than one extremely hot Redis key.

---

## Multi-region systems

Possible approaches include:

### Regional quotas

```text
Global tenant quota: 10,000/minute

Region A allowance: 5,000
Region B allowance: 5,000
```

This reduces cross-region latency but may waste unused regional allowance.

### Central global limiter

Provides stronger global enforcement but adds cross-region latency and a larger dependency boundary.

### Approximate distributed limit

Each region enforces a local quota and synchronizes usage asynchronously. This improves availability but may temporarily exceed the exact global limit.

The correct choice depends on whether the requirement is:

- Exact billing enforcement
- Abuse protection
- Fairness
- Downstream capacity protection
- Approximate traffic shaping

---

# 15. Monitoring the Rate Limiter

Track:

- Allowed requests
- Rejected requests
- Rejection rate by route
- Rejection rate by tenant
- Remaining-token distribution
- Redis latency
- Redis errors
- Fail-open events
- Fail-closed events
- Hot keys
- Gateway P95 and P99 latency
- Downstream saturation
- Retry volume after `429`

Useful alert examples:

```text
Rate-limit rejections suddenly increase 10×
```

This may mean:

- Active abuse
- A client retry loop
- Incorrectly reduced limits
- A new traffic pattern
- A broken key resolver

A low rejection rate does not automatically mean the policy is effective. The allowed rate may still exceed downstream capacity.

---

# 16. Common Mistakes

## Using only local counters in a gateway cluster

Each gateway instance enforces its own separate quota.

## Rate limiting only by IP

Shared networks punish legitimate users, while distributed attackers bypass the limit.

## Trusting client-provided identity headers

A client can forge:

```http
X-User-Id: premium-customer
```

Derive identity from validated authentication or trusted infrastructure.

## Using an unbounded map for client buckets

```java
Map<String, TokenBucketRateLimiter> limiters;
```

Without expiry or size limits, attackers can generate unlimited keys and cause a memory leak.

## Returning `500` for rate limiting

Rate limiting is not an internal server error. Use `429`.

## Retrying a rejected request immediately

This creates more traffic and may extend the overload.

## Applying one limit to every endpoint

A health check and a report export do not have the same cost.

## Relying only on the gateway

Internal callers or alternative routes may bypass it.

## Treating rate limiting as complete DDoS protection

Application-level rate limiting occurs after some network and infrastructure resources have already been consumed. Severe attacks need edge and infrastructure controls.

## Putting business logic in the gateway

A gateway should remain focused on edge concerns.

---

# Interview Questions and Answers

## Q1: Which algorithm would you choose?

> For a normal synchronous API, I would generally choose Token Bucket because it supports a stable long-term rate while allowing controlled bursts. For queued processing where output must be smoothed, I would consider Leaky Bucket. For expensive long-running operations, I would add a concurrency limit.

## Q2: Where would you implement rate limiting?

> I would use layered enforcement. The edge handles coarse malicious traffic, the API Gateway handles external user, client, tenant and route quotas, and the service enforces business-specific and concurrency limits. This protects internal paths that may bypass the gateway.

## Q3: How would you scale it?

> Gateway instances remain stateless and share rate-limit state through Redis or another distributed store. The token decision must be atomic, usually through a Lua script or a proven distributed rate-limiter implementation. I would also address Redis failure policy, hot keys, multi-region behaviour, TTL cleanup and observability.

## Q4: What key would you use?

> I prefer authenticated principal, OAuth client or tenant identifiers. For unauthenticated endpoints, I may use a trusted client IP as one dimension, but not as the only control.

## Q5: Why not store counters inside the gateway?

> Local counters enforce separate limits per instance. Behind a load balancer, a client can receive more capacity by hitting different gateway instances.

## Q6: What should happen when the limit is exceeded?

> Return `429 Too Many Requests` with a safe error body and, where possible, a `Retry-After` value. The client should retry only after the indicated delay.

## Q7: What responsibilities should an API Gateway handle?

> Routing, edge authentication, coarse authorization, rate limiting, load balancing, request limits, cross-cutting transformations, traffic management and observability. It should not own domain logic, database access or distributed business workflows.

---

# Senior Interview Answer

> I would first define the quota dimension, endpoint cost, burst requirement and whether the limit must be exact across instances. For most synchronous APIs, I would use a token bucket because it permits controlled bursts while enforcing a sustainable rate. I would apply broad quotas at the API Gateway and retain service-level concurrency or business limits for expensive operations and internal traffic.
>
> In a single JVM, a synchronized token bucket is sufficient. In a scaled system, all gateway instances must share state through Redis or another distributed backend, and the read–calculate–update operation must be atomic. With Spring Cloud Gateway, I can use `RequestRateLimiter`, a principal- or tenant-based `KeyResolver`, and the Redis token-bucket implementation.
>
> Rejected requests return `429`, preferably with retry guidance. I also define fail-open versus fail-closed behaviour, monitor rejection and Redis latency, protect against hot keys, and combine rate limiting with request-size controls, concurrency limits, backpressure, WAF protection and local authorization.

[1]: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html "RequestRateLimiter GatewayFilter Factory :: Spring Cloud Gateway"
[2]: https://redis.io/docs/latest/develop/use-cases/rate-limiter/ "Redis rate limiter | Docs"
[3]: https://docs.spring.io/spring-cloud-gateway/reference/index.html?utm_source=chatgpt.com "Spring Cloud Gateway"
[4]: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/starter.html?utm_source=chatgpt.com "How to Include Spring Cloud Gateway"
[5]: https://datatracker.ietf.org/doc/html/rfc6585 "
            
                RFC 6585 - Additional HTTP Status Codes
            
        "
[6]: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories.html "GatewayFilter Factories :: Spring Cloud Gateway"
