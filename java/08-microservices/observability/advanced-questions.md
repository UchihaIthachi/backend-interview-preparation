# Advanced Questions

## Q66: What are the three pillars of observability? How do you implement each in Java microservices?
Logs (structured JSON), Metrics (Micrometer/Prometheus), Traces (Sleuth/OTEL).

## Q67: How do you implement structured logging in Spring Boot? What fields should every log entry contain?
JSON appender, traceId, spanId, userId, serviceVersion, environment, log level.

## Q68: Explain the difference between RED method and USE method for metrics.
RED = Rate/Errors/Duration (request-oriented); USE = Utilization/Saturation/Errors (resource).

## Q69: How do you create custom Micrometer metrics? Give an example for tracking order processing latency.
Timer.record(), Counter.increment(), Gauge.builder(); tag by outcome, status code.

## Q70: What is OpenTelemetry? How does it differ from Spring Cloud Sleuth?
OTEL = vendor-neutral standard; Sleuth = Spring-specific; OTEL SDK replaces Sleuth in Boot 3.

## Q71: How do you set up alerting for SLO violation in a microservices system?
Define SLI (error rate, latency p99); burn rate alerts; multi-window alerting (1h + 6h).

## Q72: How do you debug a performance issue in production microservices without reproducing locally?
Async profiler, JFR, heap dump analysis, distributed trace sampling, thread dump.


PRODUCTION:
1. Deploying microservices with Docker.
2. Circuit breaker.
3. Rate limiting.
4. Tracing one request across six services.