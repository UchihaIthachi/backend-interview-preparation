# 12-Week Java + Go Interview Plan

| Week | Main focus | Coding deliverable | Blog/Notes deliverable |
|---:|---|---|---|
| 1 | Java syntax, strings, OOP | Immutable domain model | OOP principles notes |
| 2 | Java collections & generics | In-memory product catalogue | HashMap internals notes |
| 3 | Java streams, lambdas, exceptions | Data-processing pipeline | Streams notes |
| 4 | Java concurrency fundamentals | Thread-safe counter + transfer service | Race conditions notes |
| 5 | ExecutorService & futures | Parallel file-processing service | ExecutorService deep dive |
| 6 | JVM & garbage collection | JVM profiling experiment | GC notes |
| 7 | Go fundamentals + interfaces | CLI tool using structs/interfaces | Go fundamentals notes |
| 8 | Go concurrency (goroutines/channels) | Worker pool with cancellation | Concurrency patterns notes |
| 9 | Spring Boot + REST + security | Secured CRUD API (Java) | JWT auth flow notes |
| 10 | Go backend (net/http, database/sql) | REST API in Go with Postgres | Go backend notes |
| 11 | Testing & observability (both langs) | Testcontainers + metrics | Unit vs integration testing notes |
| 12 | System design + DSA sprint | Event-driven order workflow | Reliable order system design doc |

## Guiding principle

Every week should touch: **1 concept**, **1 coding deliverable**, **5+ DSA problems**,
**1 system design topic**, and end with an updated progress tracker.


1. Core Java Mastery
 - OOP principles (SOLID, DRY, KISS)
 - Generics, Lambda expressions, Functional interfaces
 - Java Streams API (map/reduce, collectors)
 - Java Collections framework
 - Java Reflection API
 - Exception handling

2. Multithreading & Concurrency
 - Thread synchronization, Executors, Locks
 - Fork/Join framework
 - Understanding of race conditions, deadlocks, and thread pools
 - Concurrency utilities (java.util.concurrent)

3. Design Patterns & Architecture
 - Common design patterns (Singleton, Factory, Builder)
 - Architectural patterns (MVC, Microservices, Event-Driven Architecture)
 - Dependency Injection (DI), Inversion of Control (IoC)

4. Java Memory Management
 - Garbage Collection (G1, CMS, ZGC)
 - JVM heap and stack management
 - Profiling tools (JProfiler, VisualVM)
 - Analyzing memory leaks, thread dumps, and heap dumps

5. Classloaders and Reflection
 - Custom class loaders
 - Dynamic class loading
 - Reflection for runtime behavior manipulation

6. Spring Framework & Spring Boot
 - Spring Core (Dependency Injection, AOP)
 - Spring Boot (Auto-configuration, Microservices support)
 - Spring Security (OAuth2, JWT)
 - Spring Data (JPA, Hibernate integration)
 - Spring Cloud (Netflix OSS, Circuit Breakers)

7. Microservices Architecture
 - Service discovery (Eureka, Consul)
 - Load balancing, distributed tracing, and circuit breaking
 - API Gateway (Zuul, NGINX)
 - Asynchronous communication with Kafka, RabbitMQ

8. RESTful Web Services
 - REST principles, building APIs
 - JSON/XML handling
 - API versioning, OpenAPI/Swagger documentation

9. Java I/O and NIO
 - Blocking vs non-blocking I/O (NIO)
 - Asynchronous I/O, channels, selectors
 - File handling, serialization, and deserialization

10. Reactive Programming
 - Project Reactor, RxJava
 - Event-driven architecture, backpressure
 - Reactive streams, non-blocking IO

11. JPA/Hibernate
 - ORM principles, entity relationships
 - Lazy vs eager loading
 - Caching strategies, query optimization

12. Database Optimization
 - SQL optimization, indexing, and transactions
 - NoSQL databases (MongoDB, Cassandra)
 - ACID principles, CAP theorem

13. Distributed Systems
 - Consistency, availability, partitioning (CAP)
 - Event sourcing, CQRS (Command Query Responsibility Segregation)
 - Distributed caching (Redis, Hazelcast)
 - Tools: Apache ZooKeeper, Consul, etcd

14. Testing & TDD/BDD
 - Unit testing (JUnit, Mockito)
 - Integration and functional testing
 - Behavior-driven development (Cucumber)

15. CI/CD & DevOps
 - Continuous integration (Jenkins, CircleCI)
 - Containerization with Docker
 - Orchestration with Kubernetes
 - Git, versioning, and branching strategies