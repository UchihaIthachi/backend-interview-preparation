# Advanced Questions

## Q96: Design a real-time notification system that can handle 1 million concurrent users.
WebSocket/SSE at gateway, Pub/Sub (Kafka), fan-out per user topic, connection registry.

## Q97: How would you implement a distributed rate limiter as a shared microservice?
Redis sliding window, token bucket with Lua atomicity, per-tenant config, shadow mode.

## Q98: Walk me through designing an Idempotent Payment Service from scratch.
Idempotency key, DB dedup table, state machine (PENDING->SUCCESS/FAILED), retry safety.

## Q99: How do you implement service discovery without a dedicated registry (e.g., Eureka)?
DNS-based (Kubernetes Services), client-side load balancing, headless services.

## Q100: Explain how you would migrate a shared database (used by 3 services) to database-per-service.
Strangler fig on DB; introduce anti-corruption layer; extract tables; sync via events.

## Q101: How do you implement multi-region failover for a critical microservice?
Active-active vs active-passive; global load balancer; conflict resolution for active-active.

## Q102: You inherit a monolith with no tests and 500k LOC. How do you start decomposing it?
Identify seams, add characterization tests, extract leaf domains first, strangle gradually.

## Q103: How would you design an audit log system for compliance across all microservices?
Structured events to Kafka, immutable append-only store (S3/EventStore), correlation IDs.

## Q104: Explain the Competing Consumers pattern and how it affects message ordering guarantees.
Multiple consumers on one queue boost throughput but break ordering; partition by key.

## Q105: Design a distributed configuration management system for 50+ microservices.
Git-backed config server, environment overlays, dynamic refresh, secret management, auditing.
