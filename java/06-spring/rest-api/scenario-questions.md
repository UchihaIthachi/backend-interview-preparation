# Scenario-Based Questions — Production REST APIs with Spring Boot

## Scenario 1: Design a REST API that handles one million requests per minute

One million requests per minute is approximately:

```text
1,000,000 ÷ 60 ≈ 16,667 requests per second
```

The design must account for:

- Normal and peak request rates
- Read-to-write ratio
- Payload sizes
- Latency requirements
- Consistency requirements
- Geographic distribution
- Failure behavior
- Downstream database capacity

The architecture should not be designed only around average traffic.

---

## High-Level Architecture

```text
Clients
   ↓
CDN / WAF
   ↓
API Gateway
├── Authentication
├── Rate limiting
├── Request-size limits
├── Routing
└── Observability
   ↓
Load Balancer
   ↓
Stateless Spring Boot instances
├── Local cache
├── Distributed cache
├── Business logic
└── Bounded executors
   ↓
Primary database / Read replicas
   ↓
Kafka or message queue
   ↓
Asynchronous workers
```

API limits should cover request rate, body size, execution time, pagination size and resource-intensive operations. Without such limits, one client can exhaust CPU, memory, threads, connections or downstream capacity. ([OWASP Foundation][1])

---

## 1. Keep Spring Boot Instances Stateless

Do not store user or request state in mutable fields inside singleton Spring beans.

Unsafe:

```java
@Service
public class OrderService {

    private Order currentOrder;

    public Order process(Order order) {
        currentOrder = order;
        return processCurrentOrder();
    }
}
```

Prefer local variables and durable shared stores:

```java
@Service
public class OrderService {

    public Order process(Order order) {
        Order validatedOrder = validate(order);
        return save(validatedOrder);
    }
}
```

Stateless instances can be added or removed behind a load balancer without losing request state.

---

## 2. Use Several Caching Layers

### HTTP and CDN caching

Use for public or safely cacheable responses:

```http
Cache-Control: public, max-age=300
ETag: "product-catalog-v82"
```

Suitable for:

- Product catalogues
- Public configuration
- Static metadata
- Images and documents

Do not publicly cache user-specific financial or personal responses.

---

### Local in-process cache

A local cache provides very low latency:

```text
Spring Boot instance
└── Local cache
```

Suitable for:

- Reference data
- Feature configuration
- Currency metadata
- Frequently accessed immutable values

Limitations:

- Every instance has a separate copy.
- Invalidation must reach every instance.
- Cold instances initially have empty caches.

---

### Distributed cache

Redis can provide a cache shared across application instances:

```text
Spring Boot instances
        ↓
      Redis
```

Suitable for:

- Shared reference data
- Rate-limit counters
- Idempotency lookup
- Short-lived computed responses
- Session data when server-managed sessions are unavoidable

A common pattern is cache-aside:

```text
Read cache
   ├── Hit  → return value
   └── Miss → read database
               ↓
             update cache
               ↓
             return value
```

### Cache correctness requirements

Every cache needs an explicit policy for:

- Maximum size
- Time to live
- Invalidation
- Stale-data tolerance
- Failure behavior
- Cache stampede protection
- Source of truth

---

## 3. Protect the Database

Use:

- Proper indexes
- Connection-pool limits
- Read replicas when replica lag is acceptable
- Query projections
- Batch writes
- Partitioning only when required
- Atomic conditional updates
- Optimistic locking
- Database constraints

Do not increase application concurrency beyond database capacity.

```text
Application workers:     500
Database connections:     30

Result:
Most workers wait for connections.
```

The bottleneck remains the database even though the application has hundreds of threads.

---

## 4. Add Backpressure

Do not accept unlimited work into an unbounded queue.

```java
ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
                32,
                64,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2_000),
                new ThreadPoolExecutor.AbortPolicy()
        );
```

When capacity is exhausted, reject or defer work explicitly:

```text
429 Too Many Requests
503 Service Unavailable
202 Accepted for asynchronous processing
```

This is safer than accepting every request and allowing memory and latency to grow without limits.

---

## 5. Use Asynchronous Processing Appropriately

Long-running operations can be converted into jobs:

```text
POST /reports
        ↓
Validate and create job
        ↓
Publish command
        ↓
Return 202 Accepted
        ↓
Worker generates report
        ↓
Client checks /jobs/{jobId}
```

Example response:

```http
HTTP/1.1 202 Accepted
Location: /api/v1/jobs/job-781
```

Do not return “completed” merely because Kafka accepted a command. Message acceptance does not prove that business processing succeeded.

---

## 6. Enforce Idempotency and Consistency

Use:

- Idempotency keys
- Unique database constraints
- Atomic SQL
- Optimistic concurrency
- Transaction boundaries
- Outbox/inbox processing
- Versioned resources
- Idempotent consumers

HTTP distinguishes idempotent operations by their intended effect, and conditional request headers such as `If-Match` can make updates dependent on the current resource version. ([RFC Editor][2])

### Interview-ready answer

> I would use stateless Spring Boot instances behind a gateway and load balancer, layered caching, bounded concurrency, database indexes and constraints, and asynchronous processing for long-running work. I would protect writes using idempotency keys, atomic database operations, optimistic versions and durable event publication. The design would be capacity-tested at peak traffic rather than only average throughput.

---

# Scenario 2: A REST API intermittently returns HTTP 500 in production

An HTTP 500 response means the server encountered an unexpected condition. The objective is to identify:

```text
Which request failed?
On which instance?
At which processing stage?
Under which production condition?
```

---

## Step 1: Add a Trace or Correlation ID

Every request should have an identifier:

```text
X-Request-Id: req-8d71f
traceId: 76b9f2...
spanId: 23a814...
```

Include it in:

- Structured logs
- Error responses
- Distributed traces
- Downstream HTTP calls
- Kafka message headers

Example log:

```json
{
  "level": "ERROR",
  "traceId": "76b9f2",
  "requestId": "req-8d71f",
  "method": "POST",
  "path": "/api/v1/orders",
  "status": 500,
  "durationMs": 823,
  "exception": "DataIntegrityViolationException"
}
```

Do not log:

- Access tokens
- Passwords
- Full card information
- Sensitive request bodies
- Session cookies

---

## Step 2: Identify the Failure Boundary

Use distributed tracing to divide the request:

```text
Gateway
  ↓ 10 ms
Controller
  ↓ 2 ms
Service
  ↓ 20 ms
Database
  ↓ 750 ms
Downstream API
  ↓ failed
```

Inspect:

- Controller binding and validation
- Business logic
- Database operations
- Connection-pool waits
- Remote HTTP calls
- Serialization
- Executor queues
- Cache failures
- Messaging publication

---

## Step 3: Compare Local and Production Environments

Intermittent production-only failures often involve:

- Different data volume
- Different database constraints
- Concurrent requests
- Replica lag
- Configuration differences
- Missing environment variables
- Time-zone or locale differences
- Feature flags
- Multiple application versions
- Network timeouts
- Container resource limits
- Production-only security rules

A local single-threaded test rarely reproduces concurrency or capacity failures.

---

## Step 4: Inspect the Exception Chain

Log the complete internal exception chain:

```text
Controller exception
→ service exception
→ database exception
→ database constraint name
```

Return only a safe client response.

Avoid:

```json
{
  "message": "ORA-00001 on T_CUSTOMER_INTERNAL..."
}
```

Prefer:

```json
{
  "title": "Unexpected server error",
  "status": 500,
  "traceId": "76b9f2"
}
```

---

## Step 5: Reproduce with Production-Like Conditions

Use:

- Sanitized failing payload
- Same database engine
- Similar data volume
- Same configuration
- Concurrent requests
- Same timeout values
- Same JDK and container limits
- Fault injection for downstream failures

Run repeated and concurrent tests rather than sending the request only once.

---

## Step 6: Capture JVM Evidence

When the error correlates with resource pressure, collect:

```bash
jcmd <pid> Thread.print -l
```

```bash
jcmd <pid> JFR.start \
  name=api-failure \
  settings=profile \
  duration=2m \
  filename=/tmp/api-failure.jfr
```

Also inspect:

- Heap usage
- GC pauses
- Thread-pool queues
- HikariCP pending connections
- HTTP-client connection pools
- CPU throttling
- Container restarts

### Interview-ready answer

> I correlate every failure using a trace ID, inspect the complete exception internally, and use tracing to locate the failing span. I compare production configuration, data, concurrency and dependency behavior with local conditions, then reproduce using the same payload and production-like load. For resource-related failures, I capture thread dumps, JFR, pool metrics and GC evidence.

---

# Scenario 3: Update an API without breaking existing clients

## Prefer Backward-Compatible Changes First

Usually safe changes include:

- Adding an optional response field
- Adding a new endpoint
- Adding an optional query parameter
- Supporting another media type
- Expanding an enum only when clients tolerate unknown values

Potentially breaking changes include:

- Removing or renaming a field
- Changing a field’s type
- Changing nullability
- Changing field meaning
- Making an optional field mandatory
- Changing status-code semantics
- Reinterpreting an existing enum value

---

## Versioning Strategies

| Strategy   | Example                                | Strength                | Limitation             |
| ---------- | -------------------------------------- | ----------------------- | ---------------------- |
| URI        | `/api/v2/accounts`                     | Visible and simple      | Version appears in URL |
| Header     | `API-Version: 2`                       | Keeps URI stable        | Less visible           |
| Media type | `Accept: application/vnd.bank.v2+json` | Versions representation | More client complexity |

For public mobile APIs, URI versioning is often the easiest operationally:

```text
/api/v1/accounts
/api/v2/accounts
```

---

## Safe Migration Process

```text
Create v2
→ keep v1 operational
→ add contract tests
→ publish migration guide
→ monitor v1 usage
→ announce deprecation
→ migrate clients
→ retire v1 after agreed period
```

Use compatibility adapters when possible:

```text
V1 request
   ↓
V1-to-domain adapter
   ↓
Shared business service
   ↓
V1 response mapper
```

This avoids duplicating the complete business implementation.

### Interview-ready answer

> I avoid versioning for additive compatible changes. For a real breaking change, I normally introduce a new URI version, keep the old version operational, reuse the same domain service through version-specific adapters, run consumer contract tests and monitor old-version usage before deprecation.

---

# Scenario 4: The API returns millions of records

Never return an unbounded dataset from one endpoint.

```text
GET /transactions
```

must require:

- Page or cursor limit
- Stable ordering
- Maximum page size
- Optional filters

---

## Offset Pagination

Example:

```http
GET /transactions?page=1000&size=50
```

Conceptual query:

```sql
SELECT *
FROM transaction
ORDER BY created_at DESC, id DESC
LIMIT 50 OFFSET 50000;
```

### Advantages

- Simple
- Supports arbitrary page numbers
- Easy to display total pages

### Limitations

- Large offsets can become expensive.
- The database may scan and discard many rows.
- Inserts or deletes can cause skipped or duplicated results.
- Counting all matching records can be expensive.

Use offset pagination for:

- Small datasets
- Administrative screens
- Shallow navigation
- Stable datasets

---

## Cursor or Keyset Pagination

First request:

```http
GET /transactions?limit=50
```

Next request:

```http
GET /transactions?limit=50&cursor=eyJjcmVhdGVkQXQi...
```

Conceptual query:

```sql
SELECT *
FROM transaction
WHERE created_at < :lastCreatedAt
   OR (
       created_at = :lastCreatedAt
       AND id < :lastId
   )
ORDER BY created_at DESC, id DESC
LIMIT :limit;
```

The ordering must be stable and include a unique tie-breaker.

Spring Data supports offset- and keyset-based scrolling; keyset scrolling reconstructs the next query from the last-seen sort values and benefits from matching indexes. ([Home][3])

### Advantages

- Efficient for deep navigation
- Stable under concurrent inserts
- Does not scan a large offset
- Suitable for feeds and timelines

### Limitations

- Cannot jump directly to arbitrary page 9,000.
- Cursor creation and validation are more complex.
- Sorting options may be restricted.
- Total-page counts may require a separate query.

### Interview-ready answer

> I use bounded offset pagination for small or shallow datasets. For millions of records or continuously changing data, I use cursor-based keyset pagination with a stable indexed order such as `createdAt` plus `id`. The cursor is opaque to the client and represents the last-seen sort values.

---

# Scenario 5: A REST API returns inconsistent responses

Possible causes include:

- Reading from replicas with lag
- Stale distributed caches
- Different cache contents across instances
- Race conditions
- Missing database transaction boundaries
- Multiple deployment versions
- Feature-flag differences
- Non-deterministic collection ordering
- Time-zone differences
- Partial downstream failures
- Missing optimistic locking
- Concurrent updates

## Investigation

Add response and trace metadata internally:

```text
trace ID
application version
instance ID
database source
cache hit or miss
resource version
feature-flag version
```

Then compare two inconsistent requests.

Ask:

1. Did they reach different application versions?
2. Did one read from cache and one from the database?
3. Did one read from a replica?
4. Was the resource updated between requests?
5. Was response ordering undefined?
6. Was the operation missing a transaction?
7. Did a downstream service return different data?

For read-after-write consistency, route the immediate follow-up read to the authoritative store or include a resource version that lets the client detect staleness.

---

# Scenario 6: How would you implement global exception handling?

Use `@RestControllerAdvice` and a consistent problem response.

Spring supports RFC 9457 Problem Details through `ProblemDetail`, `ErrorResponse`, `@ExceptionHandler` and `ResponseEntityExceptionHandler`. RFC 9457 supersedes the older RFC 7807 specification. ([RFC Editor][4])

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(
            OrderNotFoundException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle("Order not found");
        problem.setInstance(
                URI.create(request.getRequestURI())
        );
        problem.setProperty(
                "errorCode",
                "ORDER_NOT_FOUND"
        );

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        String traceId = currentTraceId();

        log.error(
                "Unexpected API error traceId={}",
                traceId,
                exception
        );

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred"
                );

        problem.setTitle("Internal server error");
        problem.setInstance(
                URI.create(request.getRequestURI())
        );
        problem.setProperty("traceId", traceId);

        return problem;
    }

    private String currentTraceId() {
        return UUID.randomUUID().toString();
    }
}
```

Do not expose:

- Stack traces
- SQL statements
- Internal hostnames
- Secret configuration
- Database table names
- Internal exception classes

---

# Scenario 7: How would you design an idempotent API?

## Idempotency Record

```sql
CREATE TABLE idempotency_record (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    request_hash VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL,
    resource_id VARCHAR(100),
    response_body TEXT,
    created_at TIMESTAMP NOT NULL
);
```

Possible states:

```text
PROCESSING
COMPLETED
FAILED_RETRYABLE
FAILED_FINAL
```

## Workflow

```text
Receive Idempotency-Key
        ↓
Canonicalize and hash logical request
        ↓
Atomically insert key
        ├── New key → process operation
        └── Existing key
              ├── Different hash → reject
              ├── Completed → return stored result
              └── Processing → return current status
```

The business write and idempotency state should be committed atomically when they use the same database.

A unique constraint is required because this sequence is unsafe:

```text
Check whether key exists
→ two requests both see absent
→ both create a resource
```

### Important distinction

This provides an **exactly-once business effect**, not exactly-once network delivery.

---

# Scenario 8: How would you handle partial failures?

First define whether the operation is:

- One atomic business operation
- A bulk request containing independent items
- A long-running distributed workflow
- A synchronous aggregation of several downstream calls

---

## Single atomic operation

If the core operation fails, return the corresponding failure:

```text
4xx for client or business-state failure
5xx for unexpected server or dependency failure
```

Do not return `200 OK` with a hidden failed state.

---

## Bulk operation

For a small synchronous batch, return item-level outcomes:

```json
{
  "results": [
    {
      "reference": "A",
      "status": "SUCCESS"
    },
    {
      "reference": "B",
      "status": "FAILED",
      "errorCode": "INVALID_ACCOUNT"
    }
  ]
}
```

`207 Multi-Status` originates from WebDAV. A generic API may deliberately adopt it, but it should not be the automatic default for every bulk endpoint. A normal `200` response with documented per-item outcomes is often clearer.

For a large batch:

```text
POST /bulk-jobs
→ 202 Accepted
→ process asynchronously
→ poll /bulk-jobs/{id}
```

---

## Distributed workflow

Use explicit states:

```text
PENDING
IN_PROGRESS
COMPLETED
PARTIALLY_COMPLETED
COMPENSATION_REQUIRED
FAILED
```

For operations spanning several services, consider:

- Saga orchestration
- Compensation
- Transactional outbox
- Idempotent consumers
- Retry with backoff
- Dead-letter handling
- Reconciliation

---

# Scenario 9: How would you handle request validation effectively?

Validation should occur at several boundaries.

## Layer 1: Protocol and gateway

Validate:

- Maximum body size
- Content type
- Header size
- Required authentication
- Rate limit
- File size
- Allowed HTTP methods

## Layer 2: DTO validation

```java
public record CreatePaymentRequest(

        @NotBlank
        String accountId,

        @Positive
        long amountCents,

        @NotBlank
        String currency
) {
}
```

```java
@PostMapping
public PaymentResponse create(
        @Valid
        @RequestBody
        CreatePaymentRequest request
) {
    return paymentService.create(request);
}
```

Spring MVC applies Bean Validation to request bodies annotated with `@Valid` or `@Validated`. ([Home][5])

## Layer 3: Business validation

Examples:

- Account exists
- Account is active
- Currency is supported
- Sufficient funds exist
- User owns the account

## Layer 4: Database protection

Use:

- Unique constraints
- Foreign keys
- Check constraints
- Optimistic versions
- Atomic updates

Validation in Java does not replace database consistency rules.

---

# Scenario 10: How would you log and trace API requests?

Use structured logs with:

- Trace ID
- Span ID
- Request ID
- HTTP method
- Route template
- Status
- Duration
- Service name
- Application version
- Safe tenant/user identifier
- Downstream dependency
- Error code

Prefer:

```text
route=/api/v1/orders/{id}
```

over logging every concrete URI as a separate metric:

```text
/api/v1/orders/1001
/api/v1/orders/1002
```

The second approach creates high-cardinality telemetry.

### Never log by default

- Passwords
- Authorization headers
- Cookies
- JWT contents
- Full personal data
- Full file contents
- Complete payment payloads

For large or sensitive bodies, log:

- Size
- Content type
- Hash where appropriate
- Validation outcome
- Business reference

---

# Scenario 11: How would you design APIs for large file uploads?

For very large files, prefer direct upload to object storage:

```text
Client requests upload session
        ↓
API returns signed upload details
        ↓
Client uploads directly to object storage
        ↓
Client confirms completion
        ↓
API creates asynchronous processing job
```

This avoids routing multi-gigabyte files through every Spring Boot instance.

Include:

- Multipart or resumable upload
- File-size limits
- Checksum validation
- Content-type validation
- Malware scanning
- Encryption
- Upload expiration
- Idempotent completion
- Asynchronous processing
- Quarantine before publication

When files pass through Spring, stream them instead of loading them into one byte array. Spring’s `MultipartFile` may use memory or temporary disk storage and exposes an `InputStream`; the application is responsible for moving the data to durable storage when required. ([Home][6])

Avoid:

```java
byte[] fileContents = file.getBytes();
```

for very large files.

Prefer:

```java
try (InputStream input = file.getInputStream()) {
    storageService.upload(input, file.getSize());
}
```

---

# Scenario 12: How would you handle bulk operations?

A bulk endpoint must define:

- Maximum item count
- Maximum body size
- Atomic or per-item semantics
- Idempotency scope
- Ordering
- Timeout
- Result retention
- Retry behavior

## Small synchronous bulk request

```http
POST /api/v1/payments/batch
```

Return item-level results.

## Large asynchronous bulk request

```text
Upload file
→ create job
→ return 202
→ process bounded chunks
→ expose status and error report
```

Use:

- Bounded batches
- Checkpoints
- Per-item idempotency keys
- Dead-letter handling
- Progress metrics
- Retryable and non-retryable error classification

Do not wrap millions of records in one database transaction.

---

# Scenario 13: How would you design a multi-tenant API?

## Tenant Identification

Obtain the tenant from a verified identity source:

```text
Validated token claim
→ tenant context
```

Do not trust an arbitrary client header without validating that the caller belongs to that tenant.

## Isolation Models

| Model                          | Isolation | Operational complexity |
| ------------------------------ | --------: | ---------------------: |
| Shared tables with `tenant_id` |     Lower |                  Lower |
| Schema per tenant              |    Medium |                 Medium |
| Database per tenant            |    Higher |                 Higher |

For shared tables:

```sql
SELECT *
FROM orders
WHERE tenant_id = :authenticatedTenantId
  AND id = :orderId;
```

Apply tenant filtering consistently to:

- Queries
- Cache keys
- Event messages
- Object-storage paths
- Logs
- Metrics
- Idempotency keys

Also implement:

- Tenant quotas
- Per-tenant rate limits
- Noisy-neighbour controls
- Tenant-aware encryption where required
- Object-level authorization
- Audit trails

---

# Scenario 14: How would you secure REST APIs?

Use defense in depth:

1. TLS for all network communication
2. OAuth 2.0/OIDC or another appropriate authentication mechanism
3. Endpoint and object-level authorization
4. Short-lived credentials
5. Request and response size limits
6. Input validation
7. Rate limits and business-flow protection
8. Secure secret management
9. Safe logging
10. Dependency and image scanning
11. Audit events
12. CSRF protection for cookie-based browser authentication
13. Narrow CORS configuration
14. mTLS for selected internal or partner communication

Opaque public identifiers may reduce enumeration, but they do not replace authorization. IDOR is fundamentally caused by missing object-level authorization, not merely by exposing a numeric database identifier. OWASP highlights broken object-property authorization and unrestricted resource consumption among major API risks. ([OWASP Foundation][7])

---

# Advanced Interview Questions

## Question 16: A balance API receives 50,000 requests per minute. How do you reduce database load without returning incorrect balances?

Fifty thousand requests per minute is approximately 833 requests per second.

Do not blindly cache the balance with a TTL and claim it is always correct. Even a one-second TTL may return an invalid available balance immediately after a debit.

Better options include:

- Maintain a versioned ledger-derived balance projection.
- Update the projection transactionally with the ledger where possible.
- Use an event-driven read model with explicit freshness guarantees.
- Return an `asOf` timestamp or version.
- Route exact-balance reads to an authoritative store.
- Cache only when the business explicitly accepts bounded staleness.
- Use replicas only when their lag is acceptable.

For highly sensitive operations:

```text
Display balance
→ potentially bounded-stale projection

Authorize withdrawal
→ authoritative consistency check
```

---

## Question 17: How do you guarantee that only one loan application is created after retries?

Require an idempotency key and enforce uniqueness in the database.

```text
Idempotency-Key + customer + operation
→ unique record
→ same transaction as loan creation
```

Store:

- Key
- Canonical request hash
- Loan application ID
- Status
- Original result

A repeated request with the same logical payload returns the original result. The same key with different data is rejected.

---

## Question 18: A customer repeatedly refreshes transaction history. How do you reduce load?

Use:

- Bounded cursor pagination
- `ETag`
- Conditional `If-None-Match`
- Appropriate private caching
- Incremental “since cursor” queries
- User-level rate limiting

When unchanged:

```http
HTTP/1.1 304 Not Modified
```

User-specific transaction history should not be placed in a shared public CDN cache without a carefully designed security policy.

---

## Question 19: An API returns `200 OK` even though part of the process failed. How should it be redesigned?

For a single synchronous operation, return a failure status when its core business outcome failed.

For independent bulk items:

```text
200 with documented per-item outcomes
```

For long-running work:

```text
202 Accepted + status endpoint
```

Use `207 Multi-Status` only when the API deliberately adopts and documents that model; it should not be treated as the universal response for partial success.

---

## Question 20: How do you version a banking API while keeping old mobile clients working?

Use additive changes whenever possible. For breaking changes:

```text
/api/v1/accounts
/api/v2/accounts
```

Keep v1 active, isolate version-specific mapping, monitor usage and provide a migration period. Test old mobile clients against the v1 contract continuously.

---

## Question 21: How do you design safe error responses?

Use RFC 9457 Problem Details:

```json
{
  "type": "https://api.example.com/problems/account-not-found",
  "title": "Account not found",
  "status": 404,
  "detail": "The requested account could not be found",
  "instance": "/api/v1/accounts/acc-81",
  "errorCode": "ACCOUNT_NOT_FOUND",
  "traceId": "19a62e"
}
```

Log technical details internally and return a stable error code plus trace ID to the client. Spring’s current error-response support is built around RFC 9457. ([RFC Editor][4])

---

## Question 22: The same JSON fields arrive in a different order. Should idempotency change?

No.

These payloads are logically equivalent:

```json
{
  "amount": 100,
  "currency": "USD"
}
```

```json
{
  "currency": "USD",
  "amount": 100
}
```

Use the client-provided idempotency key as the primary identifier. If a request hash is required, hash a canonical logical representation—not the raw JSON byte sequence.

---

## Question 23: A partner sends malformed JSON for 20% of requests. How do you protect healthy traffic?

- Reject malformed bodies with `400 Bad Request`.
- Apply maximum request-body size.
- Limit parser depth and complexity where supported.
- Apply per-partner rate limits.
- Use bounded request threads and queues.
- Track validation failure rates.
- Temporarily throttle a consistently faulty partner.
- Avoid logging every complete malformed body.

Malformed JSON still consumes network, parser and connection resources, so early rejection alone is not sufficient without resource limits.

---

## Question 24: How do you prevent replay attacks?

Depending on risk level, use:

- TLS
- Short-lived tokens
- Issuer, audience and signature validation
- Request timestamp
- Unique nonce
- Signed request body
- Nonce-reuse detection
- Idempotency keys
- mTLS for trusted partners

A short-lived JWT does not fully prevent replay while that token remains valid.

---

## Question 25: How do you keep APIs backward compatible for five years?

- Prefer additive response changes.
- Never repurpose an existing field.
- Avoid changing field types.
- Preserve existing status-code semantics.
- Introduce new enum values carefully.
- Maintain contract tests for old clients.
- Version genuine breaking changes.
- Track client usage.
- Publish deprecation timelines.

A tolerant reader can ignore unknown response fields. However, silently ignoring unknown request fields in sensitive commands may hide client defects, so request strictness should be chosen deliberately.

---

## Question 26: Two clients update the same address concurrently. What should happen?

Use an `ETag` or explicit version.

Read:

```http
ETag: "customer-address-v8"
```

Update:

```http
If-Match: "customer-address-v8"
```

If another update already created version 9, reject the stale operation with:

```text
412 Precondition Failed
```

A `409 Conflict` can also represent a domain-level conflict, but `412` directly reflects a failed HTTP precondition. Conditional request semantics are defined by HTTP. ([RFC Editor][2])

---

## Question 27: Should internal database IDs be exposed?

Exposing a numeric ID is not automatically a security vulnerability.

The real requirement is:

```text
Every resource access
→ verify authenticated caller
→ verify tenant
→ verify object ownership or permission
```

Opaque UUIDs or public identifiers can:

- Reduce easy enumeration
- Decouple API identity from storage
- Support migrations

They still do not replace authorization.

---

## Question 28: How do you support partial updates safely?

Use `PATCH` with a documented patch format.

Protect against:

- Updating restricted fields
- Ambiguous null behavior
- Lost concurrent updates
- Mass assignment
- Unsupported operations

Combine PATCH with:

```text
Field whitelist
+ validation
+ If-Match/version
+ authorization
```

Do not bind an arbitrary patch directly onto a JPA entity.

---

## Question 29: How do you support synchronous acceptance and asynchronous completion?

```text
Client submits request
        ↓
Synchronous authentication and validation
        ↓
Create durable job
        ↓
202 Accepted
Location: /jobs/{id}
        ↓
Worker processes operation
        ↓
Job becomes COMPLETED or FAILED
```

The status endpoint should expose:

- State
- Progress
- Result link
- Failure code
- Creation and completion timestamps
- Retryability

---

## Question 30: How do you detect abusive API clients?

Use:

- Gateway rate limits
- Per-user, tenant and API-key quotas
- WAF rules
- IP reputation
- Credential-stuffing detection
- Request-pattern anomaly detection
- Business-flow limits
- Payload-size limits
- Cost-based limits for expensive endpoints
- Alerts on unusual error and traffic patterns

Rate limiting must be tailored to the endpoint and business operation rather than applying one global number to every API. ([OWASP Foundation][1])

---

# Corrections to Common Claims

## “A short Redis TTL guarantees correct account balances”

It does not. A short TTL only limits how long stale data might remain. Exact financial decisions should use an authoritative or strongly consistent balance source.

## “207 is the correct response for every partial failure”

It is not a universal bulk-response requirement. Use a response contract that clearly represents item outcomes, or use asynchronous `202` processing.

## “UUIDs prevent IDOR”

They reduce easy enumeration but do not fix missing authorization.

## “JWT prevents replay attacks”

A stolen bearer JWT can normally be replayed until it expires or is otherwise rejected.

## “Replica reads are always safe for scaling”

Replica lag can violate read-after-write expectations.

## “Kafka makes the operation successful”

Kafka can durably accept work, but the final business operation may still fail.

---

# Short Interview Answer

> I design scalable Spring Boot APIs using stateless instances, bounded concurrency, layered caching, database constraints, asynchronous processing and strong observability. I protect side effects with idempotency keys and atomic persistence, use cursor pagination for large changing datasets, and handle concurrent updates through versions or `If-Match`. I return RFC 9457 problem responses, stream large files and payloads, use asynchronous jobs for large bulk operations, and enforce tenant-aware authentication, authorization, quotas and resource limits. Most importantly, I define consistency, failure and retry semantics explicitly rather than assuming that caching, Kafka or UUIDs solve them automatically.

[1]: https://owasp.org/API-Security/editions/2023/en/0xa4-unrestricted-resource-consumption/?utm_source=chatgpt.com "API4:2023 Unrestricted Resource Consumption"
[2]: https://www.rfc-editor.org/info/rfc9110/?utm_source=chatgpt.com "RFC 9110: HTTP Semantics | RFC ..."
[3]: https://docs.spring.io/spring-data/jpa/reference/data-commons/repositories/scrolling.html?utm_source=chatgpt.com "Scrolling :: Spring Data JPA"
[4]: https://www.rfc-editor.org/info/rfc9457/?utm_source=chatgpt.com "RFC 9457: Problem Details for HTTP APIs"
[5]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html?utm_source=chatgpt.com "Validation :: Spring Framework"
[6]: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/multipart/MultipartFile.html?utm_source=chatgpt.com "MultipartFile (Spring Framework 7.0.8 API)"
[7]: https://owasp.org/www-project-api-security/?utm_source=chatgpt.com "OWASP API Security Project"
