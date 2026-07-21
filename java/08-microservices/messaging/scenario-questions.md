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