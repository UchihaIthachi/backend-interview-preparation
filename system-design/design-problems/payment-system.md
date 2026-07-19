# Design a Payment Processing Service

## 1. Requirements

**Functional**: charge a payment method, support refunds, prevent duplicate
charges.
**Non-functional**: strong consistency for money movement, auditability,
security/compliance (PCI-DSS considerations), high availability.

## 2. Capacity estimation

Payments are typically much lower volume than reads elsewhere in a system but
have zero tolerance for data loss or double-processing — design priorities
here are correctness and auditability over raw throughput.

## 3. API design

```http
POST /payments   Idempotency-Key: <client-generated-uuid>
                 { "orderId", "amount", "paymentMethodId" }
```

## 4. Data model

```text
payments(id, order_id, amount, status, idempotency_key UNIQUE, created_at)
```

## 5. High-level architecture

```text
Client -> Payment API -> Idempotency check -> Payment gateway (Stripe/etc.)
                                             -> DB (transactional write)
                                             -> Outbox -> downstream events
```

## 6. Detailed components

A unique constraint on `idempotency_key` at the database level is the actual
safety net — even if application logic has a bug, the DB will reject a
duplicate charge attempt with the same key.

## 7. Scalability

Payments volume is rarely the bottleneck; the external payment gateway's rate
limits usually are — batch or queue if you ever need to exceed provider limits.

## 8. Reliability

Use the outbox pattern to publish "payment succeeded" events reliably in the
same transaction as the DB write — never publish the event as a separate,
un-transactional step after commit.

## 9. Security

PCI-DSS scope reduction: never store raw card numbers — use the payment
gateway's tokenization so your own systems only ever see tokens, not real
card data.

## 10. Observability

Track payment success/failure rate, gateway latency, and reconciliation
discrepancies between your records and the gateway's records (run a
scheduled reconciliation job).

## 11. Bottlenecks

Duplicate payment scenario: two concurrent requests for the same order. This
is solved with the idempotency key's DB-level uniqueness constraint plus a
row-level lock or optimistic locking on the order during charge processing.

## 12. Trade-offs

Synchronous charge (wait for gateway response) is simpler but ties up a
request thread for the gateway's latency; async processing with a status
polling/webhook model scales better but adds complexity for the client.

## 13. Interview summary

"The core problem is preventing double charges under concurrent retries. A
unique DB constraint on a client-supplied idempotency key is the actual
enforcement point — everything else, like the outbox pattern for reliably
publishing payment events, exists to support that core guarantee without
introducing a separate way to duplicate work."
