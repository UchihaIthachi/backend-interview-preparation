# System Design Components

## Load balancers

Distribute incoming requests across multiple backend instances to ensure optimal utilization, prevent overload on any single server, and improve scalability, reliability, and performance. 

Algorithms:
- **Round Robin:** Requests are distributed across servers in a circular order, ensuring a fair distribution. However, it doesn’t consider each server’s current workload.
- **Least Connections:** Requests are sent to the server with the fewest active connections, aiming to balance the load evenly among servers.
- **Least Time:** Requests are directed to the server with the fastest response time and the fewest active connections. This algorithm prioritizes efficiency and responsiveness.
- **Hash / Consistent Hashing:** Requests are distributed based on a unique code derived from the request (like client IP or URL). This ensures consistency, useful for maintaining session persistence or cache-friendly load balancing.
- **Random:** Requests are randomly assigned to available servers. Simple, but may lead to uneven distribution.
- **Random with Two Choices:** Requests are randomly assigned to two servers, and then the less loaded one is selected, reducing the risk of overloading a single server.

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
