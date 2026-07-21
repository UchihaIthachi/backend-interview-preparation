# Scenario-based Questions

**Scenario/Question:** How will you monitor microservices health?

**Scenario/Question:** How will you handle distributed tracing in Spring Cloud?

**Scenario/Question:** How will you debug latency issues between services?

**Scenario/Question:** How will you debug a failing microservice in production?



## Production Investigation Scenarios

1. Your API response time suddenly increases from 200 ms to 5 seconds. How would you investigate the root cause?
2. A downstream microservice is slow. How do you prevent it from affecting your entire application?
3. When would you use a Circuit Breaker, Retry, Timeout, and Bulkhead pattern?
4. Your database connection pool is exhausted in production. What could be the possible reasons?
5. How would you handle duplicate payment requests caused by retries or multiple button clicks?
6. Where would you store an idempotency key, and how would you handle concurrent requests with the same key?
7. Redis is down. Should your application also go down? How would you design the fallback?
8. How would you solve the cache invalidation problem when multiple service instances are running?
9. An API works perfectly with 100 users but fails with 10,000 concurrent users. What would you check first?
10. How would you process a 5 GB CSV file without causing an OutOfMemoryError?
11. When would you choose synchronous communication over asynchronous messaging between microservices?
12. What happens if a Kafka consumer processes a message successfully but crashes before committing the offset?
13. How would you prevent the same Kafka event from being processed twice?
14. Your Kubernetes pod keeps restarting, but application logs show no clear error. How would you debug it?
15. CPU usage is normal, but API latency is very high. What could be happening?
16. How would you identify whether a performance issue is caused by application code, the database, network latency, or a downstream service?
17. One SQL query is taking 10 seconds in production but runs quickly in the development environment. How would you investigate?
18. How would you handle distributed transactions across multiple microservices?
19. When would you use the Saga pattern, and how would you handle compensation failures?
20. Your service receives the same request twice because of a network retry. How would you guarantee consistent results?
21. How would you deploy a new version of a Spring Boot service without downtime?
22. What happens to in-flight requests during pod termination, and how would you handle graceful shutdown?
23. How would you design rate limiting for an API used by millions of users?
24. Your application memory keeps increasing slowly and eventually crashes after several days. How would you investigate the memory leak?
25. A production issue happens only under heavy traffic and cannot be reproduced locally. What is your debugging approach?

Production and DevOps
1. Microservice A calls B. B is slow. A keeps waiting. Entire system slows down. What pattern fixes this?
2. Circuit Breaker is open. What happens to requests? How does it recover?
3. Your Docker container is running but API is not responding. Where do you start?
4. How do you monitor logs across 10 microservices simultaneously?

# Advanced Interview Questions & Answers

**Scenario/Question:** 61. Customers report slow fund transfers, but CPU, memory, and database metrics look normal. What else would you investigate?

**Answer:** Check network latency, thread pool exhaustion, garbage collection pauses, third-party API response times (using distributed tracing), and database connection pool saturation.

**Scenario/Question:** 62. A production issue occurs only once every three days. How would you collect enough evidence to diagnose it?

**Answer:** Implement comprehensive structured logging, keep highly detailed traces using sampling, configure alerts on specific error codes, and capture heap dumps automatically on OutOfMemory errors.

**Scenario/Question:** 63. How would you correlate logs across multiple banking microservices without exposing sensitive customer data?

**Answer:** Use a unique, opaque Correlation ID (trace ID) passed in HTTP headers across all services. Log this ID with every log statement, ensuring no PII is logged alongside it.

**Scenario/Question:** 64. A transaction disappears from the logs due to asynchronous processing. How would you trace it end-to-end?

**Answer:** Propagate the Trace ID into the message queue metadata (Kafka headers or JMS properties) so the consuming service can pick it up and continue logging with the same context.

**Scenario/Question:** 65. What metrics would you expose for a fund transfer API?

**Answer:** Throughput (requests/sec), latency (p50, p95, p99), error rate (4xx, 5xx), business metrics (successful transfers vs failed), and resource metrics (thread pool usage).

**Scenario/Question:** 66. How would you distinguish between business failures and technical failures in monitoring dashboards?

**Answer:** Map technical errors to 5xx HTTP codes and business errors (insufficient funds) to 4xx codes. Use distinct tags in metrics (e.g., `error_type=technical` vs `error_type=business`).

**Scenario/Question:** 67. A customer complains about duplicate SMS notifications, but transaction logs show only one request. How would you investigate?

**Answer:** Check the SMS gateway logs for network retries, investigate the message queue for uncommitted consumer offsets causing redelivery, and verify if the SMS service itself lacks idempotency.

**Scenario/Question:** 68. How would you detect slow database queries before customers notice performance issues?

**Answer:** Enable database slow query logs, monitor APM tools (e.g., Dynatrace, New Relic) for database span latency, and alert on query execution times exceeding specific thresholds.

**Scenario/Question:** 69. A sudden increase in exception count has no visible customer impact. Would you treat it as critical? Why?

**Answer:** Yes, it must be investigated. It could indicate failing background jobs, ignored errors that cause silent data corruption, or resilience mechanisms (like retries) masking a failing downstream dependency.

**Scenario/Question:** 70. How would you prove to auditors that every transaction request received a response?

**Answer:** Use access logs at the API Gateway level to record every incoming request and outgoing response, storing them in a secure, immutable log management system (e.g., Splunk).

**Scenario/Question:** 71. How would you monitor cache effectiveness in production?

**Answer:** Track the cache hit ratio (Hits / (Hits + Misses)), eviction rates, memory usage, and latency differences between cache hits vs cache misses using tools like Prometheus and Grafana.

**Scenario/Question:** 72. What alerts would you configure for a high-value payment service?

**Answer:** Alerts on API error rate spike (>1%), p99 latency threshold breach, Queue depth exceeding limits, drop in successful transaction volume (anomaly detection), and dependency health check failures.

**Scenario/Question:** 73. How would you identify memory leaks that occur only after several days of uptime?

**Answer:** Monitor JVM heap usage over time for a "sawtooth" pattern that trends upward. Take periodic heap dumps and analyze them with tools like Eclipse MAT to identify objects surviving multiple GC cycles.

**Scenario/Question:** 74. A transaction succeeds, but metrics show it as failed. Which source of truth would you trust and why?

**Answer:** Trust the database state (transaction tables) as the ultimate source of truth. Metrics are often eventually consistent or sampled and can miss data due to monitoring agent failures.

**Scenario/Question:** 75. How would you build dashboards that help operations teams resolve incidents quickly?

**Answer:** Build RED (Rate, Errors, Duration) dashboards. Group metrics by service, include links to related traces/logs, show dependency health, and highlight clear "Golden Signals" for immediate situational awareness.

