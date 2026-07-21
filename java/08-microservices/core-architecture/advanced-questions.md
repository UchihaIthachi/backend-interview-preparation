# Advanced Questions

## Q1: What is the difference between Microservices and SOA? When would you choose one over the other?
Focus on coupling, communication protocols, data ownership, and team independence.

## Q2: Explain the Strangler Fig Pattern. How would you use it to migrate a monolith to microservices?
Talk about incremental migration, routing strategies, and rollback mechanisms.

## Q3: What are the SOLID principles as they apply to microservices design?
Map Single Responsibility -> service boundaries, Interface Segregation -> API contracts.

## Q4: How do you define service boundaries using Domain-Driven Design (DDD)?
Bounded contexts, ubiquitous language, aggregates, and anti-corruption layers.

## Q5: What is the difference between orchestration and choreography in microservices?
Central orchestrator vs event-driven peer communication; saga pattern fits here.

## Q6: How do you handle distributed transactions across multiple microservices?
2PC problems, Saga pattern (choreography/orchestration), eventual consistency.

## Q7: What are the twelve-factor app principles? Which ones are most critical for microservices?
Config, backing services, stateless processes, logs as streams are key.

## Q8: Explain the Sidecar and Ambassador patterns. Give a real-world use case for each.
Sidecar = proxy/logging/config; Ambassador = protocol translation at ingress.

## Q9: How would you design an e-commerce system using microservices? Identify key services and their boundaries.
Order, Inventory, Payment, User, Notification—discuss data ownership per service.

## Q10: What are the trade-offs of microservices vs monolith for a startup with 5 engineers?
Operational complexity vs velocity; premature decomposition is a real anti-pattern.


𝗛𝗲𝗿𝗲’𝘀 𝘄𝗵𝗮𝘁 𝘀𝗲𝗽𝗮𝗿𝗮𝘁𝗲𝘀 𝗺𝗶𝗱-𝗹𝗲𝘃𝗲𝗹 𝗱𝗲𝘃𝘀 𝗳𝗿𝗼𝗺 𝘀𝗲𝗻𝗶𝗼𝗿 𝗲𝗻𝗴𝗶𝗻𝗲𝗲𝗿𝘀:
Instead of: “I know multithreading.” → They ask: “How do you handle thread safety in high-concurrency systems?”
Instead of: “I can build REST APIs,” → “How do you design an idempotent API for financial transactions?”
Instead of: “I use Hibernate.” → “How do you optimize database access and prevent N+1 queries at scale?”

Senior Java engineers don’t just write services; they engineer distributed systems.
 - Concurrency & parallelism at scale
 - Transactions & data consistency
 - Circuit breakers, retries, fault tolerance
 - JVM performance tuning & memory leaks
 - Designing APIs for scale and resilience

SPRING BOOT AND MICROSERVICES:
1. How JWT authentication actually works.
2. OAuth2 vs JWT. This one is a trap.
3. RestTemplate vs WebClient vs RestClient.
4. How two microservices talk to each other.
5. What an API Gateway earns its place doing.
6. Global exception handling.
7. How @Transactional works internally.
8. Why self-invocation silently skips the proxy.

IDEMPOTENCY AND CONSISTENCY:
12. A user double clicks Pay. Retries fire. How do you stop a double charge?
13. Where do you store the idempotency key, and how do you handle two concurrent requests using the same one?
14. A network retry sends the same request twice. Same result both times. How?
15. A transaction that spans multiple microservices. How do you handle it?
16. When do you use Saga, and what happens when a compensation step itself fails?

System Design and Production
16. Your payment API receives same request twice due to network retry. How do you prevent double processing?
17. Microservice A calls Microservice B. B is down. A keeps retrying. B never recovers. What pattern solves this?
18. Your Redis cache is working. But database load is still at 100%. What is wrong?
19. API response time is 20ms normally. Spikes to 8 seconds under load. No errors in logs. Where do you start?
20. New feature deployed Friday evening. By Monday morning everything is slow. Nobody touched code over weekend. What happened?