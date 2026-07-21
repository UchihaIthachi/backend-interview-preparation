# Java

## Notes

- [01 Core Java](01-core-java/) — types, OOP, exceptions, generics
- [02 Collections](02-collections/) — List/Set/Map, HashMap internals
- [03 Java 8+](03-java-8-plus/) — lambdas, streams, Optional
- [04 Concurrency](04-concurrency/) — threads, locks, executors, virtual threads
- [05 JVM](05-jvm/) — memory model, GC, class loading
- [06 Spring](06-spring/) — core, boot, REST, JPA, security
- [07 Design Patterns](07-design-patterns/) — patterns used in backend systems
- [08 Microservices](08-microservices/) — architecture, discovery, gateway, resilience, messaging

## Code examples

See [`code-examples/`](code-examples/) for runnable snippets referenced from the notes above.

## Interview question banks

Each subfolder has its own `questions.md`. Read the topic notes first, then answer
the questions out loud before checking the answer.


𝗝𝗮𝘃𝗮 𝗕𝗮𝗰𝗸𝗲𝗻𝗱 𝗗𝗲𝘃𝗲𝗹𝗼𝗽𝗲𝗿 𝗚𝘂𝗶𝗱𝗲 , 

Here are 36 interview questions/topics that repeatedly showed up in my prep and interview journey:

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

## Backend Topics Summary

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

𝗕𝗮𝗰𝗸𝗲𝗻𝗱/𝗗𝗲𝗲𝗽 𝗧𝗲𝗰𝗵

1. You have a Postgres table with 2 billion rows. A query that used to run in 80ms now takes 14 seconds after a routine deploy. Explain exactly how you'd diagnose and fix this, what do you look at first and why?

2. Your Redis cache has a hit rate of 92%. Under a flash sale it drops to 31% in 40 seconds and your DB starts receiving 60k QPS it was never designed for. Walk through exactly what happened and how you'd prevent it.

3. You're running a distributed transaction across 3 microservices: payments, inventory, and orders. Payment succeeds but inventory throws a timeout. You don't know if it committed or not. How do you resolve this without 2PC?

4. Your service is producing duplicate events under load. Downstream consumers are idempotent but your event IDs are generated using UUID v4. Under 80k events/sec you're seeing collisions. What's wrong and how do you fix the ID generation strategy?

5. You deployed a change that added a single index to a high-traffic Postgres table. Write traffic dropped by 40% for 6 minutes after deploy. No errors in logs. What happened?

6. Your gRPC service has p99 latency of 1.8 seconds but p50 is 42ms. The service has no obvious bottleneck in CPU or memory. Walk through how you'd find the exact cause, what tools, what metrics, what hypothesis do you test first?

Most engineers think "latency" means bad internet.
It doesn’t.

Latency means:
 - Your DB query isn’t taking 1 second because of a missing index.
 - Your cache isn’t slow because it keeps evicting data.
 - Your API call isn’t stuck sending a huge JSON payload.
 - Your frontend isn’t waiting forever for one slow request.

The truth? 80% of latency issues aren’t fixed by CDNs or faster servers.

They’re fixed by basics:
 - Avoid N+1 queries
 - Use faster data formats (Protobuf > JSON)
 - Batch requests instead of sending too many
 - Go async where you can

Next time someone says "our system is slow," don’t blame the internet.
Ask: Did we design it right in the first place?

𝗝𝗮𝘃𝗮/𝗕𝗮𝗰𝗸𝗲𝗻𝗱
1. How HashMap works internally
2. ConcurrentHashMap vs HashMap
3. String immutability in Java
4. ArrayList vs LinkedList
5. Garbage Collection in Java
6. JVM vs JRE vs JDK
7. Checked vs Unchecked Exceptions
8. try-catch-finally flow
9. Multithreading, synchronization, race conditions
10. Runnable vs Callable
11. Executor Framework
12. Deadlock, starvation, livelock
13. volatile vs synchronized
14. Java 8 Streams
15. Optional in Java
16. LRU Cache design in Java
17. Microservices vs Monolith
18. Idempotent APIs
19. SQL vs NoSQL
20. Database Indexing
21. Redis caching use cases
22. Pagination for large datasets
23. REST API best practices
24. Authentication vs Authorization
25. Connection Pooling
26. Debugging a slow Spring Boot API
27. Dependency Injection in Spring
28. Designing for high throughput and low latency

Top JAVA and Spring Boot Interview Questions:

Java Core:
 - How is Java platform independent (JVM vs JRE)?
 - What is a ClassLoader?
 - Difference: ClassNotFoundException vs NoClassDefFoundError
 - String Pool, intern(), == vs equals()

OOP Concepts:
 - What is Encapsulation?
 - Serializable interface
 - Can a class be static, final, or private?
 - OOP vs scripting languages

Exceptions:
 - Superclass of all exceptions
 - Error vs Exception
 - Checked vs Unchecked
 - Try-with-resources
 - Exception propagation

Java Language Features:
 - AutoBoxing
 - Key Java 8 features (Streams, Lambdas, Functional interfaces)
 - Lambda functions and why they are useful
 - Why interfaces cannot have constructors
 - Performance: for-loop vs for-each

Concurrency:
 - Code using 5 threads printing in sequence
 - Volatile vs Atomic

Design Patterns:
 - Builder pattern (usage + example)
 - Singleton implementation
 - Factory pattern concept
 - Why design patterns are needed
 - Patterns you know and how to invent one

Spring and Spring Boot:
 - What is Spring Boot and why it’s popular
 - Spring annotations: @Component, @Bean, @Qualifier, @Value
 - @Controller vs @RestController
 - @Mock vs @InjectMocks
 - OAuth basics and role-based access
 - Spring Data JPA main interfaces
 - Exception handling in controllers
 - Transaction propagation
 - application.properties purpose
 - How Spring/Hibernate generates SQL
 - MVC: Controller vs Service vs Repository
 - Pagination and unique constraints

Collections:
 - How HashMap works internally
 - HashMap vs Hashtable vs ConcurrentHashMap
 - List vs Set vs Map (when to use which)
 - What is LinkedList
 - Comparable interface
 - How hashCode is generated
 - Iterable interface

Streams API:
 - What Streams API is
 - Ways to create Streams
 - Streams vs Collections
 - Intermediate vs terminal operations
 - map vs flatMap
 - collect(), findFirst(), findAny()
 - Exception handling in Streams
 - Converting Stream to Collection
 - Sequential vs parallel streams

Functional Programming:
 - What is a functional interface
 - Can it have default and static methods
 - Common examples (Runnable, Comparator, Callable)

These 20 questions are being asked in 2026

Core Java
1. HashMap has 1 million entries. Performance is degrading. Why and what do you do?
2. Your code uses synchronized everywhere. Application is slow. What's wrong?
3. Explain double-checked locking in Singleton. Why do you need volatile?
4. ThreadLocal variable in your app is causing a memory leak. How?
5. What is the difference between wait() and sleep()? Where would each cause a deadlock?

JVM and Memory
6. Your app ran fine for 30 days. Crashed with OutOfMemoryError. No code changes. What happened?
7. GC is running every 30 seconds. Application pauses for 2 seconds each time. How do you fix it?
8. When would you choose G1GC over ZGC in production?

Spring Boot
9. @Transactional on a private method. Why does it silently fail?
10. Two beans of same type in Spring context. How does Spring decide which to inject?
11. Your @Async method throws an exception. Nobody catches it. What happens?
12. Spring Boot startup taking 45 seconds. How do you diagnose and fix it?

Database and JPA
13. Lazy loading throws LazyInitializationException in production but not in dev. Why?
14. Your JPA query works fine with 100 rows. Takes 45 seconds with 1 million rows. What is wrong?
15. Two transactions updating same row simultaneously. One silently overwrites the other. How do you prevent it?

System Design and Production
16. Your payment API receives same request twice due to network retry. How do you prevent double processing?
17. Microservice A calls Microservice B. B is down. A keeps retrying. B never recovers. What pattern solves this?
18. Your Redis cache is working. But database load is still at 100%. What is wrong?
19. API response time is 20ms normally. Spikes to 8 seconds under load. No errors in logs. Where do you start?
20. New feature deployed Friday evening. By Monday morning everything is slow. Nobody touched code over weekend. What happened?