# Scenario-Based Questions — Notification Systems, Kafka, Messaging, and Production

The repeated questions are consolidated below into an interview-ready guide. Several claims from the supplied notes are also corrected, especially around payment processing, Kafka exactly-once semantics, static membership, and database consistency.

---

# 1. Design a Notification System

## Requirements

The system should support:

- Email
- SMS
- Mobile push notifications
- Multiple providers per channel
- Templates and localization
- User preferences
- Scheduling
- Retry and failure handling
- Delivery-status tracking
- High throughput
- Easy addition of new channels

## High-level architecture

```text
Business Services
        ↓
Notification API or Events
        ↓
Notification Orchestrator
        ↓
Message Broker
   ┌────┼─────────┐
   ↓    ↓         ↓
Email  SMS       Push
Worker Worker    Worker
   ↓    ↓         ↓
Email  SMS       Push
Provider Provider Provider
```

## Core components

### Notification API

Accepts a request such as:

```json
{
  "notificationId": "N-100",
  "recipientId": "U-101",
  "channel": "EMAIL",
  "templateCode": "ORDER_CONFIRMED",
  "parameters": {
    "orderId": "O-500"
  }
}
```

For asynchronous delivery, return:

```http
HTTP/1.1 202 Accepted
```

```json
{
  "notificationId": "N-100",
  "status": "QUEUED"
}
```

### Notification record

Persist the notification before delivery:

```text
QUEUED
PROCESSING
SENT
DELIVERED
FAILED_RETRYABLE
FAILED_PERMANENT
CANCELLED
```

### Channel strategy

```java
public interface NotificationSender {

    NotificationChannel supportedChannel();

    DeliveryResult send(NotificationMessage message);
}
```

```java
@Component
public class EmailNotificationSender implements NotificationSender {

    private final EmailProvider emailProvider;

    public EmailNotificationSender(EmailProvider emailProvider) {
        this.emailProvider = emailProvider;
    }

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public DeliveryResult send(NotificationMessage message) {
        return emailProvider.send(message);
    }
}
```

```java
@Component
public class SmsNotificationSender implements NotificationSender {

    private final SmsProvider smsProvider;

    public SmsNotificationSender(SmsProvider smsProvider) {
        this.smsProvider = smsProvider;
    }

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public DeliveryResult send(NotificationMessage message) {
        return smsProvider.send(message);
    }
}
```

Registry:

```java
@Component
public class NotificationSenderRegistry {

    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationSenderRegistry(List<NotificationSender> senderList) {
        this.senders = senderList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        NotificationSender::supportedChannel,
                        Function.identity()
                ));
    }

    public NotificationSender get(NotificationChannel channel) {
        NotificationSender sender = senders.get(channel);

        if (sender == null) {
            throw new UnsupportedNotificationChannelException(channel);
        }

        return sender;
    }
}
```

This combines:

- Strategy
- Factory/Registry
- Adapter
- Dependency Injection

## Provider abstraction

Do not expose provider-specific SDK types to the business layer.

```java
public interface EmailProvider {

    ProviderDeliveryResult send(NotificationMessage message);
}
```

Possible adapters:

```text
EmailProvider
├── Amazon SES adapter
├── SendGrid adapter
└── SMTP adapter
```

## Retry policy

Retry only temporary failures:

- Provider timeout
- Temporary provider unavailability
- HTTP `429`
- HTTP `5xx`

Do not retry permanently invalid requests:

- Invalid email address
- Unsubscribed recipient
- Unsupported phone number
- Missing template
- Rejected payload

Use:

```text
Attempt 1
→ 1-second delay

Attempt 2
→ 2-second delay

Attempt 3
→ 4-second delay plus jitter

Attempts exhausted
→ dead-letter topic or manual review
```

## Important production concerns

- Idempotency using `notificationId`
- User channel preferences
- Opt-out and consent
- Provider rate limits
- Template versioning
- Localization
- Sensitive-data redaction
- Delivery callbacks
- Provider fallback
- Per-tenant quotas
- Duplicate-provider callbacks
- Audit trail

## Interview answer

> I would expose a channel-independent notification contract, persist notifications before delivery, and publish delivery work to a broker. Email, SMS, and push workers implement a common Strategy interface, while provider-specific integrations are hidden behind adapters. Delivery is idempotent, retries are bounded, invalid messages go to a monitored dead-letter path, and notification status is persisted for tracking and reconciliation.

---

# 2. When Should You Use REST, Kafka, or RabbitMQ?

## Synchronous REST or gRPC

Use synchronous communication when the caller requires an immediate answer.

Examples:

- Validate available inventory
- Retrieve account details
- Authorize an interactive request
- Obtain a price quotation

```text
Service A
   ↓ request
Service B
   ↓ response
Service A continues
```

### Advantages

- Simple request-response flow
- Immediate result
- Easy status-code semantics
- Easier interactive debugging

### Disadvantages

- Temporal coupling
- Latency accumulation
- Cascading failures
- Connections remain occupied
- Caller and dependency must be available together

---

## Kafka

Kafka is appropriate when the system needs:

- Retained event streams
- Event replay
- High-throughput pipelines
- Multiple independent consumer groups
- CDC
- Stream processing
- Event-driven projections

```text
Producer
   ↓
Kafka retained log
   ├── Accounting consumer group
   ├── Analytics consumer group
   └── Notification consumer group
```

---

## RabbitMQ

RabbitMQ is appropriate for:

- Work queues
- Command messaging
- Flexible routing
- Request-reply
- Priority or delayed processing
- Per-message acknowledgement workflows

```text
Publisher
   ↓
Exchange
   ↓ routing rules
Queues
   ↓
Workers
```

## Decision guide

| Requirement                        | Common choice     |
| ---------------------------------- | ----------------- |
| Immediate response required        | REST or gRPC      |
| Retained event history and replay  | Kafka             |
| Independent event consumers        | Kafka             |
| Work distribution to workers       | RabbitMQ          |
| Flexible exchange routing          | RabbitMQ          |
| High-volume event stream           | Kafka             |
| Simple internal query              | REST/gRPC         |
| Long-running asynchronous workflow | Kafka or RabbitMQ |

Neither Kafka nor RabbitMQ is universally better.

---

# 3. How Would You Design Event-Driven Architecture?

## Core flow

```text
Business transaction
        ↓
Transactional outbox
        ↓
Event broker
        ↓
Independent consumers
```

Example:

```text
Order Service
→ OrderCreated

Inventory Service
→ consumes OrderCreated
→ publishes InventoryReserved

Payment Service
→ consumes InventoryReserved
→ publishes PaymentAuthorized

Order Service
→ confirms order
```

## Event contract

```json
{
  "eventId": "E-100",
  "eventType": "OrderCreated",
  "aggregateId": "O-100",
  "aggregateVersion": 1,
  "occurredAt": "2026-07-24T10:30:00Z",
  "correlationId": "C-900",
  "payload": {
    "customerId": "U-500"
  }
}
```

Useful fields include:

- Stable event ID
- Event type
- Aggregate ID
- Aggregate version
- Timestamp
- Correlation ID
- Causation ID
- Schema version

## Essential design requirements

- Transactional event publication
- Idempotent consumers
- Explicit ordering requirements
- Schema compatibility
- Bounded retry
- Dead-letter handling
- Replay capability
- Consumer-lag monitoring
- Security
- Reconciliation

---

# 4. Kafka Consumer Processes a Message but Crashes Before Offset Commit

Flow:

```text
1. Consumer receives record.
2. Business update succeeds.
3. Consumer crashes before committing offset.
4. Kafka assigns the partition again.
5. The record is delivered again.
```

This is normal **at-least-once delivery**.

The business operation may execute twice unless the handler is idempotent.

## Safe consumer design

```java
@Transactional
public void process(PaymentCompletedEvent event) {
    boolean firstProcessing = processedEventRepository.tryInsert(
            "accounting-service",
            event.eventId()
    );

    if (!firstProcessing) {
        return;
    }

    ledgerRepository.credit(
            event.accountId(),
            event.amount(),
            event.eventId()
    );
}
```

The processed-event row and business update must commit in the same database transaction.

Database protection:

```sql
CREATE UNIQUE INDEX uk_ledger_source_event
ON ledger_entry(source_system, source_event_id, operation_type);
```

This prevents duplicate credit even if application deduplication fails.

---

# 5. How Do You Handle Duplicate Kafka Messages?

Duplicates can arise from:

- Producer retries
- Consumer crash before offset commit
- Rebalance
- Outbox relay retry
- Manual replay
- Dead-letter replay
- Network ambiguity

## Recommended controls

### Stable event ID

```json
{
  "eventId": "16fdb182-f194-4aae-bb63-a0c6513d29e5"
}
```

The same logical event must retain the same ID across retries.

### Consumer-specific deduplication table

```sql
CREATE TABLE processed_event (
    consumer_name VARCHAR(100) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    PRIMARY KEY (consumer_name, event_id)
);
```

### Business uniqueness constraint

```sql
CREATE UNIQUE INDEX uk_payment_event
ON payment_posting(external_event_id);
```

### Conditional state transition

```sql
UPDATE payment
SET status = 'CAPTURED'
WHERE payment_id = :paymentId
  AND status = 'AUTHORIZED';
```

The second attempt affects zero rows.

## Redis warning

A Redis-only check is not sufficient for critical financial correctness because:

- Keys can expire.
- Entries can be evicted.
- Redis can be unavailable.
- A crash can occur between the check and database write.

Use a durable database constraint as the authoritative guarantee.

---

# 6. How Do You Maintain Kafka Message Ordering?

Kafka guarantees ordering only **within a partition**.

```text
Partition 2

Offset 100 → OrderCreated
Offset 101 → OrderPaid
Offset 102 → OrderShipped
```

Use the aggregate ID as the Kafka key:

```java
kafkaTemplate.send(
        "order-events",
        orderId,
        event
);
```

```text
key = orderId
```

This keeps events for the same order in the same partition under a stable partitioning setup.

## Ordering limitations

Ordering can still be broken by:

- Multiple partitions
- Async processing inside the consumer
- Retry topics
- Changing partition count
- Incorrect or null keys
- Concurrent workers processing records from one partition
- External systems completing operations out of order

## Stronger protection

Add an aggregate sequence:

```json
{
  "orderId": "O-100",
  "sequence": 7
}
```

Consumer:

```text
Expected 7, received 7
→ process

Expected 7, received 9
→ delay, reject, or reconcile
```

## Global ordering

A single partition provides one total order, but restricts consumer-group parallelism to one active consumer.

Usually the correct requirement is:

```text
Order events ordered per order
Account events ordered per account
```

not global ordering across the whole company.

---

# 7. Kafka Consumer Lag Is Increasing

Consumer lag means producers are advancing faster than the consumer group’s committed position.

## Possible causes

- Producer throughput increased
- Consumer processing slowed
- Slow database queries
- Downstream API latency
- GC pauses
- CPU throttling
- Connection-pool waiting
- Hot partition
- Rebalance loop
- Large messages
- Poison-pill retries
- Too few partitions
- Too few consumers
- Consumer blocked longer than `max.poll.interval.ms`

## Investigation order

### Check whether lag is evenly distributed

```text
Partition 0: 100
Partition 1: 120
Partition 2: 950,000
```

One large partition suggests:

- Hot message key
- Skewed partitioning
- Slow records in that partition

### Check processing time

Measure:

- Records per second
- Time per record
- Batch processing duration
- Database time
- External-call time
- Serialization time

### Check consumer health

Inspect:

- Rebalance count
- Poll interval
- Heartbeats
- GC pauses
- Thread dumps
- Pod CPU throttling
- Memory pressure

## Fix options

- Optimize handler logic
- Batch database writes
- Remove unnecessary network calls
- Reduce `max.poll.records`
- Increase consumers up to the partition count
- Add partitions after considering key mapping and ordering
- Fix hot-key distribution
- Use asynchronous work carefully
- Separate slow event types into different topics
- Tune database and connection pools
- Move long retries to retry topics when ordering permits

Increasing `max.poll.interval.ms` may stop unnecessary rebalances, but it does not make slow processing faster.

---

# 8. How Do You Scale Kafka Consumers Horizontally?

Kafka distributes partitions across consumers in the same group.

```text
Topic: 12 partitions

3 consumers
→ approximately 4 partitions each

6 consumers
→ approximately 2 partitions each

12 consumers
→ approximately 1 partition each

15 consumers
→ 3 consumers idle
```

Maximum active parallelism in one consumer group is constrained by partition count.

## Scaling checklist

- Enough partitions
- Even key distribution
- Idempotent handlers
- Database capacity
- External provider capacity
- Connection-pool capacity
- Rebalance impact
- Per-partition ordering requirements

Adding consumers can make the database or downstream provider the next bottleneck.

---

# 9. How Do You Design Topic Partitioning?

## Choose a key according to the ordering boundary

Examples:

```text
Order events   → orderId
Account events → accountId
Customer events → customerId
Tenant events  → tenantId + aggregateId
```

## Avoid poor keys

### Constant key

```text
key = "all"
```

All records enter one partition.

### Highly skewed key

One tenant or customer may generate most traffic and create a hot partition.

### Random key

Provides distribution but loses entity-level ordering.

## Partition-count considerations

More partitions provide:

- More consumer parallelism
- More throughput potential

But also increase:

- Broker metadata
- Open files
- Replication work
- Rebalance work
- Operational complexity

There is no universal formula such as “100 partitions for 100,000 TPS.” Measure:

- Expected throughput
- Record size
- Broker capacity
- Consumer processing rate
- Replication factor
- Ordering requirements
- Future growth

---

# 10. How Do You Handle Message Retries?

## Classify the error first

### Retryable

- Temporary database outage
- Network timeout
- HTTP `429`
- Dependency `5xx`
- Broker interruption

### Non-retryable

- Invalid schema
- Unsupported event type
- Missing mandatory field
- Business validation failure
- Unauthorized request

## Retry architecture

```text
Main topic
   ↓ failure
Retry topic: 10 seconds
   ↓ failure
Retry topic: 1 minute
   ↓ failure
Retry topic: 10 minutes
   ↓
Dead-letter topic
```

Use:

- Bounded attempts
- Exponential backoff
- Jitter
- Error classification
- Metrics
- Correlation metadata

## Ordering trade-off

Retry topics allow later messages to continue while a failed event waits. This can violate per-aggregate ordering.

When ordering is critical, alternatives include:

- Pause the partition
- Perform a few short blocking retries
- Store failed work and stop processing later events for the aggregate
- Validate aggregate sequence numbers

---

# 11. How Do You Handle Dead-Letter Topics?

A dead-letter topic receives messages that cannot be processed successfully after the defined retry policy.

## Send to DLT when

- Payload is malformed
- Schema is unsupported
- Business state is permanently invalid
- Retry attempts are exhausted
- Processing repeatedly fails with the same error

## Include metadata

```text
Original topic
Original partition
Original offset
Message key
Event ID
Schema version
Consumer group
Failure category
Attempt count
First failure time
Last failure time
Trace ID
```

## DLT operation

```text
DLT alert
→ investigate
→ fix data or code
→ replay selected records
→ verify success
```

A DLT is not useful without:

- Monitoring
- Ownership
- Retention
- Search capability
- Replay tooling
- Runbooks

A DLT is not universally mandatory for every consumer, but there must be an explicit strategy for permanently unprocessable records.

---

# 12. How Do You Achieve Exactly-Once Processing?

The term must be scoped carefully.

## Kafka-to-Kafka processing

Kafka transactions can atomically combine:

```text
Consumed offsets
+
Produced records
=
One Kafka transaction
```

Downstream consumers use:

```text
isolation.level=read_committed
```

This can provide exactly-once processing within the Kafka transactional boundary.

## Kafka plus external database

This is not automatically exactly once:

```text
Consume Kafka record
→ update PostgreSQL
→ commit Kafka offset
```

A crash between the database commit and offset commit causes redelivery.

Use:

- Idempotent consumer
- Processed-event table
- Database uniqueness constraint
- Transactional outbox
- Conditional updates
- Reconciliation

## Important correction

The following alone does not provide exactly-once processing:

```text
read_committed
+
manual immediate acknowledgement
```

Manual acknowledgement controls offset timing. Exactly-once Kafka processing requires transactional coordination of produced records and consumed offsets.

For financial processing, the practical target is often:

> At-least-once delivery with exactly-once business effect through idempotency.

---

# 13. How Do You Debug Apparent Message Loss?

First determine whether the message was:

- Never produced
- Produced but not durable
- Written to another topic or partition
- Consumed by another group
- Consumed but not logged
- Filtered
- Deserialization-failed
- Sent to a retry/DLT topic
- Expired by retention
- Removed through compaction
- Processed but not committed
- Skipped because of offset reset

## Investigation checklist

### Producer

- Was `send()` successful?
- Was the future/result checked?
- Which topic and partition?
- What offset was returned?
- What key was used?
- What were `acks` and retry settings?

### Broker

- Does the record exist at the expected partition and offset?
- Was retention reached?
- Is the topic compacted?
- Was there ISR shrinkage?
- Were broker errors reported?

### Consumer

- Correct group ID?
- Correct topic subscription?
- Current committed offset?
- `auto.offset.reset` behavior?
- Deserialization errors?
- Filters?
- Retry or DLT forwarding?
- Rebalance during processing?

### Business system

- Was the handler idempotently ignored?
- Did the DB transaction roll back?
- Did a conditional update affect zero rows?
- Was a duplicate already processed?

Use event IDs and trace IDs across producer logs, Kafka metadata, consumer logs, and database records.

---

# 14. How Do You Monitor Kafka?

## Broker metrics

- Under-replicated partitions
- Offline partitions
- ISR changes
- Request latency
- Produce/fetch throughput
- Disk usage
- Network utilization
- Controller health
- Replication lag

## Producer metrics

- Send rate
- Error rate
- Retry rate
- Record size
- Batch size
- Compression ratio
- Request latency
- Buffer exhaustion

## Consumer metrics

- Lag per partition
- Records consumed per second
- Commit latency
- Rebalance count
- Poll idle ratio
- Processing duration
- Failed records
- Retry backlog
- DLT growth

## Business metrics

- Payments processed
- Orders awaiting events
- Oldest pending Saga
- Duplicate events ignored
- Failed compensations
- Projection freshness

Infrastructure metrics alone cannot reveal that an order has remained `PAYMENT_PENDING` for two hours.

---

# 15. How Do You Handle Event Schema Evolution?

## Prefer additive changes

Original:

```json
{
  "orderId": "O-100",
  "status": "CONFIRMED"
}
```

Compatible addition:

```json
{
  "orderId": "O-100",
  "status": "CONFIRMED",
  "confirmedAt": "2026-07-24T10:30:00Z"
}
```

Risky changes:

- Rename field
- Remove field
- Change type
- Change meaning
- Reuse event type for another purpose
- Make an optional field mandatory

## Strategies

- Versioned event contracts
- Tolerant readers
- Defaults for missing fields
- Ignore unknown fields where safe
- Schema Registry compatibility checks
- Consumer contract tests
- Dual publishing during migration only when necessary
- Upcasters for event-sourced systems

## Event-type evolution

Prefer:

```text
OrderAddressChanged
```

over changing the historical meaning of:

```text
OrderUpdated
```

Consumers should depend on explicit business semantics rather than raw database-table structures.

---

# 16. How Do You Secure Kafka?

## Transport security

Use TLS for:

- Client-to-broker traffic
- Broker-to-broker traffic
- Connector traffic

## Authentication

Common mechanisms include:

- mTLS
- SASL/SCRAM
- OAuth bearer mechanisms
- Kerberos in applicable environments

## Authorization

Apply least-privilege ACLs:

```text
Order Service
→ write order-events

Inventory Service
→ read order-events
→ write inventory-events

Notification Service
→ read order-confirmed
```

Do not grant every service read/write access to every topic.

## Data protection

- Do not place unnecessary PII in events.
- Encrypt sensitive payloads where required.
- Use references or tokenized identifiers.
- Protect schema registries.
- Secure DLTs and retry topics.
- Rotate credentials.
- Audit administrative operations.

## Secrets

Store broker credentials in a secret manager, not:

- Source code
- Docker image
- Plain Git files
- Logs

---

# 17. High-Throughput Messaging

## Producer-side techniques

- Batching
- Compression
- Asynchronous sends
- Appropriate `linger.ms`
- Appropriate batch size
- Stable partition key
- Sufficient broker capacity
- Avoid synchronous wait per message

## Consumer-side techniques

- Batch reads
- Batch database writes
- Avoid remote call per record
- Partition-level parallelism
- Bounded worker pools
- Efficient serialization
- Local caching where safe
- Backpressure
- Idempotent bulk operations

## Avoid

```text
Consume one record
→ open DB connection
→ perform five queries
→ call three HTTP services
→ close connection
```

For very high rates, redesign the processing path:

```text
Consume batch
→ validate batch
→ bulk database operation
→ write outbox results
```

---

# 18. Corrected Payment Processing System at 10,000 TPS

The source material proposes:

```text
User → API Gateway → Kafka → Payment Service
```

That can be valid for asynchronous payment acceptance, but several claims require correction.

## Incorrect generalization

> “At 10,000 TPS, you never hit the database directly.”

High-throughput systems still use databases, ledgers, or durable stores. The real questions are:

- How many writes?
- How are writes partitioned?
- What is the consistency requirement?
- Can the operation be asynchronous?
- What latency is promised to the user?
- What is the authoritative payment state?

Kafka can absorb bursts and decouple processing, but it does not remove the need for durable transaction state.

## Safer architecture

```text
Client
   ↓
API Gateway
   ↓
Payment API
   ├── validate authentication
   ├── validate request
   ├── reserve idempotency key durably
   ├── create payment as ACCEPTED/PENDING
   └── write PaymentRequested to outbox
               ↓
             Kafka
               ↓
Payment Processor
   ├── provider idempotency key
   ├── provider call
   ├── ledger/database transaction
   └── publish result through outbox
```

## Response options

### Synchronous authorization

```text
Client waits for payment result
```

Use when immediate authorization is part of the contract.

### Asynchronous acceptance

```http
HTTP/1.1 202 Accepted
```

```json
{
  "paymentId": "PAY-100",
  "status": "PENDING"
}
```

Use when settlement or processing can complete later.

## Idempotency

A Redis pre-check alone is insufficient.

Use:

```text
Durable idempotency record
+
database uniqueness constraint
+
provider idempotency key
```

## Database failure

A database crash does not automatically mean “use Saga.”

- One database transaction → rely on ACID and recovery.
- Multiple services/resources → Saga may be appropriate.
- Ambiguous external payment result → query provider and reconcile.

## Scale techniques

- Partition by merchant or payment ID carefully
- Horizontally scale stateless API instances
- Batch non-interactive writes
- Separate authorization from settlement
- Use immutable ledger entries
- Use outbox publication
- Apply per-merchant rate limits
- Backpressure when processors are saturated
- Measure end-to-end payment state age

---

# 19. Production Performance Scenarios

## API rises from 200 ms to 5 seconds

Check:

- Distributed traces
- Database connection-pool wait
- HTTP connection-pool wait
- Downstream latency
- Lock contention
- Queue depth
- GC
- CPU throttling
- Retry loops
- Cache misses

No error log may exist because all operations eventually succeed.

---

## Fine at 100 users, fails at 10,000

Likely saturation points:

- Request threads
- Database connections
- HTTP connections
- CPU
- Memory
- File descriptors
- Queue capacity
- Database locks
- Hot cache keys
- Downstream rate limits

Use load tests that reproduce concurrency, data volume, payload distribution, and hot keys.

---

## CPU normal but latency high

Possible causes:

- Waiting for DB connection
- Waiting for row lock
- Waiting for HTTP connection
- Slow network
- Queueing
- Disk I/O
- Thread contention
- Downstream timeout
- Sleep/backoff
- Consumer lag

Low CPU can mean the application is mostly waiting.

---

## Memory grows for days

Investigate:

- Heap trend
- Object histograms
- Heap dump
- ThreadLocal retention
- Unbounded cache
- Unbounded queues
- Classloader leak
- Listener registration
- HTTP response buffering
- Native/direct memory
- Thread growth

Use Java Flight Recorder, heap analysis, and allocation profiling rather than guessing.

---

# 20. Resilience Scenarios

## Downstream service is slow

Use:

```text
Deadline/timeout
+
bounded retry
+
circuit breaker
+
bulkhead
+
concurrency limit
+
load shedding
```

A circuit breaker does not replace a timeout. A timeout does not limit total concurrency. A retry can amplify overload.

## Database pool exhausted

Possible causes:

- Slow SQL
- Long transactions
- Connection leak
- N+1 queries
- Remote calls inside a transaction
- Too much request concurrency
- Lock waits
- Pool too small
- Database unable to support a larger pool

Measure acquisition time and connection hold time before increasing the pool.

## Redis unavailable

Choose by use case:

| Redis purpose     | Possible fallback                           |
| ----------------- | ------------------------------------------- |
| Optional cache    | Fall back to DB with protection             |
| Session store     | May require fail-closed                     |
| Rate limiter      | Fail-open or fail-closed by endpoint        |
| Distributed lock  | Do not proceed if correctness depends on it |
| Idempotency cache | Use authoritative DB record                 |

When falling back to the database, add concurrency limits to prevent a cache-outage stampede.

---

# 21. Quick Interview Answers

## When do you prefer synchronous calls?

> I use synchronous communication when the caller needs an immediate result and the dependency can respond within a bounded deadline. I use asynchronous messaging when buffering, decoupling, replay, or independent processing is more important than an immediate response.

## Consumer crashes before offset commit—what happens?

> Kafka redelivers the record after partition reassignment or restart. The business handler must therefore be idempotent, normally using a stable event ID and a unique database constraint.

## How do you prevent duplicate processing?

> I store processed event IDs per consumer in the same local transaction as the business change, and I also protect the business operation with a unique reference or conditional state transition.

## How do you maintain ordering?

> Kafka guarantees order per partition. I key messages by the business aggregate ID and preserve sequential processing within each partition. Where needed, I include an aggregate sequence number to detect gaps.

## How do you fix lag?

> I first determine whether lag is global or partition-specific, then inspect handler time, database waits, GC, rebalances, hot keys, and downstream latency. I optimize processing and scale consumers only up to the available partition count and downstream capacity.

## Exactly once?

> Kafka transactions can provide exactly-once consume–process–produce behavior inside Kafka. For database or external API effects, I use idempotency, constraints, outbox/inbox patterns, and reconciliation to achieve one business effect despite possible redelivery.
