# Monitoring Tools in Microservices

## Overview

Monitoring is essential in microservices because one user request may pass through multiple independently deployed services, databases, message brokers, caches, and external systems.

A useful observability platform should help answer:

```text
What happened?
Where did it happen?
Why did it happen?
Who was affected?
How long has it been happening?
```

Spring Boot describes observability through three primary signals:

1. **Metrics** — numerical measurements over time
2. **Logs** — detailed records of discrete events
3. **Traces** — the path of a request across distributed components ([Home][1])

---

# 1. Monitoring vs Observability

## Monitoring

Monitoring checks known conditions:

```text
Is CPU above 90%?
Is the service returning 5xx errors?
Is Kafka consumer lag increasing?
Is the database connection pool exhausted?
```

It is especially useful for:

- Dashboards
- Threshold alerts
- Capacity planning
- Availability checks
- Known failure modes

## Observability

Observability helps investigate unexpected conditions using telemetry emitted by the system.

Example:

```text
Checkout latency increased to eight seconds
        ↓
Trace shows five seconds waiting for a DB connection
        ↓
Metrics show pool saturation
        ↓
Logs identify a long-running transaction
```

Monitoring tells you that something is wrong. Observability helps explain why.

---

# 2. The Three Main Signals

## Metrics

Metrics are aggregated numerical measurements.

Examples:

```text
HTTP requests per second
HTTP error rate
P95 response time
JVM heap usage
Database connection usage
Kafka consumer lag
Cache hit ratio
```

Metrics are efficient for dashboards, trends, and alerts.

## Logs

Logs describe individual events.

Examples:

```text
Order O-100 was created.
Payment PAY-200 was rejected.
Database connection timed out.
Kafka message E-300 failed validation.
```

Logs provide detailed context but are usually more expensive to store and search than aggregated metrics.

## Traces

A distributed trace follows one operation across several components.

```text
Trace ID: abc-123

API Gateway               15 ms
└── Order Service         80 ms
    ├── PostgreSQL        20 ms
    ├── Inventory API    400 ms
    └── Payment API      120 ms
```

A trace contains **spans**, where each span represents one operation such as an HTTP call, database query, or message-processing step.

---

# 3. Prometheus

## Type

Prometheus is an open-source monitoring and alerting system designed around time-series metrics.

It normally collects metrics by periodically scraping HTTP endpoints exposed by applications and infrastructure components. Metrics are stored as time series identified by a metric name and labels. ([Prometheus][2])

## Architecture

```text
Spring Boot Services
    ↓ /actuator/prometheus
Prometheus
    ├── stores time-series metrics
    ├── evaluates recording rules
    └── evaluates alert rules
             ↓
        Alertmanager
```

Alertmanager receives alerts from Prometheus and handles concerns such as grouping, inhibition, silencing, and notification routing. ([Prometheus][3])

## What Prometheus monitors

### Application metrics

- HTTP request count
- Request duration
- Error rate
- Business operations
- Retry counts
- Circuit-breaker state

### JVM metrics

- Heap usage
- Garbage collections
- Thread count
- Class loading
- CPU usage
- Buffer memory

### Infrastructure metrics

- Host CPU
- Memory
- Disk
- Network
- Container usage
- Kubernetes Pod health

### Dependency metrics

- Database connection pools
- Kafka lag
- Redis latency
- HTTP client latency

## Example metric

```text
http_server_requests_seconds_count{
  application="order-service",
  method="POST",
  status="200"
}
```

## PromQL example

Request rate over five minutes:

```promql
sum(
  rate(
    http_server_requests_seconds_count{
      application="order-service"
    }[5m]
  )
)
```

Server error ratio:

```promql
sum(
  rate(
    http_server_requests_seconds_count{
      application="order-service",
      status=~"5.."
    }[5m]
  )
)
/
sum(
  rate(
    http_server_requests_seconds_count{
      application="order-service"
    }[5m]
  )
)
```

## Important limitation: label cardinality

Avoid unbounded labels such as:

```text
userId
orderId
transactionId
requestId
emailAddress
full URL containing IDs
```

This is dangerous:

```text
payment_processed_total{
  transactionId="TX-928391"
}
```

Every unique label combination creates another time series, increasing storage, memory, and query cost.

Use low-cardinality labels:

```text
service
environment
region
method
status
operation
result
```

Put individual transaction IDs in logs and traces, not metric labels.

---

# 4. Grafana

## Type

Grafana is a visualization, exploration, and alerting platform. It connects to data sources such as Prometheus, Loki, Elasticsearch, SQL databases, and cloud monitoring platforms. ([Grafana Labs][4])

Grafana generally queries telemetry stored elsewhere; it should not be confused with Prometheus or Elasticsearch, which store the underlying data.

## Architecture

```text
Prometheus ───────┐
Loki ─────────────┤
Elasticsearch ────┼──→ Grafana dashboards
Jaeger/Tempo ─────┤
PostgreSQL ───────┘
```

## Typical dashboards

### Service dashboard

- Request rate
- Error rate
- P50, P95, and P99 latency
- Active requests
- Dependency failures
- JVM memory
- Thread count
- Deployment version

### Kafka dashboard

- Produce and consume rate
- Consumer lag by partition
- Rebalances
- Under-replicated partitions
- Dead-letter message count

### Database dashboard

- Active connections
- Pending connection requests
- Slow queries
- Query latency
- Locks and deadlocks
- CPU and storage

### Business dashboard

- Orders per minute
- Payment success rate
- Failed transfers
- Pending Sagas
- Notification delivery rate

## Observability as code

Grafana supports managing dashboards and related observability configuration programmatically so they can be version-controlled and deployed through CI/CD rather than edited only through the UI. ([Grafana Labs][5])

---

# 5. Elastic Stack — Elasticsearch, Logstash, and Kibana

## Type

The Elastic Stack is commonly used for centralized log ingestion, storage, searching, and visualization.

Its major components include:

- **Elasticsearch** — indexes, stores, and searches data
- **Logstash** — processes, enriches, and routes data
- **Kibana** — searches and visualizes Elasticsearch data
- **Elastic Agent or Beats** — collects and ships telemetry

Modern Elastic deployments are broader than the original “ELK” combination and may use Elastic Agent or Filebeat instead of sending every service directly through Logstash. ([elastic.co][6])

## Architecture

```text
Microservices
    ↓ stdout JSON logs
Log collector
    ├── Elastic Agent
    ├── Filebeat
    ├── Fluent Bit
    └── Logstash
          ↓
    Elasticsearch
          ↓
        Kibana
```

Kibana allows logs stored in Elasticsearch to be searched, filtered, explored, and visualized centrally rather than requiring engineers to connect to individual hosts. ([elastic.co][7])

## Structured logging

Avoid:

```java
log.info("Payment failed");
```

Prefer:

```java
log.error(
        "event=payment_failed paymentId={} orderId={} provider={} errorCode={}",
        paymentId,
        orderId,
        provider,
        errorCode
);
```

Useful fields include:

```text
timestamp
level
service
environment
applicationVersion
traceId
spanId
correlationId
operation
resourceId
outcome
errorCode
instanceId
```

## Sensitive logging

Never log:

- Passwords
- Complete JWTs
- Refresh tokens
- Session cookies
- API keys
- Private keys
- Full card details
- Sensitive personal payloads

Prefer safe identifiers:

```text
paymentId=PAY-100
customerId=C-200
errorReference=ERR-300
```

---

# 6. Zipkin and Jaeger

## Type

Zipkin and Jaeger are distributed-tracing backends that store and visualize request traces.

They help answer:

- Which service caused the delay?
- Which dependency failed?
- How many retries occurred?
- Which database query was slow?
- Where did the trace stop?
- Which requests were affected?

## Trace structure

```text
Trace: checkout-123

Span 1: API Gateway
Span 2: Order Service
Span 3: Inventory Service
Span 4: Payment Service
Span 5: PostgreSQL query
```

## Jaeger architecture

Current Jaeger architecture supports collector, query, ingester, and all-in-one roles. The all-in-one in-memory setup is suitable primarily for development, while scalable deployments separate collection, querying, and persistent storage. Jaeger currently recommends the OpenTelemetry Collector for agent or sidecar telemetry collection when metrics and logs are also involved. ([Jaeger][8])

## Zipkin

Zipkin provides a trace backend and user interface and can be started locally using its Java distribution or container image. ([Zipkin][9])

## Sampling

Recording every trace can become expensive in a high-volume system.

Common strategies include:

- Probability sampling
- Always sample errors
- Always sample slow requests
- Sample specific tenants or endpoints
- Tail-based sampling after the complete trace is available

A low normal sampling rate can be combined with higher sampling for failures and slow operations.

---

# 7. Micrometer

## Type

Micrometer is an application instrumentation facade used by Spring applications to record metrics and observations through a consistent API.

It integrates with monitoring backends, but Micrometer itself is not:

- A metrics database
- A dashboard
- A log-storage system
- A complete APM server

Micrometer provides instrumentation abstractions for many observability systems, while Spring Boot uses Micrometer Observation for metrics and tracing integration. ([Micrometer Application Observability][10])

## Example custom metric

```java
@Service
public class OrderService {

    private final Counter orderCreatedCounter;

    public OrderService(MeterRegistry meterRegistry) {
        this.orderCreatedCounter = Counter.builder("orders.created")
                .description("Number of created orders")
                .register(meterRegistry);
    }

    public Order createOrder(CreateOrderCommand command) {
        Order order = create(command);
        orderCreatedCounter.increment();
        return order;
    }
}
```

Timer example:

```java
@Service
public class PaymentService {

    private final Timer authorizationTimer;

    public PaymentService(MeterRegistry meterRegistry) {
        this.authorizationTimer = Timer.builder("payment.authorization")
                .description("Payment authorization duration")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    public PaymentResult authorize(PaymentRequest request) {
        return authorizationTimer.record(
                () -> performAuthorization(request)
        );
    }
}
```

Do not create a metric tag from every payment ID:

```java
// Avoid this high-cardinality tag.
meterRegistry.counter(
        "payments.processed",
        "paymentId",
        paymentId
).increment();
```

---

# 8. OpenTelemetry

OpenTelemetry is a vendor-neutral framework for generating, collecting, and exporting telemetry such as traces, metrics, and logs. It provides APIs, SDKs, semantic conventions, auto-instrumentation, and the OpenTelemetry Collector. ([OpenTelemetry][11])

## Architecture

```text
Spring Boot applications
        ↓ OTLP
OpenTelemetry Collector
        ├── enriches telemetry
        ├── batches data
        ├── applies sampling
        ├── removes sensitive fields
        └── exports to backends
              ├── Prometheus
              ├── Jaeger
              ├── Grafana Tempo
              ├── Elastic
              └── Commercial APM
```

## Why use a collector?

The application sends telemetry to one nearby endpoint instead of integrating directly with every vendor.

The collector can provide:

- Batching
- Retry
- Filtering
- Redaction
- Attribute enrichment
- Sampling
- Backend routing

---

# 9. New Relic, Datadog, and AppDynamics

## Type

These are commercial observability and APM platforms.

They typically provide integrated capabilities such as:

- Application metrics
- Distributed tracing
- Error tracking
- Database monitoring
- Infrastructure monitoring
- Log management
- Service maps
- Alerting
- Anomaly detection
- Code-level performance analysis

New Relic’s APM documentation, for example, describes transaction traces, error tracking, database-query analysis, external-call monitoring, dashboards, and service relationships. ([New Relic Documentation][12])

## Benefits

- Faster initial setup
- Integrated user experience
- Managed storage
- Vendor support
- Built-in service maps
- Automatic instrumentation
- Preconfigured dashboards

## Trade-offs

- Licensing cost
- Telemetry ingestion cost
- Vendor dependency
- Data residency concerns
- Agent overhead
- Need to control high-volume logs and custom attributes

---

# 10. Recommended Microservices Observability Architecture

```text
                           ┌─────────────────────┐
                           │ Spring Boot Service │
                           └──────────┬──────────┘
                                      │
             ┌────────────────────────┼────────────────────────┐
             │                        │                        │
             ↓                        ↓                        ↓
     Metrics endpoint            Structured logs          OTLP traces
 /actuator/prometheus               stdout                   │
             │                        │                       │
             ↓                        ↓                       ↓
       Prometheus             Log collector          OTel Collector
             │                        │                       │
             ↓                        ↓                       ↓
          Grafana            Elasticsearch/Loki       Jaeger/Tempo
             │                        │                       │
             └────────────────────────┴───────────────────────┘
                                      ↓
                           Dashboards and alerts
```

The most important part is not the number of tools. It is the ability to move from one signal to another:

```text
Alert
→ dashboard
→ trace
→ related logs
→ root cause
```

---

# 11. Spring Boot Metrics Setup

## Dependencies

For Prometheus metrics:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

## Configuration

```yaml
spring:
  application:
    name: order-service

management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - info
          - metrics
          - prometheus

  endpoint:
    health:
      probes:
        enabled: true

  metrics:
    tags:
      application: ${spring.application.name}
```

Spring Boot exposes Prometheus-formatted metrics through `/actuator/prometheus` when the relevant registry is present and the endpoint is explicitly exposed. ([Home][13])

Prometheus configuration:

```yaml
scrape_configs:
  - job_name: order-service
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - order-service:8080
```

## Actuator security

Do not expose every actuator endpoint publicly.

Potentially sensitive endpoints include:

```text
/env
/configprops
/beans
/heapdump
/logfile
/loggers
```

Spring Boot allows endpoint exposure and access to be controlled individually. The `heapdump` endpoint can return a JVM heap dump containing application memory, so it must be especially protected. ([Home][14])

---

# 12. Spring Boot Tracing

Current Spring Boot tracing support is based on Micrometer Tracing and supports OpenTelemetry with OTLP as well as Brave with Zipkin. ([Home][15])

Example tracing configuration:

```yaml
management:
  tracing:
    sampling:
      probability: 0.1
```

A probability of `0.1` means approximately ten percent of traces are sampled.

For development, temporarily using `1.0` may be acceptable. For a high-traffic production service, recording every trace may generate excessive storage and network overhead.

When Micrometer Tracing is enabled, Spring Boot can add `traceId` and `spanId` correlation information to application logs, allowing an engineer to navigate between log entries and a distributed trace. ([Home][15])

---

# 13. What Should Be Monitored?

## The RED method for services

### Rate

```text
Requests per second
Messages consumed per second
Orders processed per minute
```

### Errors

```text
HTTP 5xx ratio
Failed payment ratio
Kafka processing failures
Timeout rate
```

### Duration

```text
P50 latency
P95 latency
P99 latency
Maximum latency
```

## The USE method for resources

### Utilization

```text
CPU percentage
Heap utilization
Connection usage
Disk usage
```

### Saturation

```text
Queued requests
Threads waiting for DB connections
Kafka consumer lag
CPU throttling
Disk queue depth
```

### Errors

```text
Disk errors
Network failures
Connection timeouts
Container restarts
```

## JVM metrics

Monitor:

- Heap used and maximum
- Post-GC heap usage
- GC pause duration
- Allocation rate
- Thread count
- Deadlocked threads
- CPU usage
- Loaded classes
- Direct buffer usage

## Database metrics

Monitor:

- Connection-pool active count
- Pending connection requests
- Acquisition time
- Query duration
- Lock waits
- Deadlocks
- Transaction duration
- Replication lag

## Messaging metrics

Monitor:

- Produce rate
- Consume rate
- Consumer lag
- Oldest message age
- Retry-topic depth
- Dead-letter-topic growth
- Rebalance frequency
- Processing failure rate

## Business metrics

Monitor:

- Payment authorization rate
- Payment success ratio
- Orders pending too long
- Failed Saga compensations
- Inventory reservations
- Notification delivery success
- Reconciliation mismatches

Technical metrics may appear normal while the business workflow is broken.

---

# 14. Alerting Strategy

## Alert on symptoms, not only causes

Weak alert:

```text
CPU > 80%
```

CPU may be high while users experience no problem.

Stronger alert:

```text
P99 checkout latency exceeds the SLO
and the error rate is increasing
```

## Useful alerts

- Service availability below target
- P95/P99 latency above SLO
- Sudden 5xx increase
- Database pool acquisition timeout
- Kafka consumer lag continuously increasing
- Dead-letter backlog growing
- Pod restart loop
- Heap remains high after GC
- Disk nearly full
- Saga remains incomplete beyond its deadline
- Payment reconciliation mismatch

## Avoid alert fatigue

Do not create alerts for every transient spike.

Use:

- Minimum duration
- Severity levels
- Grouping
- Deduplication
- Maintenance silences
- Actionable descriptions
- Runbook links
- Clear ownership

Every alert should answer:

```text
What is wrong?
Who is affected?
What should the responder inspect first?
Which team owns it?
```

---

# 15. Common Monitoring Mistakes

## Mistake 1: Using only dashboards

Dashboards are useful after someone knows there is a problem. Important user-impacting conditions also require alerts.

## Mistake 2: Monitoring only CPU and memory

A service can have normal CPU while waiting several seconds for:

- A database connection
- A row lock
- A downstream API
- A message backlog
- Disk I/O

## Mistake 3: Logging everything

Excessive logging increases:

- Storage cost
- Network usage
- Search noise
- Security exposure
- Application overhead

## Mistake 4: High-cardinality metric labels

Do not use IDs, full URLs, or exception messages as metric labels.

## Mistake 5: Missing correlation

A log without `traceId`, `service`, `environment`, and operation context is difficult to connect to the rest of the request.

## Mistake 6: Treating health checks as monitoring

A health endpoint might return `UP` while:

- P99 latency is ten seconds
- Kafka lag is one million records
- Half the requests fail
- The database pool is saturated

## Mistake 7: Exposing observability endpoints publicly

Metrics, configuration, heap dumps, and logs can reveal internal architecture and sensitive information.

## Mistake 8: Sampling away every failure

Sampling policies should preserve important errors and unusually slow traces where possible.

---

# 16. Tool Comparison

| Tool                          | Primary purpose                          |                                       Stores data? | Typical use                             |
| ----------------------------- | ---------------------------------------- | -------------------------------------------------: | --------------------------------------- |
| Prometheus                    | Metrics and alert-rule evaluation        |                                                Yes | Service and infrastructure metrics      |
| Grafana                       | Visualization and alerting               |                    Usually queries another backend | Dashboards across multiple data sources |
| Elasticsearch                 | Searchable telemetry storage             |                                                Yes | Centralized logs and analytics          |
| Logstash                      | Data processing pipeline                 |                              Not the primary store | Parse, enrich, and route logs           |
| Kibana                        | Elasticsearch visualization              |                          No separate primary store | Log searching and dashboards            |
| Zipkin                        | Distributed tracing                      |                          Yes, depending on backend | Trace visualization                     |
| Jaeger                        | Distributed tracing platform             |                       Yes, through storage backend | Trace collection and analysis           |
| Micrometer                    | Java instrumentation facade              |                                                 No | Record metrics and observations         |
| OpenTelemetry                 | Telemetry instrumentation and collection | Collector routes rather than being the final store | Vendor-neutral telemetry pipeline       |
| Datadog/New Relic/AppDynamics | Managed APM and observability            |                                       Yes, managed | Integrated enterprise monitoring        |

---

# Interview Questions and Answers

## Q1: What is the difference between Prometheus and Grafana?

> Prometheus collects and stores time-series metrics and evaluates alert rules. Grafana connects to Prometheus and other data sources to visualize, explore, and alert on the data. Grafana is not a replacement for the Prometheus metrics store.

## Q2: What is Micrometer?

> Micrometer is an instrumentation facade used by Spring Boot to record metrics and observations consistently. A registry exports those metrics to a backend such as Prometheus, Datadog, or another monitoring platform.

## Q3: Why do microservices need distributed tracing?

> A single request can cross several services and dependencies. Distributed tracing records the complete path as spans, allowing engineers to identify which service, database query, message handler, or external call contributed to the latency or failure.

## Q4: What should every log contain?

> At minimum, logs should contain a timestamp, level, service, environment, operation, result, and trace or correlation identifier. Business-resource identifiers may be included when safe, but secrets, complete tokens, and sensitive payloads must not be logged.

## Q5: What is the difference between logs, metrics, and traces?

> Metrics show trends and are efficient for alerts. Logs provide detailed event context. Traces show the path and timing of a distributed request. The strongest observability solution correlates all three.

## Q6: How do you monitor a Spring Boot service?

> I enable Actuator, export Micrometer metrics through the Prometheus endpoint, add JVM and business metrics, enable Micrometer Tracing with OpenTelemetry or Zipkin-compatible tracing, produce structured correlated logs, and build Grafana or APM dashboards and alerts around latency, traffic, errors, saturation, and business outcomes.

## Q7: What metrics are most important?

> I begin with request rate, error rate, and latency, then monitor resource utilization, saturation, and errors. I add dependency metrics such as database pool waiting and Kafka lag, plus business metrics such as payment success rate and stuck workflows.

## Q8: How do you trace one request across six services?

> I propagate standard trace context across HTTP and messaging boundaries. Each service creates child spans and writes the trace ID into its logs. The trace backend then reconstructs the complete request path, while the shared trace ID allows related logs to be searched.

---

# Interview-Ready Summary

> In a microservices platform, I use metrics, logs, and traces together. Spring Boot Actuator and Micrometer instrument the services. Prometheus collects time-series metrics, Grafana visualizes them, and Alertmanager routes alerts. Structured logs are collected centrally into Elastic or Loki, while OpenTelemetry exports traces to Jaeger, Zipkin, Tempo, or a commercial APM.
>
> I monitor the RED signals—rate, errors, and duration—for every service and the USE signals—utilization, saturation, and errors—for infrastructure. I also monitor JVM health, connection pools, Kafka lag, retries, dead-letter messages, and business workflows. Every signal includes consistent service, environment, and trace context so an engineer can move from an alert to a dashboard, then to the relevant trace and logs.

[1]: https://docs.spring.io/spring-boot/reference/actuator/observability.html?utm_source=chatgpt.com "Observability :: Spring Boot"
[2]: https://prometheus.io/docs/introduction/overview/?utm_source=chatgpt.com "Overview | Prometheus"
[3]: https://prometheus.io/docs/alerting/latest/alertmanager/ "Alertmanager | Prometheus"
[4]: https://grafana.com/docs/grafana/latest/introduction/?utm_source=chatgpt.com "About Grafana | Grafana documentation"
[5]: https://grafana.com/docs/grafana/latest/as-code/observability-as-code/?utm_source=chatgpt.com "Observability as code | Grafana documentation"
[6]: https://www.elastic.co/docs/get-started/the-stack?utm_source=chatgpt.com "The Elastic Stack | Elastic Docs"
[7]: https://www.elastic.co/docs/solutions/observability/logs/explore-logs?utm_source=chatgpt.com "Explore logs | Elastic Docs"
[8]: https://www.jaegertracing.io/docs/latest/architecture/ "Architecture | Jaeger"
[9]: https://zipkin.io/pages/quickstart.html "
    
      Quickstart · OpenZipkin
    
  "
[10]: https://micrometer.io/docs/?utm_source=chatgpt.com "Micrometer Documentation"
[11]: https://opentelemetry.io/docs/what-is-opentelemetry/ "What is OpenTelemetry? | OpenTelemetry"
[12]: https://docs.newrelic.com/docs/apm/new-relic-apm/getting-started/introduction-apm/ "Improve your app performance with APM | New Relic Documentation"
[13]: https://docs.spring.io/spring-boot/reference/actuator/metrics.html "Metrics :: Spring Boot"
[14]: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html "Endpoints :: Spring Boot"
[15]: https://docs.spring.io/spring-boot/reference/actuator/tracing.html "Tracing :: Spring Boot"
