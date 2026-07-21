# Advanced Questions

## Q59: Explain bulkhead pattern. How do you implement thread pool isolation with Resilience4j?
Separate thread pools per downstream; bulkhead prevents cascade; semaphore vs thread pool.

## Q60: What is retry with exponential backoff and jitter? Why is jitter important?
Thundering herd problem; full jitter avoids synchronized retries; max attempts, base delay.

## Q61: How do you implement timeout strategies in a microservice chain to prevent cascading failures?
Timeout < upstream SLA; Hystrix/Resilience4j timeouts; async with deadline propagation.

## Q62: Explain graceful degradation. Give an example in a product recommendation service.
Return cached/default recommendations if ML service is down; user sees stale but not error.

## Q63: How do you test resilience? What is chaos engineering and how would you apply it?
Chaos Monkey, Gremlin, kill pods, inject latency; verify circuit breakers activate.

## Q64: What is the difference between fail-fast and fail-safe patterns?
Fail-fast: throw immediately (circuit open); Fail-safe: return default/null silently.

## Q65: How do you implement health checks for readiness vs liveness probes in Kubernetes?
Liveness = restart dead container; Readiness = remove from LB; startup probe for slow init.
