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
