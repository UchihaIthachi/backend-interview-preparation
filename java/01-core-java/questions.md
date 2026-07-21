# Core Java — Interview Question Bank

1. Is Java pass-by-value or pass-by-reference?
2. Why is `String` immutable?
3. What is the String Pool?
4. Interface vs abstract class?
5. Overloading vs overriding?
6. Composition vs inheritance?
7. Why must `equals()` and `hashCode()` agree?
8. What happens when a mutable object is used as a `HashMap` key?
9. Checked vs unchecked exceptions?
10. What is type erasure?
11. What is a record and when would you use one over a class?
12. What are sealed classes and what problem do they solve?
13. Why prefer try-with-resources over manual `finally` blocks?

> Answer each out loud in under 90 seconds before checking notes in
> [`01-core-java/`](.).


𝗗𝗦𝗔
1. Implement LRU/LFU Cache
2. Find Median from Data Stream
3. Word Ladder problem
4. Merge K Sorted Lists
5. Detect Cycle in a Directed Graph
6. Maximum Subarray Sum
7. Kth Largest Element in a Stream
8. Task Scheduler/Priority-based scheduling
9. Longest Substring Without Repeating Characters
10. Trapping Rain Water
11. Find all permutations of a string
12. Lowest Common Ancestor in a Binary Tree

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

𝗦𝗲𝗿𝘃𝗲𝗿/𝗦𝘆𝘀𝘁𝗲𝗺
 1. How to find server crash reasons?
 2. How to find server memory?
 3. How do you debug high CPU or memory issues in JVM?
 4. How do you capture heap/thread dumps?

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

𝗖𝗼𝗿𝗲 𝗝𝗮𝘃𝗮
 - What is the difference between HashMap and ConcurrentHashMap internally?
 - How does the JVM memory model work? Heap, Stack, Metaspace explained.
 - Explain double-checked locking in Singleton. Why do you need volatile?
 - What is the difference between Comparable and Comparator?
 - How does garbage collection work? When would you choose G1GC over ZGC?
 - What is ThreadLocal and where does it cause memory leaks?
 - What is the difference between volatile and synchronized?

𝗗𝗦𝗔
1. Implement LRU/LFU Cache
2. Find Median from Data Stream
3. Word Ladder problem
4. Merge K Sorted Lists
5. Detect Cycle in a Directed Graph
6. Maximum Subarray Sum
7. Kth Largest Element in a Stream
8. Task Scheduler/Priority-based scheduling
9. Longest Substring Without Repeating Characters
10. Trapping Rain Water
11. Find all permutations of a string
12. Lowest Common Ancestor in a Binary Tree

𝗝𝗮𝘃𝗮 𝗕𝗮𝗰𝗸𝗲𝗻𝗱
13. HashMap vs ConcurrentHashMap vs Hashtable
14. JVM memory model: Heap, Stack, GC
15. ExecutorService, Callable, and Future
16. How Spring Boot Dependency Injection works
17. REST vs Kafka/RabbitMQ in Microservices
18. Transactions in Spring Boot
19. SQL vs NoSQL
20. Designing scalable REST APIs
21. How does @Transactional silently fail?
22. Thread safety: synchronized vs volatile vs atomic
23. How does HikariCP connection pooling work?
24. Circuit Breaker pattern with Resilience4J

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

𝗦𝗲𝗿𝘃𝗲𝗿/𝗦𝘆𝘀𝘁𝗲𝗺
 1. How to find server crash reasons?
 2. How to find server memory?
 3. How do you debug high CPU or memory issues in JVM?
 4. How do you capture heap/thread dumps?

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

𝗖𝗼𝗿𝗲 𝗝𝗮𝘃𝗮
 - What is the difference between HashMap and ConcurrentHashMap internally?
 - How does the JVM memory model work? Heap, Stack, Metaspace explained.
 - Explain double-checked locking in Singleton. Why do you need volatile?
 - What is the difference between Comparable and Comparator?
 - How does garbage collection work? When would you choose G1GC over ZGC?
 - What is ThreadLocal and where does it cause memory leaks?
 - What is the difference between volatile and synchronized?

𝗦𝗽𝗿𝗶𝗻𝗴 𝗕𝗼𝗼𝘁 𝗮𝗻𝗱 𝗠𝗶𝗰𝗿𝗼𝘀𝗲𝗿𝘃𝗶𝗰𝗲𝘀
 - How does @Transactional work internally? Where does it silently fail?
 - What happens inside Spring Boot during startup?
 - What is the difference between @Component, @Service and @Repository?
 - How does Dependency Injection work internally in Spring?
 - RestTemplate vs WebClient when do you choose which?
 - How do you implement Global Exception Handling in Spring Boot?
 - What is the difference between monolithic and microservices architecture?
 - How do microservices communicate? When REST, when Kafka?

𝗞𝗮𝗳𝗸𝗮 𝗮𝗻𝗱 𝗠𝗲𝘀𝘀𝗮𝗴𝗶𝗻𝗴
 - Why Kafka over RabbitMQ? When would you choose RabbitMQ?
 - Consumer crashes mid-processing. Message already consumed. What happens?
 - How do you maintain message ordering across partitions?
 - What is idempotency in distributed systems and why does it matter?

𝗗𝗮𝘁𝗮𝗯𝗮𝘀𝗲 𝗮𝗻𝗱 𝗝𝗣𝗔
 - What is the N+1 problem? How do you detect and fix it?
 - Two transactions updating same row simultaneously. One silently overwrites. How do you prevent this?
 - First level vs second level cache in Hibernate what is the difference?
 - How does indexing work? When does an index hurt performance?
 - What are ACID properties? Explain with a banking transaction example.

𝗦𝘆𝘀𝘁𝗲𝗺 𝗗𝗲𝘀𝗶𝗴𝗻
 - Design a Payment Processing System handling 10,000 TPS
 - Design a Rate Limiter for a public API
 - Design a Notification System for email, SMS and push
 - Design a URL Shortener like Bit.ly
 - Design a Chat Application like WhatsApp
 - Design an E-commerce Checkout System

𝗗𝗦𝗔
 - Implement LRU Cache
 - Find the maximum sum of any contiguous subarray
 - Detect if a linked list contains a cycle
 - Merge K sorted linked lists into one sorted list
 - Given course prerequisites, determine if all courses can be finished

CORE JAVA:
1. Fail-fast vs fail-safe iterators.
2. ConcurrentHashMap internals.
3. What is a daemon thread.
4. Comparable vs Comparator.
5. SOLID with a real example, not a definition.
6. Optional: isPresent vs ifPresent.
7. How the JVM manages memory.
8. Virtual threads. When they help and when they do nothing.

Core Java
1. HashMap has 1 million entries. Performance is degrading. Why and what do you do?
2. Your code uses synchronized everywhere. Application is slow. What's wrong?
3. Explain double-checked locking in Singleton. Why do you need volatile?
4. ThreadLocal variable in your app is causing a memory leak. How?
5. What is the difference between wait() and sleep()? Where would each cause a deadlock?

𝗖𝗼𝗿𝗲 𝗝𝗮𝘃𝗮 & 𝗖𝗼𝗻𝗰𝘂𝗿𝗿𝗲𝗻𝗰𝘆
1. Race Condition - Two threads read and act on the same value before either finishes
2. Deadlock - Two threads each holding a lock the other needs, both stuck forever
3. Optimistic Locking - Assume no conflict, check a version before you write
4. Pessimistic Locking - Lock the row up front so nobody else can touch it
5. volatile - Forces a read from main memory, not the thread's local cache