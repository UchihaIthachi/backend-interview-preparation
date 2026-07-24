# Advanced Questions — Observability and Production Diagnostics

## Important Corrections

- Logs, metrics, and traces are commonly called the **three pillars of observability**, but they are not the only useful signals. Continuous profiling, runtime events, audit data, and business events can also be important.
- Spring Cloud Sleuth is for the Spring Boot 2.x generation. Its last minor version is 3.1, and its core tracing functionality moved to **Micrometer Tracing**. For Spring Boot 3 and newer applications, use Micrometer Tracing with OpenTelemetry or Brave rather than adding Sleuth. ([Home][1])
- `userId` should not be treated as a mandatory log field. It may be sensitive and can create privacy, security, and storage concerns.
- A Micrometer `Timer` already records the number of timed operations and their total duration. A separate counter is necessary only when it represents a distinct business metric. ([Micrometer Docs][2])
- A heap dump can pause or significantly affect a production JVM and may contain passwords, tokens, personal data, and request payloads.
- Alerting only on CPU or memory thresholds is insufficient. Alerts should primarily reflect user-visible symptoms or threats to an agreed SLO. ([Prometheus][3])

---

# Q66: What Are the Three Pillars of Observability?

The commonly accepted pillars are:

1. Logs
2. Metrics
3. Traces

```text
Application request
        ↓
Metrics detect a latency increase
        ↓
Trace identifies the slow service
        ↓
Logs explain the specific failure
```

Spring Boot uses Micrometer Observation and Micrometer Tracing to connect application observations with metrics and traces, while OpenTelemetry provides a vendor-neutral framework for producing, collecting, and exporting traces, metrics, and logs. ([Home][4])

---

## 1. Logs

Logs record individual events with contextual information.

Example:

```json
{
  "@timestamp": "2026-07-24T10:30:00.142Z",
  "log.level": "ERROR",
  "service.name": "payment-service",
  "service.version": "2.4.1",
  "deployment.environment": "production",
  "trace.id": "7d16be51abfc42ed",
  "span.id": "fc2014a21200ecff",
  "event.action": "payment_authorization",
  "payment.id": "PAY-100",
  "outcome": "failure",
  "error.code": "PROVIDER_TIMEOUT"
}
```

Logs are best for:

- Exception details
- State transitions
- Business-event context
- Audit information
- Diagnostic messages
- Individual request investigation

Logs should be centralized in a platform such as:

- Elasticsearch and Kibana
- Grafana Loki
- Datadog
- New Relic
- Splunk
- A cloud logging service

---

## 2. Metrics

Metrics are numerical measurements aggregated over time.

Examples:

```text
Requests per second
Error ratio
P95 and P99 latency
JVM heap utilization
Database pool waiting
Kafka consumer lag
Cache hit ratio
```

For Spring Boot:

```text
Spring Boot Actuator
        ↓
Micrometer
        ↓
Prometheus or another registry
        ↓
Grafana and alerts
```

Spring Boot Actuator auto-configures Micrometer and supports multiple monitoring systems, including Prometheus, OTLP, Datadog, New Relic, and others. ([Home][5])

---

## 3. Traces

A distributed trace follows one operation across multiple services.

```text
Trace ID: 7d16be51abfc42ed

API Gateway                12 ms
└── Order Service         180 ms
    ├── PostgreSQL         22 ms
    ├── Inventory Service  35 ms
    └── Payment Service   120 ms
        └── Provider API  105 ms
```

Traces are best for:

- Identifying which service introduced latency
- Understanding synchronous call chains
- Visualizing retries
- Finding database or external API delays
- Correlating logs across services
- Understanding distributed workflows

Spring Boot currently provides Micrometer Tracing auto-configuration for OpenTelemetry with OTLP and Brave with Zipkin. ([Home][6])

---

## Typical Java microservices architecture

```text
Spring Boot Service
├── Structured JSON logs → Log collector → Elastic/Loki
├── Micrometer metrics   → Prometheus → Grafana
└── Micrometer Tracing   → OTLP Collector → Jaeger/Tempo/APM
```

## Interview-ready answer

> The three primary observability signals are logs, metrics, and traces. I use structured JSON logs for detailed event context, Micrometer and Prometheus for numerical measurements and alerts, and Micrometer Tracing with OpenTelemetry for following requests across services. I correlate all three using service identity, environment, trace ID, span ID, operation name, and deployment version.

---

# Q67: How Do You Implement Structured Logging in Spring Boot?

Structured logging writes logs as machine-readable fields rather than unstructured text.

Avoid:

```java
log.info("Order " + orderId + " completed for customer " + customerId);
```

Prefer:

```java
log.atInfo()
        .addKeyValue("event", "order_completed")
        .addKeyValue("orderId", orderId)
        .addKeyValue("outcome", "success")
        .log("Order processing completed");
```

This makes fields searchable independently.

---

## Current Spring Boot configuration

Current Spring Boot supports ECS, GELF, and Logstash-compatible structured JSON formats directly. The format can be selected using `logging.structured.format.console` or `logging.structured.format.file`. ([Home][7])

```yaml
spring:
  application:
    name: order-service

logging:
  structured:
    format:
      console: ecs
```

For older Spring Boot versions, the same result can be achieved using:

- Logstash Logback Encoder
- A custom Logback JSON encoder
- Log4j2 JSON layout

---

## Recommended fields

### Infrastructure context

```text
timestamp
log level
logger
thread
service name
service version
environment
region
instance or Pod name
```

### Request context

```text
traceId
spanId
correlationId
HTTP method
route or normalized endpoint
operation
duration
outcome
```

### Business context

```text
orderId
paymentId
transactionReference
tenantId
business operation
state transition
```

### Error context

```text
error type
stable error code
safe error message
retryable flag
attempt number
```

---

## Fields that require caution

Avoid or carefully control:

- User email
- Phone number
- National identifier
- Complete IP address
- Access token
- Refresh token
- Session cookie
- API key
- Password
- Card number
- Full request or response body

A pseudonymous user or account reference may be logged only when operationally necessary and permitted by the organization’s privacy policy.

---

## Trace correlation

When Micrometer Tracing is configured, Spring Boot includes correlation information derived from `traceId` and `spanId` in logs by default. Custom correlation formatting can be configured through `logging.pattern.correlation`. ([Home][6])

```yaml
logging:
  pattern:
    correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
  include-application-name: false
```

---

## Log semantic events, not implementation noise

Good:

```java
log.atWarn()
        .addKeyValue("event", "inventory_reservation_failed")
        .addKeyValue("orderId", orderId)
        .addKeyValue("productId", productId)
        .addKeyValue("errorCode", "INSUFFICIENT_STOCK")
        .log("Inventory reservation rejected");
```

Less useful:

```java
log.info("Entering method");
log.info("Calling repository");
log.info("Repository returned");
log.info("Leaving method");
```

## Interview-ready answer

> I configure Spring Boot to emit JSON logs and use stable structured fields rather than building text messages through concatenation. Every entry includes service, environment, version, level, timestamp, operation, outcome, and trace context. I add safe business identifiers where necessary, but never log credentials, complete tokens, or sensitive payloads. Logs are written to standard output and collected centrally.

---

# Q68: Explain the RED and USE Methods

RED and USE answer different observability questions.

## RED method

RED is request or service oriented:

```text
R — Rate
E — Errors
D — Duration
```

Examples for an Order API:

| RED signal | Example                  |
| ---------- | ------------------------ |
| Rate       | Requests per second      |
| Errors     | HTTP 5xx ratio           |
| Duration   | P50, P95 and P99 latency |

RED tells you how users are experiencing a service.

```text
Is traffic increasing?
Are requests failing?
Are requests slow?
```

---

## USE method

USE is resource oriented:

```text
U — Utilization
S — Saturation
E — Errors
```

For every resource, examine:

| Resource | Utilization        | Saturation             | Errors               |
| -------- | ------------------ | ---------------------- | -------------------- |
| CPU      | Busy percentage    | Run queue              | CPU/system errors    |
| Memory   | Memory consumed    | Paging or swapping     | Allocation failures  |
| DB pool  | Active connections | Waiting threads        | Acquisition timeouts |
| Executor | Active workers     | Queue size             | Rejected tasks       |
| Disk     | Busy percentage    | I/O queue              | Device errors        |
| Network  | Bandwidth used     | Queued/dropped packets | Transmission errors  |

The USE method is summarized as checking utilization, saturation, and errors for every resource. Saturation is especially important because average utilization can look normal while short periods of queueing create severe latency. ([Brendan Gregg][8])

---

## RED vs USE

| RED                            | USE                                     |
| ------------------------------ | --------------------------------------- |
| Service/request focused        | Resource focused                        |
| Shows user impact              | Helps identify the constrained resource |
| Rate, errors, duration         | Utilization, saturation, errors         |
| Good starting point for alerts | Good starting point for diagnosis       |
| API, consumer or worker view   | CPU, memory, pool, queue or disk view   |

## Investigation example

```text
RED:
P99 checkout latency rose from 300 ms to 5 seconds.

USE:
CPU is only 40%.
DB pool utilization is 100%.
120 request threads are waiting for connections.
```

The application is slow because of saturation, even though CPU is normal.

## Interview-ready answer

> RED monitors user-facing services through request rate, error rate, and duration. USE monitors the underlying resources through utilization, saturation, and errors. I normally use RED to detect that a service is unhealthy and USE to identify whether CPU, memory, a connection pool, a queue, or another resource is causing the problem.

---

# Q69: How Do You Create Custom Micrometer Metrics?

Micrometer provides meter types including:

- `Counter`
- `Timer`
- `Gauge`
- `DistributionSummary`
- `LongTaskTimer`

A `Timer` measures both the frequency and duration of short-running operations. A `Gauge` represents a current sampled value, such as queue size or active workers. ([Micrometer Docs][2])

---

## Example: order-processing metrics

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderProcessor {

    private final Timer successfulOrderTimer;
    private final Timer failedOrderTimer;

    private final Counter successfulOrders;
    private final Counter failedOrders;

    private final AtomicInteger inFlightOrders =
            new AtomicInteger();

    public OrderProcessor(MeterRegistry registry) {
        this.successfulOrderTimer = Timer.builder(
                        "orders.processing.duration"
                )
                .description("Time taken to process an order")
                .tag("outcome", "success")
                .publishPercentileHistogram()
                .register(registry);

        this.failedOrderTimer = Timer.builder(
                        "orders.processing.duration"
                )
                .description("Time taken to process an order")
                .tag("outcome", "failure")
                .publishPercentileHistogram()
                .register(registry);

        this.successfulOrders = Counter.builder(
                        "orders.processed"
                )
                .description("Number of processed orders")
                .tag("outcome", "success")
                .register(registry);

        this.failedOrders = Counter.builder(
                        "orders.processed"
                )
                .description("Number of processed orders")
                .tag("outcome", "failure")
                .register(registry);

        Gauge.builder(
                        "orders.processing.inflight",
                        inFlightOrders,
                        AtomicInteger::get
                )
                .description("Orders currently being processed")
                .register(registry);
    }

    public Order process(CreateOrderCommand command) {
        inFlightOrders.incrementAndGet();
        long startedAt = System.nanoTime();

        try {
            Order order = performProcessing(command);

            successfulOrders.increment();
            successfulOrderTimer.record(
                    System.nanoTime() - startedAt,
                    TimeUnit.NANOSECONDS
            );

            return order;
        } catch (RuntimeException exception) {
            failedOrders.increment();
            failedOrderTimer.record(
                    System.nanoTime() - startedAt,
                    TimeUnit.NANOSECONDS
            );

            throw exception;
        } finally {
            inFlightOrders.decrementAndGet();
        }
    }

    private Order performProcessing(
            CreateOrderCommand command
    ) {
        // Business logic
        return new Order();
    }
}
```

---

## Choosing tags

Good tags have a small bounded set of values:

```text
outcome = success | failure
channel = web | mobile | partner
operation = create | cancel | update
region = ap-south | eu-west
```

Avoid high-cardinality tags:

```text
orderId
customerId
transactionId
traceId
full exception message
raw URL
email
```

Each unique metric-label combination creates another time series. High-cardinality identifiers belong in logs and traces.

---

## Counter

Use a counter for a monotonically increasing total:

```java
paymentFailureCounter.increment();
```

Examples:

- Orders processed
- Payments rejected
- Retries attempted
- Messages sent to a DLT

---

## Timer

Use a timer for short-duration operations:

```java
timer.record(() -> paymentProvider.authorize(request));
```

Timers expose at least count and total time, while histograms and percentiles depend on registry configuration. ([Micrometer Docs][2])

---

## Gauge

Use a gauge for current state:

```text
Current queue size
Current active jobs
Current open sessions
Current connection utilization
```

Do not use a gauge for a value that should be accumulated as a counter. Micrometer also keeps a weak reference to many gauge-observed objects, so the application should retain a strong reference to the observed object. ([Micrometer Docs][9])

## Interview-ready answer

> I use counters for totals, timers for operation latency and frequency, and gauges for current state such as queue depth. I expose low-cardinality tags such as operation and outcome, enable histograms for aggregatable latency percentiles, and avoid identifiers such as order IDs or user IDs in metric labels.

---

# Q70: What Is OpenTelemetry, and How Does It Differ from Sleuth?

## OpenTelemetry

OpenTelemetry is a vendor-neutral observability framework and toolkit for generating, collecting, and exporting:

- Traces
- Metrics
- Logs

It provides:

- APIs
- SDKs
- Instrumentation
- Semantic conventions
- OTLP
- OpenTelemetry Collector

OpenTelemetry does not itself replace a telemetry storage or visualization backend. It sends telemetry to systems such as Jaeger, Tempo, Prometheus-compatible storage, Elastic, Datadog, or another APM. ([OpenTelemetry][10])

---

## Spring Cloud Sleuth

Sleuth provided Spring-specific tracing auto-configuration, instrumentation, context propagation, sampling, and trace-log correlation.

However:

```text
Spring Boot 2.x
→ Spring Cloud Sleuth

Spring Boot 3+
→ Micrometer Tracing
   ├── OpenTelemetry
   └── Brave
```

Spring Cloud Sleuth’s final minor release is 3.1, and it does not support Spring Boot 3.x onward. Its core functionality moved to Micrometer Tracing. ([Home][1])

---

## Comparison

| OpenTelemetry                       | Spring Cloud Sleuth                           |
| ----------------------------------- | --------------------------------------------- |
| Vendor-neutral standard and toolkit | Spring-specific tracing integration           |
| Supports multiple languages         | Primarily Spring and Java                     |
| Covers traces, metrics, and logs    | Primarily distributed tracing                 |
| Provides OTLP and Collector         | Exported using supported tracing integrations |
| Independent of Spring               | Built around the Spring ecosystem             |
| Current strategic standard          | Legacy choice for Boot 2.x                    |

## Micrometer Tracing’s role

Micrometer Tracing acts as a tracing facade for Spring applications.

```text
Spring Observation
        ↓
Micrometer Tracing
        ↓
OpenTelemetry or Brave
        ↓
OTLP/Zipkin exporter
        ↓
Tracing backend
```

Current Spring Boot auto-configuration supports OpenTelemetry with OTLP and Brave with Zipkin. ([Home][6])

## Interview-ready answer

> OpenTelemetry is a vendor-neutral, cross-language standard and toolkit for traces, metrics, and logs. Sleuth was Spring-specific distributed tracing support for Spring Boot 2.x. In modern Spring Boot applications, I use Micrometer Tracing as the Spring facade and OpenTelemetry as the tracing implementation and export standard.

---

# Q71: How Do You Alert on an SLO Violation?

## Step 1: Define the SLI

An **SLI** is the measured indicator.

Availability example:

```text
SLI =
successful requests
/
eligible requests
```

Latency example:

```text
SLI =
requests completed within 500 ms
/
eligible requests
```

Not every request must be included. Health checks, invalid client requests, and intentionally rejected traffic may be excluded according to the documented SLI definition.

---

## Step 2: Define the SLO

Example:

```text
99.9% of checkout requests
must complete successfully
over a rolling 30-day period.
```

For a 99.9% SLO:

```text
Allowed error ratio = 0.1% = 0.001
```

The error budget is:

```text
100% - 99.9% = 0.1%
```

---

## Step 3: Record the error ratio

Conceptual Prometheus recording rules:

```yaml
groups:
  - name: checkout-slo
    rules:
      - record: checkout:error_ratio:rate5m
        expr: |
          sum(rate(checkout_bad_requests_total[5m]))
          /
          sum(rate(checkout_requests_total[5m]))

      - record: checkout:error_ratio:rate1h
        expr: |
          sum(rate(checkout_bad_requests_total[1h]))
          /
          sum(rate(checkout_requests_total[1h]))

      - record: checkout:error_ratio:rate6h
        expr: |
          sum(rate(checkout_bad_requests_total[6h]))
          /
          sum(rate(checkout_requests_total[6h]))
```

---

## Step 4: Alert on error-budget burn rate

Burn rate measures how quickly the service is consuming its error budget.

```text
Burn rate 1
→ budget is being consumed exactly at the allowed rate

Burn rate 10
→ budget is being consumed ten times too quickly
```

Google’s SRE guidance recommends multiwindow, multi-burn-rate alerting as a strong general approach. Example starting points for a 99.9% SLO include:

- 14.4× burn over one hour and five minutes
- 6× burn over six hours and thirty minutes
- Longer-window ticket alerts for slower sustained budget consumption ([Google SRE][11])

```yaml
- alert: CheckoutSloFastBurn
  expr: |
    (
      checkout:error_ratio:rate1h > (14.4 * 0.001)
      and
      checkout:error_ratio:rate5m > (14.4 * 0.001)
    )
    or
    (
      checkout:error_ratio:rate6h > (6 * 0.001)
      and
      checkout:error_ratio:rate30m > (6 * 0.001)
    )
  labels:
    severity: page
  annotations:
    summary: "Checkout is rapidly consuming its error budget"
```

The shorter window verifies that the failure is still active, while the longer window ensures that enough budget has been consumed to justify waking an engineer.

---

## Latency SLO

Suppose the objective is:

```text
99% of requests finish within 500 ms.
```

A histogram bucket can count requests within the threshold:

```promql
sum(rate(http_request_duration_seconds_bucket{le="0.5"}[5m]))
/
sum(rate(http_request_duration_seconds_count[5m]))
```

Prometheus histograms can be aggregated across service instances, but the bucket boundaries must be chosen around meaningful latency objectives. ([Prometheus][12])

---

## Alert design principles

Alerts should include:

- Affected service
- SLO and SLI
- Current burn rate
- Error-budget remaining
- Affected environment and region
- Dashboard
- Trace search
- Runbook
- Owning team

Alert on:

```text
User-visible error ratio
User-visible latency
Stuck business workflows
Data loss or financial risk
Rapid error-budget consumption
```

Do not page merely because:

```text
One Pod restarted
CPU briefly reached 85%
One dependency emitted a warning
```

Prometheus guidance recommends alerting primarily on symptoms connected to user pain and ensuring that page-level alerts are actionable. ([Prometheus][3])

## Interview-ready answer

> I define a measurable SLI, an agreed SLO, and the resulting error budget. I then calculate the ratio of bad events to total eligible events and alert on how quickly that ratio consumes the budget. I use multiwindow, multi-burn-rate alerts so severe failures page quickly while slower sustained failures create lower-priority tickets.

---

# Q72: How Do You Debug a Production Performance Issue That Cannot Be Reproduced Locally?

The goal is to gather evidence from production safely rather than immediately changing configuration or restarting the service.

---

## Step 1: Define the symptom precisely

Determine:

- Which endpoint, event, or job is slow?
- P50, P95, or P99?
- Which region, tenant, or instance?
- Constant or intermittent?
- Which deployment version?
- Did traffic volume or data size change?
- Did configuration or infrastructure change?
- Does the issue affect every request or only specific inputs?

```text
Bad:
“The service is slow.”

Better:
“P99 latency for POST /payments increased from
300 ms to 4.8 seconds in region A after release 2.4.1.”
```

---

## Step 2: Use RED to locate the affected service

Inspect:

- Request rate
- Error rate
- Latency distribution
- Message-processing rate
- Retry rate
- Timeout rate

Compare before and after the incident.

---

## Step 3: Compare fast and slow traces

Example:

```text
Fast trace:
API Gateway             10 ms
Payment Service         90 ms
Database                15 ms
Provider API            55 ms

Slow trace:
API Gateway             10 ms
Payment Service      4,900 ms
DB pool wait         4,500 ms
Database query         300 ms
```

This reveals that the main problem is connection acquisition rather than the SQL execution itself.

For automatic propagation in Spring Boot, HTTP clients should be built using auto-configured `RestTemplateBuilder`, `RestClient.Builder`, or `WebClient.Builder`; bypassing those builders can prevent automatic trace propagation. ([Home][6])

---

## Step 4: Apply USE to the suspected resources

Check:

### CPU

- Process CPU
- Per-thread CPU
- CPU throttling
- Run queue

### Memory

- Heap after GC
- Allocation rate
- Native memory
- Container memory limit
- Swap or paging

### Thread pools

- Active threads
- Queue length
- Rejected tasks
- Blocked threads

### Database pool

- Active connections
- Idle connections
- Pending acquisition
- Acquisition timeout
- Connection hold time

### HTTP client pool

- Leased connections
- Pending requests
- Connection timeout
- Response timeout

### Kafka

- Consumer lag
- Processing time
- Poll interval
- Rebalances
- Hot partitions

---

## Step 5: Capture repeated thread dumps

```bash
for i in 1 2 3 4 5; do
  jcmd <PID> Thread.print -l \
    > "thread-dump-$i.txt"
  sleep 5
done
```

Look for:

- Repeated identical runnable stacks
- Threads waiting for database connections
- Lock contention
- Deadlocks
- Executor starvation
- Network I/O
- Excessive logging
- Infinite loops

A single dump is a snapshot. Repeated dumps show whether threads remain stuck in the same code.

For modern JDKs with large numbers of virtual threads, `Thread.dump_to_file` can produce text or JSON thread dumps:

```bash
jcmd <PID> Thread.dump_to_file \
  -format=json \
  /tmp/threads.json
```

---

## Step 6: Capture Java Flight Recorder data

```bash
jcmd <PID> JFR.start \
  name=production-investigation \
  settings=profile \
  duration=5m \
  filename=/tmp/production-investigation.jfr
```

JFR can capture:

- CPU samples
- Allocation hotspots
- Garbage collection
- Thread contention
- Lock profiles
- Exceptions
- File and socket activity

JFR is integrated into the JDK and is designed for production diagnostics with relatively low overhead, though every production environment should still validate its chosen recording settings. ([Oracle Docs][13])

Analyze the result with JDK Mission Control.

---

## Step 7: Investigate memory only when evidence supports it

Class histogram:

```bash
jcmd <PID> GC.class_histogram \
  > /tmp/class-histogram.txt
```

Heap dump:

```bash
jcmd <PID> GC.heap_dump \
  /var/dumps/application.hprof
```

Automatic heap dump:

```text
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/dumps
```

Heap dumps are one of the most useful artifacts for diagnosing memory leaks, but they can be large, operationally expensive, and security-sensitive. ([Oracle Docs][14])

---

## Step 8: Reproduce the production traffic shape

A local test may fail to reproduce the problem because it lacks:

- Production-sized data
- Hot account or tenant keys
- Real payload-size distribution
- Cache misses
- Concurrent requests
- Downstream latency
- Database lock contention
- Retry behavior
- Connection-pool limits
- Container CPU throttling
- Long-running runtime state

Use a controlled staging or canary environment with:

- Production-like data volume
- Sanitized request shapes
- Realistic concurrency
- Injected downstream latency
- Burst traffic
- Cache-hit and miss scenarios

---

## Step 9: Mitigate before fully fixing

Possible mitigations include:

- Reduce traffic through rate limiting
- Disable an expensive feature
- Roll back a release
- Reduce retry attempts
- Add temporary concurrency limits
- Scale a safe stateless layer
- Stop a problematic batch
- Route traffic away from one region
- Increase a resource only when the bottleneck is understood

Do not automatically:

- Increase every timeout
- Increase every thread pool
- Increase every connection pool
- Add retries
- Restart repeatedly

These actions can hide the symptom or amplify overload.

## Interview-ready answer

> I begin with production telemetry rather than guessing. I scope the affected requests, compare normal and slow distributed traces, and use RED metrics to locate the failing service. I then apply USE to CPU, memory, thread pools, connection pools, queues, and dependencies. I capture repeated thread dumps and a bounded JFR recording, and take a heap dump only when memory evidence justifies its impact. Finally, I reproduce the actual production traffic shape and validate the fix with a canary rollout.

---

# Production Topics

# 1. Deploying Microservices with Docker

A production container should be:

- Built from an immutable image
- Versioned using a tag and preferably an image digest
- Run as a non-root user
- Configured externally
- Given CPU and memory limits
- Capable of graceful shutdown
- Observable through logs, metrics, and traces
- Scanned for vulnerabilities
- Started with readiness and health checks

Example multi-stage Dockerfile:

```dockerfile
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

RUN useradd --system --uid 10001 appuser

WORKDIR /application

COPY --from=build \
    /workspace/target/application.jar \
    application.jar

USER appuser

ENTRYPOINT ["java", "-jar", "application.jar"]
```

Do not bake passwords or environment-specific configuration into the image.

---

# 2. Circuit Breaker

A circuit breaker prevents repeated calls to an unhealthy dependency.

```text
CLOSED
→ calls are permitted

OPEN
→ calls fail fast

HALF_OPEN
→ limited test calls are permitted
```

Use it together with:

```text
Timeout
+
bounded retries
+
circuit breaker
+
bulkhead
+
concurrency limit
```

A circuit breaker does not replace a timeout. A retry does not replace a circuit breaker. A fallback must be truthful.

Safe:

```text
Recommendation service unavailable
→ return cached recommendations
```

Unsafe:

```text
Inventory unavailable
→ claim that stock exists
```

---

# 3. Rate Limiting

For general API traffic, token bucket is often a useful default:

```text
Bucket capacity: 100
Refill rate: 50 tokens/second
Request cost: 1 token
```

Use layered controls:

| Layer       | Purpose                                |
| ----------- | -------------------------------------- |
| CDN/WAF     | Coarse malicious traffic               |
| API Gateway | User, tenant, client, and route quotas |
| Service     | Business and concurrency limits        |
| Consumer    | Backpressure and processing capacity   |

At scale, rate-limit state must normally be shared using an atomic distributed mechanism such as Redis rather than separate counters in every gateway instance.

---

# 4. Tracing One Request Across Six Services

```text
Client
  ↓
API Gateway
  ↓
Order Service
  ↓
Inventory Service
  ↓
Payment Service
  ↓
Fraud Service
  ↓
Notification Service
```

The trace context must propagate over:

- HTTP headers
- gRPC metadata
- Kafka or RabbitMQ message headers
- Asynchronous executor boundaries

Each service creates a child span and writes the trace ID and span ID into its structured logs.

```text
One trace ID
├── Gateway span
├── Order span
├── Inventory span
├── Payment span
├── Fraud span
└── Notification span
```

Do not use a single span for the complete request. Each meaningful remote call or processing operation should have its own span.

---

# Quick Interview Cheat Sheet

| Topic                    | Key answer                               |
| ------------------------ | ---------------------------------------- |
| Three pillars            | Logs, metrics, traces                    |
| Structured logging       | JSON fields, not concatenated text       |
| Trace correlation        | `traceId` and `spanId`                   |
| RED                      | Rate, errors, duration                   |
| USE                      | Utilization, saturation, errors          |
| Counter                  | Monotonically increasing total           |
| Timer                    | Operation count and duration             |
| Gauge                    | Current sampled state                    |
| OpenTelemetry            | Vendor-neutral telemetry framework       |
| Sleuth                   | Boot 2.x tracing integration             |
| Modern Spring tracing    | Micrometer Tracing with OTel or Brave    |
| SLI                      | Actual measured reliability indicator    |
| SLO                      | Agreed reliability objective             |
| Error budget             | Allowed unreliability                    |
| Burn-rate alert          | Alerts based on budget-consumption speed |
| Production CPU diagnosis | Hot thread + repeated dumps + JFR        |
| Memory diagnosis         | Heap trend + histogram + heap dump       |
| Trace across services    | Propagate context through every boundary |

# Senior Interview Summary

> I implement observability using structured logs, low-cardinality Micrometer metrics, and distributed traces through Micrometer Tracing and OpenTelemetry. I monitor services using RED and diagnose constrained resources using USE. Logs and traces share trace and span identifiers, while metrics use bounded dimensions such as service, operation, region, and outcome.
>
> For alerting, I define user-focused SLIs and SLOs and use multiwindow burn-rate alerts rather than relying only on infrastructure thresholds. During a production incident, I compare fast and slow traces, inspect pool and queue saturation, capture repeated thread dumps, and use JFR for CPU, allocation, lock, and GC evidence. Heap dumps are reserved for justified memory investigations because of their operational and security impact.

[1]: https://docs.spring.io/spring-cloud-sleuth/docs/current/reference/html/README.html "Spring Cloud Sleuth"
[2]: https://docs.micrometer.io/micrometer/reference/concepts/timers.html "Timers :: Micrometer"
[3]: https://prometheus.io/docs/practices/alerting/?utm_source=chatgpt.com "Alerting"
[4]: https://docs.spring.io/spring-boot/reference/actuator/observability.html?utm_source=chatgpt.com "Observability :: Spring Boot"
[5]: https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com "Metrics :: Spring Boot"
[6]: https://docs.spring.io/spring-boot/reference/actuator/tracing.html "Tracing :: Spring Boot"
[7]: https://docs.spring.io/spring-boot/reference/features/logging.html "Logging :: Spring Boot"
[8]: https://www.brendangregg.com/usemethod.html?utm_source=chatgpt.com "The USE Method"
[9]: https://docs.micrometer.io/micrometer/reference/concepts/gauges.html "Gauges :: Micrometer"
[10]: https://opentelemetry.io/docs/what-is-opentelemetry/?utm_source=chatgpt.com "What is OpenTelemetry?"
[11]: https://sre.google/workbook/alerting-on-slos/ "Google SRE - Prometheus Alerting: Turn SLOs into Alerts"
[12]: https://prometheus.io/docs/practices/histograms/?utm_source=chatgpt.com "Histograms and summaries"
[13]: https://docs.oracle.com/en/java/javase/21/troubleshoot/diagnostic-tools.html "Diagnostic Tools"
[14]: https://docs.oracle.com/en/java/javase/17/troubleshoot/troubleshooting-memory-leaks.html?utm_source=chatgpt.com "3 Troubleshoot Memory Leaks - Java"
