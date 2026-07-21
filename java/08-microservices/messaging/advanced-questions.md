# Advanced Questions

## Q31: Explain the Outbox Pattern. Why is it needed and how does it solve dual write problems?
Atomic DB write + outbox row; CDC or polling publishes events; guarantees at-least-once.

## Q32: What is Event Sourcing? How is it different from traditional CRUD?
Events as source of truth; replay to rebuild state; snapshots for performance.

## Q33: Explain CQRS (Command Query Responsibility Segregation) and when to apply it.
Separate write model (commands) from read model (projections); eventual consistency.

## Q34: How does Kafka guarantee message ordering? What are the limitations?
Partition-level ordering; single partition = sequential; multiple consumers = parallel.

## Q35: What is the difference between at-least-once, at-most-once, and exactly-once delivery in Kafka?
Producer acks, consumer commits, idempotent producer, transactional APIs.

## Q36: How do you handle poison pill messages in a Kafka consumer?
Dead letter topic, retry topics with exponential backoff (Spring Kafka RetryableTopic).

## Q37: Explain Kafka consumer group rebalancing. What triggers it and how do you minimize disruption?
New consumer, crash, topic change; cooperative rebalancing (incremental); static membership.

## Q38: How would you implement a Saga using Kafka? Walk through a choreography saga.
Each service publishes domain event; next service listens, acts, publishes; compensations.

## Q39: What is RabbitMQ's Exchange types? How does topic exchange differ from direct?
Direct (exact key), Fanout (broadcast), Topic (wildcard), Headers-based routing.

## Q40: How do you ensure message idempotency in an event-driven system?
Event ID deduplication table, UUID per event, idempotent handlers, TTL cleanup.


𝗞𝗮𝗳𝗸𝗮 𝗮𝗻𝗱 𝗠𝗲𝘀𝘀𝗮𝗴𝗶𝗻𝗴
 - Why Kafka over RabbitMQ? When would you choose RabbitMQ?
 - Consumer crashes mid-processing. Message already consumed. What happens?
 - How do you maintain message ordering across partitions?
 - What is idempotency in distributed systems and why does it matter?

KAFKA AND MESSAGING:
1. Why Kafka instead of RabbitMQ.
2. How partitions work.
3. Consumer crashes mid-batch. Now what.
4. How you keep message ordering.
5. Idempotency in distributed systems.
6. Dead letter queues. When and why.

Kafka and Messaging
1. Why Kafka over RabbitMQ? When would you choose RabbitMQ?
2. How do Kafka partitions work internally?
3. Consumer crashes mid-processing. Message already consumed. What happens?
4. How do you maintain message ordering across partitions?
5. What is idempotency in distributed systems and why does it matter?