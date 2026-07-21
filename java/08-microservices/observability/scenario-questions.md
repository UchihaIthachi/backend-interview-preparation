# Scenario-based Questions

**Scenario/Question:** How will you monitor microservices health?

**Scenario/Question:** How will you handle distributed tracing in Spring Cloud?

**Scenario/Question:** How will you debug latency issues between services?

**Scenario/Question:** How will you debug a failing microservice in production?



## Production Investigation Scenarios

1. Your API response time suddenly increases from 200 ms to 5 seconds. How would you investigate the root cause?
2. A downstream microservice is slow. How do you prevent it from affecting your entire application?
3. When would you use a Circuit Breaker, Retry, Timeout, and Bulkhead pattern?
4. Your database connection pool is exhausted in production. What could be the possible reasons?
5. How would you handle duplicate payment requests caused by retries or multiple button clicks?
6. Where would you store an idempotency key, and how would you handle concurrent requests with the same key?
7. Redis is down. Should your application also go down? How would you design the fallback?
8. How would you solve the cache invalidation problem when multiple service instances are running?
9. An API works perfectly with 100 users but fails with 10,000 concurrent users. What would you check first?
10. How would you process a 5 GB CSV file without causing an OutOfMemoryError?
11. When would you choose synchronous communication over asynchronous messaging between microservices?
12. What happens if a Kafka consumer processes a message successfully but crashes before committing the offset?
13. How would you prevent the same Kafka event from being processed twice?
14. Your Kubernetes pod keeps restarting, but application logs show no clear error. How would you debug it?
15. CPU usage is normal, but API latency is very high. What could be happening?
16. How would you identify whether a performance issue is caused by application code, the database, network latency, or a downstream service?
17. One SQL query is taking 10 seconds in production but runs quickly in the development environment. How would you investigate?
18. How would you handle distributed transactions across multiple microservices?
19. When would you use the Saga pattern, and how would you handle compensation failures?
20. Your service receives the same request twice because of a network retry. How would you guarantee consistent results?
21. How would you deploy a new version of a Spring Boot service without downtime?
22. What happens to in-flight requests during pod termination, and how would you handle graceful shutdown?
23. How would you design rate limiting for an API used by millions of users?
24. Your application memory keeps increasing slowly and eventually crashes after several days. How would you investigate the memory leak?
25. A production issue happens only under heavy traffic and cannot be reproduced locally. What is your debugging approach?

Production and DevOps
1. Microservice A calls B. B is slow. A keeps waiting. Entire system slows down. What pattern fixes this?
2. Circuit Breaker is open. What happens to requests? How does it recover?
3. Your Docker container is running but API is not responding. Where do you start?
4. How do you monitor logs across 10 microservices simultaneously?