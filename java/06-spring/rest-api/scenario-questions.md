# Scenario-based Questions

**Scenario/Question:** Design a Scalable REST API You are asked to design a REST API for handling 1 million requests per minute. How would you design the system architecture? What caching strategies would you use? How would you ensure idempotency and consistency?

**Scenario/Question:** API Failure Debugging A REST API intermittently returns HTTP 500 errors in production but works fine locally. How do you debug this? What tools/logging strategies would you use? How would you reproduce the issue?

**Scenario/Question:** API Versioning You need to update an API used by multiple clients without breaking them. What versioning strategy would you use? How do you ensure backward compatibility?

**Scenario/Question:** Pagination Problem Your API returns millions of records. How do you implement efficient pagination? Offset vs cursor-based?

**Scenario/Question:** Spring Boot + REST APIs A REST API is returning inconsistent responses - how will you debug?

**Scenario/Question:** How will you design a highly scalable REST API using Spring Boot?

**Scenario/Question:** How will you implement global exception handling?

**Scenario/Question:** How will you handle API versioning in production?

**Scenario/Question:** How will you design idempotent APIs?

**Scenario/Question:** How will you handle partial failures in APIs?

**Scenario/Question:** How will you handle request validation effectively?

**Scenario/Question:** How will you log and trace API requests?

**Scenario/Question:** How will you design APIs for large file uploads?

**Scenario/Question:** How will you handle bulk operations in APIs?

**Scenario/Question:** How will you design a multi-tenant API system?

**Scenario/Question:** How will you handle large payload requests?

**Scenario/Question:** How will you secure REST APIs?

**Scenario/Question:** How will you secure REST APIs?



# Advanced Interview Questions & Answers

**Scenario/Question:** 16. A Balance Inquiry API is receiving 50,000 requests per minute. How would you reduce database load without returning incorrect balances?

**Answer:** Use an in-memory cache (Redis) configured with a short TTL, or use CQRS where read requests hit a read-replica optimized for queries. Ensure cache invalidation is triggered immediately upon any balance-altering transaction.

**Scenario/Question:** 17. How would you design an API that guarantees exactly one loan application is created even if the client retries multiple times?

**Answer:** Require the client to send a unique `Idempotency-Key` header. Store this key in the database alongside the loan application. If a request arrives with an existing key, return the cached successful response.

**Scenario/Question:** 18. A customer refreshes the transaction history page repeatedly. How would you prevent unnecessary backend load?

**Answer:** Implement rate limiting (e.g., using Redis token bucket) based on IP or user ID. Also, leverage HTTP caching headers (`ETag`, `Cache-Control`) to allow the browser/CDN to serve cached content if unchanged.

**Scenario/Question:** 19. An API returns 200 OK even though part of the business process failed. How would you redesign the response model?

**Answer:** Return a 207 Multi-Status (for bulk operations) or a 202 Accepted if processed asynchronously. For synchronous single operations, if the core business process fails, return an appropriate 4xx or 5xx code with a detailed JSON error body.

**Scenario/Question:** 20. How would you version a banking API while ensuring older mobile app versions continue working?

**Answer:** Use URI versioning (`/v1/accounts`) or header versioning (`Accept: application/vnd.bank.v1+json`). Maintain older API versions alongside new ones until usage metrics show old versions can be deprecated.

**Scenario/Question:** 21. How would you design error responses so that internal implementation details are never exposed to customers?

**Answer:** Use a standardized error structure (like RFC 7807 Problem Details). Catch all exceptions globally, log the stack trace internally with a unique error ID, and return a generic message to the client along with that error ID for support reference.

**Scenario/Question:** 22. A client sends the same request body but with fields in different order. Should the idempotency behavior change? Why?

**Answer:** No, idempotency should remain unchanged. The idempotency key should be based on a distinct header value or a hash of the logical data content, not the exact string representation of the JSON.

**Scenario/Question:** 23. A third-party partner accidentally sends malformed JSON for 20% of requests. How would your API handle this without affecting healthy traffic?

**Answer:** The API framework should immediately reject malformed JSON at the controller/gateway level with a 400 Bad Request, before allocating significant CPU/memory resources, thus preventing resource exhaustion.

**Scenario/Question:** 24. How would you prevent replay attacks on sensitive REST APIs?

**Answer:** Enforce short-lived JWT tokens, require a unique nonce and timestamp in the request signature, and validate that the nonce hasn't been used recently within the timestamp window.

**Scenario/Question:** 25. How would you design APIs that remain backward compatible for at least five years?

**Answer:** Follow the "Tolerant Reader" pattern: add new fields but never remove or rename existing ones. Ignore unrecognized fields in requests. Strictly avoid changing the data type of existing fields.

**Scenario/Question:** 26. A customer submits an address update while another update is already in progress. How would the API behave?

**Answer:** Use optimistic concurrency control (ETag/Version header). The second request fails with 412 Precondition Failed or 409 Conflict because the resource version it attempts to modify is outdated.

**Scenario/Question:** 27. Would you expose internal database IDs in banking APIs? If not, what would you expose instead?

**Answer:** No, exposing database IDs is a security risk (IDOR). Expose UUIDs or opaque public identifiers generated specifically for external use, mapped internally to the DB ID.

**Scenario/Question:** 28. How would you support partial updates without risking accidental data loss?

**Answer:** Use the HTTP `PATCH` method with `application/merge-patch+json` or `application/json-patch+json`, allowing the client to specify exactly which fields to modify rather than sending the entire resource state.

**Scenario/Question:** 29. How would you design APIs to support both synchronous confirmation and asynchronous completion?

**Answer:** Accept the request, validate it synchronously, and return 202 Accepted with a `Location` header pointing to a status polling endpoint (e.g., `/jobs/123`). The client polls this endpoint until the status is COMPLETE.

**Scenario/Question:** 30. How would you detect abusive API clients before they affect legitimate banking users?

**Answer:** Implement Web Application Firewalls (WAF), rate limiting by API gateway, anomaly detection on request frequency, and IP reputation filtering to auto-block suspicious patterns.

