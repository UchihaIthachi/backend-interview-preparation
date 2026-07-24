# Scenario-Based Questions — Resilience, Messaging, Scale, Banking, and AWS Cloud

The following expands, corrects, and organizes the uploaded material into interview-ready answers while preserving its main scenarios and terminology.

## Important Corrections

- A circuit breaker should not replace **timeouts**, **bulkheads**, or **load shedding**. It prevents repeated calls to an unhealthy dependency after failures have crossed a threshold.
- Azure Service Bus duplicate detection works using the application-supplied `MessageId` during a configured window. It does not make database updates or external side effects automatically idempotent. ([Microsoft Learn][1])
- Azure Service Bus queues provide point-to-point competing-consumer delivery, while topics and subscriptions provide durable publish-subscribe delivery. ([Microsoft Learn][2])
- A DLQ is not an automatic retry solution. Messages must be inspected, corrected or classified, and deliberately replayed or discarded.
- A timeout from a payment provider does not prove that the payment failed. The provider may have completed the charge before its response was lost.
- A cache should not normally be the authoritative source for balances, account-freeze decisions, payment state, or inventory reservations.
- Event Sourcing is not mandatory for building a customer-profile source of truth. A normal transactional database, transactional outbox, and downstream projections may be sufficient.
- HPA or ECS autoscaling cannot repair a saturated database, shared lock, hot partition, or restricted third-party quota.
- SQS FIFO reduces duplicates introduced during sending and maintains ordering within message groups, but business consumers should remain idempotent.
- JMH is useful for isolated Java microbenchmarks. It does not replace application load testing, production-like integration tests, JFR analysis, or end-to-end latency measurement.

---

# Part I — General Scenario-Based Questions

# 1. A Dependent User Service Is Intermittently Failing

## Scenario

```text
Order API
    ↓
User Service
    ↓
Intermittent timeout or 5xx
```

If every Order API request requires a successful synchronous call to User Service, the failure propagates directly to customers.

## Step 1: Classify the Dependency

Determine whether user data is:

- Required for correctness
- Required only for display
- Stable enough to cache
- Available from a local projection
- Safe to omit temporarily

Examples:

```text
Customer identity required for authorization
→ do not invent a fallback

Customer display name required for UI
→ cached or placeholder value may be acceptable
```

## Step 2: Add Strict Timeouts

Every remote call should have:

- Connection timeout
- Response timeout
- Overall request deadline

Example with `WebClient`:

```java
HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
        .responseTimeout(Duration.ofSeconds(2));

WebClient webClient = WebClient.builder()
        .clientConnector(
                new ReactorClientHttpConnector(httpClient)
        )
        .baseUrl("http://user-service")
        .build();
```

Spring’s WebClient supports explicit connection, read, write, and response timeout configuration through its underlying HTTP client. ([Home][3])

## Step 3: Add a Circuit Breaker

Conceptual states:

```text
CLOSED
Requests pass normally.

OPEN
Requests fail fast without calling the dependency.

HALF_OPEN
A limited number of probe requests determine recovery.
```

Example Resilience4j configuration:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      userService:
        sliding-window-type: count_based
        sliding-window-size: 20
        minimum-number-of-calls: 10
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 2s
        wait-duration-in-open-state: 15s
        permitted-number-of-calls-in-half-open-state: 3

  timelimiter:
    instances:
      userService:
        timeout-duration: 2s
```

Spring Cloud Circuit Breaker provides an abstraction over implementations such as Resilience4j, with separate starters for blocking and reactive applications. ([Home][4])

## Step 4: Add a Bulkhead

Without a bulkhead:

```text
User Service becomes slow
→ all request threads wait
→ Order API becomes unavailable
```

With a bulkhead:

```text
Maximum concurrent User Service calls = 30
Additional requests
→ rejected or degraded quickly
```

A circuit breaker reacts after failures occur. A bulkhead limits how much capacity the dependency may consume.

## Step 5: Choose a Safe Fallback

### Last-known-good cache

Suitable for:

- Display name
- Profile image reference
- Non-security preferences

Include freshness metadata:

```json
{
  "userId": "U-100",
  "displayName": "Customer",
  "dataStatus": "STALE",
  "lastUpdatedAt": "2026-07-24T07:40:00Z"
}
```

### Local read projection

User Service publishes:

```text
UserCreated
UserProfileUpdated
UserDeactivated
```

Order Service maintains only the fields it needs.

### Partial response

```json
{
  "orderId": "O-100",
  "customer": {
    "userId": "U-100",
    "detailsAvailable": false
  }
}
```

### Queue for later processing

Use when the operation can be asynchronous.

### Fail closed

Use where stale or missing user data could cause:

- Unauthorized access
- Payment to the wrong account
- Compliance violation

## Step 6: Retry Carefully

Retry only:

- Timeouts before a response
- Connection-reset failures
- Selected transient `5xx` responses
- Idempotent operations

Use:

```text
bounded attempts
+
exponential backoff
+
jitter
+
overall deadline
```

Do not retry:

- Validation errors
- Authorization failures
- Business rejections
- Non-idempotent writes without protection

## Interview-Ready Answer

> I first classify whether the dependency is essential for correctness or only for enrichment. I configure short connection and response timeouts, isolate the dependency with a bulkhead, and use a circuit breaker to fail fast after repeated failures. A fallback may use a last-known-good cache, a local event-driven projection, a partial response, or asynchronous processing. I never invent security-sensitive or financial data. Retries are bounded, use exponential backoff and jitter, and apply only to transient, idempotent operations.

---

# 2. Process Orders Asynchronously with Azure Service Bus

## High-Level Architecture

```text
Client
    ↓
Order API
    ↓ local transaction
Order DB + Outbox
    ↓
Outbox Publisher
    ↓
Azure Service Bus topic
    ├── Inventory subscription
    ├── Payment subscription
    ├── Notification subscription
    └── Analytics subscription
```

Azure Service Bus queues provide single logical consumption with competing consumers. Topics and subscriptions provide durable one-to-many publish-subscribe messaging. ([Microsoft Learn][5])

## Queue vs Topic

### Queue

Use when one logical worker group should handle each order command:

```text
order-processing queue
    ├── worker 1
    ├── worker 2
    └── worker 3
```

Only one competing consumer should successfully complete a given delivery.

### Topic

Use when several systems need independent copies:

```text
OrderCreated topic
    ├── inventory subscription
    ├── fraud subscription
    ├── notification subscription
    └── analytics subscription
```

Each subscription can have:

- Filters
- Independent retry behavior
- Independent DLQ
- Separate scaling

## Event Contract

```json
{
  "eventId": "evt-100",
  "eventType": "OrderCreated",
  "orderId": "O-100",
  "customerId": "C-200",
  "occurredAt": "2026-07-24T08:00:00Z",
  "aggregateVersion": 1,
  "schemaVersion": 2
}
```

Avoid placing complete sensitive customer or payment details in every event.

## Prevent Dual-Write Failure

Incorrect:

```text
Commit Order DB
→ publish message
→ process crashes before publish
→ order exists but no event exists
```

Use the transactional outbox:

```text
BEGIN TRANSACTION
  INSERT order
  INSERT outbox_event
COMMIT
```

A relay publishes the outbox record later.

## Message Processing

Recommended sequence:

```text
Receive message with lock
        ↓
Validate schema
        ↓
Check deduplication record
        ↓
Perform business update
        ↓
Record message as processed
        ↓
Commit database transaction
        ↓
Complete Service Bus message
```

If the process crashes after the DB commit but before message completion, the message may be delivered again. The consumer must therefore be idempotent.

## Idempotent Consumer Table

```sql
CREATE TABLE processed_message (
    consumer_name VARCHAR(100) NOT NULL,
    message_id VARCHAR(200) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    PRIMARY KEY (consumer_name, message_id)
);
```

Perform the deduplication insert and business update in the same database transaction.

Azure Service Bus also supports duplicate detection based on `MessageId`, but that broker-level feature should complement rather than replace application idempotency. ([Microsoft Learn][1])

## Processing Failures

### Transient failure

Examples:

- Temporary database outage
- Network reset
- Provider `503`
- Short lock timeout

Use bounded retries.

### Permanent failure

Examples:

- Invalid schema
- Missing required field
- Unknown business entity
- Unsupported event version

Do not retry indefinitely. Dead-letter with:

```text
DeadLetterReason
DeadLetterErrorDescription
originalMessageId
correlationId
consumerVersion
```

### Poison message

A message that repeatedly fails because of its content should move to the DLQ rather than blocking the queue.

Azure Service Bus DLQs hold messages that could not be delivered or processed, and operators or repair processes can inspect and resubmit them. ([Microsoft Learn][6])

## Ordering

When order-level ordering is required, use a session or partitioning strategy based on:

```text
SessionId = orderId
```

This can serialize messages for the same order while allowing different orders to process concurrently.

## Interview-Ready Answer

> I accept the order through an API, store the order and an outbox event in one local transaction, and publish the event to Azure Service Bus. A queue is appropriate for one competing-consumer workflow, while a topic with subscriptions is appropriate when inventory, payment, notification, and analytics need independent copies. Every consumer is idempotent using a durable processed-message record. Transient failures use bounded retries, while poison or permanent failures move to the DLQ with diagnostic metadata for controlled replay.

---

# 3. Design an E-Commerce Checkout for Millions of Users

## High-Level Architecture

```text
Clients
    ↓
CDN / WAF
    ↓
API Gateway
    ↓
Checkout Service
    ├── Cart Service
    ├── Pricing Service
    ├── Inventory Service
    ├── Payment Service
    ├── Order Service
    └── Fraud Service
             ↓
       Event Backbone
             ↓
 Notification / Fulfilment / Analytics
```

## Core Design Principles

### Stateless compute

Application instances should not store authoritative session or checkout state only in process memory.

### Explicit service ownership

```text
Cart Service      → carts
Inventory Service → stock and reservations
Payment Service   → payment attempts
Order Service     → order lifecycle
```

### Durable workflow

Checkout crosses several systems and should be modeled as a Saga:

```text
Create pending order
→ reserve inventory
→ authorize payment
→ confirm order
```

Failure:

```text
Inventory reserved
→ payment rejected
→ release inventory
→ cancel order
```

## Checkout API

```http
POST /checkouts
Idempotency-Key: checkout-C100-cart-77
```

Response:

```json
{
  "checkoutId": "CHK-100",
  "status": "PROCESSING",
  "statusUrl": "/checkouts/CHK-100"
}
```

For fast successful cases, the API may complete synchronously. Under longer workflows, return an operation resource.

## Scale Strategy

Scale based on the actual pressure signal:

- Request rate
- Active checkouts
- Queue backlog
- Payment concurrency
- Inventory command backlog
- P95/P99 latency
- Thread or connection-pool saturation

Do not allow application autoscaling to create more database connections than the database can support.

## Availability

- Multiple instances across failure domains
- No single-node dependency
- Replicated database or managed HA datastore
- Durable messaging
- Readiness probes
- Graceful shutdown
- Timeouts, circuit breakers, and bulkheads
- Idempotent writes
- Tested rollback and disaster recovery

## Prevent Overselling

Do not implement:

```text
Read stock = 1
Two requests both see 1
Both decrement
```

Use one of:

- Atomic conditional update
- Optimistic version check
- Short pessimistic lock
- Reservation ledger
- Single-writer partition

Example:

```sql
UPDATE inventory
SET available = available - :quantity,
    version = version + 1
WHERE product_id = :productId
  AND available >= :quantity;
```

Success requires exactly one updated row.

## Graceful Degradation

During peak traffic:

- Disable recommendations
- Delay loyalty-point calculation
- Serve cached catalog data
- Queue non-critical notifications
- Preserve checkout, payment, and inventory correctness

## Interview-Ready Answer

> I separate checkout into domain-owned services and coordinate the workflow through a Saga rather than a distributed transaction. The API is protected by an idempotency key, inventory uses atomic reservations, and payment timeouts move to an unknown state followed by reconciliation. Services are stateless at the compute layer, distributed across failure domains, and scale on request or backlog signals. Non-critical capabilities degrade first so inventory, payment, and order correctness remain protected.

---

# 4. Third-Party Service Is Slow

## Required Controls

```text
Timeout
→ concurrency limit
→ circuit breaker
→ bounded retry
→ fallback
```

The order matters conceptually: a request must not wait indefinitely before the circuit breaker receives a result.

## Timeout Types

- DNS resolution timeout
- Connection timeout
- TLS handshake timeout
- Response timeout
- Read timeout
- Overall business deadline

Example:

```java
HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
        .responseTimeout(Duration.ofSeconds(2));

WebClient client = WebClient.builder()
        .clientConnector(
                new ReactorClientHttpConnector(httpClient)
        )
        .build();
```

## Retry Strategy

Recommended:

```text
attempt 1 → immediate
attempt 2 → 200 ms + jitter
attempt 3 → 600 ms + jitter
then stop
```

Use exponential backoff with random jitter so all instances do not retry simultaneously.

## Retry Budget

Suppose:

```text
Client deadline: 3 seconds
Per-attempt timeout: 1 second
Maximum attempts: 3
Backoff: 200 ms and 500 ms
```

The budget does not fit:

```text
3 × 1 second + backoff > client deadline
```

Timeout and retry configuration must be designed together.

## Avoid Retry Storms

- Limit retries at one architectural layer.
- Use jitter.
- Cap maximum attempts.
- Use a circuit breaker.
- Limit concurrent requests.
- Respect `Retry-After`.
- Stop retries when the caller’s deadline is exhausted.
- Do not retry all `5xx` indiscriminately.
- Do not let gateway, service, SDK, and mesh each retry three times.

Three retries at three layers can create:

```text
3 × 3 × 3 = 27 downstream attempts
```

## Fallback

Choose based on business semantics:

- Return cached reference data
- Return partial response
- Queue operation
- Return `202 Processing`
- Fail clearly
- Reconcile unknown outcomes

## Interview-Ready Answer

> I configure strict connection and response timeouts, isolate the provider with a concurrency bulkhead, and add a circuit breaker. I retry only transient and idempotent calls using bounded exponential backoff with jitter and an overall deadline. I ensure only one layer owns retry policy to prevent multiplicative retries. The fallback depends on business correctness: cached data may be acceptable for enrichment, while payment timeouts require an unknown state and reconciliation.

---

# 5. Process JSON Payloads Larger Than 10 MB

## Risks

A 10 MB payload can create much more than 10 MB of memory use because it may exist as:

```text
Network buffer
+
raw byte array
+
String representation
+
Jackson object graph
+
DTO copy
+
entity copy
+
log entry
```

Concurrent requests multiply the problem.

## Preferred Design

Question whether one large JSON request is the correct interface.

Alternatives:

- Upload the file to object storage
- Send a reference to the uploaded object
- Use NDJSON
- Split into batches
- Provide an asynchronous import API
- Stream records through messaging

## Streaming Parsing

Avoid loading the entire document into one object graph.

```java
try (JsonParser parser =
             objectMapper.getFactory().createParser(inputStream)) {

    if (parser.nextToken() != JsonToken.START_ARRAY) {
        throw new IllegalArgumentException(
                "Expected an array"
        );
    }

    List<Record> batch = new ArrayList<>(500);

    while (parser.nextToken() != JsonToken.END_ARRAY) {
        Record record =
                objectMapper.readValue(parser, Record.class);

        batch.add(record);

        if (batch.size() == 500) {
            processBatch(batch);
            batch.clear();
        }
    }

    if (!batch.isEmpty()) {
        processBatch(batch);
    }
}
```

## Required Limits

- Maximum request bytes
- Maximum number of objects
- Maximum nesting depth
- Maximum string length
- Decompression ratio
- Processing deadline
- Per-tenant concurrent imports
- Database batch size

## Asynchronous Import

```text
POST /imports
→ validate metadata
→ upload file
→ return 202

Worker
→ stream file
→ validate each record
→ process bounded batches
→ checkpoint progress
→ quarantine invalid rows
```

## Do Not Log Raw Payloads

Large payload logging increases:

- CPU
- Memory allocation
- Log costs
- PII exposure
- Latency

## Interview-Ready Answer

> I avoid materializing a 10 MB JSON document into one large Java object graph. I prefer object-storage upload plus an asynchronous import job, or stream the payload with Jackson and process bounded batches. I enforce limits on bytes, record count, depth, strings, decompression, concurrency, and processing time. The worker checkpoints progress and quarantines invalid records so one bad item does not require restarting the complete import.

---

# 6. Frequently Accessed Endpoint Hits the Database Every Time

## Decide What to Cache

Ask:

- Is the database the source of truth?
- How stale may the response be?
- How frequently does the data change?
- Is it user- or tenant-specific?
- Is it security-sensitive?
- What happens when the cache fails?

## Cache Placement

### In-memory L1 cache

Examples: Caffeine.

Advantages:

- Lowest latency
- No network call

Trade-offs:

- Per-instance copy
- Memory use
- Cross-instance invalidation

### Distributed L2 cache

Example: Redis.

Advantages:

- Shared across instances
- Central TTL
- Larger dataset

Trade-offs:

- Network dependency
- Serialization cost
- Redis capacity and availability

### Database-level optimization

Before adding a cache, verify:

- Correct indexes
- Query shape
- N+1 behavior
- Projection size
- Connection-pool waiting
- Query plan

Caching an inefficient query can hide rather than solve the problem.

## Cache-Aside Flow

```text
Read cache
    ├── hit → return
    └── miss
          ↓
       read DB
          ↓
       populate cache
          ↓
       return
```

## Invalidation Options

- TTL
- Explicit eviction after write
- Event-driven invalidation
- Versioned cache keys
- Refresh ahead
- Stale-while-revalidate

## Safe L1 + L2 Design

```text
Request
→ L1 Caffeine
→ L2 Redis
→ database
```

When the source changes:

```text
Commit DB transaction
→ publish versioned event through outbox
→ evict Redis
→ all instances evict L1
```

Use TTL as a safety net when an invalidation is missed.

## Stampede Protection

```text
Popular key expires
→ thousands of misses
→ DB overload
```

Use:

- Single-flight refresh
- Randomized TTL
- Proactive refresh
- Stale-while-revalidate
- Bounded database fallback

## Interview-Ready Answer

> I first optimize the query and confirm that the data is safe to cache. For instance-local hot data I may use Caffeine, while Redis is appropriate when instances need a shared cache. The database remains the source of truth. I use TTL plus versioned or event-driven invalidation, protect against cache stampedes through request coalescing and randomized expiry, and bound database fallback so a Redis outage does not become a database outage.

---

# 7. Service Memory Usage Keeps Increasing

## First Distinguish Memory Areas

```text
Container RSS
├── Java heap
├── Metaspace
├── Direct buffers
├── Thread stacks
├── Code cache
├── JNI/native libraries
└── Memory-mapped files
```

A stable heap with rising RSS suggests native or non-heap growth rather than a normal heap leak.

## First Metrics

- Heap used after GC
- Old-generation usage
- Allocation rate
- GC frequency and pauses
- Metaspace
- Direct-buffer memory
- Thread count
- Process RSS
- Cache and queue sizes

## Diagnostic Commands

```bash
jcmd <PID> GC.heap_info
jcmd <PID> GC.class_histogram
jcmd <PID> Thread.print -l
```

Heap dump:

```bash
jcmd <PID> GC.heap_dump /secure/path/application.hprof
```

Oracle recommends `jcmd` for heap histograms, heap dumps, thread dumps, and JFR operations on running JVMs. Heap dumps are high-impact operations and may contain sensitive information. ([Oracle Docs][7])

## Heap-Dump Analysis

Use Eclipse MAT to inspect:

- Dominator tree
- Retained heap
- Leak suspects
- Paths to GC roots
- Duplicate large objects

## Common Causes

- Unbounded cache
- Unbounded executor queue
- `ThreadLocal` not removed
- Static map
- Listener not deregistered
- Incomplete `CompletableFuture`
- Retained request bodies
- Classloader leak
- Direct `ByteBuffer`
- Excessive thread creation

Example:

```java
private static final ThreadLocal<RequestContext> CONTEXT =
        new ThreadLocal<>();

public void process(Request request) {
    try {
        CONTEXT.set(createContext(request));
        handle(request);
    } finally {
        CONTEXT.remove();
    }
}
```

## Native Memory Tracking

Must be enabled at JVM startup:

```text
-XX:NativeMemoryTracking=summary
```

Then:

```bash
jcmd <PID> VM.native_memory baseline
jcmd <PID> VM.native_memory summary.diff
```

## Validate the Fix

- Reproduce the workload.
- Run a long soak test.
- Confirm post-GC live set stabilizes.
- Compare histograms.
- Verify RSS, threads, queues, and cache entries stabilize.

## Interview-Ready Answer

> I first determine whether growth is in heap, Metaspace, direct memory, threads, or another native area. A rising post-GC live set suggests retained heap objects. I use GC logs, JFR, class histograms, a secured heap dump, and Eclipse MAT to identify the retaining path. If RSS grows while heap remains stable, I inspect direct buffers, thread count, and Native Memory Tracking. I validate the correction through a long production-like soak test.

---

# 8. Design an Email, SMS, and Push Notification System

## Architecture

```text
Business Services
    ↓ NotificationRequested event
Azure Service Bus topic
    ├── email subscription
    ├── sms subscription
    ├── push subscription
    └── audit subscription
          ↓
Channel Workers
          ↓
Email / SMS / Push providers
```

Topics are appropriate because each channel needs an independent copy of the notification event. ([Microsoft Learn][5])

## Notification Record

```text
notification_id
recipient_id
channel
template_id
status
attempt_count
provider_reference
scheduled_at
expires_at
created_at
```

## State Machine

```text
RECEIVED
→ VALIDATED
→ SENDING
    ├── SENT
    ├── RETRY_PENDING
    ├── FAILED_FINAL
    └── EXPIRED
```

## Template Rendering

Do not place arbitrary HTML or raw sensitive payloads directly in the event.

Use:

```json
{
  "notificationId": "N-100",
  "recipientId": "C-200",
  "templateId": "PAYMENT_SUCCESS",
  "templateVersion": 3,
  "parameters": {
    "paymentReference": "PAY-900",
    "amount": "250.00"
  }
}
```

## Reliability

- Durable message
- Idempotent channel workers
- Provider idempotency reference where supported
- Bounded retries
- Exponential backoff and jitter
- DLQ for permanent failure
- Delivery receipt processing
- Reconciliation
- Rate and cost limits

## Provider Failover

Do not automatically send through two SMS providers simultaneously.

Preferred:

```text
Provider A times out
→ outcome may be unknown
→ query status or wait for callback
→ fail over only when safe
```

Otherwise, the customer may receive duplicate SMS or email.

## Scalability

Scale each subscription independently:

```text
Email backlog high
→ scale email workers

SMS provider throttled
→ reduce SMS concurrency

Push traffic high
→ scale push workers
```

Azure Service Bus provides monitoring and performance guidance for throughput, client configuration, and broker utilization. ([Microsoft Learn][8])

## Interview-Ready Answer

> Business services publish a channel-neutral `NotificationRequested` event to an Azure Service Bus topic. Email, SMS, push, and audit subscriptions process their own copies and scale independently. Each notification has a durable ID and state machine, and workers are idempotent. Transient provider failures use bounded backoff, permanent failures move to a DLQ, and delivery receipts update status. Templates are versioned and sensitive data is minimized.

---

# 9. Duplicate Payment Requests After a Network Failure

## Idempotency Definition

Idempotency means repeating the same logical request produces the same business result rather than executing the side effect again.

## API Contract

```http
POST /payments
Idempotency-Key: order-O100-payment-1
```

## Required Record

```sql
CREATE TABLE payment_idempotency (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    request_fingerprint VARCHAR(128) NOT NULL,
    payment_id VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    http_status INTEGER,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

## Request Fingerprint

The same key must not be accepted for different requests.

```text
fingerprint =
SHA-256(
  merchantId
  + orderId
  + amount
  + currency
  + paymentMethodToken
)
```

## Algorithm

```text
BEGIN TRANSACTION

Try to insert idempotency key.

If key already exists:
    if fingerprint differs:
        return 409 Conflict

    if completed:
        return stored response

    if processing:
        return 202 Processing

Create payment in PROCESSING state.

COMMIT

Call provider using paymentId
as provider idempotency key.

Persist final or unknown result.
```

## State Machine

```text
RECEIVED
→ PROCESSING
    ├── SUCCEEDED
    ├── FAILED_FINAL
    └── OUTCOME_UNKNOWN
            ↓
        RECONCILING
```

## Why DB Constraints Matter

This is unsafe:

```text
Check whether key exists
→ no
→ two threads both insert payment
```

The database unique constraint provides the atomic winner.

## Interview-Ready Answer

> The client supplies an idempotency key for one logical payment. I store that key with a canonical request fingerprint, payment ID, state, and original response under a database uniqueness constraint. Repeated requests with the same data return the existing result; reuse with different data returns a conflict. Provider calls use the same internal payment ID as their idempotency reference. A timeout moves the operation to `OUTCOME_UNKNOWN`, followed by webhook processing or reconciliation.

---

# 10. Return Millions of Records Through an API

## First Question

Why must one API consumer receive millions of records?

For interactive UI:

- Pagination
- Filtering
- Search
- Projections

For complete export:

- Asynchronous export job
- Object storage
- Compressed file
- Status endpoint
- Download URL

Do not keep one HTTP request open while serializing millions of records.

## Offset Pagination

```sql
SELECT *
FROM transaction
ORDER BY created_at, id
LIMIT 100
OFFSET 1000000;
```

Problems:

- Database may scan or discard many earlier rows.
- Inserts and deletes can cause duplicates or skipped rows.
- Performance degrades at high offsets.

## Cursor/Keyset Pagination

```sql
SELECT *
FROM transaction
WHERE created_at < :cursorCreatedAt
   OR (
       created_at = :cursorCreatedAt
       AND id < :cursorId
   )
ORDER BY created_at DESC, id DESC
LIMIT :pageSize;
```

Advantages:

- Stable index-driven access
- Better deep-page performance
- More predictable under concurrent inserts

## Cursor Design

Encode:

```json
{
  "createdAt": "2026-07-24T08:00:00Z",
  "id": "TX-900",
  "sortVersion": 1
}
```

Sign or authenticate the cursor so clients cannot alter protected query state.

## Consistency Options

### Live pagination

New data may appear between requests.

Suitable for feeds.

### Snapshot boundary

First response includes:

```text
snapshotTime = 2026-07-24T08:00:00Z
```

Every subsequent page includes:

```sql
WHERE created_at <= :snapshotTime
```

### Database snapshot

Useful for controlled exports, but long-lived database snapshots may be expensive.

## Spring Data Example

Repository:

```java
@Query("""
    select t
    from Transaction t
    where
        t.createdAt < :createdAt
        or (
            t.createdAt = :createdAt
            and t.id < :id
        )
    order by t.createdAt desc, t.id desc
    """)
List<Transaction> findNextPage(
        Instant createdAt,
        UUID id,
        Pageable pageable
);
```

## Interview-Ready Answer

> I use cursor-based pagination for large and frequently changing datasets because it avoids expensive deep offsets and provides stable index-driven traversal. The cursor contains the last sort values, such as creation time and ID. For a consistent export, I also include a snapshot boundary. When a client genuinely needs all records, I create an asynchronous export job and place the result in object storage rather than streaming millions of rows through one request.

---

# 11. Messages Repeatedly Move to the Azure Service Bus DLQ

## What Is the DLQ?

A dead-letter queue is a secondary subqueue associated with a queue or subscription. It stores messages that could not be delivered or processed successfully. ([Microsoft Learn][5])

## Common Reasons

- Maximum delivery count exceeded
- Explicit dead-letter action
- Message expired with dead-letter-on-expiration enabled
- Subscription filter evaluation failure
- Invalid payload
- Unsupported schema version
- Missing business entity
- Consumer bug
- Processing duration exceeds lock handling

Azure documentation recommends examining dead-letter metadata such as the dead-letter reason when diagnosing message loss or repeated processing. ([Microsoft Learn][9])

## DLQ Processing Flow

```text
DLQ monitor detects messages
        ↓
Read without immediately deleting
        ↓
Inspect metadata and payload safely
        ↓
Classify root cause
        ├── consumer defect
        ├── invalid data
        ├── dependency failure
        └── obsolete message
        ↓
Correct issue
        ↓
Replay to controlled recovery queue
        ↓
Verify successful processing
        ↓
Complete original DLQ item
```

## Do Not Blindly Replay

Blind replay can:

- Repeat invalid messages
- Recreate the incident
- Trigger duplicate side effects
- Overload downstream services
- Destroy evidence

## Prevent DLQ Growth

- Schema validation before publishing
- Contract tests
- Idempotent consumers
- Correct lock handling
- Bounded retries
- Clear transient/permanent error classification
- Consumer compatibility
- Monitoring DLQ count and age
- Quarantine malformed payloads
- Operational replay tooling

## Metrics

- DLQ message count
- Oldest DLQ message age
- Dead-letter reasons
- Delivery count
- Replay success/failure
- Messages by producer and schema version

## Interview-Ready Answer

> I treat a DLQ as an operational quarantine, not a normal retry queue. I inspect the dead-letter reason, delivery count, schema version, correlation ID, consumer version, and dependency failures. After fixing the root cause, I replay messages through a controlled recovery path with idempotency protection and verify the resulting business state before removing the DLQ item.

---

# 12. API Takes Two to Five Minutes

## Recommended Model

Use asynchronous processing.

```text
Client
    ↓
POST /reports
    ↓
Validate and create job
    ↓
202 Accepted
    ↓
Queue
    ↓
Worker
    ↓
Result storage
```

Response:

```http
HTTP/1.1 202 Accepted
Location: /operations/OP-100
Retry-After: 5
```

```json
{
  "operationId": "OP-100",
  "status": "QUEUED",
  "statusUrl": "/operations/OP-100"
}
```

## Polling

```http
GET /operations/OP-100
```

```json
{
  "operationId": "OP-100",
  "status": "RUNNING",
  "progress": 65
}
```

Completed:

```json
{
  "operationId": "OP-100",
  "status": "COMPLETED",
  "resultUrl": "/exports/EXP-900"
}
```

## Callback/Webhook

A client can register a callback URL.

Required controls:

- HTTPS
- Signed callback
- Replay protection
- Callback idempotency
- Bounded retries
- Callback status tracking
- SSRF protection for callback destinations

## Cancellation

```http
DELETE /operations/OP-100
```

Cancellation should be best effort:

```text
CANCEL_REQUESTED
→ worker reaches safe checkpoint
→ CANCELLED
```

## Worker Requirements

- Durable job state
- Checkpoints
- Idempotency
- Retry classification
- Progress updates
- Timeout and expiry
- Dead-letter/manual review
- Graceful shutdown

## Interview-Ready Answer

> A two-to-five-minute operation should normally be modeled as a durable asynchronous job. The API validates the request, creates an operation resource, enqueues work, and returns `202 Accepted` with a status URL. Clients poll with backoff or receive a signed webhook or push notification. Workers checkpoint progress, handle retries idempotently, and persist the result outside process memory.

---

# 13. API Has High Latency

Use a layered investigation:

```text
Client
→ DNS/TLS
→ gateway/load balancer
→ application queueing
→ service logic
→ database
→ cache
→ downstream services
```

## Check First

- Request rate
- P50/P95/P99 latency
- Error and timeout rate
- Active requests
- Thread/executor queue depth
- DB connection acquisition time
- SQL execution time
- Cache hit rate
- HTTP connection-pool waiting
- Downstream span duration
- CPU throttling
- GC pauses

## Typical Fixes

| Cause                | Fix                                            |
| -------------------- | ---------------------------------------------- |
| Slow SQL             | Index/query redesign                           |
| DB pool waiting      | Shorten transactions and right-size pool       |
| HTTP pool waiting    | Reuse connections and right-size bounded pool  |
| Chatty calls         | Batch or aggregate                             |
| Slow dependency      | Timeout, bulkhead, circuit breaker             |
| Excess serialization | Smaller DTOs and streaming                     |
| Cache misses         | Correct caching and stampede control           |
| CPU saturation       | Profile hot path and scale when parallelizable |
| GC pressure          | Reduce allocation and right-size memory        |
| Queueing             | Bound concurrency and shed overload            |

## Interview-Ready Answer

> I use distributed traces and pool-wait metrics to find where time is spent rather than assuming the code itself is slow. I compare service time with queueing time, database acquisition with execution time, and downstream connection waiting with response time. I then fix the first saturated resource and repeat the same load test.

---

# 14. Handle Millions of Records Efficiently

Use:

- Keyset pagination
- Streaming
- Bounded batches
- Database projections
- Correct indexes
- Asynchronous exports
- Partitioning
- Parallelism aligned with database capacity
- Checkpoint/restart support

Avoid:

```java
List<Transaction> all =
        transactionRepository.findAll();
```

For batch jobs:

```text
Read 1,000 rows
→ process
→ commit/checkpoint
→ read next cursor
```

For exports:

```text
Create export job
→ stream DB rows
→ compress
→ upload to object storage
→ provide signed URL
```

---

# 15. Implement Pagination

Use offset pagination for:

- Small datasets
- Administrative screens
- Direct page-number navigation

Use cursor pagination for:

- Large datasets
- Infinite scrolling
- Frequently changing records
- High-throughput APIs

A stable cursor needs a deterministic unique order:

```text
ORDER BY created_at DESC, id DESC
```

Do not sort only by a non-unique timestamp.

---

# 16. Ensure Data Integrity

Use several layers:

## Application validation

- Required fields
- Range checks
- State-machine rules
- Authorization

## Database guarantees

- Primary keys
- Unique constraints
- Foreign keys within one service-owned database
- Check constraints
- Transactions
- Optimistic locking
- Atomic updates

## Messaging guarantees

- Transactional outbox
- Idempotent consumer
- Event IDs and aggregate versions
- Reconciliation
- DLQ handling

## Distributed workflows

- Saga
- Compensation
- Pending/unknown states
- Durable retries
- Manual review

## Financial integrity

Use an append-oriented ledger rather than overwriting only the current balance.

```text
Balance
=
sum of posted ledger entries
```

---

# 17. Optimize a Write-Heavy System

## Start with the Workload

Measure:

- Writes per second
- Record size
- Index count
- Transaction duration
- Lock wait
- WAL/log throughput
- Storage latency
- Hot keys
- Replication lag

## Techniques

- Keep transactions short.
- Remove unnecessary indexes.
- Batch independent writes.
- Partition by a suitable business key.
- Avoid global sequences when they become bottlenecks.
- Use append-oriented models where appropriate.
- Queue non-immediate work.
- Use optimistic or atomic updates.
- Shard only after simpler bottlenecks are understood.
- Separate analytical queries from the write path.
- Apply backpressure before the database fails.

More replicas of the API do not increase one database writer’s capacity automatically.

---

# 18. Mentor Junior Developers

## Approach

### Establish psychological safety

Make questions welcome and review mistakes without humiliation.

### Teach reasoning

Instead of only giving the fix, ask:

- What evidence supports the cause?
- What invariant must hold?
- What could fail after this change?
- How would we test it?

### Use progressive ownership

```text
Observe
→ pair
→ implement with review
→ own small feature
→ own production support
→ mentor another developer
```

### Give actionable reviews

Weak:

```text
This is bad.
```

Better:

```text
This method performs a remote call while holding a database
transaction. That can hold the connection and lock during a timeout.
Move the call outside the transaction and preserve correctness using
an explicit state transition.
```

### Protect standards

Mentoring does not mean lowering correctness, testing, security, or operational requirements.

## Interview-Ready Answer

> I mentor through context, pairing, small ownership steps, and specific feedback. I explain why a change is needed, ask the developer to reason about failure cases and invariants, and let them implement the correction. I gradually expand their ownership to design, testing, deployment, and incident analysis while maintaining the same engineering standards.

---

# 19. Design a Highly Concurrent System Handling Millions of Requests

## Architecture Principles

- Stateless compute
- Partitioned work
- Bounded queues
- Asynchronous processing
- Backpressure
- Caching
- Idempotency
- Per-dependency bulkheads
- Horizontal scaling
- Data ownership and sharding where justified

## Capacity Model

Use Little’s Law conceptually:

```text
Concurrency ≈ throughput × latency
```

At:

```text
100,000 requests/second
× 0.2 seconds
=
20,000 concurrent requests
```

Every layer must support the expected concurrency:

- Gateway
- Application
- HTTP clients
- Database pools
- Cache
- Broker partitions
- Third-party systems

## Control Overload

Do not allow unlimited queue growth.

Use:

- Rate limiting
- Concurrency limits
- Bounded executor queues
- Load shedding
- `429` or `503`
- Priority classes
- Backpressure

## Interview-Ready Answer

> I partition the workload, keep instances stateless, and bound every finite resource. The design uses asynchronous messaging for work that does not require an immediate response, idempotency for retries, caches for safe reads, and separate bulkheads for databases and external services. Capacity planning covers the entire dependency chain, and overload is rejected or degraded before queues and memory grow without limit.

---

# 20. Design Idempotent Consumers

## Failure Scenario

```text
Consumer receives message
→ updates DB
→ crashes before completing message
→ broker redelivers
```

Without idempotency, the side effect occurs twice.

## Durable Deduplication

```sql
CREATE TABLE processed_event (
    consumer_name VARCHAR(100) NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    PRIMARY KEY (consumer_name, event_id)
);
```

Transaction:

```text
BEGIN
  insert processed_event
  update business state
COMMIT
```

If the event ID already exists, treat the delivery as completed.

## Stronger Business Idempotency

Also use natural uniqueness:

```text
one payment per provider_reference
one inventory reservation per order_id
one refund per refund_request_id
```

Broker deduplication windows are not a substitute for durable business constraints.

---

# 21. Design a Resilient System

Use a layered model:

```text
Timeout
+
bounded retry
+
circuit breaker
+
bulkhead
+
backpressure
+
idempotency
+
durable state
+
reconciliation
```

## Key Principles

- Assume every network call can fail ambiguously.
- Assume messages may be duplicated.
- Assume instances will restart.
- Keep failure domains isolated.
- Degrade non-critical features first.
- Preserve correctness over cosmetic availability.
- Monitor technical and business outcomes.
- Test recovery rather than only normal execution.

---

# 22. Design for High Availability

High availability requires removal of single points of failure.

```text
Multi-instance compute
+
multiple failure zones
+
replicated data
+
health-based routing
+
automated recovery
+
capacity headroom
```

Also required:

- Backward-compatible deployment
- Graceful shutdown
- Durable messaging
- Data backup and restore
- Tested failover
- Defined RTO/RPO
- Operational runbooks

Availability means the system successfully performs its agreed function, not merely that a health endpoint returns `200`. AWS guidance recommends distributing production workloads across multiple Availability Zones or Regions according to the required fault tolerance. ([AWS Documentation][10])

---

# 23. Design Service Isolation

Isolation should exist at several layers.

| Layer      | Isolation technique                      |
| ---------- | ---------------------------------------- |
| Compute    | Separate deployments and resource limits |
| Threads    | Separate bounded executors               |
| Network    | Security groups or NetworkPolicies       |
| Identity   | Separate service roles and credentials   |
| Data       | Private schemas/databases and users      |
| Messaging  | Separate queues, topics, subscriptions   |
| Failure    | Bulkheads and circuit breakers           |
| Deployment | Independent release and rollback         |
| Rate       | Per-tenant and per-service quotas        |

Example:

```text
Reporting Service becomes slow
→ its thread pool fills
→ payment thread pool remains available
```

Avoid one shared executor, database user, queue, and connection pool for unrelated critical and non-critical workloads.

---

# Part II — Advanced Banking Scenarios

# Q91: Migrate from Java 11 to Java 21 Safely

## Risks

- Removed or changed JDK behavior
- Unsupported libraries
- Build plugin incompatibility
- Reflection and module-access issues
- Changed GC ergonomics
- Performance regression
- Changed TLS or security behavior
- Monitoring-agent incompatibility

JDK 21 was released as a long-term-support release line, but migration still requires validating the complete application ecosystem rather than only compiling source code. ([OpenJDK][11])

## Migration Plan

### 1. Inventory

Record:

- Framework version
- Third-party libraries
- Maven/Gradle plugins
- Agents
- JDBC drivers
- Native libraries
- JVM flags
- Container base image

### 2. Stabilize Java 11 baseline

Before changing the JDK:

- All critical tests pass.
- Performance baseline exists.
- GC and memory baseline exists.
- Known production behavior is documented.

### 3. Build and test on both JDKs

```text
Java 11 build/test
Java 21 build/test
```

### 4. Remove unsupported flags and dependencies

Run with:

- Startup warnings treated seriously
- Dependency analysis
- Integration tests
- Security tests

### 5. Performance validation

Use:

- JMH for isolated hot methods
- JFR for application profiling
- Load tests for throughput and latency
- Soak tests for memory
- GC-log comparison

JMH should not be used as evidence that the complete service has equivalent end-to-end performance.

### 6. Deploy progressively

```text
Internal environment
→ staging
→ 1% canary
→ 10%
→ 50%
→ 100%
```

Compare by runtime version:

- Error rate
- P99 latency
- CPU
- GC pauses
- Allocation
- Memory
- Connection-pool behavior
- Business transaction success

### 7. Rollback

Keep:

- Previous Java 11 image
- Backward-compatible schema
- Compatible events
- Reversible configuration

## Interview-Ready Answer

> I establish a Java 11 functional and performance baseline, inventory every library, plugin, agent, JVM flag, driver, and native dependency, and run CI on both Java versions. I use unit, integration, contract, load, soak, and security tests. JMH validates isolated code paths, while JFR and production-like load tests validate the service. I deploy Java 21 through a canary and keep the data, configuration, and event changes compatible with rollback.

---

# Q92: Core Banking Dependency Is Available Only During Business Hours

## Architecture

```text
Channel API
    ↓
Validate request
    ↓
Persist command
    ↓
Return PENDING
    ↓
Durable queue
    ↓
Processing window opens
    ↓
Core integration worker
```

## Request State

```text
RECEIVED
→ WAITING_FOR_CORE
→ PROCESSING
    ├── COMPLETED
    ├── REJECTED
    ├── EXPIRED
    └── MANUAL_REVIEW
```

## Important Rules

- Assign an idempotency key.
- Define execution deadline.
- Preserve the customer-visible exchange rate or terms where applicable.
- Revalidate conditions before execution.
- Do not accept requests that legally require immediate core confirmation.
- Notify the customer when processed.
- Reconcile ambiguous outcomes.

## Interview-Ready Answer

> I accept eligible requests into a durable command store and queue, return a transparent `PENDING` status, and process them when the core system is available. Every operation has an idempotency key, deadline, state machine, retry policy, and reconciliation path. Before execution, I revalidate rules that may have changed while waiting. Operations that legally require real-time core authorization fail clearly rather than pretending they were accepted.

---

# Q93: Freeze an Account Across All Channels

## Critical Correction

A distributed cache cannot be the sole enforcement point.

The authoritative debit decision must atomically check the account status.

## Safe Flow

```text
Freeze request
→ authorize maker/checker
→ update authoritative account state
→ commit FROZEN version
→ publish AccountFrozen event
→ invalidate caches
```

Every debit path performs:

```sql
UPDATE account
SET available_balance = available_balance - :amount,
    version = version + 1
WHERE account_id = :accountId
  AND status = 'ACTIVE'
  AND available_balance >= :amount;
```

If no row is updated, the debit must not proceed.

## Race Condition

```text
Debit starts at 10:00:00.100
Freeze commits at 10:00:00.110
```

The system needs a defined serialization rule based on:

- Database locking
- Optimistic version
- Ledger sequence
- Effective freeze timestamp
- Command ordering

## Cache Role

Cache may accelerate rejection:

```text
Cached FROZEN
→ reject quickly
```

But cached `ACTIVE` should not override the authoritative state.

## Interview-Ready Answer

> I store the freeze status in the authoritative account or ledger domain and make every debit authorization atomically conditional on `status = ACTIVE`. The freeze request is audited and may require maker-checker approval. After commit, a versioned `AccountFrozen` event invalidates gateways and local caches. Caches provide fast rejection but never authorize a debit on their own.

---

# Q94: Nationwide Festival Produces 15× Traffic

## Prepare Before the Event

- Forecast demand.
- Run a 15× or higher load test.
- Pre-scale compute.
- Reserve database and cache capacity.
- Confirm provider quotas.
- Increase queue capacity.
- Validate autoscaling maximums.
- Prepare operational dashboards and runbooks.

Autoscaling alone may react too slowly to a known event.

## During the Event

Priority tiers:

```text
Tier 1:
authentication, balance checks, transfers, payment status

Tier 2:
notifications, statements

Tier 3:
recommendations, rewards, analytics
```

Actions:

- Defer reward calculation.
- Queue notifications.
- Serve cached reference data.
- Rate-limit abusive traffic.
- Protect dependencies with bulkheads.
- Shed low-priority work.
- Monitor business success, not only CPU.

## Database Protection

```text
15× API Pods
× 20 connections
=
potentially 300× new DB connections
```

Cap:

- Local connection pools
- Global DB budget
- Query concurrency
- Batch size

## Interview-Ready Answer

> Because the event is predictable, I pre-scale rather than relying only on reactive HPA. I validate the full system at 15× load, including database, cache, queues, authentication, and third-party quotas. Critical transaction paths receive reserved capacity, while rewards, recommendations, analytics, and non-urgent notifications degrade or move asynchronously. Autoscaling remains bounded by downstream capacity.

---

# Q95: Add Fraud Checking Without Noticeable Latency

## First Determine Correctness Boundary

A fraud decision may need to occur before:

- Payment authorization
- Payment capture
- Settlement
- Withdrawal release

Running it asynchronously after an irreversible action may be unacceptable.

## Options

### Parallel synchronous validation

```text
Validate account ─┐
Check limits ─────┼→ combine
Fraud check ──────┘
```

Use only for independent checks.

```java
CompletableFuture<AccountResult> account =
        accountClient.validateAsync(command);

CompletableFuture<FraudResult> fraud =
        fraudClient.checkAsync(command);

return account.thenCombine(
        fraud,
        this::makeDecision
);
```

Bound the executor and apply deadlines.

### Pre-screen and full async review

```text
Fast risk rules
→ provisional authorization
→ full async fraud analysis
→ hold settlement
```

Only suitable when funds or settlement can safely remain held.

### Local fraud projection/model

Move frequently needed risk data closer to the payment service.

## Failure Policy

Do not use:

```text
Fraud service unavailable
→ assume low risk
```

Choose:

- Fail closed
- Manual review
- Pending
- Limited low-risk path

## Interview-Ready Answer

> I first identify when the fraud decision must become authoritative. If it is required before authorization, I execute it in parallel with independent validations under a shared deadline and bounded concurrency. If business rules permit delayed settlement, I can perform a fast synchronous screen and place the transaction on hold for asynchronous review. A fraud-service outage never silently becomes an approval.

---

# Q96: Regulatory Interest Rule Changes Overnight

## Safe Rollout

```text
Rule version 12
effective until 2026-07-31

Rule version 13
effective from 2026-08-01
```

Store:

- Rule version
- Effective date
- Jurisdiction/product
- Approval reference
- Input data version
- Calculation result
- Previous result where required

## Validation

- Regulatory examples
- Golden test dataset
- Boundary dates
- Leap-year/month-end cases
- Rounding rules
- Large account sample
- Historical replay
- Shadow comparison

## Shadow Mode

```text
Production result = old rule
Shadow result = new rule
Compare
→ classify expected and unexpected differences
```

Do not log full PII in difference reports.

## Deployment

- Feature/effective-date control
- Canary by account cohort
- Reconciliation totals
- Approval before activation
- Immutable audit evidence
- Rollback to old calculation for eligible periods

## Interview-Ready Answer

> I implement the change as a versioned, effective-dated rule rather than an untraceable code branch. The new rule is validated against regulatory examples and historical account data, then run in shadow mode beside the current calculation. I compare per-account results and aggregate totals, investigate discrepancies, and activate the new version through a controlled canary. Every calculation records its rule version for audit and recalculation.

---

# Q97: Customer Profile as Source of Truth During Outages

## Important Correction

Event Sourcing is one option, not a requirement.

A simpler design:

```text
Customer Profile Service
    ↓ authoritative database
Transactional outbox
    ↓
CustomerProfileUpdated events
    ↓
Dependent service projections
```

Dependent services store only the fields needed for their domain.

Example Order projection:

```text
customer_id
display_name
segment
status
profile_version
```

## Outage Behavior

During profile-service outage:

- Reads may use the local projection.
- Writes remain queued or fail clearly.
- Security-sensitive status has short staleness bounds.
- Projection version and age are observable.
- Recovery replays missed events.

## Interview-Ready Answer

> The Profile Service owns the authoritative profile database and publishes versioned updates through a transactional outbox. Other services maintain small local projections so read workflows continue during temporary outages. Event Sourcing may be selected when full event history is a core requirement, but CQRS-style read projections do not require Event Sourcing. Staleness limits are stricter for identity, account status, and authorization data.

---

# Q98: Conflicting Profile Updates from Mobile and Web

Use optimistic concurrency.

Client reads:

```http
ETag: "profile-v18"
```

Update:

```http
PATCH /profiles/C-100
If-Match: "profile-v18"
```

If current version is 19:

```http
HTTP/1.1 412 Precondition Failed
```

or an application-specific `409 Conflict`.

## Resolution Options

- Reject and ask client to refresh.
- Merge independent fields.
- Present conflict to user.
- Apply domain-specific precedence.

Do not use last-write-wins blindly for high-risk fields such as:

- Legal name
- Contact used for OTP
- Address used for compliance
- Account status

## Interview-Ready Answer

> Every profile has a version or ETag. The update includes the version read by the client, and the database update succeeds only when that version is still current. Independent field changes may be merged, but conflicting changes are rejected and shown to the user. The decision is domain-specific rather than a blanket last-write-wins rule.

---

# Q99: Credit Bureau Is Intermittently Unavailable

## Flow

```text
Loan application
→ local validation
→ bureau request
    ├── success → continue
    └── unavailable
          ↓
       PENDING_CREDIT_CHECK
          ↓
       durable retry queue
```

## Controls

- Timeout
- Bulkhead
- Circuit breaker
- Bounded retry
- Provider request ID
- Idempotency
- Expiry/SLA
- Manual review
- Customer notification

## Correct Fallback

Do not invent a credit score.

Possible customer response:

```json
{
  "applicationId": "L-100",
  "status": "PROCESSING",
  "message": "The external credit check is temporarily delayed."
}
```

## Interview-Ready Answer

> I isolate the bureau with timeouts, a bulkhead, and a circuit breaker. If the bureau is unavailable, the loan application moves to a durable `PENDING_CREDIT_CHECK` state and is retried with the same provider reference. The customer receives a transparent status and later notification. The fallback never assumes creditworthiness or bypasses a mandatory check.

---

# Q100: Priorities for a Major Bank’s Fund-Transfer Platform

Without access to the named bank’s internal architecture, this should be answered as a **hypothetical regulated fund-transfer platform**, not as a statement about its current systems.

## First 90 Days — Establish Risk and Evidence

- Map critical payment flows.
- Identify sources of truth.
- Review duplicate-payment and reconciliation controls.
- Define SLOs.
- Review privileged access.
- Test backup and recovery.
- Map service, database, and message dependencies.
- Establish architecture and risk register.

## Months 3–6 — Correctness and Security

- Durable idempotency at every payment boundary
- Transactional outbox
- Automated reconciliation
- mTLS/workload identity
- Least-privilege IAM
- JIT privileged access
- Secret rotation
- Immutable audit retention
- PII-safe logging

## Months 6–9 — Resilience and Scale

- Bulkheads and dependency budgets
- Capacity/load testing
- Peak-event pre-scaling
- Multi-zone validation
- Chaos and failover exercises
- Queue-based buffering
- Recovery from ambiguous payment states

## Months 9–12 — Delivery and Operational Maturity

- Canary deployments
- Backward-compatible migrations
- Automated rollback
- Contract testing
- Trace-based incident analysis
- SLO burn-rate alerts
- Reduced change lead time
- Game days and failback exercises

## Interview-Ready Answer

> My first priority would be payment correctness: idempotency, durable state machines, transactional outbox, and automated reconciliation. Next would be zero-trust workload identity, least privilege, JIT production access, secret rotation, and tamper-evident audit records. I would then improve dependency isolation, peak-capacity planning, failover testing, observability, and canary delivery. The roadmap would be measured through duplicate-payment rate, unreconciled items, availability, recovery time, deployment failure rate, and lead time.

---

# Part III — AWS Cloud Scenarios for Java Engineers

# AWS Q1: Lambda, ECS on Fargate, or EKS for a Long-Running Spring Boot API?

## ECS on Fargate

Strong default when:

- It is a continuously running HTTP service.
- The team wants containers without managing EC2 hosts.
- Normal JVM connection pools and background processes are required.
- Kubernetes APIs are not a requirement.

AWS positions Fargate as suitable for long-running containerized applications and microservices, while Lambda is oriented toward event-driven execution. ([AWS Documentation][12])

## Lambda

Suitable when:

- Work is event-driven.
- Execution is naturally bounded.
- Scale-to-zero is valuable.
- Stateless invocation fits the design.

Trade-offs for conventional Spring APIs include:

- Cold starts
- Connection management
- Execution-model constraints
- Per-invocation architecture

## EKS

Choose when the organization needs:

- Kubernetes portability
- Existing Kubernetes platform
- Operators or CRDs
- Service-mesh ecosystem
- Kubernetes-native operational standards

AWS supports Fargate with both ECS and EKS, but EKS introduces Kubernetes control-plane and operational concerns that should be justified. ([AWS Documentation][13])

## Interview Answer

> For a conventional always-on Spring Boot payment API, ECS on Fargate is usually my default because it preserves the standard container and JVM model without requiring node management. I use Lambda for naturally event-driven, bounded work. I use EKS where Kubernetes capabilities and organizational maturity are genuine requirements.

---

# AWS Q2: Highly Available Payment API Across Availability Zones

```text
Route 53 / CloudFront / WAF
            ↓
Multi-AZ Application Load Balancer
            ↓
ECS tasks in private subnets across AZs
            ↓
Multi-AZ database
```

Requirements:

- At least two AZs
- Multiple healthy ECS tasks
- Stateless compute
- Multi-AZ data layer
- Cross-AZ capacity
- Health checks
- Graceful shutdown
- No AZ-specific secret or dependency
- Tested zonal failure

AWS Well-Architected guidance recommends distributing production workloads across multiple AZs or Regions rather than depending on one AZ. ([AWS Documentation][10])

---

# AWS Q3: Idempotent Payment-Creation API

Use:

```text
Idempotency-Key
+
request fingerprint
+
DB unique constraint
+
stored result
+
provider idempotency key
```

Duplicate requests return the original result rather than a second payment.

The idempotency record and payment creation must be committed atomically.

---

# AWS Q4: SQS Standard vs FIFO

## Standard Queue

Use for:

- High-throughput work queues
- Independent messages
- Idempotent consumers
- No strict order requirement

Standard queues provide at-least-once delivery and may occasionally deliver duplicates or messages out of order. ([AWS Documentation][14])

## FIFO Queue

Use when:

- Ordering is required within a business key
- Send-side duplicate suppression is useful
- Throughput model fits FIFO

Set:

```text
MessageGroupId = paymentId or accountId
MessageDeduplicationId = eventId
```

FIFO deduplication uses a deduplication identifier and maintains ordered processing within message groups. ([AWS Documentation][15])

## Important Nuance

Consumers should still be idempotent because:

- A side effect may succeed before message deletion.
- Visibility timeout can expire.
- Operator replay can occur.
- Downstream APIs can be retried.

---

# AWS Q5: Update DB and Publish `PaymentAuthorized`

Use a transactional outbox:

```text
BEGIN
  update payment
  insert outbox event
COMMIT
```

Publisher:

```text
Read unpublished outbox
→ send to SQS/SNS/EventBridge/MSK
→ record publication
```

The relay may publish more than once, so consumers deduplicate by `eventId`.

EventBridge is designed to connect application components through event-driven architecture, but it does not remove the local database/broker dual-write problem. ([AWS Documentation][16])

---

# AWS Q6: Aurora PostgreSQL or DynamoDB?

## Aurora PostgreSQL

Prefer for:

- Relational invariants
- Multi-row transactions
- Ledger relationships
- Complex reconciliation
- SQL reporting
- Strong schema constraints

## DynamoDB

Prefer for:

- Known key-based access patterns
- Idempotency records
- High-scale status lookup
- Conditional updates
- Deduplication
- Simple aggregate storage

DynamoDB design begins with access patterns and key design rather than normalizing a relational model afterward. ([AWS Documentation][17])

A hybrid design may use Aurora for the core ledger and DynamoDB for high-scale idempotency or status views.

---

# AWS Q7: RDS Proxy and HikariCP

RDS Proxy:

- Pools database connections
- Reuses backend connections
- Helps absorb connection storms
- Can make failover more transparent
- Limits database connection pressure

AWS documents RDS Proxy connection pooling, connection reuse, surge handling, and failover routing. ([AWS Documentation][18])

HikariCP remains useful between application threads and the proxy:

```text
Application threads
→ bounded Hikari pool
→ RDS Proxy
→ bounded DB connections
```

Do not configure:

```text
100 ECS tasks
× 100 Hikari connections
```

without calculating total backend capacity. RDS Proxy can multiplex container-side connections, but transaction/session behavior may pin connections and reduce reuse. ([AWS Documentation][19])

---

# AWS Q8: Multi-Step Payment Workflow with Step Functions

Use a Standard Workflow when the process needs:

- Durable history
- Long-running execution
- Explicit retries and catches
- Human/manual steps
- Compensation
- Auditability

```text
Authorize
→ Fraud Check
→ Ledger Post
→ Notify
```

Failure:

```text
Ledger post fails
→ reverse authorization
→ mark compensation pending
```

AWS Step Functions Standard Workflows provide durable execution history and an exactly-once workflow execution model unless retries are explicitly configured; Express Workflows use an at-least-once model. External side-effecting tasks should still be idempotent because retries and ambiguous remote outcomes remain possible. ([AWS Documentation][20])

---

# AWS Q9: Retry AWS or Payment-Processor Calls

Use:

- Connect and response timeout
- Transient-error classification
- Maximum attempts
- Exponential backoff
- Jitter
- Overall deadline
- Idempotency key
- Circuit breaker
- Concurrency limit

Do not retry:

- Invalid payment data
- Insufficient funds
- Authorization rejection
- Unsupported currency
- Confirmed duplicate operation

A timeout from the processor becomes `OUTCOME_UNKNOWN`, not automatically `FAILED`.

---

# AWS Q10: Protect Card Data, Credentials, and Keys

## Card Data

- Tokenize where possible.
- Minimize storage and processing scope.
- Mask display values.
- Never log full card data.

## Secrets

Use Secrets Manager or another dedicated secret store.

AWS Secrets Manager encrypts stored secrets through AWS KMS and supports rotation-related capabilities. ([AWS Documentation][21])

## Network

Use private subnets and interface VPC endpoints where appropriate. Secrets Manager and KMS support private access through AWS PrivateLink endpoints without routing through the public internet. ([AWS Documentation][22])

## Identity

Use least-privilege ECS task roles rather than embedding static access keys.

---

# AWS Q11: ECS Task Role vs Task Execution Role

## Task role

Used by the application container:

```text
Java application
→ SQS
→ Secrets Manager
→ S3
```

The permissions are delivered to the running task. ([AWS Documentation][23])

## Task execution role

Used by ECS/Fargate platform operations:

- Pull image from ECR
- Publish container logs
- Retrieve selected deployment-time secrets
- Perform agent-level actions

([AWS Documentation][24])

Keep the two roles separate and least privileged.

---

# AWS Q12: Protect a Public Payment API

Use layered controls:

```text
CloudFront
→ AWS WAF
→ API Gateway or ALB
→ authentication
→ application rate limit
→ business authorization
```

Controls:

- TLS
- OAuth/OIDC or signed partner authentication
- WAF managed rules
- WAF rate-based rules
- Request-size limits
- Schema validation
- Per-merchant quotas
- Idempotency
- Backend concurrency limits
- Fraud and velocity checks

AWS WAF can inspect HTTP requests and apply rate-based rules using aggregation keys such as IP addresses or headers. AWS recommends testing rules in count mode before enforcing them in production. ([AWS Documentation][25])

WAF rate limiting does not replace per-customer or payment-specific business limits.

---

# AWS Q13: Monitor a Java Payment Platform

## Business

- Payment authorization success
- Duplicate-attempt detection
- Unknown-outcome count
- Reconciliation mismatch
- Settlement delay
- Refund failure

## Application

- Request rate
- Error rate
- P95/P99 latency
- Queue depth
- Thread-pool saturation
- DB connection wait
- Dependency timeout

## JVM

- Heap after GC
- Allocation rate
- GC pauses
- Thread count
- Direct memory
- Process CPU

## AWS

- ECS desired/running/pending tasks
- ALB target response time and errors
- RDS load and connections
- SQS age and depth
- WAF blocks
- Secret/KMS failures
- Deployment events

Use correlation identifiers, but exclude PII, tokens, and complete payment payloads.

---

# AWS Q14: Autoscale an ECS Payment API

ECS Service Auto Scaling supports target tracking, step scaling, scheduled scaling, and predictive approaches through Application Auto Scaling. ([AWS Documentation][26])

Possible signals:

- CPU
- Memory
- ALB request count per target
- Active requests
- Queue backlog per task
- P95 latency
- Thread saturation

For predictable events:

```text
Scheduled pre-scale
+
target tracking for variation
```

Maintain:

- Safe minimum capacity
- Maximum bounded by DB/provider capacity
- Scale-in stabilization
- Graceful task draining

---

# AWS Q15: Deploy Without Downtime or Unsafe Mixed Versions

Use:

- Immutable image digest
- Health checks
- Multiple tasks
- Minimum healthy percentage
- Maximum deployment percentage
- Graceful shutdown
- Compatible database changes
- Compatible events
- Automated smoke tests
- Canary or blue-green where required

ECS deployment circuit breakers can detect failure to reach steady state and automatically roll back to the last completed deployment. CloudWatch alarms can also participate in deployment failure detection. ([AWS Documentation][27])

Application rollback is safe only when the previous version can still read the data and events produced by the new version.

---

# AWS Q16: Disaster Recovery, RTO, and RPO

## RTO

Maximum acceptable time to restore service.

## RPO

Maximum acceptable data-loss window.

## Strategies

| Strategy            |        Cost | Typical recovery      |
| ------------------- | ----------: | --------------------- |
| Backup and restore  |         Low | Slow                  |
| Pilot light         |  Medium-low | Moderate              |
| Warm standby        | Medium-high | Faster                |
| Multi-region active |        High | Fastest, most complex |

Design also needs:

- Payment reconciliation
- Duplicate prevention during failover
- Write fencing
- Data replication monitoring
- Failback plan
- Regular game days

AWS reliability guidance emphasizes routing to healthy resources, rehearsing failover and failback, and confirming quotas and capacity before exercises. ([AWS Documentation][28])

---

# AWS Q17: VPC Design for ECS Payment API

```text
Internet
    ↓
Public ALB/API endpoint
    ↓
Private ECS tasks
    ↓
Private database
```

Use:

- Public subnets only for public-facing ingress components
- Private subnets for ECS tasks and databases
- Security-group references
- Restricted egress
- VPC endpoints for selected AWS services
- Separate route tables
- No direct public database access
- Flow logs and network monitoring

ECS, Secrets Manager, and KMS support interface endpoints through AWS PrivateLink for private API access. ([AWS Documentation][29])

---

# AWS Q18: Size the JVM in an ECS Fargate Task

Container memory includes:

```text
Heap
+
Metaspace
+
direct buffers
+
thread stacks
+
code cache
+
GC/JVM structures
+
native libraries
```

Unsafe:

```text
Task memory = 2 GiB
-Xmx2g
```

Safer conceptual budget:

```text
Task memory: 2 GiB
Heap:        1.2–1.4 GiB
Native/non-heap and margin: remainder
```

Exact values come from load and soak tests.

Monitor:

- Container memory
- Process RSS
- Heap after GC
- Direct memory
- Threads
- OOM termination
- CPU throttling
- P99 latency

Keep thread pools and connection pools consistent with allocated CPU and downstream capacity.

---

# AWS Q19: SQS, Kinesis Data Streams, or MSK

## SQS

Use for:

- Work queues
- Competing consumers
- Task distribution
- Simple retry and DLQ patterns

## Kinesis Data Streams

Use for:

- Managed partitioned data streams
- Ordered records per partition key
- Multiple stream-processing consumers
- Real-time analytics and ingestion

AWS describes Kinesis Data Streams as a managed service for collecting and processing large real-time data streams. ([AWS Documentation][30])

## Amazon MSK

Use when:

- Kafka APIs and semantics are required
- Existing Kafka tooling is important
- Consumer groups, topics, partitions, and Kafka ecosystem are needed
- Workloads must integrate with existing Kafka systems

Amazon MSK is a managed service running open-source Apache Kafka data-plane APIs. ([AWS Documentation][31])

## Interview Answer

> I use SQS for a work queue, Kinesis for AWS-managed partitioned streaming and real-time processing, and MSK where Kafka compatibility and ecosystem capabilities are requirements. The choice depends on replay, consumer model, ordering, operational model, and existing platform standards.

---

# AWS Q20: Auditability and Reconciliation

## Audit Event

Record:

- Event ID
- Payment ID
- Actor/workload
- Previous state
- New state
- Timestamp
- Provider reference
- Rule or reason
- Trace/request ID
- Service version

## Generation

```text
Payment state update
+
audit/outbox record
=
one local transaction
```

## Storage

- Searchable operational store
- Long-term protected archive
- Separate access boundary
- Retention policy
- Integrity verification

S3 Object Lock supports WORM-style retention. In compliance mode, protected object versions cannot be overwritten or deleted during the retention period, including by the account root user. ([AWS Documentation][32])

## Reconciliation

Compare:

```text
Internal payment records
vs
processor authorizations
vs
settlement files
vs
ledger postings
vs
refund records
```

Classify:

- Missing internal record
- Missing provider record
- Amount mismatch
- Duplicate charge
- Status mismatch
- Stale pending payment

Reconciliation should create durable cases, alerts, ownership, and resolution evidence.

---

# Quick Interview Cheat Sheet

| Scenario              | Core answer                                              |
| --------------------- | -------------------------------------------------------- |
| Dependency failure    | Timeout, bulkhead, breaker, safe fallback                |
| Azure async orders    | Outbox, queue/topic, idempotent consumers                |
| Checkout at scale     | Saga, reservation, idempotency, graceful degradation     |
| Slow provider         | Deadline, bounded backoff, jitter, circuit breaker       |
| Large JSON            | Stream, batch, object storage, async job                 |
| Caching               | Source of truth, TTL, events, stampede protection        |
| Memory leak           | Heap vs native, `jcmd`, JFR, MAT                         |
| Notification system   | Topic subscriptions, channel workers, durable state      |
| Duplicate payment     | Key, fingerprint, unique constraint, stored response     |
| Millions of records   | Cursor pagination or async export                        |
| DLQ                   | Inspect, classify, repair, controlled replay             |
| Long API operation    | `202 Accepted`, operation resource, queue                |
| Data integrity        | DB constraints, outbox, idempotency, reconciliation      |
| Write-heavy           | Short transactions, batching, partitioning, backpressure |
| Freeze account        | Authoritative atomic status check                        |
| Peak festival traffic | Pre-scale and reserve critical capacity                  |
| Java migration        | Dual-runtime testing and canary                          |
| AWS long-running API  | ECS on Fargate as strong default                         |
| SQS FIFO              | Per-group order plus application idempotency             |
| RDS Proxy             | Shared pooling and failover support                      |
| Step Functions        | Durable Saga orchestration                               |
| AWS security          | Task roles, Secrets Manager, KMS, private endpoints      |
| AWS deployment        | Immutable artifact and circuit-breaker rollback          |
| AWS audit             | Transactional audit event, WORM archive, reconciliation  |

# Senior Interview Summary

> I design every remote interaction with a deadline, isolation boundary, and explicit failure policy. Circuit breakers prevent repeated calls to unhealthy dependencies, but timeouts, bulkheads, backpressure, and idempotency provide the broader resilience model.
>
> Asynchronous workflows use durable messaging, transactional outbox, versioned contracts, and idempotent consumers. Azure Service Bus queues support competing consumers, while topics and subscriptions support independent downstream processing. Poison messages move to a DLQ for controlled diagnosis and replay.
>
> Financial operations use explicit state machines. Payment retries are protected by durable idempotency records and provider references, while ambiguous timeouts move to reconciliation rather than being declared failed. Account freezes and balance-changing operations are enforced atomically at the authoritative data source, not only through cache state.
>
> On AWS, ECS on Fargate is a strong default for long-running containerized Java APIs, while Lambda and EKS are selected for their specific execution or platform advantages. Multi-AZ deployment, bounded autoscaling, RDS connection protection, least-privilege task roles, private service access, immutable deployment artifacts, and tested recovery provide the operational foundation.
>
> Performance and scale are handled through bounded resources, partitioning, streaming, cursor pagination, caching with explicit consistency, and graceful degradation. High availability is proven through load tests, failure exercises, reconciliation, and failback—not inferred from replica count alone.

[1]: https://learn.microsoft.com/en-us/azure/service-bus-messaging/duplicate-detection "https://learn.microsoft.com/en-us/azure/service-bus-messaging/duplicate-detection"
[2]: https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-queues-topics-subscriptions "https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-queues-topics-subscriptions"
[3]: https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-builder.html "https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-builder.html"
[4]: https://docs.spring.io/spring-cloud-circuitbreaker/docs/current/reference/html/ "https://docs.spring.io/spring-cloud-circuitbreaker/docs/current/reference/html/"
[5]: https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-messaging-overview "https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-messaging-overview"
[6]: https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-dead-letter-queues "https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-dead-letter-queues"
[7]: https://docs.oracle.com/en/java/javase/23/docs/specs/man/jcmd.html "https://docs.oracle.com/en/java/javase/23/docs/specs/man/jcmd.html"
[8]: https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-performance-improvements "https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-performance-improvements"
[9]: https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-message-loss-and-duplicates "https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-message-loss-and-duplicates"
[10]: https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/rel_fault_isolation_multiaz_region_system.html "https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/rel_fault_isolation_multiaz_region_system.html"
[11]: https://openjdk.org/projects/jdk/21/ "https://openjdk.org/projects/jdk/21/"
[12]: https://docs.aws.amazon.com/decision-guides/latest/fargate-or-lambda/fargate-or-lambda.html "https://docs.aws.amazon.com/decision-guides/latest/fargate-or-lambda/fargate-or-lambda.html"
[13]: https://docs.aws.amazon.com/decision-guides/latest/containers-on-aws-how-to-choose/choosing-aws-container-service.html "https://docs.aws.amazon.com/decision-guides/latest/containers-on-aws-how-to-choose/choosing-aws-container-service.html"
[14]: https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/standard-queues-at-least-once-delivery.html "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/standard-queues-at-least-once-delivery.html"
[15]: https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues-exactly-once-processing.html "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues-exactly-once-processing.html"
[16]: https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-what-is.html "https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-what-is.html"
[17]: https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/data-modeling-foundations.html "https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/data-modeling-foundations.html"
[18]: https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/rds-proxy.howitworks.html "https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/rds-proxy.howitworks.html"
[19]: https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/rds-proxy-best-practices.usage-scenarios.html "https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/rds-proxy-best-practices.usage-scenarios.html"
[20]: https://docs.aws.amazon.com/step-functions/latest/dg/choosing-workflow-type.html "https://docs.aws.amazon.com/step-functions/latest/dg/choosing-workflow-type.html"
[21]: https://docs.aws.amazon.com/secretsmanager/latest/userguide/best-practices.html "https://docs.aws.amazon.com/secretsmanager/latest/userguide/best-practices.html"
[22]: https://docs.aws.amazon.com/secretsmanager/latest/userguide/vpc-endpoint-overview.html "https://docs.aws.amazon.com/secretsmanager/latest/userguide/vpc-endpoint-overview.html"
[23]: https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html "https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html"
[24]: https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_execution_IAM_role.html "https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_execution_IAM_role.html"
[25]: https://docs.aws.amazon.com/waf/latest/developerguide/waf-rule-statement-type-rate-based.html "https://docs.aws.amazon.com/waf/latest/developerguide/waf-rule-statement-type-rate-based.html"
[26]: https://docs.aws.amazon.com/autoscaling/application/userguide/application-auto-scaling-target-tracking.html "https://docs.aws.amazon.com/autoscaling/application/userguide/application-auto-scaling-target-tracking.html"
[27]: https://docs.aws.amazon.com/AmazonECS/latest/developerguide/deployment-circuit-breaker.html "https://docs.aws.amazon.com/AmazonECS/latest/developerguide/deployment-circuit-breaker.html"
[28]: https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/rel_withstand_component_failures_failover2good.html "https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/rel_withstand_component_failures_failover2good.html"
[29]: https://docs.aws.amazon.com/AmazonECS/latest/developerguide/vpc-endpoints.html "https://docs.aws.amazon.com/AmazonECS/latest/developerguide/vpc-endpoints.html"
[30]: https://docs.aws.amazon.com/streams/latest/dev/introduction.html "https://docs.aws.amazon.com/streams/latest/dev/introduction.html"
[31]: https://docs.aws.amazon.com/msk/latest/developerguide/what-is-msk.html "https://docs.aws.amazon.com/msk/latest/developerguide/what-is-msk.html"
[32]: https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock.html "https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock.html"
