# Java Order Service (Project Plan)

A Spring Boot service implementing the order creation flow described in
[`system-design/design-problems/order-processing-system.md`](../../system-design/design-problems/order-processing-system.md),
scoped down to a buildable, finishable size.

## Scope (v1)

- `POST /orders` — create an order, with `Idempotency-Key` header support
- `GET /orders/{id}` — fetch order status
- PostgreSQL persistence via Spring Data JPA
- Outbox table + a simple polling relay (no Kafka needed for v1 — log the
  event instead; add Kafka in v2 once v1 works end-to-end)
- Global exception handling via `@RestControllerAdvice`
- Unit tests (service layer, Mockito) + integration tests (Testcontainers + Postgres)

## Suggested structure

```text
java-order-service/
├── README.md
├── pom.xml
├── docker-compose.yml        # postgres for local dev + tests
├── src/main/java/.../order/
│   ├── OrderController.java
│   ├── OrderService.java
│   ├── OrderRepository.java
│   ├── Order.java
│   ├── OutboxEvent.java
│   └── GlobalExceptionHandler.java
├── src/main/resources/
│   └── application.yml
└── src/test/java/.../order/
    ├── OrderServiceTest.java          # Mockito unit tests
    └── OrderControllerIntegrationTest.java  # Testcontainers + MockMvc
```

## v2 ideas (only after v1 is fully working and tested)

- Real Kafka outbox relay instead of logging
- Add the inventory reservation step from the capstone design
- Add Redis caching for `GET /orders/{id}`

## Definition of "done" for v1

- [ ] `docker-compose up` brings up Postgres and the app together
- [ ] Duplicate `POST /orders` with the same idempotency key returns the same result, doesn't double-create
- [ ] Unit tests pass (`mvn test`)
- [ ] Integration test with Testcontainers passes
- [ ] README documents how to run it and try each endpoint with curl
