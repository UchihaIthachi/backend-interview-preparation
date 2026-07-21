# Advanced Questions

## Q13: How does Spring Cloud Config Server work? How do you handle config refresh without restart?
@RefreshScope, Spring Cloud Bus with RabbitMQ/Kafka, actuator /refresh endpoint.

## Q14: Explain Spring Cloud Gateway vs Zuul 1. Why was Zuul 1 deprecated?
Blocking I/O (Zuul 1) vs reactive Netty (Gateway); filter chains, predicates, rate limiting.

## Q15: How do you implement circuit breaker pattern using Resilience4j in Spring Boot?
@CircuitBreaker, states (CLOSED/OPEN/HALF_OPEN), fallback methods, metric thresholds.

## Q17: How do you implement distributed tracing with Spring Cloud Sleuth and Zipkin?
TraceId/SpanId propagation, B3 headers, sampling rates, async context propagation.

## Q18: Explain Feign client vs RestTemplate vs WebClient. When do you use each?
Declarative (Feign), sync (RT), reactive/non-blocking (WebClient) — thread model matters.

## Q20: What is @EnableDiscoveryClient and how does Eureka client registration work internally?
Heartbeat, lease renewal, self-preservation mode, deregistration on shutdown.
