# Design a Notification System

## 1. Requirements

**Functional**: send email/SMS/push notifications; support retries on
delivery failure.
**Non-functional**: high availability, at-least-once delivery, auditable
history, horizontally scalable.

## 2. Capacity estimation

Estimate peak notifications/sec from expected triggering events (e.g. order
confirmations, marketing campaigns) — campaign sends can spike volume by
orders of magnitude versus steady-state, so design for burst, not just average.

## 3. API design

```http
POST /notifications   { "userId", "channel", "template", "data" }
GET /notifications/{id}
```

## 4. Data model

```text
notifications(id, user_id, channel, status, created_at, sent_at, attempts)
```

## 5. High-level architecture

```text
Client -> Notification API -> Message queue -> Worker pool -> Provider adapters
                                                                (email/SMS/push)
```

## 6. Detailed components

Decouple the API (fast acknowledgment) from actual delivery (queue + workers)
so a slow downstream provider (email/SMS gateway) doesn't block the caller.
Provider adapters isolate provider-specific logic behind a common interface —
a Strategy pattern — so swapping providers doesn't ripple through the codebase.

## 7. Scalability

Workers scale horizontally by consuming from partitioned queue topics; the API
layer is stateless and scales independently of worker count.

## 8. Reliability

Retries with exponential backoff for transient provider failures; a
dead-letter queue for messages that repeatedly fail, so they don't block the
queue or get silently dropped.

## 9. Security

Never log full notification content if it contains PII; validate/sanitize
templates to avoid injection into rendered messages.

## 10. Observability

Track delivery success rate per channel/provider, queue depth (a growing queue
signals workers can't keep up), and end-to-end latency from request to delivery.

## 11. Bottlenecks

A single slow provider can back up its portion of the queue — isolate queues
per channel/provider so an email outage doesn't delay push notifications.

## 12. Trade-offs

At-least-once delivery is simpler to build than exactly-once, but requires
idempotent handling downstream (e.g., a user shouldn't receive the same
"order confirmed" email 3 times after retries).

## 13. Interview summary

"I'd decouple acceptance from delivery with a queue, since notification
providers can be slow or flaky, use per-channel queues so one provider's
outage doesn't block others, and lean on retries with backoff plus a
dead-letter queue rather than trying to guarantee exactly-once delivery."
