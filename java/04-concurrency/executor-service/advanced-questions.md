# 8 Production Mistakes Java Backend Engineers Keep Making

Production failures rarely come from not knowing an API. They come from using the correct API with the wrong assumptions.

---

## 1. Treating Virtual Threads as Unlimited Capacity

### The mistake

> Platform thread pools block under load. Virtual threads do not.

### The reality

Both platform threads and virtual threads can block. The difference is that a blocked virtual thread can often be suspended without permanently occupying its underlying operating-system thread.

Virtual threads became a standard Java feature in Java 21. They are managed and scheduled primarily by the Java runtime and are designed for workloads that spend substantial time waiting for blocking I/O. They are not intended to make long-running CPU-intensive work faster. A JVM may support very large numbers of virtual threads, but “millions” is a capability—not a safe sizing target for every application. ([Oracle Docs][1])

```java
try (ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor()) {

    Future<User> future =
            executor.submit(() ->
                    userClient.fetchUser(userId)
            );

    User user = future.get();
}
```

### What virtual threads do not solve

They do not increase:

- Database connections
- External API capacity
- CPU cores
- Memory
- File-descriptor limits
- Downstream rate limits

This is still dangerous:

```java
try (ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor()) {

    for (Order order : oneMillionOrders) {
        executor.submit(() ->
                paymentService.charge(order)
        );
    }
}
```

The application may now create an enormous number of concurrent requests against the payment service.

### Better design

Use virtual threads for blocking task-per-request code, but limit scarce resources separately:

```java
private final Semaphore paymentPermits =
        new Semaphore(50);

public PaymentResult charge(Order order)
        throws InterruptedException {

    paymentPermits.acquire();

    try {
        return paymentClient.charge(order);
    } finally {
        paymentPermits.release();
    }
}
```

### Production rule

> Virtual threads make waiting cheaper. They do not make downstream capacity unlimited.

---

## 2. Choosing Between `CompletableFuture` and Reactive Streams Based on Hype

### The mistake

> `CompletableFuture` is simple async. WebFlux is for high volume.

This is too broad.

### Use `CompletableFuture` when

- One operation produces one eventual result.
- You need to run a few independent calls concurrently.
- The application is mostly imperative.
- Backpressure across a continuous stream is unnecessary.
- The team wants straightforward asynchronous composition.

```java
CompletableFuture<Price> price =
        CompletableFuture.supplyAsync(
                pricingClient::fetch,
                ioExecutor
        );

CompletableFuture<Shipping> shipping =
        CompletableFuture.supplyAsync(
                shippingClient::fetch,
                ioExecutor
        );

CompletableFuture<OrderSummary> summary =
        price.thenCombine(
                shipping,
                OrderSummary::new
        );
```

### Use Reactive Streams when

- Data arrives as an ongoing sequence.
- Consumers must control how quickly producers emit.
- The complete request path is non-blocking.
- You need streaming, transformation and cancellation across many stages.
- The application already uses Reactor-compatible libraries.

A reactive stack does not automatically make a blocking database driver or HTTP client non-blocking. Wrapping blocking calls in `Mono` or `Flux` without isolating them can still block event-loop workers. Spring WebFlux is based on a reactive, non-blocking execution model and should be used with compatible components throughout the request path. ([Home][2])

### Common production mistake

```java
public Mono<User> findUser(long id) {
    return Mono.fromCallable(
            () -> blockingRepository.findById(id)
    );
}
```

This is still blocking work. It must be moved to an appropriate scheduler or replaced with a non-blocking data-access API.

### Production rule

> Use `CompletableFuture` for finite asynchronous results. Use Reactive Streams when streaming and flow control are fundamental requirements.

---

## 3. Using a Connection-Pool Formula as a Production Configuration

### The mistake

> HikariCP defaults to 10 connections, which is almost always wrong. Use `cores × 2 + disk spindles`.

HikariCP’s default `maximumPoolSize` is commonly 10, but that value is neither automatically correct nor automatically wrong. The HikariCP project explicitly describes pool sizing as deployment-specific and warns that excessive database connections can reduce performance. ([GitHub][3])

The formula:

```text
connections ≈ CPU cores × 2 + effective disk spindles
```

is a historical heuristic, not a universal production baseline. It may be irrelevant for:

- SSD storage
- Managed cloud databases
- Read replicas
- Network-bound queries
- Multiple application instances
- Mixed short and long transactions

### What should determine pool size?

Measure:

- Database maximum connection capacity
- Number of application instances
- Query execution time
- Connection hold time
- Transaction duration
- Active and pending connections
- Database CPU and I/O saturation
- Required request latency

Suppose:

```text
Database connection limit: 200
Application instances:      10
Administrative reserve:     20
```

The theoretical remaining budget is:

```text
(200 - 20) / 10 = 18 connections per instance
```

That is still only a starting point. Other services may share the database.

### Too few connections

```text
Request threads
    → wait for database connections
    → queueing latency increases
```

### Too many connections

```text
Too many active queries
    → database CPU and lock contention
    → slower queries
    → connections held longer
    → more waiting
```

### Better configuration

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 18
      connection-timeout: 3000
      max-lifetime: 1800000
```

The values must be validated under realistic load.

### Production rule

> Size the connection pool from the database backward, across every application instance—not from one application’s thread count alone.

---

## 4. Assuming `@Transactional` Always Starts a Transaction

### Mistake 1: Self-invocation

```java
@Service
public class OrderService {

    public void placeOrder(Order order) {
        saveOrder(order);
    }

    @Transactional
    public void saveOrder(Order order) {
        repository.save(order);
    }
}
```

In Spring’s default proxy mode, `placeOrder()` calls `saveOrder()` directly on the same object. The call does not pass through the transactional proxy, so the annotation on `saveOrder()` is not independently intercepted. ([Home][4])

A transaction may still exist if the outer method already started one. Therefore, “self-invocation always means no transaction” is also too absolute.

### Better options

Move the transaction boundary to the public service operation:

```java
@Transactional
public void placeOrder(Order order) {
    repository.save(order);
}
```

Or move the transactional method into another Spring bean.

---

### Mistake 2: Treating `REQUIRED` and `REQUIRES_NEW` as interchangeable

#### `REQUIRED`

- Joins an existing transaction.
- Creates one when none exists.
- Participating operations usually commit or roll back together.

#### `REQUIRES_NEW`

- Suspends an existing transaction.
- Starts an independent physical transaction.
- May require another database connection.

Spring warns that excessive `REQUIRES_NEW` usage can exhaust a connection pool when outer transactions retain their connections while waiting for inner transactions to obtain another one. ([Home][5])

```java
@Transactional
public void placeOrder(Order order) {
    orderRepository.save(order);
    auditService.recordInNewTransaction(order);
}
```

If many threads hold outer transactions and all request another connection for audit logging, pool starvation can occur.

---

### Mistake 3: Treating lazy loading failures as silent and inevitable

`LazyInitializationException` is not silent. It is a runtime exception produced when code tries to initialize unloaded ORM state without an active persistence context.

Common fixes include:

- Fetch only required relationships.
- Use DTO projections.
- Use carefully designed fetch joins.
- Access required state inside a service transaction.
- Avoid exposing persistence entities directly through APIs.

Do not solve every case by making every relationship eager. That can create huge joins, N+1 variations and excessive memory usage.

### Production rule

> Define transactions around business operations, understand proxy boundaries and choose propagation intentionally.

---

## 5. Confusing Cache Eviction with Cache Consistency

### The mistake

> The wrong eviction policy causes stale data.

Eviction and staleness are different problems.

- **Eviction** decides what to remove when memory is constrained.
- **Expiration** removes data after a time limit.
- **Invalidation** removes data because the source changed.
- **Consistency** defines how cache state relates to authoritative storage.

Redis supports several memory-eviction policies, including recency-, frequency-, TTL- and random-based policies. The policy is applied when configured memory limits are reached. ([Redis][6])

### Common caching strategies

#### Cache-aside

```text
Read:
cache → miss → database → populate cache

Write:
database → invalidate cache
```

Failure risk:

```text
Database update succeeds
Cache invalidation fails
→ stale value remains
```

#### Write-through

```text
Application → cache → database
```

Failure risk:

- Higher write latency
- Complex partial failures
- Cache becomes part of the write path

#### Write-behind

```text
Application → cache
Cache asynchronously → database
```

Failure risk:

- Data loss before persistence
- Reordering
- Difficult recovery and reconciliation

### LRU is not always correct

- LRU favors recently accessed keys.
- LFU favors frequently accessed keys.
- TTL policies prioritize expiring keys.
- Random eviction may provide adequate behavior with lower bookkeeping cost.

Redis’s LFU implementation uses approximate counters with decay rather than perfectly counting every access. ([Redis][7])

### Other production failures

- Cache stampedes
- Hot keys
- Missing TTLs
- Caching failures indefinitely
- Serialization incompatibility
- Stale local caches across instances
- Unbounded key cardinality

### Production rule

> Eviction protects memory. TTL and invalidation control staleness. They must be designed separately.

---

## 6. Treating Kafka as a Traditional Queue

### The mistake

> Kafka topics are not queues.

This statement needs nuance.

A Kafka topic is a partitioned event log. Within one consumer group, each partition is assigned to one consumer at a time, which provides queue-like load distribution. Different consumer groups can independently consume the same events. ([Apache Kafka][8])

```text
Topic: payments

Consumer group: fraud-detection
    partition 0 → consumer A
    partition 1 → consumer B

Consumer group: accounting
    partition 0 → consumer C
    partition 1 → consumer D
```

Both groups process the same topic independently.

### Partition-key mistakes

A partition key affects:

- Ordering scope
- Load distribution
- Consumer parallelism
- Hot-partition risk

```java
ProducerRecord<String, PaymentEvent> record =
        new ProducerRecord<>(
                "payments",
                customerId,
                event
        );
```

Using `customerId` preserves order for one customer, but one extremely active customer may create a hot partition.

The key influences throughput, but it is not the only throughput limit. Performance also depends on:

- Partition count
- Broker capacity
- Batching
- Record size
- Replication
- Consumer processing speed
- Network and storage
- Producer acknowledgements

### Idempotent producer vs idempotent consumer

Kafka producer idempotence protects against certain duplicate writes caused by producer retries. Kafka also supports transactions for atomic writes and offset updates. ([Apache Kafka][9])

It does not automatically make external consumer side effects idempotent:

```text
Consume payment event
→ charge card
→ process crashes before offset commit
→ event is consumed again
→ card may be charged twice
```

Consumers may need:

- Idempotency keys
- Deduplication tables
- Unique database constraints
- Transactional outbox/inbox patterns
- State-machine validation

### Production rule

> Design the partition key for ordering and distribution, and design consumer side effects to tolerate redelivery.

---

## 7. Adding Indexes Without Measuring the Workload

### The mistake

> More indexes always improve database performance.

Indexes improve selected reads but must be maintained during writes. PostgreSQL documentation explicitly notes that indexes add system overhead and should be used selectively. ([PostgreSQL][10])

Each additional index may increase:

- Insert time
- Update time
- Delete time
- Storage usage
- Cache pressure
- Vacuum and maintenance work
- Migration duration

### Composite-index order matters

For a query such as:

```sql
SELECT *
FROM orders
WHERE tenant_id = ?
  AND status = ?
  AND created_at >= ?
ORDER BY created_at;
```

A possible index is:

```sql
CREATE INDEX idx_orders_tenant_status_created
    ON orders (tenant_id, status, created_at);
```

However, correct order depends on:

- Equality predicates
- Range predicates
- Sort requirements
- Column selectivity
- Database engine
- Other query patterns

Do not memorize “most selective column first” as a universal law.

### Use query plans correctly

```sql
EXPLAIN
SELECT *
FROM orders
WHERE tenant_id = 10;
```

For real execution statistics:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM orders
WHERE tenant_id = 10;
```

`EXPLAIN ANALYZE` actually executes the query. This matters especially for expensive statements and data-changing operations. ([PostgreSQL][11])

For write statements, use a safe environment or wrap the test appropriately:

```sql
BEGIN;

EXPLAIN ANALYZE
UPDATE orders
SET status = 'EXPIRED'
WHERE expires_at < CURRENT_TIMESTAMP;

ROLLBACK;
```

Even rollback does not eliminate all possible external effects, such as calls made by non-transactional external systems.

### Production rule

> Every index must justify its read benefit against its write, storage and maintenance cost.

---

## 8. Adding Circuit Breakers Without Timeouts or Bulkheads

### The failure chain

```text
Service B becomes slow
        ↓
Service A threads wait
        ↓
Connection and thread pools fill
        ↓
Requests queue
        ↓
Service A becomes unhealthy
        ↓
Failure spreads upstream
```

A circuit breaker records call outcomes and transitions between states such as:

```text
CLOSED → OPEN → HALF_OPEN → CLOSED
```

When open, Resilience4j rejects calls instead of invoking the unhealthy dependency. After a configured wait period, a limited number of half-open calls test whether the service has recovered. ([resilience4j][12])

### Circuit breaker states

#### `CLOSED`

Calls are allowed and outcomes are measured.

#### `OPEN`

Calls fail fast without reaching the dependency.

#### `HALF_OPEN`

A limited number of test calls are permitted.

### A circuit breaker is not enough

It does not itself:

- Time out a slow request
- Limit concurrent requests
- Retry failed calls
- Provide a business fallback
- Guarantee recovery
- Prevent every overload scenario

Resilience4j specifically distinguishes circuit breaking from limiting concurrent calls and recommends a bulkhead when concurrency must be restricted. ([resilience4j][12])

A resilient call may combine:

```text
Time limiter
→ bulkhead
→ circuit breaker
→ carefully bounded retry
→ fallback
```

Example configuration:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        sliding-window-size: 100
        minimum-number-of-calls: 20
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 2s
        wait-duration-in-open-state: 10s

  bulkhead:
    instances:
      paymentService:
        max-concurrent-calls: 30

  timelimiter:
    instances:
      paymentService:
        timeout-duration: 2s
```

### Retry warning

Blind retries can amplify an outage:

```text
1 original request × 3 retries × 4 service layers
= potentially 81 downstream attempts
```

Retry only when:

- The error is transient.
- The operation is idempotent.
- Attempts are bounded.
- Backoff includes jitter.
- The retry budget respects the caller’s deadline.

### Production rule

> A circuit breaker stops repeated calls to an unhealthy dependency. Timeouts bound waiting, bulkheads bound concurrency, and idempotency makes retries safe.

---

# Quick Production Checklist

| Area              | Dangerous assumption                        | Better question                                      |
| ----------------- | ------------------------------------------- | ---------------------------------------------------- |
| Virtual threads   | “They cannot overload the system”           | Which downstream resource limits concurrency?        |
| Async programming | “Reactive is always faster”                 | Do I need streaming and backpressure?                |
| HikariCP          | “Use one sizing formula”                    | What can the database and all app instances sustain? |
| Transactions      | “The annotation always runs”                | Did the call cross the Spring proxy boundary?        |
| Caching           | “LRU solves stale data”                     | What are the TTL and invalidation guarantees?        |
| Kafka             | “Exactly once prevents duplicate charges”   | Is the business side effect idempotent?              |
| Indexing          | “More indexes mean faster queries”          | What is the measured read/write trade-off?           |
| Resilience        | “Circuit breaker solves cascading failures” | Where are the timeout, bulkhead and retry budget?    |

---

# Short Interview Answer

> The most dangerous production mistakes come from treating concurrency and resilience tools as unlimited capacity. Virtual threads still need downstream limits, connection pools must be sized against database capacity, transactional behavior depends on proxy boundaries, cache eviction does not solve consistency, Kafka consumers must tolerate redelivery, indexes impose write costs, and circuit breakers must be combined with timeouts and bulkheads. Production reliability comes from defining limits, failure behavior and observability before traffic exposes them.

> Production will test all eight—usually at 3 a.m., usually during the least convenient release window.

[1]: https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html?utm_source=chatgpt.com "Virtual Threads"
[2]: https://docs.spring.io/spring-framework/reference/index.html?utm_source=chatgpt.com "Spring Framework Documentation"
[3]: https://github.com/brettwooldridge/hikaricp?utm_source=chatgpt.com "brettwooldridge/HikariCP: 光 HikariCP・A solid, high- ..."
[4]: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html?utm_source=chatgpt.com "Using @Transactional"
[5]: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html?utm_source=chatgpt.com "Transaction Propagation :: Spring Framework"
[6]: https://redis.io/docs/latest/develop/reference/eviction/?utm_source=chatgpt.com "Key eviction | Docs"
[7]: https://redis.io/blog/lfu-vs-lru-how-to-choose-the-right-cache-eviction-policy/?utm_source=chatgpt.com "LFU vs. LRU: How to choose the right cache eviction policy"
[8]: https://kafka.apache.org/documentation/?utm_source=chatgpt.com "Introduction | Apache Kafka"
[9]: https://kafka.apache.org/41/configuration/producer-configs/?utm_source=chatgpt.com "Producer Configs | Apache Kafka"
[10]: https://www.postgresql.org/docs/current/indexes.html?utm_source=chatgpt.com "Documentation: 18: Chapter 11. Indexes"
[11]: https://www.postgresql.org/docs/current/using-explain.html?utm_source=chatgpt.com "Documentation: 18: 14.1. Using EXPLAIN"
[12]: https://resilience4j.readme.io/docs/circuitbreaker?utm_source=chatgpt.com "CircuitBreaker - resilience4j - ReadMe"
