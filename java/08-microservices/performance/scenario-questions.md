# Scenario-based Questions

**Scenario/Question:** Microservice Failure Under Load You have a Spring Boot microservice deployed on AWS. During peak traffic, response times spike and some requests fail. How would you identify whether the issue is in the application, database, or infrastructure? What metrics/logs would you check first? If the issue is due to thread starvation, how would you fix it?

**Scenario/Question:** High CPU Usage Your service suddenly spikes CPU. How do you investigate?

**Scenario/Question:** How will you reduce GC pauses in a high-load system?

**Scenario/Question:** Your API response time increases under load - how will you troubleshoot?

**Scenario/Question:** A service becomes slow after deployment - how will you debug?

**Scenario/Question:** How will you reduce latency between services?



# Advanced Interview Questions & Answers

**Scenario/Question:** 31. A customer's balance is cached for five minutes. During that time, another transaction changes the balance. How would you prevent stale balance display?

**Answer:** Use a write-through or write-behind cache strategy. Whenever a transaction updates the database, the application must immediately update or invalidate the associated cache entry.

**Scenario/Question:** 32. Your Redis cluster becomes unavailable. Should the application fail, bypass the cache, or serve stale data? Explain your decision.

**Answer:** Bypass the cache (Cache Aside pattern). The application should gracefully handle the Redis timeout, fetch data directly from the primary database, and continue functioning, though potentially with degraded performance.

**Scenario/Question:** 33. How would you invalidate cache entries after account updates without affecting performance?

**Answer:** Publish a cache invalidation event asynchronously via a message broker (Kafka/RabbitMQ) to prevent blocking the main transaction thread. All application instances listen and invalidate their local caches.

**Scenario/Question:** 34. Multiple application nodes simultaneously rebuild the same expired cache entry, causing a database spike. How would you prevent this?

**Answer:** This is a cache stampede. Implement distributed locking (e.g., Redis Redlock) when fetching from the DB, or use background proactive refresh before the TTL expires.

**Scenario/Question:** 35. How would you decide whether beneficiary information should be cached?

**Answer:** Cache it. Beneficiary data has a high read-to-write ratio, rarely changes, and is read frequently during transfers, making it a perfect candidate for caching.

**Scenario/Question:** 36. How would you cache exchange rates that update every few minutes?

**Answer:** Set a strict TTL aligned with the update frequency. Alternatively, use a scheduled background job to pull the latest rates and aggressively overwrite the cache, rather than waiting for user-driven cache misses.

**Scenario/Question:** 37. A cache contains sensitive customer information. What security controls would you implement?

**Answer:** Encrypt sensitive fields (PII) before storing them in Redis. Use TLS for in-transit communication to Redis, and enforce authentication/authorization (Redis ACLs) for connecting clients.

**Scenario/Question:** 38. Redis memory reaches 100%. Which eviction strategy would you choose and why?

**Answer:** Use `allkeys-lru` (Least Recently Used). It ensures that the most frequently accessed data remains in memory, while dormant data is evicted, maximizing cache hit rates for active users.

**Scenario/Question:** 39. How would you measure whether caching actually improves API performance?

**Answer:** Monitor and compare the cache hit/miss ratio, track the 95th and 99th percentile latencies of the API, and observe database CPU/IOPS metrics before and after enabling the cache.

**Scenario/Question:** 40. How would you design cache keys to avoid collisions across multiple banking products?

**Answer:** Use a hierarchical namespace convention, e.g., `appName:entityType:tenantId:entityId` (e.g., `retailbanking:account:US:12345`).

**Scenario/Question:** 41. A cached interest rate changes due to a regulatory update. How do you ensure every application node refreshes immediately?

**Answer:** Use Redis Pub/Sub. When the update API is called, broadcast an invalidation message. All nodes subscribe to this channel and evict their local L1 caches simultaneously.

**Scenario/Question:** 42. How would you handle cache warm-up after a full cluster restart?

**Answer:** Write a startup script or background job that proactively queries the most frequently accessed data (e.g., active user profiles, current exchange rates) and populates the cache before opening up to full traffic.

**Scenario/Question:** 43. Would you cache failed API responses? Under what conditions?

**Answer:** Yes, cache negative responses (like 404 Not Found) for a short duration to prevent brute-force querying or repeated heavy DB lookups for non-existent resources (preventing Cache Penetration).

**Scenario/Question:** 44. How would you detect cache poisoning attempts?

**Answer:** Validate all input data strictly before it is used to construct cache keys or stored in the cache. Monitor for unusual patterns in cache keys and implement strict access controls on the cache infrastructure.

**Scenario/Question:** 45. A cache miss rate suddenly increases after deployment. How would you investigate?

**Answer:** Check if cache key generation logic was altered, verify if TTL settings were accidentally lowered, look for bugs in the cache population code, and ensure the Redis cluster is healthy and not aggressively evicting keys.

