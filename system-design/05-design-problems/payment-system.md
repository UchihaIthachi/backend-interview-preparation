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

our payment service is processing 10,000 transactions per second. Suddenly response time jumps from 20ms to 8 seconds. No errors in logs. No alerts fired. Business is losing money every second. How do you debug this?

This question separates tutorial developers from production engineers.
STAR Framework Answer (with real project reflection)

Situation: In production, our payment service response time spiked from 20ms to 8 seconds during peak traffic. No exceptions in logs. No OOM errors. No infrastructure alerts. But transactions were timing out and customers were getting charged without order confirmation.

Task: As the backend engineer on call, my task was to identify the root cause across the entire request chain without taking the service down and restore normal response times within minutes.

Action: First I checked what changed. Recent deployments. Config changes. Traffic patterns. Nothing obvious. Then I followed the request.

Step 1: Check thread pool ExecutorService thread pool was at 100% utilization. All threads waiting. No threads available to process new requests. This was the symptom not the cause.

Step 2: Find what threads were waiting on Thread dump showed all threads blocked on database connection pool. HikariCP pool size was 10. All 10 connections held open. None being released.

Step 3: Find why connections weren't releasing One specific query introduced in last deployment. Missing index on a foreign key column. Full table scan on every transaction. 100ms query became 7 second query under load. Connection held for 7 seconds per transaction. Pool exhausted in seconds.

Step 4: Immediate fix Increased connection pool size temporarily bought 10 minutes. Added database index query dropped from 7 seconds to 8ms. Thread pool cleared instantly. Response time back to 20ms.

Step 5: Permanent fix Added query performance testing to CI/CD pipeline. EXPLAIN ANALYZE on every new query before deployment. Connection pool monitoring alert at 70% utilization. Never caught this way again.

Result: Service restored in 11 minutes. Root cause identified and fixed permanently. Incident led to mandatory query review process for all future deployments. Zero recurrence in 14 months.

The concepts behind this answer thread pools, connection pooling, query optimization, HikariCP internals.