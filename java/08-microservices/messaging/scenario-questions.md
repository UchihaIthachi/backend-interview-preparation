# Scenario-based Questions

**Scenario/Question:** Design a Notification System How would you design for email, SMS, push notifications? How do you make it extensible?

**Scenario/Question:** Event-Driven Architecture When would you use Kafka/RabbitMQ? Pros/cons vs REST?

**Scenario/Question:** Service Communication Sync vs async communication trade-offs?

**Scenario/Question:** How will you design inter-service communication (sync vs async)?

**Scenario/Question:** How will you design event-driven architecture?

**Scenario/Question:** How will you handle duplicate messages in event systems?

**Scenario/Question:** How will you handle message duplication in Kafka?

**Scenario/Question:** How will you handle message ordering issues?

**Scenario/Question:** Kafka consumer is lagging behind - how will you fix it?

**Scenario/Question:** How will you design idempotent message processing?

**Scenario/Question:** How will you handle message retries?

**Scenario/Question:** How will you ensure exactly-once processing?

**Scenario/Question:** How will you handle dead letter queues?

**Scenario/Question:** How will you scale consumers horizontally?

**Scenario/Question:** How will you design topic partitioning?

**Scenario/Question:** How will you handle message ordering?

**Scenario/Question:** How will you debug message loss?

**Scenario/Question:** How will you monitor Kafka performance?

**Scenario/Question:** How will you design event schema evolution?

**Scenario/Question:** How will you secure Kafka communication?

**Scenario/Question:** How will you handle high throughput messaging?

**Scenario/Question:** How will you design event-driven workflows?

**Scenario/Question:** How will you design an event-driven architecture?

**Scenario/Question:** Messages are getting duplicated - how will you handle?



📩 Kafka & Messaging
 
When do you prefer synchronous API calls over asynchronous messaging?
A Kafka consumer processes a message successfully but crashes before committing the offset. What happens next?
How do you guarantee that the same Kafka event isn’t processed multiple times?

KAFKA AND MESSAGING:
1. Why Kafka instead of RabbitMQ.
2. How partitions work.
3. Consumer crashes mid-batch. Now what.
4. How you keep message ordering.
5. Idempotency in distributed systems.
6. Dead letter queues. When and why.

DATABASE AND JPA:
1. First-level vs second-level cache.
2. The N+1 problem.
3. How you find and fix a slow query.
4. How you handle concurrent updates.

SYSTEM DESIGN:
1. Design a payment processing system.
2. Handle 1 million transactions a day.
3. Saga vs 2PC.
4. Transactional outbox. Why it exists.
5. Data consistency across services.
6. Distributed locking.

PRODUCTION:
1. Deploying microservices with Docker.
2. Circuit breaker.
3. Rate limiting.
4. Tracing one request across six services.

In one interview, I was asked:
- Design a Payment Processing System. Handle 10,000 transactions per second.

I froze. I started talking about a REST API and a single database.

The interviewer stopped me.
- What happens if your database goes down mid-transaction?
- What if the same payment request hits your server twice due to a network retry?

I didn't know. And yes, I got rejected. But instead of moving on, I went deeper into the question and learned.

In another interview, two weeks later, I faced the same question. I smiled and explained:

 - At 10,000 TPS, you never hit the database directly. Put Kafka between your API and DB. Accept the request fast. Settle in background.
 - If the same payment hits twice due to a network retry use an idempotency key. Check Redis before processing. Already processed? Return old result. Never run twice.
 - If DB crashes mid-transaction use the Saga pattern. Every step has a compensating action that reverses it on failure. Money never gets lost.

The architecture I led with:
 - User → API Gateway → Kafka → Payment Service
 - Idempotency Check (Redis)
 - DB Transaction (Saga)
 - Kafka absorbs traffic spikes no direct DB hammering
 - Saga handles partial failures with automatic rollback

Interviewer doesn't test whether you can build a payment system.
They test whether you understand what happens when it breaks.

Here are 25 questions that separate engineers who have run systems from engineers who have only built tutorials.

PERFORMANCE AND LATENCY:
1. Your API response jumps from 200 ms to 5 seconds. Where do you look first?
2. Fine at 100 users. Falls over at 10,000. What breaks?
3. CPU is normal but latency is high. What is going on?
4. Is the slowness in your code, the DB, the network, or a downstream call? How do you tell?
5. One query runs fast in dev and takes 10 seconds in prod. Why?
6. Memory creeps up for days, then the app crashes. Find the leak.

RESILIENCE AND FAILURE:
7. A downstream service is slow. How do you stop it taking down your whole app?
8. When do you reach for Circuit Breaker, Retry, Timeout, and Bulkhead?
9. Your DB connection pool is exhausted in production. Likely reasons?
10. Redis is down. Should your app go down with it? Design the fallback.
11. Cache invalidation across multiple running instances. How?

IDEMPOTENCY AND CONSISTENCY:
12. A user double clicks Pay. Retries fire. How do you stop a double charge?
13. Where do you store the idempotency key, and how do you handle two concurrent requests using the same one?
14. A network retry sends the same request twice. Same result both times. How?
15. A transaction that spans multiple microservices. How do you handle it?
16. When do you use Saga, and what happens when a compensation step itself fails?

KAFKA AND MESSAGING:
17. When do you pick synchronous calls over async messaging?
18. A consumer processes a message, then crashes before committing the offset. Now what?
19. How do you stop the same Kafka event being processed twice?

# Advanced Kafka Interview Questions & Production Patterns

**Scenario/Question:** 1. What is Apache Kafka and why is it called a distributed event streaming platform?

**Answer:** Kafka is a distributed event streaming platform used to build real-time data pipelines and streaming applications. It acts as an intermediary (Message Broker) using a Publish/Subscribe model, decoupling microservices so they can communicate asynchronously. It is 'distributed' because it runs as a cluster of brokers, allowing horizontal scalability, fault tolerance, and high availability.

**Scenario/Question:** 2. What are the core components of Kafka?

**Answer:** • **Event/Message**: A piece of data representing something that happened.
• **Producer**: An application that publishes events to Kafka topics.
• **Consumer**: An application that subscribes to topics and reads events.
• **Topic**: A named feed or category where events are published.
• **Partition**: Topics are divided into partitions for parallelism and scalability.
• **Broker**: A Kafka server that stores and serves the events.

**Scenario/Question:** 3. Why are topics divided into partitions and how is ordering maintained?

**Answer:** Partitions enable horizontal scalability by allowing multiple consumers to read from a topic in parallel across different brokers. Kafka guarantees ordering *only within a single partition*. To ensure events related to the same entity (e.g., the same Order ID) are processed in order, producers must use key-based partitioning (using the Order ID as the key), so they are consistently routed to the same partition.

**Scenario/Question:** 4. What is a Consumer Group and how does it work?

**Answer:** A Consumer Group is a set of consumers working together to consume messages from a topic. Kafka assigns each partition of the topic to exactly one consumer within the group. This allows parallel processing. If you have more consumers than partitions, the extra consumers sit idle.

**Scenario/Question:** 5. Consumer Lag Suddenly Spikes in Production. How do you diagnose and fix it?

**Answer:** Consumer lag spikes when producers outpace consumers or consumers freeze. 
*Diagnosis:* Check consumer group lag (`kafka-consumer-groups.sh`). Check GC logs, thread dumps (long pauses freeze `poll()`, triggering `session.timeout` and rebalance). 
*Fix:* Increase `max.poll.interval.ms` or reduce `max.poll.records` to finish processing faster. If there's a rebalance loop, use static group membership (`group.instance.id`). Scale out consumers (up to the number of partitions). Check if a single partition is a hotspot due to a poorly chosen key.

**Scenario/Question:** 6. How do you implement Exactly-Once Semantics in a Payment Microservice (Kafka + DB)?

**Answer:** Enable the idempotent producer (`enable.idempotence=true`) to prevent duplicates from producer retries. For processing, Kafka transactions alone are tricky when writing to an external DB. The best pattern is the **Transactional Outbox Pattern**: Store the business transaction and the Kafka event in the same local DB transaction. A separate relay process publishes the event to Kafka. Alternatively, use Kafka Streams with `processing.guarantee=exactly_once_v2` if all state is in Kafka.

**Scenario/Question:** 7. A Poison Pill Message is Crashing Your Consumer. How do you handle this without data loss?

**Answer:** A malformed message causes a deserialization or processing exception, crashing the consumer before it commits the offset, leading to an infinite retry loop. 
*Fix:* Catch the exception per message. Route bad messages to a Dead Letter Topic (DLT) with original headers and error metadata. Manually commit the offset after publishing to the DLT so the consumer advances. (In Spring Kafka, use `@RetryableTopic` or `DeadLetterPublishingRecoverer` with a `DefaultErrorHandler`).

**Scenario/Question:** 8. Kafka Partition Rebalancing is Causing Duplicate Processing. How do you fix this?

**Answer:** When a pod restarts, Kafka triggers a rebalance. If `enable.auto.commit=true`, the new pod might reprocess messages the old pod processed but hadn't yet committed on its timer. 
*Fix:* Switch to manual commits (`commitSync()` after DB write). Use **Static Membership** (`group.instance.id`) to avoid rebalances on pod restarts entirely. Also, design idempotent processing using a unique DB idempotency key.

**Scenario/Question:** 9. Message Ordering Guarantee Across Partitions: You have events Created -> Shipped -> Delivered, but they arrive out of order.

**Answer:** Kafka only guarantees ordering within a single partition. If you use a null key, messages are sent round-robin across partitions, breaking ordering. 
*Fix:* Always use the `orderId` as the message key. The DefaultPartitioner uses a hash of the key to route it, ensuring all events for the same `orderId` always go to the same partition and are processed sequentially.

**Scenario/Question:** 10. Kafka Topic Compaction is Causing Missing Messages for a new consumer. Why?

**Answer:** Log compaction (`cleanup.policy=compact`) keeps only the latest message per key and removes older duplicates. Tombstone records (null value) mark a key for deletion. A new consumer reading from offset 0 will miss historical updates that were compacted away. 
*Fix:* Ensure the consumer handles tombstones correctly. If point-in-time snapshots or full history is needed, use a changelog topic with snapshots or a Kafka Streams `KTable` instead of relying solely on a compacted topic.

**Scenario/Question:** 11. How do you design a High-Throughput Event Sourcing System with Kafka?

**Answer:** Use an 'account-transactions' topic with `accountId` as the key (guarantees ordering). Size partitions for throughput (e.g., 100 partitions for 100k TPS). Implement CQRS: separate write path (event store) from read path. Use Kafka Streams to aggregate events into an account balance `KTable`. Periodically save state to an 'account-snapshot' topic to avoid replaying the full history on startup.

**Scenario/Question:** 12. Kafka Broker Failure and ISR Shrinkage Under Load: One broker goes down, producers get NotEnoughReplicasException.

**Answer:** The In-Sync Replicas (ISR) shrunk below the `min.insync.replicas` setting. If Replication Factor (RF)=3 and `min.insync.replicas`=3, ONE broker failure blocks all writes. 
*Fix:* Set a practical production config: `replication.factor=3`, `min.insync.replicas=2`, `acks=all`. This tolerates 1 broker failure without data loss and without write unavailability. Set `unclean.leader.election.enable=false` to prevent out-of-sync replicas from becoming leaders, avoiding data loss.

**Scenario/Question:** 13. What are 5 essential Kafka Production Patterns for enterprise Java systems?

**Answer:** 1. **DLQ is not optional**: Unhandled errors block partitions. Always configure a Dead Letter Queue (e.g., `DeadLetterPublishingRecoverer` in Spring).
2. **Kafka has no built-in DLQ**: Error handling lives in your application layer. If you haven't explicitly built it, you don't have it.
3. **HIPAA-safe DLQ Envelope**: Don't put PII/PHI in a plain text DLT. Send only a Correlation ID to Kafka, and store the PII payload in a secure, compliant DB.
4. **Exactly-once semantics**: Use `read_committed` isolation level and manual immediate acknowledgment (`MANUAL_IMMEDIATE`) after success to prevent dirty reads and duplicate processing.
5. **Monitor your DLQ backlog**: Unmonitored DLQs mean silent data loss. Alert on DLQ offset growth (e.g., Prometheus alert if DLT offset > 1000) and wire it to PagerDuty.

