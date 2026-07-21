# Scenario-based Questions

**Scenario/Question:** Concurrency Issue Multiple users are updating the same record, causing inconsistent data. How would you handle this in Java/Spring Boot? Compare optimistic vs pessimistic locking in this scenario. How would you test this issue?

**Scenario/Question:** Java + Backend + Concurrency A multi-threaded service is causing inconsistent data updates - how will you fix it?

**Scenario/Question:** How will you handle race conditions in a high-concurrency environment?

**Scenario/Question:** How will you handle shared mutable state safely?

**Scenario/Question:** A shared resource is accessed by multiple threads - how will you ensure consistency?



. No system design. No DSA. Just this: Big Billion Day. 50,000 orders per second. Your inventory shows 1 unit left for the iPhone. 10,000 users click Buy simultaneously. What happens?

Here's what most candidates say:
 - The first user gets it. Rest get rejected. Wrong.

Here's what actually happens:
 - All 10,000 API calls hit your server simultaneously.
 - All 10,000 read stock = 1.
 - All 10,000 pass the inventory check.
 - All 10,000 get confirmed.
 - You just sold 10,000 iPhones you don't have.
 - Flipkart loses crores in minutes.

This is called a Race Condition.
And it kills every naive inventory system at scale.

Here's how you actually fix it:
Fix 1: Redis DECR Atomic Inventory Never check inventory in your application layer. Use Redis DECR single atomic operation. Only one request decrements at a time. Counter hits 0 → every request after that rejected instantly. No overselling. Ever.

Fix 2: Kafka Absorb the Traffic Spike 50,000 orders per second will kill any database directly. Put Kafka between your API and DB. User clicks Buy → Kafka accepts in microseconds → User sees Order Confirmed. DB processes steadily at its own pace. Traffic spike absorbed. DB never chokes.

Fix 3: Idempotency Key No Duplicate Orders Payment times out. User clicks Buy again. Without protection two orders. Two payments. One furious user. Fix: Unique key per checkout attempt stored in Redis. Already processed? Return same result. Never run twice.

Fix 4: Centralised Sale Flag All Servers in Sync Sale starts at 12:00:00. Server A activates. Server B activates 4 seconds late. Users on Server B see sale as closed. Twitter explodes. Fix: Single Redis flag all servers check every 100ms. Flips true → every server activates simultaneously.

The architecture that survives Big Billion Day:
User → API Gateway → Redis → Kafka → Order Service → DB

 - Redis: atomic inventory + idempotency + sale flag
 - Kafka: absorbs 50,000 TPS
 - API Gateway: rate limits per user
 - DB: never sees raw traffic

The interviewer could ask asked:
 - Redis goes down at 12:00:01. What now?

Redis runs as a cluster with replicas.
Master fails → replica promotes in under a second.
Sale continues. Zero inventory corruption.

Flipkart doesn't ask this question to test your knowledge.