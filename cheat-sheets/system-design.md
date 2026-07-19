# System Design Cheat Sheet (final revision)

- Always estimate capacity first: DAU -> RPS -> storage -> bandwidth.
- CAP: under a partition, choose consistency (CP) or availability (AP).
- Cache-aside is the default caching pattern; TTL + eviction policy (usually LRU).
- Idempotency keys make client retries safe; enforce uniqueness at the DB level.
- Outbox pattern solves "update DB + publish event" atomically via one DB transaction.
- Saga pattern: sequence of local transactions + compensating actions, for
  multi-service transactions with no distributed transaction available.
- Rate limiting: token bucket (bursts allowed) vs sliding window (smoother, more accurate).
- Replication scales reads / adds availability; partitioning (sharding) scales writes.
- Message queues: usually at-least-once delivery -> consumers must be idempotent.
