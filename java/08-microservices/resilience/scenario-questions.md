# Scenario-Based Questions — Resilience and Failure Handling

## Core Resilience Principle

A fault-tolerant microservice should not assume that remote calls will always succeed quickly.

```text
Remote dependency
├── may respond slowly
├── may reject requests
├── may return partial data
├── may complete after the caller times out
├── may become unavailable
└── may recover while callers are still retrying
```

A practical resilience design combines:

```text
Overall request deadline
        ↓
Admission control / backpressure
        ↓
Bulkhead or concurrency limit
        ↓
Circuit breaker
        ↓
Bounded retries for safe failures
        ↓
Per-attempt network timeout
        ↓
Downstream dependency
```

The exact decorator order depends on the client and operation. The important rule is that concurrency, retries, and total elapsed time must all remain bounded.

---

# 1. Slow External API Dependency

## Scenario

Your service calls a third-party API that sometimes takes 10–20 seconds to respond. As traffic increases, request threads and HTTP connections remain occupied until the application becomes unresponsive.

## Failure progression

```text
Third-party API becomes slow
        ↓
Calls remain active for a long time
        ↓
HTTP connection pool fills
        ↓
Request threads wait for connections
        ↓
Application latency increases
        ↓
Clients retry
        ↓
Traffic multiplies
        ↓
Cascading failure
```

## Recommended design

### 1. Apply strict timeouts

Configure separate limits for:

- Connection-pool acquisition
- TCP connection establishment
- TLS handshake
- Response waiting
- Complete operation deadline

### 2. Limit concurrent calls

Use a semaphore or bounded bulkhead so the third party can consume only a controlled amount of your application’s capacity.

### 3. Add a circuit breaker

When recent failure or slow-call rates exceed thresholds, reject new calls without contacting the dependency. Resilience4j supports count-based and time-based sliding windows and transitions through `CLOSED`, `OPEN`, and `HALF_OPEN`; an open breaker rejects calls until limited recovery probes are permitted. ([resilience4j][1])

### 4. Retry selectively

Retry only failures likely to be temporary:

- Connection reset
- Selected `5xx` responses
- HTTP `429` when retry guidance is available
- Temporary DNS or network issues
- Short service failover

Do not retry:

- Invalid requests
- Authentication failures
- Authorization failures
- Permanent business rejection
- Non-idempotent writes without protection

### 5. Provide a truthful fallback

Examples:

```text
Recommendation API unavailable
→ return cached popular products

Address-validation API unavailable
→ allow address entry but mark validation as pending

Payment result unknown
→ return PENDING and reconcile later
```

Never fabricate a successful payment, available inventory, or authorization result.

### 6. Use asynchronous processing where possible

For notifications, exports, reconciliation, and other non-interactive work:

```text
Request
→ persist job
→ publish message
→ return 202 Accepted
→ process asynchronously
```

### 7. Monitor the dependency

Track:

- Call rate
- Error ratio
- P95/P99 latency
- Active calls
- Bulkhead rejections
- Circuit-breaker state
- Retry count
- Timeout count
- Fallback usage

---

# 2. When Should You Use Each Resilience Pattern?

| Pattern         | Problem it solves                             |
| --------------- | --------------------------------------------- |
| Timeout         | One call waits too long                       |
| Retry           | A temporary failure may succeed shortly       |
| Circuit breaker | A dependency is repeatedly unhealthy          |
| Bulkhead        | One dependency consumes all caller resources  |
| Rate limiter    | Too many requests arrive over time            |
| Backpressure    | Work arrives faster than it can be processed  |
| Cache           | Repeated reads can tolerate bounded staleness |
| Fallback        | Reduced but truthful behavior is possible     |
| Queue           | Work need not complete in the request path    |
| Idempotency     | Retries or duplicates may repeat side effects |

## Example composition

```text
Client request
    ↓
Overall deadline: 3 seconds
    ↓
Bulkhead: maximum 30 active calls
    ↓
Circuit breaker
    ↓
Maximum two attempts
    ↓
Each attempt: 800 ms response timeout
    ↓
Partner API
```

Avoid configurations such as:

```text
Upstream timeout: 3 seconds
Downstream timeout: 10 seconds
Retries: 4
```

The downstream work could continue long after the upstream caller has stopped waiting.

---

# 3. How Will You Implement Backpressure in Java Services?

Backpressure prevents producers from creating work faster than consumers can safely process it.

```text
Arrival rate > processing rate
        ↓
Queue grows
        ↓
Latency grows
        ↓
Memory grows
        ↓
Application crashes
```

Backpressure requires one or more of the following:

- Slow or pause producers
- Limit concurrent work
- Use bounded queues
- Reject excess traffic
- Drop replaceable data
- Buffer durably outside the process
- Scale consumers within downstream capacity

---

## 3.1 Bounded executor

Avoid unbounded executor queues.

```java
@Bean
ExecutorService partnerExecutor() {
    return new ThreadPoolExecutor(
            10,
            20,
            30,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(500),
            new ThreadPoolExecutor.AbortPolicy()
    );
}
```

When the queue is full, reject new work and map the rejection to a controlled response such as:

```http
HTTP/1.1 503 Service Unavailable
Retry-After: 2
```

An unbounded queue does not remove overload. It converts overload into delayed failure and potentially an `OutOfMemoryError`.

---

## 3.2 Semaphore-based admission control

```java
@Component
public final class PartnerConcurrencyGuard {

    private final Semaphore permits = new Semaphore(30);

    public <T> T execute(Callable<T> operation) throws Exception {
        boolean acquired = permits.tryAcquire(
                50,
                TimeUnit.MILLISECONDS
        );

        if (!acquired) {
            throw new ServiceOverloadedException(
                    "Partner call capacity is exhausted"
            );
        }

        try {
            return operation.call();
        } finally {
            permits.release();
        }
    }
}
```

The permit must be released only when it was successfully acquired.

---

## 3.3 Resilience4j bulkhead

Resilience4j provides:

- `SemaphoreBulkhead`
- `FixedThreadPoolBulkhead`

The semaphore version limits concurrent executions. The thread-pool version uses a fixed pool with a bounded queue. ([resilience4j][2])

```yaml
resilience4j:
  bulkhead:
    instances:
      partnerApi:
        maxConcurrentCalls: 30
        maxWaitDuration: 0

  thread-pool-bulkhead:
    instances:
      reportProvider:
        coreThreadPoolSize: 8
        maxThreadPoolSize: 12
        queueCapacity: 100
```

Use a semaphore bulkhead when concurrency limiting is enough. Use a thread-pool bulkhead when a blocking dependency needs executor isolation.

---

## 3.4 Reactive backpressure

Project Reactor propagates downstream demand to upstream publishers and supports backpressure-aware `Flux` processing. ([Project Reactor][3])

```java
public Flux<Result> process(Flux<Input> input) {
    return input
            .limitRate(128)
            .onBackpressureBuffer(
                    1_000,
                    dropped -> log.warn(
                            "Processing buffer overflow"
                    ),
                    BufferOverflowStrategy.ERROR
            )
            .flatMap(this::processOne, 16);
}
```

Here:

- `limitRate(128)` controls upstream request demand.
- `flatMap(..., 16)` limits concurrent asynchronous operations.
- The buffer is bounded.
- Overflow causes an explicit failure instead of unlimited memory growth.

Reactive backpressure works only where the producer can respond to demand. An uncontrolled external HTTP client still requires admission control, rate limiting, or rejection.

---

## 3.5 Kafka backpressure

For Kafka consumers:

- Reduce `max.poll.records`.
- Process bounded batches.
- Pause partitions when internal capacity is full.
- Resume after capacity becomes available.
- Scale consumers only up to the partition count.
- Monitor consumer lag and oldest-message age.
- Avoid unbounded handoff queues after `poll()`.

Consumer lag is itself a useful backpressure signal. Scaling consumers without checking database or third-party capacity may only move the bottleneck.

---

# 4. How Will You Implement Retry Without Overloading the System?

## Retry requirements

A safe retry policy needs:

```text
Retryable failure classification
+
maximum attempts
+
exponential backoff
+
jitter
+
overall deadline
+
idempotency
+
observability
```

Resilience4j allows configuring maximum attempts, wait duration, retryable exceptions, ignored exceptions, and custom interval calculations. Its `maxAttempts` value includes the original call. ([resilience4j][4])

## Exponential backoff

```text
Attempt 1 fails
→ wait 200 ms

Attempt 2 fails
→ wait 400 ms

Attempt 3 fails
→ wait 800 ms
```

## Jitter

Without jitter:

```text
10,000 requests fail together
→ all retry after exactly one second
→ another traffic spike
```

With jitter:

```text
Caller A retries after 310 ms
Caller B retries after 620 ms
Caller C retries after 770 ms
```

## Full-jitter example

```java
public final class RetryDelay {

    private RetryDelay() {
    }

    public static Duration fullJitter(
            int retryNumber,
            Duration baseDelay,
            Duration maximumDelay
    ) {
        if (retryNumber < 1) {
            throw new IllegalArgumentException(
                    "retryNumber must be positive"
            );
        }

        long exponential = (long) Math.min(
                maximumDelay.toMillis(),
                baseDelay.toMillis()
                        * Math.pow(2, retryNumber - 1)
        );

        long randomized = ThreadLocalRandom.current()
                .nextLong(exponential + 1);

        return Duration.ofMillis(randomized);
    }
}
```

## Resilience4j configuration

```yaml
resilience4j:
  retry:
    instances:
      partnerApi:
        maxAttempts: 3
        waitDuration: 200ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignoreExceptions:
          - com.example.partner.InvalidPartnerRequestException
```

Resilience4j’s Spring Boot configuration supports exponential backoff and exception classification. ([resilience4j][5])

## Prevent retry amplification

Suppose three service layers each retry three times:

```text
Gateway retries Service A 3 times
Service A retries Service B 3 times
Service B retries Provider 3 times
```

One original request could produce:

```text
3 × 3 × 3 = 27 provider attempts
```

Define retry ownership. Usually only one layer should retry a particular dependency failure.

---

# 5. How Will You Handle Downstream Timeouts?

## Configure timeouts at the HTTP client

For Reactor Netty and `WebClient`:

```java
@Configuration
public class PartnerClientConfiguration {

    @Bean
    WebClient partnerWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        1_000
                )
                .responseTimeout(Duration.ofSeconds(2))
                .doOnConnected(connection -> connection
                        .addHandlerLast(
                                new ReadTimeoutHandler(2)
                        )
                        .addHandlerLast(
                                new WriteTimeoutHandler(2)
                        )
                );

        return WebClient.builder()
                .baseUrl("https://partner.example")
                .clientConnector(
                        new ReactorClientHttpConnector(httpClient)
                )
                .build();
    }
}
```

Spring Framework documents separate Reactor Netty configuration for connection, read, write, and response timeouts. ([Home][6])

## Timeout hierarchy

```text
Client deadline:            5 seconds
Gateway processing budget:  4.5 seconds
Service A deadline:         4 seconds
Service B call timeout:     2 seconds
Provider attempt timeout:   800 ms
```

A lower-level timeout should not exceed its caller’s remaining deadline.

## Unknown outcome problem

Consider a payment call:

```text
Provider receives request
→ provider charges customer
→ response is lost
→ caller times out
```

The timeout means:

```text
Outcome unknown
```

It does not necessarily mean:

```text
Operation failed
```

For side-effecting operations:

- Send an idempotency key.
- Store the local operation as `PENDING`.
- Query the provider’s status endpoint.
- Reconcile unresolved operations.
- Never blindly retry with a new transaction reference.

---

# 6. How Will You Implement Circuit Breaker in Spring Boot?

## Dependencies for Spring Boot 3

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

The Resilience4j Spring Boot starter expects Actuator and Spring AOP at runtime; reactive applications also require the Reactor integration. ([resilience4j][5])

## Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      partnerApi:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 20
        minimumNumberOfCalls: 10

        failureRateThreshold: 50
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 2s

        permittedNumberOfCallsInHalfOpenState: 3
        waitDurationInOpenState: 15s
        automaticTransitionFromOpenToHalfOpenEnabled: true

        recordExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException

        ignoreExceptions:
          - com.example.partner.InvalidPartnerRequestException

  bulkhead:
    instances:
      partnerApi:
        maxConcurrentCalls: 30
        maxWaitDuration: 0
```

## Client implementation

```java
@Service
public class PartnerClient {

    private final WebClient partnerWebClient;

    public PartnerClient(WebClient partnerWebClient) {
        this.partnerWebClient = partnerWebClient;
    }

    @CircuitBreaker(
            name = "partnerApi",
            fallbackMethod = "partnerFallback"
    )
    @Bulkhead(
            name = "partnerApi",
            type = Bulkhead.Type.SEMAPHORE
    )
    public PartnerResponse fetchPartnerData(
            PartnerRequest request
    ) {
        return partnerWebClient.post()
                .uri("/v1/data")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PartnerResponse.class)
                .block();
    }

    private PartnerResponse partnerFallback(
            PartnerRequest request,
            Throwable failure
    ) {
        return PartnerResponse.temporarilyUnavailable(
                request.requestId()
        );
    }
}
```

For a reactive request path, return `Mono<PartnerResponse>` rather than calling `block()`.

## Exception classification

Do not count every business error as dependency failure.

| Result             | Breaker treatment                   |
| ------------------ | ----------------------------------- |
| Connection timeout | Failure                             |
| Provider `503`     | Failure                             |
| Provider `504`     | Failure                             |
| Invalid request    | Ignore                              |
| Insufficient funds | Ignore as infrastructure failure    |
| Resource not found | Contract-dependent                  |
| HTTP `429`         | Usually failure or controlled retry |

The breaker protects technical health. It should not open because users submit invalid requests.

## Monitoring

Spring Cloud Circuit Breaker can expose Resilience4j metrics through Actuator when Actuator and `resilience4j-micrometer` are present. ([Home][7])

Monitor:

- Breaker state
- State transitions
- Failed calls
- Slow calls
- Rejected calls
- Half-open outcomes
- Fallback count
- Retry count
- Dependency latency

---

# 7. How Will You Design Fallback Mechanisms?

A fallback should answer:

> What truthful value or behavior can the application provide without the normal dependency?

## Fallback categories

### Cached data

```text
ML recommendation unavailable
→ return recent cached recommendations
```

### Simpler data source

```text
Personalized recommendations unavailable
→ return category-popular products
```

### Reduced functionality

```text
Review service unavailable
→ display product page without reviews
```

### Asynchronous continuation

```text
SMS provider unavailable
→ persist notification
→ retry through worker
```

### Pending state

```text
Payment outcome unknown
→ return PENDING
→ reconcile later
```

### Explicit failure

```http
HTTP/1.1 503 Service Unavailable
Retry-After: 10
```

Sometimes an honest failure is safer than a fallback.

## Fallback checklist

A fallback must be:

- Truthful
- Fast
- Capacity-bounded
- Observable
- Secure
- Tested
- Temporary
- Clear about data freshness

## Avoid fallback storms

```text
Cache unavailable
→ every request queries database
→ database overload
```

Protect fallback paths with:

- Concurrency limits
- Rate limits
- Short-lived local cache
- Request coalescing
- Load shedding

---

# 8. How Will You Integrate Third-Party APIs Reliably?

Use an adapter boundary:

```text
Business service
        ↓
Partner interface
        ↓
Partner-specific adapter
        ↓
Third-party API
```

## Adapter contract

```java
public interface AddressValidationProvider {

    ValidationResult validate(Address address);
}
```

The business layer should not depend directly on provider-specific:

- DTOs
- Error codes
- SDK classes
- Authentication mechanisms
- Retry headers

## Reliability checklist

### Request design

- Stable request ID
- Idempotency key for side effects
- Explicit version
- Validated payload
- Bounded request and response size

### Network controls

- Connection timeout
- Response timeout
- HTTP connection-pool limits
- Bulkhead
- Circuit breaker
- Safe retries
- Rate limiting

### Security

- TLS validation
- Secret manager
- Credential rotation
- Minimal scopes
- Request signing where required
- No credentials in logs

### Contract protection

- Schema validation
- Tolerant response parsing
- Unknown-field handling
- Provider contract tests
- Sandbox integration tests
- API-version monitoring

### Recovery

- Provider status lookup
- Reconciliation
- Webhook deduplication
- Dead-letter processing
- Manual operational tooling

### Observability

- Provider latency
- Status categories
- Rate-limit responses
- Timeout count
- Unknown outcomes
- Idempotency conflicts
- Reconciliation mismatches

---

# 9. One Microservice Failure Is Impacting Others

## Isolation strategy

```text
Service A
├── Dependency B bulkhead
├── Dependency C bulkhead
└── Database bulkhead/pool
```

Use separate failure boundaries for:

- Critical versus optional dependencies
- Third-party APIs
- Long-running operations
- Batch workloads
- Interactive traffic
- Message consumers

## Techniques

### Timeouts

Stop waiting indefinitely.

### Bulkheads

Limit concurrency per dependency.

### Circuit breakers

Stop calling repeatedly failing dependencies.

### Bounded queues

Prevent unlimited backlog.

### Rate limiting

Control admission.

### Separate thread pools

Protect interactive work from long-running jobs where justified.

### Resource quotas

Use container CPU and memory limits carefully.

### Graceful degradation

Keep unaffected features available.

### Asynchronous messaging

Decouple work that does not require immediate completion.

### Load shedding

Reject low-priority work when capacity is exhausted.

---

# 10. How Will You Handle Partial Failures?

In distributed systems, some steps may succeed while others fail.

## Example

```text
Order created
Inventory reserved
Payment authorization timed out
Notification not sent
```

Do not model the entire workflow as only:

```text
SUCCESS or FAILED
```

Use explicit states:

```text
PENDING
INVENTORY_RESERVED
PAYMENT_PENDING
CONFIRMED
COMPENSATION_PENDING
CANCELLED
MANUAL_REVIEW_REQUIRED
```

## Required patterns

- Local database transactions
- Transactional outbox
- Idempotent consumers
- Saga workflow
- Compensating transactions
- Timeouts
- Reconciliation
- Manual recovery path

## Compensation failure

```text
Payment failed
→ release inventory
→ release operation fails
```

Persist:

```text
COMPENSATION_PENDING
```

Then:

- Retry with bounded backoff
- Alert operations
- Reconcile state
- Escalate to manual review
- Never lose the original business history

---

# 11. How Will You Handle Network Failures?

Network failures include:

- DNS lookup failure
- TCP connection timeout
- Connection refused
- TLS handshake error
- Connection reset
- Partial response
- Read timeout
- Proxy timeout
- Lost response after successful processing

## Correct handling

### Classify the failure

```text
No connection established
→ likely safe to retry when operation is idempotent

Connection established, response lost
→ outcome may be unknown
```

### Use stable operation identity

```http
Idempotency-Key: payment-9218
```

### Query status after ambiguity

```text
Request timed out
→ GET provider status using stable reference
```

### Apply bounded retry

Use exponential backoff and jitter.

### Preserve total deadline

Do not retry after the caller’s meaningful deadline has expired.

### Monitor network phases separately

- DNS duration
- Connection duration
- TLS duration
- Time to first byte
- Complete response duration

---

# 12. Database Connection Pool Is Exhausted

## Common causes

- Slow SQL
- Long transactions
- N+1 queries
- Connection leak
- Database lock waits
- Remote calls inside `@Transactional`
- Batch jobs sharing the interactive pool
- Excessive request concurrency
- Pool too small
- Database too saturated to support a larger pool
- Threads holding connections while waiting for another resource

## What to inspect

```text
Active connections
Idle connections
Pending borrowers
Acquisition latency
Acquisition timeout count
Connection hold time
Transaction duration
Query duration
Database locks
Database CPU and I/O
```

## Example failure

```text
Pool maximum: 20
20 requests call external API inside a transaction
Each holds a DB connection for 10 seconds
New requests wait for connections
```

The fix is not necessarily increasing the pool. Move network calls outside the database transaction and shorten connection hold time.

## Corrective actions

- Optimize slow queries.
- Remove N+1 access.
- Keep transactions short.
- Use separate bounded pools for batch and API workloads where appropriate.
- Add request concurrency limits.
- Fix connection leaks.
- Tune pool size against the database’s total connection budget.
- Monitor acquisition time, not only active count.

---

# 13. Redis Becomes Unavailable

The fallback depends on what Redis is used for.

| Redis role              | Possible behavior                           |
| ----------------------- | ------------------------------------------- |
| Optional cache          | Fall back to DB with concurrency protection |
| Session store           | Reauthenticate or fail closed               |
| Rate limiter            | Endpoint-specific fail-open or fail-closed  |
| Distributed lock        | Stop if correctness requires the lock       |
| Idempotency accelerator | Fall back to durable database record        |
| Feature configuration   | Use a safe local snapshot when permitted    |
| Queue                   | Preserve work elsewhere or reject safely    |

## Cache fallback

```text
Redis unavailable
→ bounded DB fallback
→ local short TTL
→ request coalescing
→ load shedding if DB saturates
```

Do not allow every application instance to flood the database simultaneously.

## Rate-limiter fallback

### Fail open

```text
Limiter unavailable
→ permit requests
```

Preserves availability but removes abuse protection.

### Fail closed

```text
Limiter unavailable
→ reject requests
```

Preserves protection but creates an availability dependency.

Example policies:

```text
Public product search
→ fail open with local limit

Login or OTP endpoint
→ fail closed or strongly restricted

Payment submission
→ use durable idempotency and conservative admission
```

---

# 14. Cache Invalidation Across Multiple Instances

## Problem

```text
Instance A updates customer
→ evicts its local cache

Instances B and C
→ still return stale customer data
```

## Option 1: Shared distributed cache

```text
All instances
→ Redis
```

Updates and evictions are visible through one shared cache, although local near-caches still require invalidation.

## Option 2: Invalidation events

```text
Database update
+
outbox event
        ↓
CustomerUpdated
        ↓
All service instances evict customer key
```

Use a transactional outbox so the database update and invalidation event cannot diverge.

## Option 3: Versioned keys

```text
customer:C-100:v8
```

When the version changes, old cache entries are no longer selected.

## Option 4: Short TTL

A TTL limits maximum staleness but does not guarantee immediate consistency.

## Option 5: Write-through or write-behind

These approaches may help specific workloads but introduce their own consistency and failure concerns.

## Cache stampede protection

When a popular key expires:

```text
10,000 requests miss cache
→ 10,000 database queries
```

Use:

- Per-key single-flight/request coalescing
- Randomized TTL
- Early refresh
- Stale-while-revalidate
- Negative caching for bounded periods
- Concurrency limits

---

# Fault-Tolerant System Design

```text
                         Client
                            ↓
                  Gateway / admission control
                            ↓
                 Rate limit and request limits
                            ↓
                  Stateless service replicas
                            ↓
                Per-dependency bulkhead
                            ↓
            Timeout + circuit breaker + retry
                            ↓
                 Downstream dependencies
                            ↓
               Durable state and messaging
```

## Cross-cutting requirements

- Idempotency
- Transactional outbox
- Explicit intermediate states
- Reconciliation
- Graceful degradation
- Bounded resource usage
- Structured logs
- Metrics and distributed traces
- Health checks
- Graceful shutdown
- Tested recovery procedures

---

# Common Mistakes

## Retrying every error

Permanent failures never become successful through repetition.

## No overall deadline

Several individually reasonable timeouts can exceed the client’s total budget.

## Circuit breaker without bulkhead

Many concurrent slow requests can exhaust resources before the breaker opens.

## Bulkhead with an unbounded queue

This hides overload until latency and memory become unacceptable.

## Cache fallback without DB protection

A Redis outage can become a database outage.

## Returning fake success

Fallbacks must not claim payment success, available stock, or authorized access when the result is unknown.

## Redis-only idempotency

Critical duplicate prevention should use durable database constraints.

## Reusing the same policy everywhere

Each dependency has different latency, failure rate, criticality, and recovery characteristics.

## No monitoring

A fallback or open circuit can silently remain active for hours.

---

# Interview-Ready Answers

## How do you prevent a slow downstream service from bringing down the caller?

> I configure connection and response timeouts, limit concurrent calls with a bulkhead, and use a circuit breaker based on failure and slow-call rates. Retries are bounded, use backoff and jitter, and apply only to safe transient failures. When possible, I return cached or reduced functionality; otherwise, I fail quickly with an accurate error.

## How do you implement backpressure?

> I use bounded queues, limited concurrency, controlled demand, and explicit rejection. In blocking Java services, I use bounded executors or semaphores. In Reactor, I limit demand and concurrency and use bounded overflow behavior. In Kafka, I process bounded batches, pause consumption when capacity is full, and monitor lag.

## How do you design retry logic?

> I classify failures, cap attempts, use exponential backoff with jitter, and ensure all attempts fit within one overall deadline. Writes require idempotency. I also prevent retry multiplication by assigning retry responsibility to one layer.

## How do you handle timeouts?

> I configure pool-acquisition, connection, read, write, and overall operation timeouts separately. Each downstream budget is smaller than the caller’s remaining deadline. For operations with side effects, a timeout is treated as an unknown outcome and resolved using an idempotency key and status reconciliation.

## How do you handle Redis failure?

> It depends on Redis’s role. An optional cache can fall back to the database with stampede protection. Security, distributed locking, and high-risk admission controls normally fail conservatively. Durable correctness, such as payment idempotency, remains in the database rather than depending only on Redis.

## How do you handle partial failures?

> I model explicit intermediate states and use local transactions, outbox events, idempotent handlers, Saga compensation, bounded retry, and reconciliation. Failed compensations enter a durable recovery or manual-review state rather than disappearing.

## Senior Interview Summary

> I design resilience by bounding time, concurrency, queues, and retry volume. Every remote dependency has native connection and response timeouts, a dependency-specific bulkhead, and a circuit breaker configured from observed latency and failure characteristics. Retries apply only to transient, idempotent operations and use exponential backoff with jitter within one end-to-end deadline.
>
> Optional functionality degrades through cached or simpler responses, while critical payment, inventory, and authorization operations never fabricate success. Ambiguous network outcomes remain pending and are reconciled using stable operation references. Redis and caches have role-specific fallback policies, and cache outages are prevented from overwhelming the database through concurrency limits and request coalescing.
>
> Partial failures are handled with explicit workflow states, local transactions, transactional outbox events, idempotent consumers, compensation, and reconciliation. Metrics cover latency, failures, retries, circuit states, queue depth, pool saturation, fallback usage, and unresolved business operations.

[1]: https://resilience4j.readme.io/docs/circuitbreaker "CircuitBreaker"
[2]: https://resilience4j.readme.io/docs/bulkhead "Bulkhead"
[3]: https://projectreactor.io/docs/core/release/reference/reactiveProgramming.html?utm_source=chatgpt.com "Introduction to Reactive Programming"
[4]: https://resilience4j.readme.io/docs/retry "Retry"
[5]: https://resilience4j.readme.io/docs/getting-started-3 "Getting Started"
[6]: https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-builder.html "Configuration :: Spring Framework"
[7]: https://docs.spring.io/spring-cloud-circuitbreaker/reference/spring-cloud-circuitbreaker-resilience4j/collecting-metrics.html?utm_source=chatgpt.com "Collecting Metrics :: Spring Cloud Circuitbreaker"
