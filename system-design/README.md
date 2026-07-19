# System Design

## Notes

- [Fundamentals](fundamentals.md) — capacity estimation, CAP, scalability
- [Components](components.md) — load balancers, caches, queues, gateways
- [Distributed systems](distributed-systems.md) — replication, idempotency, saga, outbox

## Design problems

Each has its own file under [`design-problems/`](design-problems/) using the
[standard template](#template) below.

- [URL shortener](design-problems/url-shortener.md)
- [Rate limiter](design-problems/rate-limiter.md)
- [Notification system](design-problems/notification-system.md)
- [Payment system](design-problems/payment-system.md)
- [Order processing system](design-problems/order-processing-system.md)

## Template

Every design doc follows: requirements -> capacity estimation -> API design ->
data model -> high-level architecture -> detailed components -> scalability ->
reliability -> security -> observability -> bottlenecks -> trade-offs ->
two-minute summary.
