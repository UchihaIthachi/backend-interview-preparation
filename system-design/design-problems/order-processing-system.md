# Design a Production-Ready Order Processing System (Capstone)

## 1. Requirements

**Functional**: create orders, reserve inventory, process payment, send
confirmation, handle cancellations.
**Non-functional**: consistency for money/inventory, resilience to downstream
failures, horizontal scalability, full observability.

## 2. Capacity estimation

Combine estimates from each sub-system (orders API, payment volume,
notification volume) — this is where the individual design docs above
(payment, notification) compose into one system.

## 3. API design

```http
POST /orders   Idempotency-Key: <uuid>
               { "customerId", "items": [...] }
```

## 4. Data model

```text
orders(id, customer_id, status, total, idempotency_key UNIQUE, created_at)
order_items(id, order_id, product_id, quantity, price)
outbox_events(id, aggregate_id, event_type, payload, published, created_at)
```

## 5. High-level architecture

```text
Client -> API Gateway -> Order Service -> PostgreSQL (order + outbox, 1 txn)
                                        -> Outbox relay -> Kafka
                                                              |
                              --------------------------------
                              |                    |
                     Inventory Service      Payment Service
                              |                    |
                        (reserve/release)    (charge/refund)
                                                     |
                                          Notification Service (Redis-cached
                                          user preferences, queued delivery)
```

## 6. Detailed components — the saga

```text
1. Order created (PENDING) — order + outbox event written in one DB transaction
2. Inventory Service reserves stock -> emits InventoryReserved or InventoryFailed
3. On InventoryReserved: Payment Service charges -> emits PaymentSucceeded or PaymentFailed
4. On PaymentFailed: compensating action -> release inventory reservation
5. On PaymentSucceeded: order marked CONFIRMED -> Notification Service sends confirmation
```

## 7. Scalability

Each service scales independently; Kafka partitioning by order ID keeps
per-order event ordering while allowing horizontal consumer scaling.

## 8. Reliability

Retries with exponential backoff + jitter for downstream calls; circuit
breakers to stop hammering a failing downstream service; the outbox pattern
guarantees no event is lost even if the process crashes right after the DB commit.

## 9. Security

Idempotency keys on order creation and payment charge; least-privilege service
credentials between internal services; no PII in logs.

## 10. Observability

Distributed tracing across the full order -> inventory -> payment ->
notification chain, so a slow or failed order can be diagnosed end-to-end
instead of guessing which service is at fault.

## 11. Bottlenecks

The payment gateway is usually the tightest external constraint — isolate it
behind its own service and queue so payment gateway slowness doesn't back up
order creation itself.

## 12. Trade-offs

A saga (local transactions + compensating actions) is chosen over a
distributed transaction because no practical distributed transaction protocol
spans independently-deployed microservices and an external payment gateway —
the trade-off is temporary inconsistency (an order briefly PENDING) in
exchange for actually being buildable and scalable.

## 13. Interview summary

"This system composes patterns from the payment, inventory, and notification
sub-designs into one saga: each step is a local transaction with the outbox
pattern guaranteeing reliable event publishing, and a defined compensating
action if a later step fails — like releasing inventory if payment fails.
Idempotency keys at each entry point make client retries safe throughout."
