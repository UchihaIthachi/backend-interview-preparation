# Distributed Systems Concepts

## Idempotency

An operation is idempotent if performing it multiple times has the same
effect as performing it once. Critical for retries — a network timeout doesn't
tell you whether the server actually processed the request, so the client
must be able to safely retry.

```text
Idempotency key pattern:
1. Client generates a unique key per logical operation (e.g., "create this order")
2. Client sends the key with the request
3. Server checks: has this key been processed before?
   - Yes -> return the previous result, don't reprocess
   - No -> process, store the result keyed by the idempotency key
```

## The outbox pattern

Solves the "dual write" problem: you need to update your database AND publish
an event, but you can't atomically do both across two different systems.

```text
1. In the SAME database transaction as your business update, insert a row
   into an "outbox" table describing the event to publish.
2. A separate poller/relay process reads unpublished outbox rows and publishes
   them to the message broker, then marks them published.
3. Since the outbox insert and the business update are in the same DB
   transaction, they either both happen or neither does -> no lost events.
```

## Saga pattern

For transactions spanning multiple services (no single distributed
transaction), a saga is a sequence of local transactions, each with a defined
compensating action if a later step fails.

```text
Reserve inventory -> Charge payment -> Confirm order
If "Charge payment" fails: run compensating action "Release inventory"
```

## Replication and partitioning

- **Replication** — copies of the same data on multiple nodes, for
  availability and read scaling. Leader-follower is the most common model;
  writes go to the leader, reads can go to followers (at the cost of possible
  replication lag / stale reads).
- **Partitioning (sharding)** — splitting data across nodes by some key, for
  write scaling. Choosing a good shard key (avoiding hotspots) is the hard part.

## Interview questions

1. Why do retries require idempotency, and how do idempotency keys work?
2. What problem does the outbox pattern solve, and why can't you just publish
   the event right after the DB commit without it?
3. Walk through a saga for an order-processing flow, including a compensating action.
4. Replication vs partitioning — what problem does each solve?

## Short interview answer

"Most distributed systems problems come down to: you can't atomically do two
things across two systems. Idempotency keys solve it for retries, the outbox
pattern solves it for 'update DB and publish an event together,' and sagas
solve it for multi-service transactions by using compensating actions instead
of a single atomic commit across services."
