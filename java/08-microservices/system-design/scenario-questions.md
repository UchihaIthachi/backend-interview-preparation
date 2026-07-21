# Scenario-based Questions

**Scenario:** Your microservice depends on another service for user data. That service is intermittently failing, causing your API to break.
**Questions:** How would you handle this failure gracefully? Would you implement circuit breaker? How? What fallback strategies would you design? How do you ensure system resilience?

**Scenario:** Your system needs to process orders asynchronously using messaging.
**Questions:** How would you design this using an event-driven approach? How would you use Azure Service Bus queues/topics? What happens if message processing fails? How do you ensure idempotency?

**Scenario:** You need to design an API for a system like an e-commerce checkout handling millions of users.
**Questions:** How would you design the architecture? How would you ensure high availability and fault tolerance? How would you scale microservices?

**Scenario:** Your API calls a third-party service which sometimes takes too long to respond.
**Questions:** How do you handle timeouts in Spring Boot? Would you implement retries? If yes, with what strategy (fixed, exponential backoff)? How do you avoid retry storms?

**Scenario:** Your API needs to process large JSON payloads (10MB+).
**Questions:** How would you optimize performance? Would you use streaming or chunking? How do you prevent memory issues?

**Scenario:** A frequently accessed API endpoint hits the database every time.
**Questions:** Where would you add caching (API, service, DB)? Would you use Redis or in-memory cache? How do you handle cache invalidation?

**Scenario:** Your service memory usage keeps increasing over time.
**Questions:** How would you identify a memory leak? What tools would you use (heap dump, profiling)? Common causes in Java applications?

**Scenario:** Build a notification system (email/SMS/push) using microservices.
**Questions:** How would you design the system? How would you use queues/topics in Azure Service Bus? How do you ensure scalability and reliability?

**Scenario:** A client retries a payment API due to network failure, causing duplicate transactions.
**Questions:** What is idempotency and why is it critical here? How would you design an idempotent API? Would you use idempotency keys or DB constraints?

**Scenario:** You need to return millions of records via an API.
**Questions:** Offset vs cursor-based pagination—what’s better and why? How do you handle consistency when data changes during pagination? How would you implement this in Spring Boot?

**Scenario:** Messages are failing repeatedly in Azure Service Bus and moving to DLQ.
**Questions:** What is a Dead Letter Queue? How do you process and debug DLQ messages? How do you prevent messages from ending up there?

**Scenario:** An API takes 2–5 minutes to process a request.
**Questions:** Should this be synchronous or async? How would you design polling or callback mechanisms? Would you use messaging queues?

**Scenario/Question:** Your API has high latency - what optimizations will you apply?

**Scenario/Question:** How will you handle millions of records efficiently?

**Scenario/Question:** How will you implement pagination?

**Scenario/Question:** How will you ensure data integrity?

**Scenario/Question:** How will you optimize write-heavy systems?

**Scenario/Question:** How will you mentor junior developers?

**Scenario/Question:** How will you design a highly concurrent system handling millions of requests?

**Scenario/Question:** How will you design idempotent consumers?

**Scenario/Question:** How will you design a resilient system?

**Scenario/Question:** How will you design a system for high availability?

**Scenario/Question:** How will you design service isolation?

