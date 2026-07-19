# System Design Fundamentals

## Capacity estimation

Always estimate before designing:

```text
Daily active users (DAU)
Requests per user per day
-> requests per second (RPS) = DAU * requests_per_user / 86,400
Peak RPS = average RPS * 2-5x (traffic isn't uniform)

Storage = records_per_day * record_size * retention_days
Bandwidth = RPS * average_payload_size
```

Rough numbers matter more than precision — the point is to know whether
you're building for 100 RPS (a single server is plausible) or 100,000 RPS
(you need horizontal scaling, caching, and careful data partitioning from day one).

## Latency vs throughput

- **Latency** — time for a single request to complete.
- **Throughput** — total requests handled per unit time.

These can trade off: batching improves throughput but adds latency per
individual item; adding more parallel workers improves throughput without
necessarily improving individual request latency.

## Availability vs consistency (CAP theorem)

Under a network partition, a distributed system must choose:
- **CP** — remain consistent, reject requests that can't be safely served (e.g. traditional RDBMS in a partition)
- **AP** — remain available, accept the risk of serving stale/inconsistent data (e.g. DNS, many NoSQL stores)

## Scalability

- **Vertical scaling** — bigger machine. Simple, but has a ceiling and a single point of failure.
- **Horizontal scaling** — more machines. Requires statelessness (or careful
  state partitioning) but scales further and improves fault tolerance.

## Interview questions

1. Walk through estimating RPS and storage for a system with 10M daily users.
2. What's the difference between latency and throughput, and how can improving one hurt the other?
3. Explain CAP theorem with a concrete example of a CP vs AP system.
4. Why does horizontal scaling require services to be stateless (or explicitly partitioned)?

## Short interview answer

"I always start with rough capacity numbers — DAU, requests per user, peak
multiplier — because that alone determines whether this is a single-server
problem or one that needs horizontal scaling and partitioning from day one.
For availability trade-offs, I think in CAP terms: under a partition, do we
reject requests to stay consistent, or serve possibly-stale data to stay available?"
