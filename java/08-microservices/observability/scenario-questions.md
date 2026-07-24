# Scenario-Based Questions — Microservices Monitoring and Production Investigation

## Important Corrections

- A health endpoint returning `UP` does not prove that the service is performing well. Health, availability, latency, saturation, and business correctness must be monitored separately.
- Do not put every dependency into a **liveness** check. A temporary database or Redis outage should not necessarily cause Kubernetes to restart every application instance.
- A timeout, retry, circuit breaker, and bulkhead solve different problems and are normally used together.
- A trace ID helps correlate telemetry; it is not a financial transaction ID, idempotency key, or audit record.
- Gateway access logs can prove that the server received a request and attempted to produce a response. They cannot prove that the client successfully received and processed that response.
- For critical state, trust the explicitly defined authoritative business store—often a ledger or transactional database—not whichever log or metric happens to look correct.

---

# 1. How Will You Monitor Microservices Health?

Monitoring health requires several layers.

```text
Business workflow health
        ↓
Application health
        ↓
Dependency health
        ↓
JVM and container health
        ↓
Host and infrastructure health
```

## 1.1 Health probes

### Liveness

Answers:

> Is this process irrecoverably stuck and should it be restarted?

Examples of valid liveness failures:

- Deadlocked application
- Corrupted internal state
- Event loop permanently stopped
- Application unable to make progress

Do not normally fail liveness merely because:

- Database is temporarily unavailable
- Redis is unavailable
- Another microservice is down

Restarting every instance will not repair the dependency and can create a restart storm.

### Readiness

Answers:

> Should this instance receive new traffic?

Readiness may fail when:

- Startup is incomplete
- The instance is shutting down
- Required local resources are unavailable
- The service cannot currently serve requests safely
- Connection pools are not initialized

### Startup probe

Protects slow-starting applications from being killed before startup completes.

Kubernetes treats readiness, liveness, and startup as different probe types, and removes unready Pods from normal service traffic without necessarily restarting them. ([Kubernetes][1])

---

## Spring Boot Actuator

Dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Configuration:

```yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - info
          - prometheus

  endpoint:
    health:
      probes:
        enabled: true
      show-details: when_authorized
```

Common endpoints:

```text
/actuator/health
/actuator/health/liveness
/actuator/health/readiness
/actuator/prometheus
```

Spring Boot Actuator provides health, metrics, auditing, and management capabilities for production applications. Sensitive endpoints should be exposed selectively and secured. ([Home][2])

---

## 1.2 RED metrics for every service

Monitor:

```text
Rate
Errors
Duration
```

Examples:

- Requests per second
- HTTP 5xx ratio
- Business rejection ratio
- P50, P95, and P99 latency
- Timeout rate
- Retry rate

## 1.3 USE metrics for resources

Monitor:

```text
Utilization
Saturation
Errors
```

Examples:

| Resource | Utilization         | Saturation           | Errors              |
| -------- | ------------------- | -------------------- | ------------------- |
| CPU      | Busy percentage     | Run queue/throttling | System errors       |
| Heap     | Used/max            | GC pressure          | Allocation failure  |
| DB pool  | Active connections  | Pending borrowers    | Acquisition timeout |
| Executor | Active threads      | Queue depth          | Rejected tasks      |
| Kafka    | Consumer throughput | Lag                  | Processing failures |
| Disk     | Busy percentage     | I/O queue            | Device errors       |

## 1.4 Business health

Technical metrics may be normal while the business workflow is broken.

Track:

- Successful payments
- Failed payments by reason
- Transfers stuck in `PROCESSING`
- Oldest incomplete Saga
- Failed compensations
- Reconciliation mismatches
- Notification-delivery success
- Orders pending beyond their expected duration

## Interview answer

> I monitor health at several levels. Kubernetes probes manage instance lifecycle, Actuator exposes application health, Prometheus collects RED and USE metrics, and distributed tracing and centralized logs support diagnosis. I also expose business-health metrics such as stuck transfers and failed compensations because CPU and HTTP health alone cannot prove that the platform is functioning correctly.

---

# 2. How Will You Handle Distributed Tracing in Spring Cloud?

Modern Spring Boot applications use **Micrometer Tracing**, commonly backed by OpenTelemetry or Brave. Spring Boot uses Micrometer Observation for metrics and traces. ([Home][3])

```text
Spring Boot instrumentation
        ↓
Micrometer Observation
        ↓
Micrometer Tracing
        ↓
OpenTelemetry
        ↓ OTLP
OpenTelemetry Collector
        ↓
Jaeger / Tempo / Elastic / APM
```

## Required trace context

Each distributed operation contains:

- Trace ID — identifies the complete distributed operation
- Span ID — identifies one operation within the trace
- Parent span ID — records the calling relationship
- Sampling decision
- Baggage only when carefully controlled

Example:

```text
Trace ID: abc123

Gateway span
└── Order Service span
    ├── Database span
    ├── Inventory Service span
    └── Payment Service span
```

## HTTP propagation

Use auto-configured HTTP client builders so instrumentation and context propagation are applied:

```java
@Service
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("http://inventory-service")
                .build();
    }

    public InventoryResponse getInventory(String productId) {
        return restClient.get()
                .uri("/inventory/{id}", productId)
                .retrieve()
                .body(InventoryResponse.class);
    }
}
```

## Kafka propagation

Trace context should be propagated through message headers:

```text
Producer span
    ↓ Kafka headers
Consumer receive/process span
```

Also propagate business identifiers separately:

```text
traceId        → observability
correlationId  → workflow correlation
eventId        → message identity
sagaId         → Saga identity
orderId        → domain identity
```

Do not use the trace ID as an idempotency key because traces may be resampled, split, or generated differently during retries.

## Sampling

A high-traffic system may not retain every trace.

Use:

- Probability sampling for normal traffic
- Higher sampling for errors
- Higher sampling for slow requests
- Tail-based sampling where available
- Temporary targeted sampling for a tenant or endpoint

## Interview answer

> I use Micrometer Tracing with OpenTelemetry and propagate standard trace context through HTTP, gRPC, Kafka, and asynchronous executors. Every service creates child spans and includes trace and span IDs in structured logs. Business IDs remain separate from trace IDs. I control cost through sampling while preserving errors and unusually slow requests.

---

# 3. How Will You Debug Latency Between Services?

Use a top-down process.

```text
Detect user impact
        ↓
Locate the slow service
        ↓
Locate the slow span
        ↓
Identify the saturated resource
        ↓
Capture runtime evidence
```

## Step 1: Confirm the symptom

Determine:

- Which endpoint?
- P50, P95, or P99?
- Which region or tenant?
- Constant or intermittent?
- Which application version?
- Did traffic or data volume change?
- Did a deployment or configuration change?

## Step 2: Compare fast and slow traces

Example:

```text
Normal request: 200 ms

Gateway                 10 ms
Order Service           40 ms
Inventory Service       50 ms
Payment Service         80 ms
```

Slow request:

```text
Slow request: 5 seconds

Gateway                 10 ms
Order Service        4,900 ms
├── DB pool wait     4,500 ms
└── SQL execution      300 ms
```

The SQL is not the main bottleneck; connection acquisition is.

## Step 3: Inspect queueing and saturation

Check:

- Database pool pending count
- HTTP client pool pending count
- Executor queue size
- Request-thread usage
- Kafka consumer lag
- Database locks
- CPU throttling
- GC pauses
- Network and DNS latency
- Retry count

## Step 4: Capture evidence

Repeated thread dumps:

```bash
for i in 1 2 3 4 5; do
  jcmd <PID> Thread.print -l > "threads-$i.txt"
  sleep 5
done
```

Java Flight Recording:

```bash
jcmd <PID> JFR.start \
  name=latency-investigation \
  settings=profile \
  duration=5m \
  filename=/tmp/latency-investigation.jfr
```

`jcmd` supports thread inspection, heap diagnostics, and Java Flight Recorder management on a running JVM. ([Oracle Docs][4])

## Interview answer

> I begin with P95 and P99 latency and compare fast and slow distributed traces. Once I identify the slow span, I inspect resource saturation—especially connection-pool waiting, executor queues, locks, downstream latency, GC, and CPU throttling. I then capture repeated thread dumps or a bounded JFR recording rather than increasing timeouts blindly.

---

# 4. How Will You Debug a Failing Microservice in Production?

## Initial runbook

```text
1. Determine user impact.
2. Identify the affected version and instances.
3. Preserve logs and runtime evidence.
4. Check Pod/container termination state.
5. Inspect metrics and traces.
6. Check dependencies.
7. Mitigate safely.
8. Confirm recovery.
9. Complete root-cause analysis.
```

## Kubernetes commands

```bash
kubectl get pods -n <namespace>
kubectl describe pod <pod-name> -n <namespace>
kubectl logs <pod-name> -n <namespace>
kubectl logs <pod-name> -n <namespace> --previous
kubectl get events -n <namespace> --sort-by=.lastTimestamp
kubectl top pod <pod-name> -n <namespace>
```

Look for:

- `OOMKilled`
- Probe failures
- Exit code
- `CrashLoopBackOff`
- Missing secrets
- Invalid configuration
- Node-pressure eviction
- Volume errors
- Deployment replacement

## Check the application

- Startup exception
- Configuration validation failure
- Port binding error
- Database authentication failure
- Missing migration
- Classpath incompatibility
- JVM fatal-error file
- Heap exhaustion
- Native memory exhaustion

## Check dependencies

- Database availability and pool state
- Kafka broker and consumer state
- Redis availability
- DNS resolution
- Identity provider
- External APIs
- Certificates and credentials

## Mitigation options

- Roll back
- Route traffic away from affected instances
- Disable a feature flag
- Scale a safe stateless component
- Stop a harmful batch
- Reduce retries
- Apply temporary rate limits
- Restore a previous configuration

---

# Production Investigation Scenarios

# 1. API Latency Rises from 200 ms to 5 Seconds

Start with:

1. RED dashboard
2. Distributed trace comparison
3. Queue and pool metrics
4. Downstream latency
5. Database locks and plans
6. JVM and container saturation

Likely causes include:

- DB pool waiting
- HTTP connection-pool waiting
- Lock contention
- Retry storm
- Cache miss/stampede
- Slow downstream API
- GC pause
- Thread-pool saturation
- CPU throttling

Do not increase the request timeout first. That allows more slow work to accumulate.

---

# 2. A Downstream Service Is Slow

Use layered resilience:

```text
Timeout
+
limited retry
+
circuit breaker
+
bulkhead
+
concurrency limit
+
load shedding
```

A circuit breaker prevents repeated calls after failure thresholds are exceeded. Spring Cloud Circuit Breaker supports circuit-breaker and time-limiter configuration, while Resilience4j provides retry and bulkhead components. ([Home][5])

Possible fallback:

```text
Recommendation unavailable
→ return cached recommendations
```

Unsafe fallback:

```text
Inventory unavailable
→ claim stock is available
```

---

# 3. Circuit Breaker vs Retry vs Timeout vs Bulkhead

| Pattern         | Purpose                                              |
| --------------- | ---------------------------------------------------- |
| Timeout         | Bounds how long one call can wait                    |
| Retry           | Reattempts a temporary failure                       |
| Circuit breaker | Stops calls to a repeatedly failing dependency       |
| Bulkhead        | Prevents one dependency from consuming all resources |

## Retry only when

- Failure is transient
- Operation is idempotent or protected
- Attempts are bounded
- Backoff and jitter are used
- The total request deadline permits it

## Circuit-breaker states

```text
CLOSED
→ normal calls allowed

OPEN
→ calls fail fast

HALF_OPEN
→ limited test calls determine recovery
```

When open, requests normally receive a controlled failure or safe fallback without calling the unhealthy dependency.

---

# 4. Database Connection Pool Is Exhausted

Possible causes:

- Slow queries
- Long transactions
- Connection leak
- N+1 queries
- Database lock waits
- Remote calls inside transactions
- Too much request concurrency
- Pool undersizing
- Database saturation
- Batch jobs sharing the same pool
- Threads retaining connections while waiting

Check:

```text
active connections
idle connections
pending borrowers
connection acquisition latency
connection hold time
transaction duration
database CPU and locks
```

Do not enlarge the pool until you know whether the database can support more concurrent work.

---

# 5. Duplicate Payment Requests

Require an idempotency key:

```http
POST /payments
Idempotency-Key: checkout-9182-payment
```

Store durably:

```text
idempotency key
request hash
status
payment reference
stored result
created time
expiry
```

Protect it with a database uniqueness constraint:

```sql
CREATE UNIQUE INDEX uk_payment_idempotency
ON payment_request(idempotency_key);
```

A Redis pre-check can improve speed, but durable financial correctness should not depend solely on an expiring cache entry.

---

# 6. Two Concurrent Requests Use the Same Idempotency Key

Unsafe:

```text
SELECT key
→ not found
→ process
```

Both requests can see “not found.”

Safe flow:

```text
Attempt atomic insert of PROCESSING record
    ├── succeeds → own the operation
    └── unique conflict → load existing operation
```

Results:

```text
same key + same request + completed
→ return stored result

same key + same request + processing
→ return pending/conflict

same key + different request
→ reject key reuse
```

---

# 7. Redis Is Down

The answer depends on Redis’s role.

| Redis use                | Possible fallback                              |
| ------------------------ | ---------------------------------------------- |
| Optional cache           | Query DB with concurrency protection           |
| Session store            | Often fail closed or require reauthentication  |
| Rate limiter             | Endpoint-specific fail-open/fail-closed policy |
| Distributed lock         | Stop if correctness requires the lock          |
| Idempotency acceleration | Fall back to durable DB                        |
| Feature data             | Use stale local value when safe                |

For a cache failure, protect the database from a cache stampede with:

- Concurrency limits
- Request coalescing
- Local short-lived cache
- Stale-while-revalidate
- Load shedding

---

# 8. Cache Invalidation Across Multiple Instances

Do not rely only on an in-memory eviction performed by the instance handling the write.

Options:

## Shared distributed cache

All instances use Redis or another shared cache.

## Invalidation event

```text
Service updates database
        ↓
publishes CustomerUpdated
        ↓
all instances evict customer cache entry
```

## Versioned keys

```text
customer:C-100:v8
```

## TTL

Provides eventual cleanup but does not guarantee immediate freshness.

For critical data, define:

- Source of truth
- Allowed staleness
- Invalidation ordering
- Behavior if the invalidation event is lost
- Whether read-through repopulation is safe

---

# 9. Works with 100 Users but Fails with 10,000

Check the first saturated finite resource:

- Request threads
- DB connections
- HTTP connections
- CPU quota
- Heap
- File descriptors
- Queue capacity
- Database locks
- Kafka partitions
- Downstream quotas
- Redis hot keys

Also check:

```text
P95/P99 latency
arrival rate
active concurrency
queue depth
connection wait
retries
timeouts
rejections
```

The failure is often caused by queueing rather than raw CPU usage.

---

# 10. Process a 5 GB CSV Safely

Do not use:

```java
Files.readAllLines(path);
```

Stream the file:

```java
try (BufferedReader reader = Files.newBufferedReader(path)) {
    List<Record> batch = new ArrayList<>(1_000);
    String line;

    while ((line = reader.readLine()) != null) {
        batch.add(parse(line));

        if (batch.size() == 1_000) {
            writeBatch(batch);
            batch.clear();
        }
    }

    if (!batch.isEmpty()) {
        writeBatch(batch);
    }
}
```

Production design:

```text
Upload to object storage
→ return 202 Accepted
→ validate file
→ process in chunks
→ checkpoint committed progress
→ quarantine invalid rows
→ publish completion report
```

Use a real streaming CSV parser rather than `split(",")`.

---

# 11. Synchronous vs Asynchronous Communication

Use synchronous communication when:

- Immediate result is required
- The operation is short
- Caller cannot proceed without the answer
- Failure can be returned directly

Use asynchronous messaging when:

- Work can complete later
- Buffering is valuable
- Producer and consumer should scale independently
- Several consumers react
- Temporary consumer downtime must be tolerated

Request-reply over Kafka is still semantically synchronous if the caller waits for the reply.

---

# 12. Kafka Consumer Crashes Before Offset Commit

Flow:

```text
Process record successfully
→ crash before offset commit
→ restart/rebalance
→ record delivered again
```

Kafka resumes from the committed offset, so processing before committing gives at-least-once behavior and can cause redelivery. ([Apache Kafka][6])

The handler must be idempotent.

---

# 13. Prevent Duplicate Kafka Processing

Use:

- Stable event ID
- Consumer-specific deduplication table
- Unique business constraint
- Conditional state transition
- Provider idempotency key for external calls

```sql
CREATE TABLE processed_event (
    consumer_name VARCHAR(100) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    PRIMARY KEY (consumer_name, event_id)
);
```

Commit the deduplication record and business change in the same local transaction.

---

# 14. Kubernetes Pod Keeps Restarting

Check:

```bash
kubectl describe pod <pod> -n <namespace>
kubectl logs <pod> -n <namespace> --previous
kubectl get events -n <namespace> --sort-by=.lastTimestamp
```

Common reasons:

- OOM kill
- Liveness failure
- Startup timeout
- Application exit
- Invalid secret/configuration
- JVM crash
- Node eviction
- Failed volume mount

Application logs may be empty when the process is killed externally or fails before logging initializes.

---

# 15. CPU Normal but Latency High

The service may be waiting rather than computing.

Check:

- DB pool waiting
- HTTP pool waiting
- Row locks
- Executor queues
- Network latency
- DNS
- Disk I/O
- Downstream service
- Kafka backlog
- Synchronization locks
- Rate-limit delays

Normal average CPU can also hide:

- Container CPU throttling
- One saturated core
- Short CPU spikes
- Long I/O waits

---

# 16. Determine Whether Slowness Is Code, DB, Network, or Dependency

Use span timing:

```text
Request total: 5 seconds

Application computation      100 ms
Database span                300 ms
DB connection wait         3,500 ms
Downstream HTTP call       1,000 ms
Network/other                100 ms
```

Supporting evidence:

| Suspected area | Evidence                           |
| -------------- | ---------------------------------- |
| Application    | CPU profile/JFR                    |
| Database       | Query plan, wait events, locks     |
| Network        | Connection/DNS/TLS timing          |
| Downstream     | Client span and dependency metrics |
| Pool           | Pending count and acquisition time |
| JVM            | GC, threads, allocation, locks     |

---

# 17. Query Fast in Development, Slow in Production

Investigate:

- Different data volume
- Different data distribution
- Missing or different index
- Stale statistics
- Different execution plan
- Lock contention
- Parameter-sensitive plan
- Different database version/configuration
- Cold vs warm cache
- Larger result set
- Replica lag
- Concurrent workload
- Disk or I/O pressure

Capture the exact SQL and bind values. Compare actual execution plans, not only the query text.

---

# 18. Distributed Transactions

Use:

```text
Local transaction
+
Saga
+
transactional outbox
+
idempotent consumers
+
compensation
+
reconciliation
```

A Spring transaction in one service cannot automatically roll back another service’s database or an external provider call.

---

# 19. Saga and Compensation Failures

Use Saga when:

- A business workflow spans independent services
- 2PC is unsuitable
- Eventual consistency is acceptable
- Compensating actions exist

When compensation fails:

```text
COMPENSATION_PENDING
→ bounded retry
→ recovery queue
→ alert
→ MANUAL_REVIEW_REQUIRED
```

Compensations must be durable and idempotent.

---

# 20. Same Request Arrives Twice

Use a stable idempotency key and return the previously stored result.

```text
First request
→ create resource R-100
→ store result

Second request with same key
→ return R-100
```

Bind the key to a request fingerprint so it cannot be reused for a different amount or recipient.

---

# 21. Deploy Without Downtime

Use:

- Multiple replicas
- Rolling, canary, or blue-green deployment
- Readiness probes
- Graceful shutdown
- Backward-compatible APIs
- Backward-compatible database migration
- Backward-compatible events
- Deployment health metrics
- Automatic rollback criteria

Old and new versions may run simultaneously, so schema and contract compatibility is essential.

---

# 22. In-Flight Requests During Pod Termination

During normal termination, Kubernetes begins the grace-period countdown, executes any `PreStop` hook, and terminates the container within the configured grace period. The default Pod grace period is 30 seconds unless changed. ([Kubernetes][7])

Recommended flow:

```text
Pod marked terminating
→ stop receiving new traffic
→ SIGTERM sent
→ application drains bounded in-flight work
→ resources close
→ process exits
```

Spring Boot supports graceful shutdown and allows the shutdown-phase timeout to be configured. ([Home][8])

```yaml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

Kubernetes:

```yaml
spec:
  terpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63: 45
```

Long-running operations should still be idempotent because forced termination can occur after the grace period.

---

# 23. Rate Limiting for Millions of Users

Layer the controls:

```text
CDN/WAF
→ coarse abuse protection

API Gateway
→ tenant/user/client/route limits

Service
→ business and concurrency limits
```

Token Bucket is a strong default for synchronous APIs because it allows controlled bursts while enforcing a sustainable rate.

Distributed gateways need shared atomic state:

```text
Gateway instances
        ↓
Redis-backed atomic limiter
```

Return:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 10
```

Also define:

- Limit key
- Burst allowance
- Request cost
- Fail-open/fail-closed behavior
- Multi-region policy
- Monitoring and abuse response

---

# 24. Memory Grows for Several Days

Determine what grows:

```text
Heap
Native memory
Direct buffers
Thread count
Metaspace
Queue
Cache
Mapped files
```

Monitor post-GC heap usage. A healthy heap typically falls after major collections; a leak often shows a rising retained baseline.

Commands:

```bash
jcmd <PID> GC.heap_info
jcmd <PID> GC.class_histogram
jcmd <PID> Thread.print -l
```

Heap dump:

```bash
jcmd <PID> GC.heap_dump /var/dumps/application.hprof
```

Common causes:

- Unbounded cache
- `ThreadLocal` retention
- Listener leak
- Unbounded executor queue
- Buffered payloads
- Classloader leak
- Unclosed resources
- Direct-buffer leak

Heap dumps may be large, disruptive, and contain sensitive data, so capture and secure them carefully.

---

# 25. Issue Occurs Only Under Heavy Traffic

Use production-safe evidence gathering:

1. Preserve traces and metrics around the incident.
2. Enable bounded JFR recording.
3. Capture repeated thread dumps on an alert.
4. Compare successful-fast and successful-slow requests.
5. Reproduce traffic shape, not only average RPS.
6. Simulate downstream slowness and retries.
7. Test hot keys and production-sized datasets.
8. Introduce controlled fault injection.
9. Validate the fix through a canary.

Heavy-traffic-only defects commonly involve:

- Race conditions
- Queueing
- Pool exhaustion
- Lock contention
- Cache stampede
- Retry storm
- Hot partition
- Memory visibility
- CPU throttling

---

# Production and DevOps

## 1. Service A Calls Slow Service B

Use:

```text
short timeout
→ limited retry when safe
→ circuit breaker
→ bulkhead
→ load shedding
```

A timeout limits one call. A circuit breaker stops continued calls. A bulkhead prevents B from consuming all of A’s resources.

## 2. Circuit Breaker Is Open

Requests fail fast or receive a safe fallback. After the configured wait period, limited trial traffic is allowed in half-open state. Successful trials close the circuit; failures reopen it.

## 3. Docker Container Is Running but API Is Unresponsive

Check:

```bash
docker ps
docker logs <container>
docker inspect <container>
docker stats <container>
docker exec <container> ps -ef
docker exec <container> ss -lntp
```

Verify:

- Correct port is listening
- Port is published
- Application bound to `0.0.0.0`, not only loopback
- Health check
- Thread and connection pools
- CPU/memory limits
- DNS and dependency connectivity
- Firewall/network
- Reverse-proxy configuration

“Container running” only means its main process has not exited.

## 4. Monitor Logs Across Ten Services

Use centralized structured logging:

```text
Services write JSON to stdout
        ↓
Fluent Bit / OTel Collector / agent
        ↓
Elasticsearch / Loki / APM
        ↓
Search by service, environment and trace ID
```

Required fields:

- Timestamp
- Service
- Environment
- Version
- Level
- Trace ID
- Span ID
- Operation
- Outcome
- Safe business reference
- Error code

---

# Advanced Investigation Scenarios

# 61. Transfers Slow but CPU, Memory, and DB Metrics Look Normal

Investigate:

- DB connection-pool waiting
- HTTP client pool waiting
- Network/DNS/TLS latency
- External payment provider
- Thread or executor queues
- Row locks
- Kafka lag
- CPU throttling
- Retry/backoff delays
- Synchronized blocks

The database’s CPU can be normal while the application waits for an available connection or row lock.

---

# 62. Issue Occurs Once Every Three Days

Prepare evidence collection before the next occurrence:

- Continuous low-overhead JFR ring buffer
- Event-triggered JFR dump
- Tail-based tracing
- Structured logs with stable error codes
- Short retention of detailed telemetry
- Automatic thread dumps on saturation
- Heap dump only for an OOM or strong leak evidence
- Deployment/configuration audit trail

Do not permanently enable unrestricted payload logging.

---

# 63. Correlate Banking Logs Without PII

Propagate opaque identifiers:

```text
traceId
correlationId
transactionReference
```

Do not log:

- Full account number
- Token
- Card number
- National ID
- Full customer payload

Use masked or tokenized references only where operationally necessary.

---

# 64. Trace an Asynchronous Transaction

Put context in message headers:

```text
traceparent
tracestate
correlationId
causationId
eventId
sagaId
```

The consumer extracts the context and creates a new consumer-processing span.

Do not rely on thread-local MDC alone because the producer and consumer run on different threads, processes, and times.

---

# 65. Fund Transfer Metrics

## RED

- Requests per second
- P50/P95/P99 latency
- Technical failure ratio
- Timeout ratio

## Business

- Transfers initiated
- Transfers completed
- Transfers rejected by reason
- Transfers stuck in processing
- Compensation failures
- Reconciliation mismatches
- Total transferred value, only when safe and useful

## Resources

- DB pool usage and waiting
- Executor queue depth
- Kafka lag
- Provider latency
- JVM and container metrics

Avoid customer or transaction IDs as Prometheus labels.

---

# 66. Business vs Technical Failures

Use explicit outcome dimensions:

```text
outcome=success
outcome=business_rejection
outcome=technical_failure
```

Examples:

| Failure                   | Type               |
| ------------------------- | ------------------ |
| Insufficient funds        | Business rejection |
| Invalid account state     | Business rejection |
| Database timeout          | Technical failure  |
| Provider connection error | Technical failure  |
| Unhandled exception       | Technical failure  |

Business errors may use `400`, `409`, or `422` depending on the API contract. Do not assume every business failure maps to the same status.

---

# 67. Duplicate SMS but Only One Transaction Request

Investigate the complete delivery chain:

```text
Transaction event
→ notification request
→ Kafka/RabbitMQ delivery
→ SMS worker
→ provider API
→ provider delivery callback
```

Check:

- Duplicate event delivery
- Consumer offset commit
- Worker retry after ambiguous timeout
- Provider-side retry
- Duplicate callback
- Multiple notification consumers
- Missing idempotency key
- Replayed DLT message

Use a durable unique key such as:

```text
notificationId + channel + template/purpose
```

---

# 68. Detect Slow Queries Early

Use:

- Database slow-query log
- Query latency histogram
- APM database spans
- Execution-plan monitoring
- Lock-wait metrics
- Connection-pool acquisition metrics
- Alerts based on P95/P99 and sustained duration

Do not alert on one isolated slow query unless it creates material risk. Track frequency and user impact.

---

# 69. Exceptions Increase Without Visible Customer Impact

Investigate, but choose severity based on risk.

Possible explanations:

- Retries hiding dependency failure
- Fallback masking an outage
- Background job failure
- Data loss in asynchronous processing
- Invalid client traffic
- Expected control-flow exception
- Logging regression

Page immediately when there is:

- Data-loss risk
- Security risk
- Financial risk
- Rapid SLO burn

Otherwise create an actionable warning or ticket. Prometheus recommends paging on symptoms and actionable user impact rather than every possible internal cause. ([Prometheus][9])

---

# 70. Prove Every Transaction Request Received a Response

Gateway access logs can prove:

- Request reached the gateway
- Request ID
- Response status generated
- Response bytes attempted
- Processing duration
- Connection outcome where available

They cannot prove the client successfully received and acted on the response.

For financial auditability, retain:

```text
request ID
idempotency key
received timestamp
authenticated actor
business transaction ID
final authoritative state
response status
response-generation timestamp
audit signature/hash
```

A client-facing status endpoint allows recovery when delivery of the response is ambiguous.

---

# 71. Monitor Cache Effectiveness

Track:

```text
hit ratio = hits / (hits + misses)
```

Also monitor:

- Miss rate
- Eviction rate
- Entry count
- Memory usage
- Load duration
- Cache errors
- Hit latency
- Miss latency
- Stampede/single-flight activity
- Database traffic after misses

Break down only by bounded dimensions such as cache name and outcome.

---

# 72. Alerts for a High-Value Payment Service

Use SLO and risk-based alerts:

- Fast error-budget burn
- P99 latency breach
- Payment success-rate drop
- Duplicate-payment constraint violations
- Queue age and depth
- Oldest processing payment
- Failed compensation
- Reconciliation mismatch
- DB pool acquisition timeout
- Provider error spike
- Idempotency-store failure
- Security/authentication anomaly

A fixed `>1%` error threshold may be suitable for one service but too loose or too strict for another. Base it on the defined SLO and business risk.

---

# 73. Detect a Multi-Day Memory Leak

Monitor:

```text
heap used after GC
allocation rate
object count
thread count
direct-buffer memory
metaspace
process RSS
```

Procedure:

1. Capture class histogram.
2. Capture another later.
3. Identify growing classes.
4. Take a secured heap dump.
5. Analyze dominator tree and retained size.
6. Identify the GC root preventing reclamation.
7. Validate the fix over a sufficiently long soak test.

---

# 74. Transaction Succeeds but Metric Says Failed

First determine the authoritative business source.

For a payment:

```text
Ledger/payment transaction state
→ authoritative business result

Metric
→ telemetry observation
```

Investigate why telemetry disagrees:

- Metric recorded before commit
- Exception after commit
- Retry produced conflicting metrics
- Metric exporter failure
- Incorrect tag
- Asynchronous update delay
- Status transition race

The fix is not merely to “trust the database”; it is to define and reconcile the source of truth explicitly.

---

# 75. Operations Dashboard Design

A useful service dashboard should show:

## User impact

- Request rate
- Error rate
- P50/P95/P99
- SLO burn
- Affected region/tenant where bounded

## Resources

- CPU and throttling
- Heap and GC
- Thread/executor saturation
- DB pool waiting
- HTTP pool waiting
- Queue depth

## Dependencies

- Database latency
- Redis errors
- Kafka lag
- External provider latency
- Circuit state
- Retry rate

## Business workflow

- Success volume
- Stuck operations
- Failed compensations
- Reconciliation mismatches

## Navigation

Each panel should link to:

```text
dashboard
→ relevant traces
→ correlated logs
→ runbook
→ recent deployments
```

---

# Senior Investigation Framework

Use this structure for almost every production scenario:

```text
1. Define user impact and time window.
2. Identify the affected service and version.
3. Compare healthy and unhealthy telemetry.
4. Use RED to locate the problem.
5. Use USE to find the saturated resource.
6. Inspect traces for the slow or failed span.
7. Inspect correlated structured logs.
8. Capture thread dumps or JFR when needed.
9. Check recent code, configuration, and infrastructure changes.
10. Mitigate without amplifying the failure.
11. Validate recovery using user-facing metrics.
12. Preserve evidence and complete root-cause analysis.
```

# Interview-Ready Summary

> I monitor microservices using health probes, RED metrics, USE resource metrics, structured logs, distributed tracing, and business workflow indicators. For Spring Boot, Actuator and Micrometer expose health and metrics, while Micrometer Tracing with OpenTelemetry propagates trace context across HTTP and messaging boundaries.
>
> During an incident, I start with user impact and P95/P99 latency, compare healthy and slow traces, and inspect queue and pool saturation. I use timeouts, bounded retries, circuit breakers, and bulkheads to isolate slow dependencies. For Kubernetes failures, I inspect Pod events, previous container logs, probe results, exit reasons, and memory limits. For JVM problems, I collect repeated thread dumps, JFR recordings, class histograms, or secured heap dumps based on evidence rather than guessing.

[1]: https://kubernetes.io/docs/concepts/workloads/pods/probes/?utm_source=chatgpt.com "Liveness, Readiness, and Startup Probes"
[2]: https://docs.spring.io/spring-boot/reference/actuator/index.html?utm_source=chatgpt.com "Production-ready Features"
[3]: https://docs.spring.io/spring-boot/reference/actuator/observability.html?utm_source=chatgpt.com "Observability :: Spring Boot"
[4]: https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/tooldescr006.html?utm_source=chatgpt.com "2.6 The jcmd Utility"
[5]: https://docs.spring.io/spring-cloud-circuitbreaker/reference/spring-cloud-circuitbreaker-resilience4j/circuit-breaker-properties-configuration.html?utm_source=chatgpt.com "Circuit Breaker Properties Configuration"
[6]: https://kafka.apache.org/08/design/design/?utm_source=chatgpt.com "Design | Apache Kafka"
[7]: https://kubernetes.io/docs/concepts/containers/container-lifecycle-hooks/?utm_source=chatgpt.com "Container Lifecycle Hooks"
[8]: https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html?utm_source=chatgpt.com "Graceful Shutdown :: Spring Boot"
[9]: https://prometheus.io/docs/practices/alerting/?utm_source=chatgpt.com "Alerting"
