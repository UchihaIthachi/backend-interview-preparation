

## 8 Production Mistakes

1. Thread Pools vs Virtual Threads
 - Default thread pool blocks under load. Virtual Threads don't.
 - Java 21. Millions of threads. JVM manages them. Not OS.
 - Most developers still don't know this exists.

2. CompletableFuture vs Reactive Streams
 - CompletableFuture simple async. Good for most cases.
 - WebFlux backpressure + streaming. Good for high volume.
 - Pick based on problem. Not hype.

3. Connection Pooling (HikariCP)
 - Default pool size 10. Almost always wrong.
 - Formula: CPU cores x 2 + disk spindles = your baseline.
 - Too many → DB dies. Too few → threads wait at 3am.

4. @ Transactional Pitfalls
 - Self-invocation → proxy bypassed → transaction never starts. No error.
 - REQUIRES_NEW vs REQUIRED. Not interchangeable. Most think they are.
 - Lazy loading in closed session → LazyInitializationException. Silent. Always.

5. Caching Strategy (Redis)
 - Cache-aside. Write-through. Write-behind. Three strategies. Three failure modes.
 - Wrong eviction policy → stale data under load.
 - LRU is not always right.

6. Event-Driven Design with Kafka
 - Topics are not queues. This distinction matters.
 - Partition key design determines your throughput ceiling.
 - No idempotent consumers → duplicate processing → duplicate charges.

7. Database Indexing Mistakes
 - Too many indexes → writes slow down.
 - Composite index column order matters.
 - No EXPLAIN ANALYZE before deployment → production incident waiting to happen.

8. Circuit Breaker (Resilience4J)
 - Service B slows → Service A waits → everything dies.
 - Circuit Breaker fails fast. Recovers gracefully.
 - CLOSED → OPEN → HALF-OPEN.
Most teams add this after the incident. Not before.

Production will test all 8. Usually at 3am.
Usually on a Friday.