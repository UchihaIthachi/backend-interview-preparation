# Advanced Questions

## Q81: How do you identify and fix a memory leak in a running Java microservice?
Heap dump (jmap/jcmd), MAT analysis, GC logs, metaspace leak vs heap leak patterns.

## Q82: What JVM GC tuning would you apply to a low-latency microservice handling 10k RPS?
G1GC vs ZGC vs Shenandoah; pause targets, region sizing, IHOP tuning.

## Q83: Explain Virtual Threads (Project Loom) and their impact on microservice throughput.
Eliminates thread-per-request bottleneck; platform thread parking replaced; Spring Boot 3.2+.

## Q84: How do you implement caching in a microservice? What are cache invalidation strategies?
L1 (Caffeine) + L2 (Redis); TTL, event-driven invalidation, cache-aside, write-through.

## Q85: What is the difference between horizontal and vertical scaling? When does horizontal scaling fail?
Horizontal fails with stateful sessions, external locks, non-idempotent operations.

## Q86: How would you design a rate limiter that works across multiple instances of a microservice?
Centralized Redis (token bucket), sliding window log, Lua script for atomicity.

## Q87: What is backpressure? How do you implement it in a reactive Spring WebFlux service?
Publisher slows to consumer capacity; Flux.limitRate(), onBackpressureBuffer/Drop/Error.

## Q88: How do you optimize a Spring Boot microservice's cold start time for serverless deployment?
GraalVM native image, CRaC, AppCDS class sharing, lazy bean initialization.
