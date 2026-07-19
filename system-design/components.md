# System Design Components

## Load balancers

Distribute incoming requests across multiple backend instances. Algorithms:
round robin, least connections, consistent hashing (useful when you want the
same client/key to consistently hit the same backend, e.g. for caching).

## API gateways

A single entry point handling cross-cutting concerns (auth, rate limiting,
routing to the right microservice) so individual services don't each
reimplement them.

## Caching

```text
Cache-aside: application checks cache first, falls back to DB on miss, then
             populates cache
Write-through: writes go to cache and DB together
TTL: expire entries after a fixed time to bound staleness
Eviction: LRU (least recently used) is the most common default policy
```

Cache invalidation is famously hard — stale data after an update is the most
common caching bug; consider short TTLs plus explicit invalidation on write for
data that must stay fresh.

## Databases (relational vs NoSQL)

Relational: strong consistency, joins, transactions — good default for
structured, relationally-connected data (orders, payments, users).
NoSQL (document/key-value/wide-column): flexible schema, horizontal
scalability, often weaker consistency guarantees — good for high-write-volume,
loosely-structured, or massively-partitioned data.

## Message queues

Decouple producers from consumers, absorb traffic spikes, enable async
processing. Trade-off: adds operational complexity and requires idempotent
consumers since most queues offer at-least-once delivery, not exactly-once.

## Rate limiting

```text
Token bucket: tokens refill at a fixed rate; a request consumes a token; empty
              bucket = request rejected. Allows bursts up to bucket size.
Fixed window: counter per time window resets each interval. Simple, but can
              allow 2x burst at window boundaries.
Sliding window: smooths out the boundary-burst problem at the cost of more
                bookkeeping.
```

## Interview questions

1. How does consistent hashing help with cache-friendly load balancing?
2. Cache-aside vs write-through — what's the trade-off?
3. Why is cache invalidation considered hard, concretely?
4. Token bucket vs fixed window rate limiting — what's the practical difference?
5. Why do most message queues require idempotent consumers?

## Short interview answer

"Most of these components exist to solve one of two problems: spreading load
(load balancers, horizontal scaling) or absorbing bursts and decoupling
components (caches, queues). The recurring theme in all of them is that
they trade strong consistency for availability/performance — which is why
cache invalidation and exactly-once processing are both notoriously hard."
