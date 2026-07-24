# Advanced Questions — Outbox, Event Sourcing, CQRS, Kafka, and Messaging

## Important Corrections

- The **Outbox Pattern does not provide exactly-once delivery**. The relay can publish the same event more than once, so consumers must be idempotent.
- **Event Sourcing and CQRS are independent patterns**. They are often combined, but either can be used without the other.
- Kafka guarantees ordering **within one partition**, not across an entire multi-partition topic.
- Producer `acks` controls durability acknowledgement; it does not by itself define end-to-end delivery semantics.
- Kafka exactly-once processing primarily covers a **consume–process–produce flow involving Kafka topics and offsets**. An external database or HTTP side effect does not automatically join that guarantee.
- A dead-letter topic is a quarantine and recovery mechanism, not a place where failed messages should be forgotten.

---

# Q31: Explain the Outbox Pattern

## The dual-write problem

A service often needs to:

1. Update its database.
2. Publish an event to Kafka or another broker.

A direct implementation may look like this:

```java
@Transactional
public Order createOrder(CreateOrderCommand command) {
    Order order = orderRepository.save(
            Order.create(command)
    );

    kafkaTemplate.send(
            "order-events",
            new OrderCreatedEvent(order.getId())
    );

    return order;
}
```

The database and Kafka are independent systems, so one local transaction cannot normally commit both atomically.

## Failure case 1: Publish before database commit

```text
Publish OrderCreated
        ↓
Database transaction rolls back
        ↓
Consumers receive an event for an order that does not exist
```

## Failure case 2: Commit before publishing

```text
Database commit succeeds
        ↓
Application crashes
        ↓
OrderCreated is never published
```

This is the **dual-write problem**.

---

## Outbox solution

Write the business change and event record to the same database transaction:

```text
Order table update
+
Outbox row insert
=
One local transaction
```

```sql
CREATE TABLE outbox_event (
    event_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    event_sequence BIGINT
);
```

Application code:

```java
@Transactional
public Order createOrder(CreateOrderCommand command) {
    Order order = orderRepository.save(
            Order.create(command)
    );

    OutboxEvent event = new OutboxEvent(
            UUID.randomUUID(),
            "Order",
            order.getId().toString(),
            "OrderCreated",
            serialize(new OrderCreatedEvent(
                    order.getId(),
                    order.getCustomerId()
            )),
            Instant.now()
    );

    outboxRepository.save(event);

    return order;
}
```

Notice that the method does **not** publish directly to Kafka. If the transaction rolls back, both the order and outbox event roll back. If it commits, both become durable. This is the central guarantee of the pattern. ([Debezium][1])

---

## Publishing outbox events

### Option 1: Polling publisher

A background worker reads unpublished rows:

```sql
SELECT *
FROM outbox_event
WHERE published_at IS NULL
ORDER BY created_at, event_sequence
LIMIT 100
FOR UPDATE SKIP LOCKED;
```

It publishes them and marks them as published.

```text
Poll rows
→ publish to Kafka
→ mark as published
```

### Option 2: Change Data Capture

A tool such as Debezium captures changes to the outbox table from the database transaction log and publishes them to Kafka. Debezium provides an Outbox Event Router specifically for this model. ([Debezium][1])

```text
Application transaction
        ↓
Outbox table
        ↓ database WAL/binlog
Debezium
        ↓
Kafka topic
```

CDC avoids repeatedly polling the business database, but introduces connector and transaction-log operations that must be monitored.

---

## Why delivery is at least once

Consider a polling relay:

```text
1. Relay publishes event.
2. Kafka accepts event.
3. Relay crashes before marking row as published.
4. Relay restarts.
5. Same event is published again.
```

Avoiding the duplicate would require an atomic transaction across the database and broker—the original problem. Therefore, the practical model is:

```text
Reliable publication
+
possible duplicate delivery
+
idempotent consumers
```

---

## Ordering

To preserve order for one aggregate:

- Include an aggregate ID.
- Include an aggregate version or sequence.
- Use the aggregate ID as the Kafka key.
- Publish events for the same aggregate in commit order.

```text
Kafka key = orderId

OrderCreated       sequence 1
OrderPaid          sequence 2
OrderDispatched    sequence 3
```

Kafka will place records with the same key into the same partition under a stable partitioning strategy, providing partition-level ordering. ([Apache Kafka][2])

## Interview-ready answer

> The Outbox Pattern solves the dual-write problem between a service database and a message broker. The service writes its business change and an outbox event in one local transaction. A polling publisher or CDC connector later publishes the event. If the database rolls back, the event does not exist; if it commits, the event is available for publication. The relay can publish duplicates, so consumers must be idempotent.

---

# Q32: What Is Event Sourcing?

## Definition

Event Sourcing stores an entity’s state as an ordered sequence of immutable state-changing events rather than only storing its latest state.

Traditional CRUD:

```text
Account row:
balance = 75,000
status  = ACTIVE
```

Event-sourced model:

```text
AccountOpened          +0
MoneyDeposited    +100,000
MoneyWithdrawn     -20,000
MoneyWithdrawn      -5,000
```

The current balance is reconstructed by replaying the events:

```text
0 + 100,000 - 20,000 - 5,000 = 75,000
```

In Event Sourcing, the event stream is the source of truth; current state is a projection of that history. ([Microsoft Learn][3])

---

## Example event model

```java
public sealed interface AccountEvent
        permits AccountOpened, MoneyDeposited, MoneyWithdrawn {

    UUID eventId();

    long sequence();

    Instant occurredAt();
}
```

```java
public record MoneyDeposited(
        UUID eventId,
        long sequence,
        Instant occurredAt,
        BigDecimal amount
) implements AccountEvent {
}
```

Aggregate reconstruction:

```java
public final class Account {

    private BigDecimal balance = BigDecimal.ZERO;
    private long version;

    public static Account rebuild(
            List<AccountEvent> events
    ) {
        Account account = new Account();

        for (AccountEvent event : events) {
            account.apply(event);
        }

        return account;
    }

    private void apply(AccountEvent event) {
        switch (event) {
            case MoneyDeposited deposited ->
                    balance = balance.add(deposited.amount());

            case MoneyWithdrawn withdrawn ->
                    balance = balance.subtract(withdrawn.amount());

            case AccountOpened ignored ->
                    balance = BigDecimal.ZERO;
        }

        version = event.sequence();
    }
}
```

---

## Optimistic concurrency

Suppose two processes load an aggregate at version 10.

```text
Process A expects version 10
Process B expects version 10

A appends event 11 successfully
B attempts to append event 11
→ version conflict
```

The event store should append only when the expected version matches:

```sql
INSERT INTO account_event (
    account_id,
    sequence_number,
    event_type,
    payload
)
VALUES (
    :accountId,
    :nextSequence,
    :eventType,
    :payload
);
```

A uniqueness constraint on:

```text
account_id + sequence_number
```

prevents two events from occupying the same aggregate version.

---

## Snapshots

Replaying tens of thousands of events for every request may be inefficient.

A snapshot stores derived state at a known sequence:

```text
Snapshot at sequence 10,000
+
events 10,001–10,025
=
current state
```

Snapshots improve loading performance but are not usually the source of truth. They should be rebuildable from events. ([microservices.io][4])

---

## Event Sourcing vs CRUD

| Traditional CRUD                  | Event Sourcing                            |
| --------------------------------- | ----------------------------------------- |
| Stores current state              | Stores state transitions                  |
| Updates or replaces rows          | Appends immutable events                  |
| History requires audit mechanisms | History is inherent                       |
| Current state is directly queried | State is rebuilt or projected             |
| Simpler schema evolution          | Event evolution requires careful handling |
| Easy ad hoc queries               | Usually requires read projections         |
| Familiar operational model        | Higher design and operational complexity  |

---

## Challenges

### Event schema evolution

Old events remain in the store and must still be readable years later.

Possible techniques:

- Upcasters
- Versioned event types
- Tolerant readers
- New compensating events
- Rebuilding projections with old and new versions

### Replay side effects

Replaying an event to rebuild state must not resend an email or charge a payment.

Separate:

```text
Applying an event to state
```

from:

```text
Reacting externally to a newly published event
```

### Privacy and deletion

Immutable event history can conflict with deletion or privacy obligations. Sensitive data may need:

- Tokenization
- Encryption with destroyable keys
- References instead of embedded data
- Separate protected personal-data stores

### Operational complexity

Event stores, snapshots, projections, replay tools, schema management, and monitoring all add complexity.

## When to use Event Sourcing

Good candidates include:

- Financial ledgers
- Audit-intensive workflows
- Domains where historical reconstruction is essential
- Complex temporal business rules
- Systems requiring multiple derived projections

Avoid it for simple CRUD applications where retaining every state transition provides little business value.

## Interview-ready answer

> Event Sourcing stores immutable domain events as the source of truth instead of storing only the latest state. The aggregate is rebuilt by replaying its event stream, often starting from a snapshot. It provides strong history and replay capabilities, but introduces event-versioning, projection, privacy, and operational complexity.

---

# Q33: Explain CQRS

CQRS means **Command Query Responsibility Segregation**.

It separates operations that change state from operations that read state.

```text
Commands
→ Write model

Queries
→ Read model
```

A command expresses intent:

```text
PlaceOrder
CancelOrder
AuthorizePayment
```

A query asks for data:

```text
GetOrderDetails
SearchCustomerOrders
GetSalesDashboard
```

CQRS can use distinct interfaces, models, or physical databases depending on the system’s needs. It does not inherently require Event Sourcing or two databases. ([Microsoft Learn][3])

---

## Simple CQRS

The application may use one database but separate models:

```text
Command controller
→ domain model
→ normalized tables

Query controller
→ DTO projection
→ same database
```

```java
public interface OrderCommandService {

    OrderId placeOrder(PlaceOrderCommand command);

    void cancelOrder(CancelOrderCommand command);
}
```

```java
public interface OrderQueryService {

    OrderDetails findById(OrderId id);

    Page<OrderSummary> search(OrderSearchFilter filter);
}
```

This provides conceptual separation without distributed synchronization.

---

## Fully separated CQRS

```text
Command API
    ↓
Write model
    ↓
Write database
    ↓ events
Projection consumers
    ↓
Read database
    ↓
Query API
```

The write store may be relational and normalized, while read stores may be:

- Elasticsearch
- Redis
- Document database
- Denormalized relational tables
- Search index

Example read document:

```json
{
  "orderId": "O-100",
  "customerName": "Harshana",
  "orderStatus": "CONFIRMED",
  "paymentStatus": "CAPTURED",
  "shipmentStatus": "DISPATCHED",
  "total": 120.0
}
```

The read model duplicates information intentionally to answer queries efficiently.

---

## Synchronizing read models

Common approaches include:

- Domain events
- Transactional outbox
- Kafka consumers
- CDC
- Event-store subscriptions

Because projection updates happen asynchronously, the user may write data and temporarily read an older projection.

```text
Command succeeds
        ↓
Event is published
        ↓ short delay
Projection updates
```

Possible API response:

```json
{
  "orderId": "O-100",
  "status": "PENDING_PROJECTION",
  "version": 14
}
```

The client may poll until the read model reaches the required version.

---

## When CQRS is useful

- Read and write workloads differ significantly.
- Queries need highly denormalized data.
- Read traffic is much larger than write traffic.
- Several specialized read models are needed.
- Domain writes have complex invariants.
- Eventual consistency is acceptable.
- Independent scaling provides measurable value.

## When CQRS is excessive

- Basic CRUD application
- Small workload
- Same data shape for reads and writes
- Strong immediate read-after-write consistency everywhere
- Team lacks operational capacity for projections and replay

## CQRS vs Event Sourcing

| CQRS                               | Event Sourcing                     |
| ---------------------------------- | ---------------------------------- |
| Separates commands and queries     | Stores state as events             |
| Can use ordinary CRUD writes       | Uses append-only event streams     |
| May use one database               | Usually uses an event store        |
| Does not inherently require replay | Rebuilds state through replay      |
| Often eventually consistent        | Frequently paired with projections |

## Interview-ready answer

> CQRS separates state-changing commands from read queries. It can be as simple as separate command and query services over one database, or it can use separate write and read stores synchronized through events. I apply it when read and write requirements differ enough to justify projection management and eventual consistency.

---

# Q34: How Does Kafka Guarantee Message Ordering?

## Partition-level ordering

A Kafka topic is divided into partitions.

```text
orders topic
├── Partition 0
├── Partition 1
└── Partition 2
```

Each record in a partition receives a monotonically increasing offset:

```text
Partition 0

Offset 100 → OrderCreated
Offset 101 → OrderPaid
Offset 102 → OrderShipped
```

Kafka guarantees that a consumer reads one partition’s records in the order in which they were written. Events with the same key are normally routed to the same partition, allowing entity-level ordering. ([Apache Kafka][2])

```java
kafkaTemplate.send(
        "order-events",
        orderId,
        event
);
```

Using:

```text
key = orderId
```

means events for one order can remain ordered while different orders are processed in parallel.

---

## Consumer-group behavior

Within one consumer group, a partition is assigned to one active consumer at a time.

```text
Partition 0 → Consumer A
Partition 1 → Consumer B
Partition 2 → Consumer C
```

Therefore, the partition is Kafka’s unit of:

- Ordering
- Storage
- Parallelism
- Consumer assignment

---

## Limitations

### No global ordering across partitions

Kafka does not guarantee whether:

```text
Partition 0, offset 100
```

occurred before or after:

```text
Partition 1, offset 250
```

A single partition can provide global topic order, but it limits group parallelism to one active consumer for that topic partition.

### Producer retries can affect ordering

When producer idempotence is disabled and several requests are in flight, an earlier failed batch may be retried after a later batch succeeds, causing reordering. Kafka’s producer documentation explicitly warns about this combination. ([Apache Kafka][5])

### Consumer code can reorder processing

Kafka may deliver records in order, but application code can reorder completion:

```text
Record 1 → slow async task
Record 2 → fast async task

Record 2 completes first
```

Preserving processing order requires:

- Sequential processing per partition
- Partition-specific worker queues
- Ordered completion
- Controlled offset commits

### Retry topics can change order

Moving a failed record to a retry topic allows later records from the original partition to continue, which may violate strict business ordering.

For strict ordering, consider:

- Pausing the affected partition
- Blocking bounded retries
- Storing aggregate sequence numbers
- Rejecting or delaying events with sequence gaps

### Partition-count changes

If key-to-partition mapping changes after increasing the partition count, future records for a key may go to a different partition depending on the partitioning strategy. Plan partition growth and ordering requirements together.

## Interview-ready answer

> Kafka guarantees ordering only within a partition. I use the business aggregate ID, such as `orderId` or `accountId`, as the message key so related events enter the same partition. There is no global order across partitions, and application-level asynchronous processing or retry topics can still reorder effects.

---

# Q35: At-Most-Once vs At-Least-Once vs Exactly-Once

## Comparison

| Semantics               |                                                   Possible loss |     Possible duplicates |
| ----------------------- | --------------------------------------------------------------: | ----------------------: |
| At-most-once            |                                                             Yes |                      No |
| At-least-once           |                                  No, with sufficient durability |                     Yes |
| Exactly-once processing | No duplicate committed Kafka result within its defined boundary | No within that boundary |

---

## At-most-once

Commit the consumer offset before processing:

```text
Read message
→ commit offset
→ process message
```

Failure:

```text
Offset committed
→ consumer crashes before processing
→ message is not processed again
```

This avoids duplicate processing but may lose work.

---

## At-least-once

Process first and commit afterward:

```text
Read message
→ process message
→ commit offset
```

Failure:

```text
Business update succeeds
→ consumer crashes before offset commit
→ message is delivered again
```

This avoids losing the record but can repeat the business effect. Kafka’s standard delivery model is commonly at least once unless the application deliberately configures another model. ([Apache Kafka][6])

---

## Exactly-once in Kafka

For a Kafka-to-Kafka processing flow:

```text
Consume input records
+
produce output records
+
commit input offsets
=
one Kafka transaction
```

The producer uses a `transactional.id`, writes output records transactionally, and commits consumed offsets as part of that transaction. Downstream consumers use `isolation.level=read_committed` so aborted transactional records are hidden. ([Apache Kafka][6])

Conceptual code:

```java
producer.beginTransaction();

try {
    for (ConsumerRecord<String, InputEvent> record : records) {
        OutputEvent output = process(record.value());

        producer.send(
                new ProducerRecord<>(
                        "output-topic",
                        record.key(),
                        output
                )
        );
    }

    producer.sendOffsetsToTransaction(
            offsets,
            consumer.groupMetadata()
    );

    producer.commitTransaction();
} catch (Exception exception) {
    producer.abortTransaction();
    throw exception;
}
```

---

## Idempotent producer vs transactional producer

### Idempotent producer

Prevents duplicate records caused by producer retries within a producer session.

It does not atomically commit:

- Several topics
- Consumer offsets
- External database changes

### Transactional producer

Can atomically commit:

- Records across Kafka partitions or topics
- Consumed offsets associated with the processing transaction

---

## External side effects

This is not automatically exactly once:

```text
Consume Kafka message
→ charge payment provider
→ commit Kafka offset
```

A crash after the charge but before the offset commit can cause the charge call to be repeated.

Use:

- Provider idempotency keys
- Database constraints
- Transactional outbox
- Inbox/deduplication table
- Reconciliation

Kafka’s design documentation notes that exactly-once behavior for destinations outside Kafka requires cooperation from those systems. ([Apache Kafka][6])

## `acks` is not delivery semantics

- `acks=all` improves producer durability by requiring acknowledgement from all required in-sync replicas.
- It does not guarantee that a consumer’s database update happens exactly once.
- Delivery semantics depend on producer behavior, offset handling, transactions, and external side effects.

## Interview-ready answer

> At-most-once can lose messages but avoids redelivery. At-least-once processes every durable message but can produce duplicates. Kafka exactly-once processing uses an idempotent transactional producer, commits output records and input offsets atomically, and requires read-committed consumers. That guarantee does not automatically include external databases or APIs.

---

# Q36: How Do You Handle Poison-Pill Messages?

A poison pill is a record that consistently fails processing.

Examples:

- Invalid serialization
- Missing mandatory fields
- Unsupported schema version
- Permanent business-rule violation
- Unexpected data that triggers a consumer bug

## First classify the failure

### Transient failure

Examples:

- Database temporarily unavailable
- Network timeout
- Temporary rate limit
- Broker or dependency outage

Use bounded retries.

### Permanent failure

Examples:

- Invalid account identifier
- Malformed payload
- Unsupported event type
- Impossible business state

Do not repeatedly retry the same permanent error.

---

## Recommended flow

```text
Consume message
        ↓
Processing succeeds?
├── Yes → commit
└── No
     ↓
Retryable?
├── Yes → retry with backoff
└── No  → dead-letter topic
```

After retry exhaustion:

```text
Original topic
→ retry-1
→ retry-2
→ retry-3
→ dead-letter topic
```

Spring Kafka’s non-blocking retry pattern forwards records through retry topics until processing succeeds or the configured attempts are exhausted; a DLT handler can then process records routed to the dead-letter topic. ([Home][7])

---

## Spring Kafka example

```java
@RetryableTopic(
        attempts = "4",
        backoff = @Backoff(
                delay = 1_000,
                multiplier = 2.0
        )
)
@KafkaListener(
        topics = "payment-events",
        groupId = "accounting-service"
)
public void consume(PaymentEvent event) {
    paymentProcessor.process(event);
}
```

```java
@DltHandler
public void handleDeadLetter(
        PaymentEvent event
) {
    deadLetterAuditService.record(event);
}
```

Production handlers should normally preserve more context than the payload alone.

---

## Dead-letter record metadata

Store or propagate:

- Original topic
- Original partition
- Original offset
- Event ID
- Event key
- Consumer group
- Failure type
- Safe exception summary
- Attempt count
- First and latest failure timestamps
- Trace or correlation ID
- Schema version
- Original raw payload where securely permitted

## DLT operational process

A dead-letter topic needs:

- Monitoring and alerts
- Ownership
- Retention policy
- Investigation dashboard
- Replay tooling
- Ability to replay one event or a controlled batch
- Protection against replaying the same invalid message endlessly

## Deserialization failures

A record that cannot be deserialized may fail before reaching the listener. Handle these errors at the deserializer or listener-container error-handling layer and retain the raw bytes where safe.

## Ordering warning

Non-blocking retry topics may let later records continue while an earlier record waits for retry. Do not use that model when strict aggregate ordering is mandatory without an additional sequencing strategy.

## Interview-ready answer

> I classify failures as transient or permanent. Transient failures receive bounded retries with exponential backoff and jitter. Permanent failures or exhausted retries are sent to a dead-letter topic with the original topic, partition, offset, event ID, error category, and trace information. The DLT is monitored and has controlled replay tooling; it is not a permanent message graveyard.

---

# Q37: Explain Kafka Consumer-Group Rebalancing

A rebalance redistributes topic partitions among consumers in a consumer group.

## Example

Before a new consumer joins:

```text
Consumer A → Partitions 0, 1
Consumer B → Partitions 2, 3
```

After Consumer C joins:

```text
Consumer A → Partitions 0, 1
Consumer B → Partition 2
Consumer C → Partition 3
```

## Common triggers

- A consumer joins the group.
- A consumer shuts down.
- A consumer crashes or stops heartbeating.
- Processing exceeds `max.poll.interval.ms`.
- Subscribed topic partition counts change.
- Topic subscriptions change.
- Group coordination changes.

Kafka treats a consumer that fails to call `poll()` within `max.poll.interval.ms` as failed for group-management purposes and initiates reassignment according to the configured protocol and membership model. ([Apache Kafka][8])

---

## Why rebalances are disruptive

During reassignment:

- Consumers may temporarily stop receiving records.
- In-flight work may need coordination.
- Uncommitted records may be delivered again.
- Local partition state may need to move.
- Consumer lag can increase.

---

## Eager vs cooperative rebalancing

### Eager rebalancing

Consumers revoke all assignments and then receive new ones.

```text
Revoke everything
→ rebalance
→ assign partitions again
```

### Cooperative rebalancing

Consumers incrementally revoke only partitions that need to move. Kafka provides `CooperativeStickyAssignor` for cooperative rebalancing. ([Apache Kafka][8])

```text
Keep unaffected partitions
→ transfer only required partitions
```

This reduces stop-the-world disruption.

---

## Static membership

Configure a stable `group.instance.id` for each consumer instance.

Static membership can prevent unnecessary rebalances during brief restarts or rolling deployments because the restarted instance can reclaim its identity. It must be unique per active consumer instance. ([Apache Kafka][9])

---

## How to minimize disruption

### Keep `poll()` responsive

Do not block the consumer poll loop for excessively long processing.

Options:

- Reduce `max.poll.records`.
- Process smaller batches.
- Pause assigned partitions while bounded work runs.
- Offload processing carefully while preserving offset and order guarantees.
- Increase `max.poll.interval.ms` only when justified.

### Use graceful shutdown

- Stop polling new records.
- Complete or abandon bounded in-flight work safely.
- Commit appropriate offsets.
- Close the consumer.

### Avoid long inline retries

A consumer sleeping for several minutes during retry may exceed the poll interval and trigger a rebalance. Retry topics can avoid blocking the poll loop, although they change ordering behavior. ([Home][10])

### Configure cooperative assignment

Use a cooperative strategy where supported by the complete consumer group.

### Use static membership carefully

Stable identities help during planned restarts, but stale instances may delay reassignment until session expiration.

### Monitor

Track:

- Rebalance count
- Rebalance duration
- Assigned partitions
- Consumer lag
- Poll interval
- Processing time
- Commit failures
- Heartbeat failures

## Interview-ready answer

> Rebalancing occurs when group membership, subscriptions, or partition counts change, or when a consumer is considered failed. It redistributes partitions but can temporarily pause processing and cause redelivery. I reduce disruption using cooperative assignment, static membership, graceful shutdown, small enough poll batches, responsive poll loops, and careful offset handling.

---

# Q38: How Would You Implement a Saga Using Kafka?

Consider an order workflow:

```text
Create order
→ reserve inventory
→ authorize payment
→ confirm order
```

A choreography-based Saga has no central service issuing every command. Each participant reacts to an event and publishes the next event.

---

## Success flow

### Step 1: Order Service

Local transaction:

```text
Create order as PENDING
+
write OrderCreated to outbox
```

Published event:

```json
{
  "eventId": "E-101",
  "eventType": "OrderCreated",
  "orderId": "O-100",
  "sequence": 1
}
```

### Step 2: Inventory Service

Consumes `OrderCreated`:

```text
Reserve inventory locally
+
write InventoryReserved to outbox
```

### Step 3: Payment Service

Consumes `InventoryReserved`:

```text
Authorize payment locally
+
write PaymentAuthorized to outbox
```

### Step 4: Order Service

Consumes `PaymentAuthorized`:

```text
Change order from PENDING to CONFIRMED
+
publish OrderConfirmed
```

---

## Failure flow

If inventory is unavailable:

```text
OrderCreated
    ↓
InventoryRejected
    ↓
Order Service marks order REJECTED
```

If payment fails after inventory reservation:

```text
PaymentFailed
    ├── Inventory Service releases reservation
    │      ↓ InventoryReleased
    └── Order Service moves toward CANCELLED
```

Compensation is another local business transaction:

```text
Reserve stock
→ compensate with ReleaseStock
```

It does not erase the original reservation history.

---

## Topic and key design

Possible topics:

```text
order-events
inventory-events
payment-events
```

Use:

```text
Kafka key = orderId
```

for Saga events concerning one order, helping maintain per-order ordering.

## Required controls

### Transactional outbox

Every participant commits its state and outgoing event atomically.

### Idempotent handlers

Every event has a stable `eventId`.

```text
Same event delivered twice
→ business action occurs once
```

### Conditional state transitions

```sql
UPDATE orders
SET status = 'CONFIRMED'
WHERE order_id = :orderId
  AND status = 'PAYMENT_PENDING';
```

Checking the previous state protects against duplicates and invalid ordering.

### Sequence validation

```text
Expected sequence: 3
Received sequence: 5
→ delay, reject, or reconcile missing events
```

### Timeout handling

Choreography alone does not automatically detect that an expected event never arrived.

Use a timeout process:

```text
Order PAYMENT_PENDING for more than 10 minutes
→ query status or initiate recovery
```

### Compensation recovery

A failed compensation must enter a durable state:

```text
COMPENSATION_PENDING
→ retry
→ MANUAL_REVIEW_REQUIRED
```

### Observability

Propagate:

- Saga ID
- Order ID
- Event ID
- Causation ID
- Correlation ID
- Trace context

The Saga pattern can be implemented through choreography, where participants publish events that trigger the next local transaction. ([Microsoft Learn][11])

## Interview-ready answer

> In a Kafka choreography Saga, each service consumes a domain event, commits its local transaction and an outbox event, and publishes the event that triggers the next participant. I key events by the Saga aggregate, make every handler and compensation idempotent, persist workflow states, detect timeouts, validate event sequences, and provide reconciliation and manual recovery for unresolved Sagas.

---

# Q39: What Are RabbitMQ Exchange Types?

RabbitMQ publishers normally send messages to an **exchange**. The exchange routes messages to queues according to its type and bindings.

RabbitMQ provides four main built-in exchange types:

- Direct
- Fanout
- Topic
- Headers ([RabbitMQ][12])

---

## Direct exchange

Routes a message to queues whose binding key exactly matches the message’s routing key.

```text
Routing key: payment.completed
```

```text
Queue A binding: payment.completed → receives message
Queue B binding: payment.failed    → does not receive message
```

RabbitMQ documents direct exchanges as exact routing-key-to-binding-key matching. ([RabbitMQ][13])

Use for:

- Exact command routing
- Specific event categories
- Work queues divided by known keys

---

## Fanout exchange

Sends a copy to every bound queue and ignores the routing key.

```text
ApplicationLogs
    ├── Log Storage Queue
    ├── Alerting Queue
    └── Audit Queue
```

RabbitMQ’s fanout exchange broadcasts each published message to every bound destination. ([RabbitMQ][14])

Use for:

- Broadcast notifications
- Cache invalidation
- Log distribution
- Sending one event to several independent queues

---

## Topic exchange

Routes according to wildcard patterns over dot-separated routing keys.

Routing key:

```text
order.eu.created
```

Patterns:

```text
order.*.created
```

`*` matches exactly one segment.

```text
order.#
```

`#` matches zero or more segments.

Examples:

| Binding            | `order.eu.created` |
| ------------------ | -----------------: |
| `order.eu.created` |              Match |
| `order.*.created`  |              Match |
| `order.#`          |              Match |
| `payment.#`        |           No match |

Use for:

- Event categories
- Region and event-type routing
- Flexible subscription patterns

RabbitMQ’s topic exchange supports routing-key pattern matching rather than only exact equality. ([RabbitMQ][15])

---

## Headers exchange

Routes using message-header values instead of the routing key.

Example headers:

```text
format = pdf
region = eu
priority = high
```

Bindings can require:

```text
x-match = all
```

or:

```text
x-match = any
```

RabbitMQ describes headers exchanges as routing based on header-value matches rather than string routing keys. ([RabbitMQ][16])

---

## Direct vs topic exchange

| Direct                      | Topic                                   |
| --------------------------- | --------------------------------------- |
| Exact match                 | Pattern match                           |
| Simpler routing             | Flexible wildcard routing               |
| `payment.completed`         | `payment.*` or `payment.#`              |
| Good for known destinations | Good for subscriber-selected categories |

## Interview-ready answer

> A direct exchange routes on an exact key, fanout broadcasts to all bound queues, topic routes using wildcard patterns, and headers routes using message-header values. I use direct exchanges for precise routing and topic exchanges when subscribers need flexible categories such as region and event type.

---

# Q40: How Do You Ensure Message Idempotency?

Idempotency means that redelivering the same event does not repeat its intended business effect.

```text
Same event once
=
Same event five times
=
One business update
```

## Event identity

Every event should have a stable unique ID:

```json
{
  "eventId": "3b65b296-bdd7-426f-97c7-f005432ecf52",
  "eventType": "PaymentCompleted",
  "aggregateId": "PAY-100",
  "aggregateVersion": 7
}
```

Do not generate a new `eventId` every time the same outbox event is retried.

---

## Deduplication table

```sql
CREATE TABLE processed_event (
    consumer_name VARCHAR(100) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    PRIMARY KEY (consumer_name, event_id)
);
```

Include the consumer or handler name because different consumers may legitimately process the same event.

## Atomic consumer transaction

The business update and processed-event record must commit together:

```java
@Transactional
public void handle(PaymentCompletedEvent event) {
    boolean inserted = processedEventRepository.tryInsert(
            "accounting-service",
            event.eventId()
    );

    if (!inserted) {
        return;
    }

    accountRepository.credit(
            event.accountId(),
            event.amount()
    );
}
```

Failure cases:

```text
Deduplication row + business update commit
→ duplicate is ignored

Both roll back
→ redelivery can retry safely
```

Do not mark the event processed before the business change in an unrelated transaction.

---

## Database protection

Use business-level uniqueness as a second line of defence:

```sql
CREATE UNIQUE INDEX uk_ledger_external_event
ON ledger_entry(
    source_system,
    external_event_id,
    operation_type
);
```

Even if deduplication logic fails, the database prevents duplicate posting.

## Naturally idempotent update

This operation is naturally closer to idempotent:

```sql
UPDATE payment
SET status = 'CAPTURED'
WHERE payment_id = :paymentId
  AND status = 'AUTHORIZED';
```

Running it twice does not capture the payment twice.

This is not naturally idempotent:

```sql
UPDATE account
SET balance = balance + :amount
WHERE account_id = :accountId;
```

It requires a unique posting reference or ledger constraint.

---

## External side effects

If the handler calls an external provider:

```text
Consume event
→ call provider
→ crash before offset commit
```

Use the event or operation ID as the provider idempotency key.

## TTL cleanup

Deduplication records can be removed only after the latest possible replay or redelivery period.

Consider:

- Kafka retention
- DLT retention
- Manual replay windows
- Disaster recovery
- Regulatory requirements

For critical financial postings, retaining the durable uniqueness record may be safer than expiring it.

## Interview-ready answer

> I give every event a stable ID and store processed IDs per consumer. The deduplication insert and business update occur in one local transaction, protected by a unique constraint. I also design the business write itself to be idempotent, such as using a unique ledger reference or conditional state transition. For external calls, I propagate the same idempotency key to the provider.

---

# Additional Kafka and Messaging Questions

## Why Kafka Instead of RabbitMQ?

Neither technology is universally better.

| Kafka                                        | RabbitMQ                                             |
| -------------------------------------------- | ---------------------------------------------------- |
| Durable partitioned event log                | Queue and exchange-oriented broker                   |
| Events retained after consumption            | Messages commonly leave queues after acknowledgement |
| Supports replay                              | Primarily designed for delivery to queues            |
| Partition-based scaling                      | Queue and consumer-based work distribution           |
| Strong event-stream use cases                | Strong routing and task-queue use cases              |
| Independent consumer groups replay the log   | Exchanges route copies to bound queues               |
| Good for event history and stream processing | Good for commands, work queues and flexible routing  |

Choose Kafka for:

- Event streaming
- High-volume retained event history
- Replay and rebuilding projections
- CDC pipelines
- Stream processing
- Many independent consumer groups

Choose RabbitMQ for:

- Work queues
- Command delivery
- Flexible direct, topic, fanout or headers routing
- Request-reply messaging
- Per-queue processing workflows
- Cases where retaining a long event log is unnecessary

Kafka provides partition-based ordering and parallelism, while RabbitMQ exchanges provide direct, wildcard, broadcast and header-based routing. ([Apache Kafka][2])

### Interview answer

> I choose Kafka when retained event streams, replay, high throughput and independent consumer groups are central requirements. I choose RabbitMQ for queue-oriented task distribution, command messaging and flexible exchange-based routing. The choice follows the interaction model, not popularity.

---

## How Do Kafka Partitions Work?

A partition is an append-only ordered log.

```text
Topic: payments

Partition 0
offset 0
offset 1
offset 2

Partition 1
offset 0
offset 1
```

The producer selects a partition using:

- An explicit partition number
- A record key
- A partitioner

A partition has a leader broker and replicas. Producers and consumers interact with the leader, while followers replicate its log.

Consumer groups divide partitions among members:

```text
Six partitions
+
three consumers
=
approximately two partitions per consumer
```

If there are more consumers than partitions, some consumers remain idle.

Partition count controls maximum active parallelism for one consumer group, but more partitions also create broker, replication, file, metadata and rebalance overhead. Kafka’s operational documentation notes that every partition and log segment consumes broker-side resources; partitions are not free. ([Apache Kafka][17])

---

## Consumer Crashes Mid-Processing: What Happens?

### Offset committed before processing

```text
Commit offset
→ crash
→ record may be lost
```

This is at-most-once behavior.

### Process completed before offset commit

```text
Database update succeeds
→ crash
→ offset not committed
→ record delivered again
```

This is at-least-once behavior and requires idempotency.

### Kafka transaction

For Kafka-to-Kafka processing:

```text
Output records
+
input offsets
=
one Kafka transaction
```

A crash aborts or leaves an incomplete transaction that is recovered through the transactional protocol, and read-committed consumers do not expose aborted output. ([Apache Kafka][18])

---

## How Do You Maintain Ordering Across Partitions?

Strict global ordering and high partition parallelism conflict.

Options:

### Use one partition

```text
All events
→ one partition
```

Provides one total order but limits consumer-group parallelism.

### Order by business entity

```text
key = accountId
```

All events for the account enter the same partition, while different accounts process concurrently.

### Include sequence numbers

```json
{
  "accountId": "A-100",
  "sequence": 18
}
```

Consumer behavior:

```text
Expected 18, received 18
→ process

Expected 18, received 20
→ hold, reject, or reconcile missing events
```

### Use a global sequencing system

A central sequence can establish logical order, but consumers must still merge partition streams and wait for missing sequence values. This reduces scalability and adds coordination.

### Interview answer

> Kafka does not provide global ordering across partitions. I normally preserve order only where the domain needs it by keying messages with the aggregate ID. For stronger detection, I include an aggregate sequence and reject or delay gaps. I use a single partition only when total order is worth giving up parallelism.

---

## Dead-Letter Queues: When and Why?

Use a dead-letter topic or queue when:

- The message is permanently invalid.
- The schema is unsupported.
- The business state cannot accept the operation.
- Bounded retries are exhausted.
- Continuing to block the partition would cause unacceptable impact.

Do not send a message directly to a DLT for every temporary timeout without first applying an appropriate retry policy.

A DLT should support:

```text
Detection
→ investigation
→ correction
→ controlled replay
→ verification
```

Monitor:

- DLT message count
- Oldest DLT message age
- Failure category
- Source topic and service
- Replay success rate
- Repeated replay failures

---

# Quick Interview Cheat Sheet

| Topic                 | Key point                                                    |
| --------------------- | ------------------------------------------------------------ |
| Outbox                | Business update and outbox row in one transaction            |
| Outbox delivery       | At least once; consumers must deduplicate                    |
| Event Sourcing        | Events are the source of truth                               |
| Snapshot              | Performance optimization, not primary truth                  |
| CQRS                  | Separate command and query responsibilities                  |
| Kafka ordering        | Guaranteed only per partition                                |
| Kafka key             | Use aggregate ID for entity-level order                      |
| At-most-once          | Possible loss, no redelivery                                 |
| At-least-once         | Possible duplicate, avoid loss                               |
| Kafka exactly once    | Transactional Kafka consume–process–produce boundary         |
| Poison pill           | Bounded retry, then monitored DLT                            |
| Rebalance             | Partition redistribution after membership or metadata change |
| Cooperative rebalance | Incremental partition movement                               |
| Static membership     | Stable consumer identity                                     |
| Kafka Saga            | Local transactions, outbox events and compensations          |
| RabbitMQ direct       | Exact routing key                                            |
| RabbitMQ fanout       | Broadcast                                                    |
| RabbitMQ topic        | Wildcard routing                                             |
| RabbitMQ headers      | Header-value routing                                         |
| Message idempotency   | Stable event ID plus atomic deduplication                    |

[1]: https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html?utm_source=chatgpt.com "Outbox Event Router"
[2]: https://kafka.apache.org/documentation/?utm_source=chatgpt.com "Introduction | Apache Kafka"
[3]: https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs?utm_source=chatgpt.com "CQRS Pattern - Azure Architecture Center"
[4]: https://microservices.io/patterns/data/event-sourcing.html?utm_source=chatgpt.com "Pattern: Event sourcing"
[5]: https://kafka.apache.org/41/configuration/producer-configs/?utm_source=chatgpt.com "Producer Configs | Apache Kafka"
[6]: https://kafka.apache.org/41/design/design/?utm_source=chatgpt.com "Design | Apache Kafka"
[7]: https://docs.spring.io/spring-kafka/docs/3.1.1/reference/retrytopic/how-the-pattern-works.html?utm_source=chatgpt.com "How the Pattern Works :: Spring Kafka"
[8]: https://kafka.apache.org/41/configuration/consumer-configs/?utm_source=chatgpt.com "Consumer Configs | Apache Kafka"
[9]: https://kafka.apache.org/30/getting-started/upgrade/?utm_source=chatgpt.com "Upgrading - Apache Kafka"
[10]: https://docs.spring.io/spring-cloud-stream/reference/kafka/kafka-binder/retry-dlq.html?utm_source=chatgpt.com "Retry and Dead Letter Processing :: Spring Cloud Stream"
[11]: https://learn.microsoft.com/en-us/azure/architecture/patterns/saga?utm_source=chatgpt.com "Saga Design Pattern - Azure Architecture Center"
[12]: https://www.rabbitmq.com/docs/4.2/publishers?utm_source=chatgpt.com "Publishers"
[13]: https://www.rabbitmq.com/tutorials/tutorial-four-python?utm_source=chatgpt.com "RabbitMQ tutorial - Routing"
[14]: https://www.rabbitmq.com/docs/next/exchanges?utm_source=chatgpt.com "Exchanges"
[15]: https://www.rabbitmq.com/tutorials/tutorial-five-python?utm_source=chatgpt.com "RabbitMQ tutorial - Topics"
[16]: https://www.rabbitmq.com/tutorials/amqp-concepts?utm_source=chatgpt.com "AMQP 0-9-1 Model Explained"
[17]: https://kafka.apache.org/41/operations/hardware-and-os/?utm_source=chatgpt.com "Hardware and OS | Apache Kafka"
[18]: https://kafka.apache.org/41/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html?utm_source=chatgpt.com "KafkaProducer (clients 4.1.2 API)"
