# Advanced Questions

## Q21: What are REST constraints? How does HATEOAS improve API discoverability?
Stateless, uniform interface, layered system; HAL/JSON-LD links in responses.

## Q22: Explain gRPC vs REST vs GraphQL. When would you choose gRPC for inter-service communication?
Binary proto, HTTP/2 streaming, contract-first; ideal for low-latency internal services.

## Q23: How do you version a REST API? What are the pros and cons of each versioning strategy?
URI versioning, header versioning, content negotiation — backward compatibility concerns.

## Q24: What is idempotency? How do you implement idempotent APIs in a payment service?
Idempotency keys, storing request hash, 409 vs 200 on duplicate, TTL cleanup.

## Q25: How do you handle API rate limiting in a microservices API gateway?
Token bucket, leaky bucket; Redis-based counters; per-user vs per-IP; 429 responses.

## Q26: Explain the difference between synchronous and asynchronous inter-service communication. When to use each?
HTTP/gRPC sync; Kafka/RabbitMQ async; latency tolerance, coupling, resilience trade-offs.

## Q27: How do you design a Pagination API that is cursor-based vs offset-based?
Cursor stable across mutations; offset drifts on inserts; encode cursor as opaque token.

## Q28: What is the Backend for Frontend (BFF) pattern? How does it differ from API Gateway?
Per-client aggregation layer vs generic gateway; mobile BFF vs web BFF; team ownership.

## Q29: How do you implement request/response correlation in async microservices?
Correlation IDs, reply-to queues, temporary queues, timeout handling.

## Q30: What is contract testing with Pact? How does it differ from integration testing?
Consumer-driven contracts; provider verification; decoupled deployability.
