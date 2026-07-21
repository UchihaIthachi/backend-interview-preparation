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


𝗦𝘆𝘀𝘁𝗲𝗺 𝗗𝗲𝘀𝗶𝗴𝗻
25. Design a Chat Application
26. Design a URL Shortener
27. Design a Notification System
28. Scale a Video Streaming platform
29. Design a Payment System
30. Design an API Rate Limiter
31. Design an E-commerce Checkout System
32. Design a Ride-Hailing App
33. Design a Flash Sale system handling 10 million users
34. Design an Instagram Feed generation system
35. Design a Distributed Cache like Redis
36. Design a Search Autocomplete system

𝗦𝘆𝘀𝘁𝗲𝗺 𝗗𝗲𝘀𝗶𝗴𝗻
25. Design a Chat Application
26. Design a URL Shortener
27. Design a Notification System
28. Scale a Video Streaming platform
29. Design a Payment System
30. Design an API Rate Limiter
31. Design an E-commerce Checkout System
32. Design a Ride-Hailing App
33. Design a Flash Sale system handling 10 million users
34. Design an Instagram Feed generation system
35. Design a Distributed Cache like Redis
36. Design a Search Autocomplete system

𝟵𝟬% 𝗼𝗳 𝗝𝗮𝘃𝗮 𝗗𝗲𝘃𝗲𝗹𝗼𝗽𝗲𝗿𝘀 𝗙𝗮𝗶𝗹 𝗧𝗵𝗶𝘀 𝗢𝗻𝗲 𝗤𝘂𝗲𝘀𝘁𝗶𝗼𝗻.
 

 - You know Spring Boot inside out.
 - You’ve built REST APIs and microservices.
 - Your resume looks solid.

𝗕𝘂𝘁 𝘁𝗵𝗲𝗻 𝘁𝗵𝗲 𝗶𝗻𝘁𝗲𝗿𝘃𝗶𝗲𝘄𝗲𝗿 𝗮𝘀𝗸𝘀: Design a payment processing system that handles millions of transactions daily while ensuring data consistency and fault tolerance?
 - Most Java developers freeze because they have never moved beyond CRUD apps and tutorial projects.
 - The gap isn’t syntax, it’s systems design thinking.

𝗛𝗲𝗿𝗲’𝘀 𝘄𝗵𝗮𝘁 𝘀𝗲𝗽𝗮𝗿𝗮𝘁𝗲𝘀 𝗺𝗶𝗱-𝗹𝗲𝘃𝗲𝗹 𝗱𝗲𝘃𝘀 𝗳𝗿𝗼𝗺 𝘀𝗲𝗻𝗶𝗼𝗿 𝗲𝗻𝗴𝗶𝗻𝗲𝗲𝗿𝘀:
Instead of: “I know multithreading.” → They ask: “How do you handle thread safety in high-concurrency systems?”
Instead of: “I can build REST APIs,” → “How do you design an idempotent API for financial transactions?”
Instead of: “I use Hibernate.” → “How do you optimize database access and prevent N+1 queries at scale?”

Senior Java engineers don’t just write services; they engineer distributed systems.
 - Concurrency & parallelism at scale
 - Transactions & data consistency
 - Circuit breakers, retries, fault tolerance
 - JVM performance tuning & memory leaks
 - Designing APIs for scale and resilience

𝗥𝗼𝘂𝗻𝗱 𝟭: 𝗠𝗮𝗰𝗵𝗶𝗻𝗴 𝗖𝗼𝗱𝗶𝗻𝗴 𝗥𝗼𝘂𝗻𝗱
 - Cricket ScoreBoard Application (commonly found in Leetcode posts)
 - Build a cricket scorecard system that displays the team score and each player's performance.
 - Inputs: Number of players per team, number of overs, and batting order.
Ball-by-ball input includes runs (including wides, no balls, or wickets).
 - At the end of every over, print:
 - Individual scores, balls faced, number of 4s and 6s.
 - Total team score, wickets.
 - Implement strike changes, handle extras, and determine the match winner.

𝗥𝗼𝘂𝗻𝗱 𝟮: 𝗗𝗮𝘁𝗮 𝗦𝘁𝗿𝘂𝗰𝘁𝘂𝗿𝗲𝘀 & 𝗣𝗿𝗼𝗯𝗹𝗲𝗺 𝗦𝗼𝗹𝘃𝗶𝗻𝗴
 - Find all pairs in an array with a given sum.
 - Find all triplets in an array with a given sum.
 - Check if a binary tree has a duplicate subtree.
 - Given a matrix, a position, and a value k, return the sum of the element at the position and all its neighbors within distance k (including diagonals).

𝗥𝗼𝘂𝗻𝗱 𝟯: 𝗛𝗶𝗴𝗵 𝗟𝗲𝘃𝗲𝗹 𝗗𝗲𝘀𝗶𝗴𝗻 (𝗛𝗟𝗗)
 - Design an Alert Monitoring System
 - Context: Central system in a microservices environment to manage alerts from different systems.
 - Features to support:
 - SIMPLE_COUNT: Raise alert if event count crosses a threshold.
 - BUCKETED_WINDOW: Count events in fixed buckets (e.g., 10 events in 1-hour bucket).
 - MOVING_WINDOW: Count events in a sliding time window.
 - The system should handle user/system events, trigger alerts based on configuration, and support real-time monitoring.

𝗥𝗼𝘂𝗻𝗱 𝟰: 𝗛𝗠 𝗥𝗼𝘂𝗻𝗱 (𝗛𝗶𝗿𝗶𝗻𝗴 𝗠𝗮𝗻𝗮𝗴𝗲𝗿)
 - Discussion Topics: Project experiences.
 - Day-to-day responsibilities.
 - Light behavioral questions.
 - Design Question: Tiny URL system (ran out of time midway).

𝗥𝗼𝘂𝗻𝗱 𝟱: 𝗛𝗟𝗗 𝟮
 - Design Stack Overflow-like System
 - Cover user flows, question/answer posting, tagging, voting, reputation system, and content visibility.
 - Performance, scale, and consistency challenges.

𝗖𝗼𝗿𝗲 𝗝𝗮𝘃𝗮 / 𝗕𝗮𝗰𝗸𝗲𝗻𝗱
1. HashMap vs ConcurrentHashMap, equals/hashCode contract, immutability
2. Threads vs executors, JMM basics, GC tuning (G1/ZGC talk-throughs)
3. Java 8/11/17 features in practice (streams, records, var, switch, text blocks)
4. Resilience in prod: retries, timeouts, circuit breakers (what + where)
 
𝗗𝗦𝗔 / 𝗖𝗼𝗱𝗶𝗻𝗴
1. Arrays/Strings: two-sum variants, move zeros, anagrams, longest prefix
2. Graphs/Trees: BFS/DFS, shortest path, tree re-rooting, DSU
3. DP staples: LIS, coin change
4. Binary search patterns; streamy transforms (filter → map → reduce)

𝗦𝗤𝗟 / 𝗗𝗮𝘁𝗮𝗯𝗮𝘀𝗲
1. Index design & trade-offs; deduping rows; window functions vs GROUP BY
2. Transaction isolation, pagination strategies; read-vs-write models for feeds

𝗦𝗽𝗿𝗶𝗻𝗴 / 𝗠𝗶𝗰𝗿𝗼𝘀𝗲𝗿𝘃𝗶𝗰𝗲𝘀
1. Spring Boot auto-config, configuration profiles, Actuator/health
2. REST design: validation, versioning, idempotency; SAGA/outbox patterns
3. Kafka (ordering, retries, DLQs) and RabbitMQ (work queues) in payments flows
4. Observability: metrics/tracing; dealing with 300B+ metrics/day style scale

𝗦𝘆𝘀𝘁𝗲𝗺 𝗗𝗲𝘀𝗶𝗴𝗻 
1. Design a UPI-style payment service
2. Transaction feed service, inspired by TStore. 
3. In-app Inbox/Alerts/chat-like pub-sub ala Bullhorn. 
4. Job scheduler at scale like Clockwork. 
5. Metrics platform for SLOs.

1. Thread Pools vs Virtual Threads
 - Default thread pool blocks under load. Virtual Threads don't.
 - Java 21. Millions of threads. JVM manages them. Not OS.
 - Most developers still don't know this exists.

2. CompletableFuture vs Reactive Streams
 - CompletableFuture simple async. Good for most cases.
 - WebFlux backpressure + streaming. Good for high volume.
 - Pick based on problem. Not hype.

3. Connection Pooling (HikariCP)
 - Default pool size 10. Almost always wrong.
 - Formula: CPU cores x 2 + disk spindles = your baseline.
 - Too many → DB dies. Too few → threads wait at 3am.

4. @ Transactional Pitfalls
 - Self-invocation → proxy bypassed → transaction never starts. No error.
 - REQUIRES_NEW vs REQUIRED. Not interchangeable. Most think they are.
 - Lazy loading in closed session → LazyInitializationException. Silent. Always.

5. Caching Strategy (Redis)
 - Cache-aside. Write-through. Write-behind. Three strategies. Three failure modes.
 - Wrong eviction policy → stale data under load.
 - LRU is not always right.

6. Event-Driven Design with Kafka
 - Topics are not queues. This distinction matters.
 - Partition key design determines your throughput ceiling.
 - No idempotent consumers → duplicate processing → duplicate charges.

7. Database Indexing Mistakes
 - Too many indexes → writes slow down.
 - Composite index column order matters.
 - No EXPLAIN ANALYZE before deployment → production incident waiting to happen.

8. Circuit Breaker (Resilience4J)
 - Service B slows → Service A waits → everything dies.
 - Circuit Breaker fails fast. Recovers gracefully.
 - CLOSED → OPEN → HALF-OPEN.
Most teams add this after the incident. Not before.

Production will test all 8. Usually at 3am.
Usually on a Friday.

𝗥𝗼𝘂𝗻𝗱 𝟭 - 𝗗𝗦 & 𝗔𝗹𝗴𝗼:
They gave him a HashMap frequency problem first. Straightforward. Then a sliding window question with dynamic constraints, the window size kept changing based on conditions mid-stream.

He said the trick wasn't solving it. It was explaining his thought process out loud while coding. The interviewer kept nudging him toward edge cases he hadn't considered.

𝗥𝗼𝘂𝗻𝗱 𝟮 - 𝗗𝗦 & 𝗔𝗹𝗴𝗼:
This round went deeper into graphs. Model a financial network as a graph, traverse it, find anomalies. BFS vs DFS came up naturally in the discussion.

The follow-up hit hard, what happens when the graph has 10 million nodes? He said most candidates freeze here. He didn't because he'd practiced the tradeoff conversation, not just the code.

𝗥𝗼𝘂𝗻𝗱 𝟯 - 𝗟𝗼𝘄 𝗟𝗲𝘃𝗲𝗹 𝗗𝗲𝘀𝗶𝗴𝗻:
Design a Notification Service supporting push, email, and SMS. Priority-based delivery. Retry on failure with exponential backoff.

They didn't care much about perfect syntax. They wanted clean class boundaries, clear ownership, and extensible design. He said the moment he started thinking in interfaces instead of implementations the round got easier.

𝗥𝗼𝘂𝗻𝗱 𝟰 - 𝗛𝗶𝗴𝗵 𝗟𝗲𝘃𝗲𝗹 𝗗𝗲𝘀𝗶𝗴𝗻:
Design a Stock Price Feed System for millions of concurrent users.

The interesting part wasn't the architecture. It was when the interviewer said, "market just opened and 50 million users hit simultaneously. Walk me through what breaks first."

What they were really testing:
 - Do you think in failure modes, not just features?
 - Can you justify database and protocol choices?
 - Do you understand bottlenecks before being told?

𝗥𝗼𝘂𝗻𝗱 𝟱 - 𝗛𝗶𝗿𝗶𝗻𝗴 𝗠𝗮𝗻𝗮𝗴𝗲𝗿 𝗥𝗼𝘂𝗻𝗱:
Walk me through the most complex system you built. How do you handle disagreements on architecture? Tell me about a time you improved reliability of an existing service.

He said this round felt casual but was the most important one. They were evaluating whether he could own things end to end not just code them.

The interviews weren't testing whether he knew the right answer. They were testing whether he could think clearly under pressure. That's the actual skill.

Design a video streaming platform. 200 million users. New season drops tonight. Everyone hits Play simultaneously. Your streaming service crashes at 8:00 PM. What happens?

 - Your single video server just received 200 million requests in 60 seconds. What happens to it?
 - User in Mumbai and user in New York both hit Play. Same video. Your server is in California. Both are buffering. Why?

The real problems at scale:

1. 200 million users hitting one server simultaneously
 - Central server receives 200 million requests in seconds. Bandwidth exhausted instantly. Every user sees buffering. Netflix trends on Twitter for wrong reasons.
 - Fix: Content Delivery Network CDN.
Netflix pre-loads popular content across thousands of CDN servers globally.
Mumbai user → served from Mumbai CDN server.
California server never touched.

2. Video file is 4GB. User's internet is slow. What happens?
 - Entire 4GB file cannot be streamed at once. Slow internet means constant buffering. User gives up. Cancels subscription.
 - Fix: Adaptive Bitrate Streaming.
Netflix pre-encodes every video in multiple quality levels.
4K. 1080p. 720p. 480p. 360p.
Your device constantly measures internet speed.
Speed drops → quality drops automatically.
Speed recovers → quality improves automatically.

3. Same popular video requested by millions simultaneously
 - Every request hitting origin server for same file. Massive redundant load.
 - Fix: Cache popular content at CDN edge servers in advance.
New season dropping tonight?
Netflix pre-warms CDN servers hours before release.
First request fetches from origin → cached at CDN.
Every subsequent request → served from cache in milliseconds.
Origin server sees almost zero load during peak.

4. Streaming service crashes mid-watch
 - User is 40 minutes into episode.
Service crashes. Comes back up.
User starts from beginning. Furious.
 - Fix: Store playback position in Redis per user per video.
Service crashes → recovers → reads last position from Redis.
User resumes exactly where they stopped.

The architecture:
 - User → API Gateway → Load Balancer → Streaming Service
→ CDN (video delivery) → Redis (playback position)
→ Adaptive Bitrate Player (client side)

- CDN handles 95% of all video traffic origin server barely touched
- Adaptive bitrate video quality matches user's connection automatically
- Redis playback position never lost
- Pre-warming CDN always ready before major releases
- Chaos Monkey Netflix intentionally breaks its own systems to test resilience
𝗖𝗼𝗿𝗲 𝗝𝗮𝘃𝗮
 1. How to sort a map?
 2. Write a singleton class.
 3. Difference between comparable and comparator.
 4. New features in JAVA 7 and 8? Features of Java 8, 11, and 17.
 5. What is try with resources?
 6. What is a multi-catch statement in Java?
 7. What is runnable and callable?
 8. Types of exceptions in Java. Exception hierarchy in Java.
 9. What are the different Design patterns in Java? 
 10. OOP
 11. ConcurrentHashMap internals.
 12. How does Java garbage collection work?

𝗗𝗦𝗔/𝗖𝗼𝗱𝗶𝗻𝗴
 1. How to find duplicate strings in a list of strings?
 2. Write a program to find if a string is a palindrome string. 
 3. Combination Sum II (recursion problem).
 4. Given an array, remove all odd numbers, multiply each number by a constant, and return the sum using Java Streams.
 5. Missing integer in consecutive array.
 6. Move all zeroes to the end of an array.
 7. Check if two strings are anagrams.
 8. Longest common prefix among strings.
 9. Longest increasing subsequence.
 10. Best time to buy & sell stock (maximize profit).
 11. Dijkstra’s algorithm.
 12. Coin change problem (minimum coins).
 13. Reverse-add palindrome problem.

𝗪𝗲𝗯/𝗙𝗿𝗮𝗺𝗲𝘄𝗼𝗿𝗸𝘀
 1. Difference between REST and SOAP.
 2. What is Spring?
 3. Why do we use Spring?
 4. Difference between Spring and Spring Boot?
 5. How does autowiring work in Spring?
 6. What is Spring Security?
 7. What is a RESTful API?
 8. Difference between HTTP and HTTPS.

𝗦𝘆𝘀𝘁𝗲𝗺 𝗗𝗲𝘀𝗶𝗴𝗻
 1. Explain the design of one of your recent projects.
 2. Fraud detection model for transactions.
 3. Database design for ride-sharing.
 4. Data warehouse for an online retailer.
 5. Design a news aggregator.

## Top 10 Design Problems

. Design a Rate Limiter Every company asked this.
Know: Sliding window, Redis, race conditions, token bucket vs leaky bucket.

2. Design a Chat Application WhatsApp typing indicator alone can be a 45 min discussion.
Know: WebSockets, Redis Pub/Sub, message queues, offline delivery.

3. Design a URL Shortener Looks simple. Gets deep fast.
Know: Base62 encoding, collision handling, analytics, caching with Redis.

4. Design a Notification System
Know: Push vs pull, Kafka for async delivery, retry logic, user preferences.

5. Design a Payment System JP Morgan asked this. So did two others.
Know: Idempotency keys, Saga pattern, ACID vs eventual consistency.

6. Design an API Rate Limiter Different from 1. This one focuses on distributed systems.
Know: Token bucket, Redis INCR, Lua scripts, multi-server coordination.

7. Design a Video Streaming Platform
Know: CDN, chunked uploading, adaptive bitrate, storage at scale.

8. Design a Ride Hailing App
Know: Location tracking, matching algorithms, surge pricing logic, real-time updates.

9. Design an E-commerce Checkout System
Know: Inventory locking, flash sale handling, payment retries, order state machine.

10. Design a Search Autocomplete System
Know: Trie data structure, ranking by frequency, caching top results, latency under 100ms.

𝗦𝘆𝘀𝘁𝗲𝗺 𝗗𝗲𝘀𝗶𝗴𝗻
25. Design a Chat Application
26. Design a URL Shortener
27. Design a Notification System
28. Scale a Video Streaming platform
29. Design a Payment System
30. Design an API Rate Limiter
31. Design an E-commerce Checkout System

SYSTEM DESIGN:
1. Design a payment processing system.
2. Handle 1 million transactions a day.
3. Saga vs 2PC.
4. Transactional outbox. Why it exists.
5. Data consistency across services.
6. Distributed locking.

𝗦𝘆𝘀𝘁𝗲𝗺 𝗗𝗲𝘀𝗶𝗴𝗻

1. Design Google Maps routing for 1 billion daily users. How do you recompute shortest paths in real-time when traffic data changes every 30 seconds across 50 million road segments?

2. Design YouTube's view count system. It shows "1.2M views" but the number doesn't update on every single view. How do you handle the consistency vs latency trade-off at 500 million daily video plays?

3. Design a distributed search autocomplete that serves suggestions in under 100ms globally. How do you keep suggestions fresh when trending queries spike in a specific region?

4. Design Google Drive's file sync system. Two users edit the same document offline and both reconnect at the same time. How do you resolve conflicts without data loss?

5. Design Gmail's search. It indexes 15 billion emails across 1.8 billion users. How do you make per-user search feel instant when no two users share an index?

6. Design Google Meet for 100k concurrent calls. How do you handle TURN server load, packet loss recovery, and dynamic quality degradation gracefully when a user's bandwidth drops mid-call?

"200 million users. A post goes viral. 2 million likes in 60 seconds. How should the system behave?"

The naive approach (what everyone says):
 - User clicks Like
 - INSERT the like into a table
 - UPDATE post SET like_count = like_count + 1
Works in a demo. Dies in production.

2 million likes in 60 seconds is 33,000 writes per second. Your DB is fried before the post hits 100k likes.

THE REAL PROBLEMS AT SCALE:
1. Write contention on one row
 - 33,000 updates per second. All fighting for the same counter row.
 - Every update locks that row to increment it.
 - Requests queue. Latency spikes. The DB dies.
Fix: Redis INCR. Atomic. O(1).
 - Handles 100k ops per second easily.
 - An async job syncs the count to the DB every 30 seconds.
 - The DB never sees raw traffic.

2. Did this user already like it?
 - Every feed load checks this for every post.
 - 50 posts per feed. 200 million users.
 - 10 billion lookups on every refresh.
Fix: A Redis Bloom filter per post.
 - "Has user X liked post Y?"
 - False positives possible. False negatives never.
 - A no is final. Answered in microseconds. No DB hit.
 - A maybe falls through to Cassandra to confirm.

3. The owner gets flooded
 - 2 million likes means 2 million notifications.
 - Fire one per like and you bury the owner alive.
Fix: Batch and aggregate.
 - "Arjun and 4,293 others liked your post."
 - One notification. Delivered async through Kafka.

THE ARCHITECTURE THAT SURVIVES THIS:

User clicks Like to API Gateway to Redis to Kafka to Cassandra to Notification Service to Postgres.
 - API Gateway rate limits per user.
 - Redis handles the atomic count and the Bloom filter check.
 - Kafka absorbs the spike so nothing downstream chokes.
 - Cassandra stores the individual like records.
 - Notification Service batches instead of spamming.
 - Postgres holds the durable count, synced every 30 seconds.

The follow-up they will ask: "Redis crashes. 2 million likes lost. What now?"
They are not lost.
 - Every like event is already durable in Kafka and Cassandra.
 - Redis runs with AOF and a replica. The replica promotes via Sentinel in seconds.
 - Worst case you lose a sub-second window of in-memory writes.
 - The count reconciles from the durable records.
 - Bounded. Temporary. Never permanent loss at scale.

Meta does not ask this to test your Redis knowledge.
They ask it to see if you understand that a Like button is a counting problem, a lookup problem, and a notification problem, all hitting at once at 33,000 ops per second.

𝗖𝗮𝗰𝗵𝗶𝗻𝗴 𝗮𝘁 𝗦𝗰𝗮𝗹𝗲
16. Cache Penetration - Requests for keys that do not exist, each one falls through to the DB
17. Cache Breakdown - One hot key expires and thousands of requests hit the DB at once
18. Cache Avalanche - Many keys expire together and flood the DB in one wave
19. TTL Jitter - Randomise expiry so keys do not all die at the same second
20. Write-Through - Update the cache and the DB in the same operation

𝗦𝘆𝘀𝘁𝗲𝗺 𝗗𝗲𝘀𝗶𝗴𝗻
1. Design a Project Management Tool Like Jira
2. Design a Real-Time Collaboration Tool.
3. Design a Scalable Notification System
4. Design a Search System for Knowledge Base Articles
5. Design an API Gateway for Atlassian Services
6. Design a Version Control System for Documentation
7. Design a Real-Time Analytics Platform
8. Design a Scalable User Authentication and Authorization System
9. Design a Workflow Automation System
10. Design a Logging and Monitoring System
11. Design a Rate Limiter
12. Design a parking
13. Database Design
14. Design Snake Game
15. Design a Ticketing System like Jira
16. Design a URL Shortening Service
17. Design a Notification System
18. Design a Distributed Messaging System
19. Design a Scalable Chat Application
20. Design a Job Scheduler

Me: Caching looks simple on paper.
 - If data is in cache → return it
 - Else → fetch from DB and set cache

This works… until traffic hits scale.
At scale, databases don’t die because of bugs. They die because of cache failures.

Then I explained the 3 cache killers:
1. Cache Penetration
 - Problem: Users keep requesting data that doesn’t exist.
 - Example: userId = -1
 - Cache miss → DB miss → repeat.
 - If someone automates this, your DB is gone.
 - Fix: Use a Bloom Filter to block invalid keys
 - Or cache empty results for a short TTL

2. Cache Breakdown (Hot Key Expiry)
 - Problem: A celebrity profile goes viral. 1M users hit the same cache key.
 - At 12:00:01, the key expires.
 - Suddenly, thousands of requests hit DB together.
 - Fix: Use a mutex lock so only one request hits DB
 - Or use logical expiration (serve stale + refresh in background)

3. Cache Avalanche
 - Problem: Multiple keys expire at the same time (or server restarts).
 - Boom. DB gets flooded.
 - Fix: Add TTL jitter (Randomise expiry time so keys don’t die together)

System Design isn’t about the happy path. It’s about handling what breaks at scale.

System Design
1. Design a payment system handling 1 million transactions daily
2. Saga pattern vs Two Phase Commit when do you choose which?
3. How do you ensure data consistency across microservices?
4. How do you implement distributed locking?

## 10 Essential System Design Questions

1. Design a Rate Limiter Every company asked this.
Know: Sliding window, Redis, race conditions, token bucket vs leaky bucket.

2. Design a Chat Application WhatsApp typing indicator alone can be a 45 min discussion.
Know: WebSockets, Redis Pub/Sub, message queues, offline delivery.

3. Design a URL Shortener Looks simple. Gets deep fast.
Know: Base62 encoding, collision handling, analytics, caching with Redis.

4. Design a Notification System
Know: Push vs pull, Kafka for async delivery, retry logic, user preferences.

5. Design a Payment System JP Morgan asked this. So did two others.
Know: Idempotency keys, Saga pattern, ACID vs eventual consistency.

6. Design an API Rate Limiter Different from 1. This one focuses on distributed systems.
Know: Token bucket, Redis INCR, Lua scripts, multi-server coordination.

7. Design a Video Streaming Platform
Know: CDN, chunked uploading, adaptive bitrate, storage at scale.

8. Design a Ride Hailing App
Know: Location tracking, matching algorithms, surge pricing logic, real-time updates.

9. Design an E-commerce Checkout System
Know: Inventory locking, flash sale handling, payment retries, order state machine.

10. Design a Search Autocomplete System
Know: Trie data structure, ranking by frequency, caching top results, latency under 100ms.