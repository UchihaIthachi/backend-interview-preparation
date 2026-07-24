# Advanced Questions — Microservices Data and Persistence

## Important Corrections

- **Database per Service** means private data ownership, not necessarily a physically separate database server. Separate databases, schemas, or private table sets can all enforce ownership at different strengths.
- Avoiding cross-service joins does not mean cross-service reporting is impossible. Use API composition, CQRS read models, event-driven denormalization, or analytical storage.
- `SETNX` followed by a separate `EXPIRE` is unsafe. Redis lock acquisition should be one atomic operation using `SET ... NX PX`.
- Redis locks are leases, not perfect guarantees that a process still owns a resource. Use **fencing tokens** when stale lock holders could corrupt data.
- The formula `(CPU cores × 2) + disk spindles` is a database-side heuristic, not Little’s Law and not a universal per-service pool size.
- `EAGER` fetching does not automatically solve N+1 queries. Hibernate may still issue secondary selects.
- CDC events are implementation-level database changes. They should not automatically be treated as stable domain-event contracts.

---

# Q41: What is the Database per Service pattern? How do you handle cross-service queries?

## Definition

The Database per Service pattern gives each microservice exclusive ownership of its persistent data.

```text
Order Service
└── Order data

Payment Service
└── Payment data

Inventory Service
└── Inventory data
```

Other services must not directly update the owning service’s tables.

The separation may use:

1. A separate database server per service
2. A separate database on the same server
3. A separate schema per service
4. Private tables with separate database users and permissions

The important rule is **ownership and access control**, not necessarily one physical database server for every service. AWS similarly describes database-per-service as requiring additional patterns for queries spanning multiple services. ([AWS Documentation][1])

## Why use it?

- Services can evolve schemas independently.
- One service cannot silently modify another service’s data.
- Technology choices can differ where justified.
- Deployment coupling is reduced.
- Data ownership becomes explicit.
- Database failures can be isolated more effectively.

## The cross-service query problem

Suppose an order-history page requires:

- Order status from Order Service
- Payment status from Payment Service
- Shipment status from Fulfilment Service

A SQL join such as this should not be used across private service databases:

```sql
SELECT *
FROM order_service.orders o
JOIN payment_service.payments p
    ON p.order_id = o.id
JOIN shipping_service.shipments s
    ON s.order_id = o.id;
```

That creates runtime, schema, deployment, and ownership coupling.

---

## Option 1: API composition

An aggregator calls the relevant services and combines their responses.

```text
Client
  ↓
Order History Composer
  ├── Order Service
  ├── Payment Service
  └── Fulfilment Service
```

```java
@Service
public class OrderDetailsService {

    private final OrderClient orderClient;
    private final PaymentClient paymentClient;
    private final ShipmentClient shipmentClient;

    public OrderDetails getOrderDetails(String orderId) {
        Order order = orderClient.getOrder(orderId);
        Payment payment = paymentClient.getByOrderId(orderId);
        Shipment shipment = shipmentClient.getByOrderId(orderId);

        return new OrderDetails(order, payment, shipment);
    }
}
```

AWS describes API composition as invoking the services that own the relevant data and combining their results in memory. ([AWS Documentation][2])

### Advantages

- Simple for a small number of services
- Data remains with its owner
- Suitable for low-volume queries

### Disadvantages

- Latency grows with downstream calls
- Partial failures must be handled
- Pagination across multiple sources is difficult
- Large reporting queries perform poorly
- Long synchronous call chains reduce resilience

Parallel calls can reduce latency, but they do not remove the failure and consistency problems.

---

## Option 2: CQRS read model

Create a read-optimized projection containing the fields required by the query.

```text
Order Service ───────┐
Payment Service ─────┼── events → Order History Read Model
Shipping Service ────┘
```

Example read document:

```json
{
  "orderId": "O-100",
  "orderStatus": "CONFIRMED",
  "paymentStatus": "CAPTURED",
  "shipmentStatus": "DISPATCHED"
}
```

The query then reads one store:

```text
Client
  ↓
Order History API
  ↓
Order History Read Database
```

CQRS is especially useful where read and write workloads have different throughput, latency, or data-shape requirements. ([AWS Documentation][3])

### Trade-offs

- Read data is eventually consistent.
- Events must be replayable or projections rebuildable.
- Consumers must be idempotent.
- Projection lag must be monitored.
- Ownership of derived fields must remain clear.

---

## Option 3: Analytical platform

For business intelligence and large reports, stream Analytical platform

For business data into:

- A data warehouse
- A data lake
- A search index
- An analytical database

Do not make production microservices execute large distributed reporting joins during user requests.

### Interview-ready answer

> Database per Service means each service privately owns its data and other services access it through contracts rather than direct table access. For a small real-time query, I use API composition. For frequent or complex cross-service queries, I build a CQRS read model through events or CDC. For analytical reporting, I copy data into an analytical platform. I avoid runtime joins across service databases because they destroy schema and deployment independence.

---

# Q42: How do you implement distributed locking using Redis?

## When might a distributed lock be needed?

Examples include:

- Ensuring only one instance runs a scheduled job
- Coordinating access to a non-transactional external resource
- Preventing two workers from processing the same exclusive task
- Serializing an operation that cannot use a database constraint

Before adding a distributed lock, consider whether the problem can be solved more safely using:

- A database uniqueness constraint
- An atomic SQL update
- Optimistic locking
- Queue partitioning
- Idempotency
- A dedicated workflow owner

A distributed lock adds failure modes and should not be the first solution.

---

## Unsafe implementation

This is unsafe:

```text
SETNX lock:order:100 owner-1
EXPIRE lock:order:100 30
```

The process could crash after `SETNX` but before `EXPIRE`, leaving a lock without a lease.

---

## Safer acquisition

Use one atomic Redis command:

```text
SET lock:order:100 random-owner-token NX PX 30000
```

Meaning:

- `NX`: create only when the key does not exist
- `PX 30000`: expire after 30 seconds
- Random value: identifies the lock owner

Redis documents this owner-token-and-lease approach as t([Redis][4]) locking. citeturn166980search2

## Java acquisition

```java
public Optional<RedisLock> tryAcquire(
        String resource,
        Duration lease
) {
    String key = "lock:" + resource;
    String ownerToken = UUID.randomUUID().toString();

    Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(key, ownerToken, lease);

    if (Boolean.TRUE.equals(acquired)) {
        return Optional.of(
                new RedisLock(key, ownerToken)
        );
    }

    return Optional.empty();
}
```

```java
public record RedisLock(
        String key,
        String ownerToken
) {
}
```

---

## Safe release

Do not run this:

```text
DEL lock:order:100
```

The original lock could have expired and another process might already own a new lock with the same key.

Release only when the stored owner token still matches:

```lua
if redis.call("GET", KEYS[1]) == ARGV[1] then
    return redis.call("DEL", KEYS[1])
else
    return 0
end
```

Java:

```java
private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
        new DefaultRedisScript<>(
                """
                if redis.call('GET', KEYS[1]) == ARGV[1] then
                    return redis.call('DEL', KEYS[1])
                else
                    return 0
                end
                """,
                Long.class
        );

public boolean release(RedisLock lock) {
    Long result = redisTemplate.execute(
            RELEASE_SCRIPT,
            List.of(lock.key()),
            lock.ownerToken()
    );

    return Long.valueOf(1L).equals(result);
}
```

---

## The stale-owner problem

Consider:

```text
Worker A acquires lock with a 30-second lease.
Worker A pauses for 45 seconds.
The lock expires.
Worker B acquires the lock.
Worker A resumes and writes to the resource.
```

Both workers now believe they may proceed.

A lock token alone cannot prevent Worker A from performing a stale write.

## Fencing tokens

Assign every successful lock acquisition a monotonically increasing number:

```text
Worker A receives fencing token 81
Worker B later receives fencing token 82
```

The protected resource records the latest accepted token:

```text
Request with token 82 → accepted
Later with token 82 → accepted
Later request with token 81 → rejected as stale
```

```sql
UPDATE protected_resource
SET value = :value,
    fencing_token = :token
WHERE id = :id
  AND fencing_token < :token;
```

Redis’s distributed-lock documentation explicitly recommends fencin([Redis][4])re operations may take significant time. citeturn166980search2

## What is Redlock?

Redlock attempts to acquire leases on a majority of independent Redis masters and considers the lock successful only when quorum is reached within the lease period.

Even when using Redlock:

- Keep the critical section short.
- Use unique ownership tokens.
- Use bounded acquisition and execution times.
- Implement fencing at the protected resource.
- Make operations idempotent where possible.
- Do not use the lock as a substitute for database constraints.

### Interview-ready answer

> I acquire a Redis lock atomically using `SET key token NX PX lease`, and I release it through a Lua script that deletes the key only when the ownership token matches. Because a paused process can resume after its lease expires, I use monotonically increasing fencing tokens when stale writes could corrupt data. I also prefer database constraints, atomic updates, queue partitioning, or idempotency whenever they can solve the problem without distributed locking.

---

# Q43: Optimistic vs pessimistic locking

## Optimistic locking

Optimistic locking assumes conflicts are uncommon. Transactions proceed without holding a database row lock and verify at update time that the data has not changed.

```java
@Entity
public class Product {

    @Id
    private Long id;

    private int availableQuantity;

    @Version
    private Long version;
}
```

Conceptual SQL:

```sql
UPDATE product
SET available_quantity = :quantity,
    version = version + 1
WHERE id = :id
  AND version = :expectedVersion;
```

If zero rows are updated, another transaction changed the record first.

```text
Transaction A reads version 5.
Transaction B reads version 5.

A updates version 5 → 6.
B tries to update version 5.
B receives an optimistic-lock conflict.
```

Jakarta Persistence defines the versio([Jakarta EE][5])mistic-lock failures. citeturn520189search4turn520189search10

## Handling the conflict

Possible responses:

- Retry a small number of times
- Ask the user to reload
- Return `409 Conflict`
- Recalculate using the latest state
- Merge changes where business rules permit

Retries must be bounded because high contention can otherwise create a retry storm.

---

## Pessimistic locking

Pessimistic locking obtains a database lock before changing the row.

Spring Data example:

```java
public interface ProductRepository
        extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findForUpdate(Long id);
}
```

Usage:

```java
@Transactional
public void reserve(Long productId, int quantity) {
    Product product = productRepository.findForUpdate(productId)
            .orElseThrow(ProductNotFoundException::new);

    product.reserve(quantity);
}
```

This generally maps to database locking such as:

```sql
SELECT *
FROM product
WHERE id = ?
FOR UPDATE;
```

Spring Data JPA supports applying ([Home][6])redeclared CRUD methods through `@Lock`. citeturn520189search1

## Comparison

| Optimistic locking                        | Pessimistic locking                    |
| ----------------------------------------- | -------------------------------------- |
| Does not hold a row lock while processing | Acquires a database lock               |
| Detects conflicts at update or commit     | Prevents conflicting access while held |
| Good for low contention                   | Useful for frequent contention         |
| Better concurrency                        | Can block other transactions           |
| Conflict requires retry or rejection      | Can cause deadlocks and lock timeouts  |
| Suitable for user-edit workflows          | Suitable for short critical operations |

Hibernate describes optimistic locking as proceeding without resource locks and validating before commit, while pessimis([Hibernate Documentation][7]) resource during use. citeturn520189search0turn520189search11

## Often better: atomic SQL

For inventory counters, one statement may be safer than either read-modify-write approach:

```sql
UPDATE inventory
SET available_quantity = available_quantity - :requested
WHERE product_id = :productId
  AND available_quantity >= :requested;
```

```text
Affected rows = 1 → reservation succeeded
Affected rows = 0 → insufficient inventory or missing product
```

### Interview-ready answer

> I use optimistic locking with `@Version` when conflicts are uncommon and retries are acceptable. I use pessimistic locking for short, highly contended operations where allowing two transactions to proceed would be expensive. I keep pessimistic transactions short because they can block and deadlock. For counters such as stock allocation, an atomic conditional SQL update is often simpler and more scalable.

---

# Q44: How do you perform schema migrations with zero downtime?

Use the **expand–migrate–contract** approach.

```text
Expand
→ Migrate application and data
→ Contract
```

Old and new application versions must remain compatible while a rolling deployment is running.

---

## Example: replacing `customer_name`

Suppose the current table contains:

```text
customer_name
```

The new model requires:

```text
first_name
last_name
```

## Phase 1: Expand

Add new nullable fields without removing the old one:

```sql
ALTER TABLE customer
    ADD COLUMN first_name VARCHAR(100);

ALTER TABLE customer
    ADD COLUMN last_name VARCHAR(100);
```

Deploy application code that can work with both representations.

```text
Read:
Prefer new columns.
Fallback to customer_name.

Write:
Write the new columns.
Temporarily maintain the old representation if required.
```

## Phase 2: Backfill

Migrate existing rows in controlled batches:

```sql
UPDATE customer
SET first_name = split_part(customer_name, ' ', 1),
    last_name = substring(
        customer_name
        FROM position(' ' IN customer_name) + 1
    )
WHERE first_name IS NULL
  AND id BETWEEN :startId AND :endId;
```

For large tables:

- Use small batches.
- Commit between batches.
- Monitor lock time and replication lag.
- Make the migration restartable.
- Validate counts and checksums.
- Throttle when production latency increases.

## Phase 3: Switch

Deploy code that reads and writes only the new columns.

Monitor whether anything still accesses the old column.

## Phase 4: Contract

Only after all old application instances and consumers are gone:

```sql
ALTER TABLE customer
    DROP COLUMN customer_name;
```

Shared-database guidance similarly requires destructive changes to wait until curre([AWS Documentation][8])ersions no longer reference the object. citeturn166980search34

---

## Flyway or Liquibase

Migration files should be version controlled:

```text
V1__create_customer.sql
V2__add_name_columns.sql
V3__backfill_name_columns.sql
V4__enforce_new_constraints.sql
V5__drop_customer_name.sql
```

Do not edit a migration that has already run in another environment. Add a new migration.

## Risky operations

Potentially disruptive operations include:

- Adding a non-null column with an immediate full-table default
- Rewriting a column type
- Creating an index that blocks writes
- Renaming a column in the same release as the code change
- Dropping a column before old instances stop
- Rebuilding a very large table
- Holding a transaction while backfilling millions of rows

“Zero downtime” requires testing the exact database engine, version, table size, lock behavior, and production traffic pattern.

### Interview-ready answer

> I use expand–migrate–contract. First I add backward-compatible schema elements. Then I deploy code that supports old and new formats, backfill data in restartable batches, and switch reads and writes to the new structure. Only after every old instance and consumer is retired do I remove the old schema. Migrations are version controlled with Flyway or Liquibase and tested using production-like data volumes and lock behaviour.

---

# Q45: What is the N+1 query problem?

## The problem

The application executes one query for parent entities and then one additional query for each parent’s association.

```java
List<Order> orders = orderRepository.findAll();

for (Order order : orders) {
    System.out.println(order.getCustomer().getName());
}
```

Possible SQL:

```sql
SELECT * FROM orders;
```

Followed by:

```sql
SELECT * FROM customer WHERE id = ?;
SELECT * FROM customer WHERE id = ?;
```

Followed by:

```sql
SELECT *SELECT * FROM customer WHERE id = ?;
```

For 100 orders:

```text
1 order query
+
100 customer queries
=
101 queries
```

## Why it matters

- Database round trips increase.
- Connection-pool usage increases.
- Latency grows with result count.
- The issue may appear only with production-sized data.
- Database CPU may remain high even when each query is individually fast.

---

## Detection

Use several methods:

- Inspect generated SQL in development.
- Enable Hibernate statistics in controlled environments.
- Count SQL statements in integration tests.
- Use datasource instrumentation.
- Inspect APM traces for repeated queries.
- Monitor query frequency and connection-pool usage.
- Test with realistic collection sizes.

Do not rely only on total endpoint duration; a local database can hide N+1 during development.

---

## Fix 1: Fetch join

```java
@Query("""
       select o
       from Order o
       join fetch o.customer
       where o.status = :status
       """)
List<Order> findByStatusWithCustomer(OrderStatus status);
```

This retrieves the order and customer in one query.

## Fix 2: `@EntityGraph`

```java
@EntityGraph(attributePaths = "customer")
List<Order> findByStatus(OrderStatus status);
```

Entity graphs allow fetch plans to be selected for a specific use case rather than([JBoss Documentation][9])n eager. Hibernate supports entity graphs as a dynamic fetching mechanism. citeturn166980search6

## Fix 3: DTO projection

```java
@Query("""
       select new com.example.OrderSummary(
           o.id,
           o.status,
           c.name
       )
       from Order o
       join o.customer c
       where o.status = :status
       """)
List<OrderSummary> findSummaries(OrderStatus status);
```

Use projections when the endpoint requires only a subset of fields.

## Fix 4: Batch fetching

```java
@BatchSize(size = 50)
@ManyToOne(fetch = FetchType.LAZY)
private Customer customer;
```

Hibernate can then ([Hibernate Documentation][10]) associations through one `IN` query instead of one query per association. citeturn166980search7

---

## Pagination warning

Fetching a to-many collection while paginating can:

- Multiply result rows
- Produce incorrect page sizes
- Force in-memory pagination
- Create large Cartesian products

A common approach is:

```text
1. Page the root entity IDs.
2. Fetch the required associations for those IDs.
```

Also avoid fetch-joining multiple to-many collections in one query without examining the Cartesian-product size.

### Interview-ready answer

> N+1 occurs when one query loads parent records and Hibernate issues another query for each association. I detect it using SQL counts, Hibernate statistics, APM traces, and realistic integration tests. Depending on the use case, I fix it with a fetch join, `@EntityGraph`, DTO projection, or batch fetching. I on the use case, I fix do not change everything to `EAGER`, because that can over-fetch and may still produce secondary selects.

---

# Q46: How do you size a database connection pool?

## First correction

This expression:

```text
connections = (CPU cores × 2) + disk spindles
```

is a starting heuristic discussed in HikariCP guidance. It is not Little’s Law, and it does not determine the correct pool size for every microservice. Hi([GitHub][11])g is deployment-specific and that smaller pools can outperform oversized ones. citeturn166980search3turn166980search25

## Why bigger is not always better

Too many connections can cause:

- Database CPU contention
- Context switching
- Lock contention
- Memory consumption
- Cache pressure
- Longer query latency
- More concurrent expensive queries

A connection pool limits pressure on the database; it is not merely a cache of connections.

---

## Step 1: Establish the database connection budget

Suppose the database supports a practical application budget of 200 connections after reserving capacity for:

- Administration
- Replication
- Migrations
- Monitoring
- Emergency access
- Background jobs

If there are ten service replicas:

```text
200 total application connections
÷
10 replicas
=
20 connections per replica
```

This is an upper bound, not necessarily the optimum.

Account for every workload sharing that database:

```text
API replicas
Batch jobs
Reporting jobs
Migration tools
CDC connectors
Administrative tools
```

---

## Step 2: Estimate concurrency using Little’s Law

Little’s Law states:

```text
L = λ × W
```

Where:

- `L` is average concurrent work
- `λ` is throughput
- `W` is average time each operation occupies a connection

Example:

```text
Database operations per second: 500
Average connection hold time:   20 ms
```

```text
Average active connections
= 500 × 0.020
= 10
```

Ten= 500 × 0.020
= 10

````

Ten is an average, not the final maximum. Add measured headroom for burstiness and tail latency, while remaining within the database budget.

Use **connection hold time**, not total HTTP request duration, unless the request holds a connection for its complete lifetime.

---

## Step 3: Load test

Run realistic traffic and observe:

- Active connections
- Idle connections
- Threads waiting for a connection
- Connection acquisition latency
- Acquisition timeouts
- Query P95 and P99
- Database CPU
- Lock waits
- Transaction duration
- Throughput

Increase the pool only while throughput improves without unacceptable database saturation.

## HikariCP settings

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 2000
      max-lifetime: 1700000
````

The values above are examples, not universal recommendations.

Important controls include:

- `maximumPoolSize`: hard concurrency limit
- `minimumIdle`: idle connections retained
- `connectionTimeout`: how long callers wait for a connection
- `maxLifetime`: maximum pooled-connection lifetime
- `idleTimeout`: retirement of excess idle connections

Align connection lifetime with database, proxy, firewall, and credential timeouts.

## Pool starvation

Symptoms include:

- Request latency rises under load.
- Database CPU is not fully utilized.
- Threads wait for connections.
- Acquisition timeouts increase.
- Transactions hold connections while performing network calls.

Before increasing the pool, check for:

- Slow SQL
- Long transactions
- Network calls inside transactions
- Connection leaks
- N+1 queries
- Lock waits
- Unbounded request concurrency

### Interview-ready answer

> I first determine the database’s total connection budget and divide it across every service replica and background workload. I then use Little’s Law—throughput multiplied by connection hold time—to estimate average concurrency, add measured headroom, and validate it under load. I monitor acquisition latency, pending threads, query latency, locks, and database CPU. I do not blindly use `(cores × 2) + spindles` or increase the pool whenever requests become slow.

---

# Q47: How would you implement multi-tenancy?

## Common approaches

| Model                          | Isolation                | Operational cost           |
| ------------------------------ | ------------------------ | -------------------------- |
| Database per tenant            | Highest                  | Highest                    |
| Schema per tenant              | Strong                   | cost Medium to high        |
| Shared tables with `tenant_id` | Lowest logical isolation | Lowest infrastructure cost |

---

## Option 1: Database per tenant

```text
Tenant A → Database A
Tenant B → Database B
Tenant C → Database C
```

### Advantages

- Strong data isolation
- Independent backup and restore
- Independent scaling
- Easier tenant-specific encryption or residency
- Smaller blast radius

### Disadvantages

- Many databases and pools
- Expensive migrations
- Cross-tenant reporting is difficult
- Operational overhead grows with tenant count

Useful for highly regulated or high-value tenants.

---

## Option 2: Schema per tenant

```text
Database
├── tenant_a.orders
├── tenant_b.orders
└── tenant_c.orders
```

### Advantages

- Stronger separation than shared rows
- One database platform
- Tenant backup and migration may be manageable

### Disadvantages

- Every schema must be migrated
- Large tenant counts become difficult
- Dynamic routing and pool handling are more complex
- Cross-schema operations can accidentally break isolation

Do not trust an arbitrary request parameter as a schema name. Resolve the tenant from validated identity and use an allow-listed mapping.

---

## Option 3: Shared tables with `tenant_id`

```sql
CREATE TABLE orders (
    tenant_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    PRIMARY KEY (tenant_id, order_id)
);
```

Every tenant-specific uniqueness constraint should normally include the tenant:

```sql
CREATE UNIQUE INDEX uk_order_external_reference
ON orders(tenant_id, external_reference);
```

Repository query:

```java
Optional<Order> findByTenantIdAndOrderId(
        String tenantId,
        String orderId
);
```

### Main risk

One missing tenant condition can leak data:

```sql
SELECT *
FROM orders
WHERE order_id = :orderId;
```

---

## PostgreSQL Row-Level Security

RLS can enforce isolation in the database:

```sql
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy
ON orders
USING (
    tenant_id = current_setting('app.tenant_id')
)
WITH CHECK (
    tenant_id = current_setting('app.tenant_id')
);
```

Inside a transaction:

```sql
SET LOCAL app.tenant_id = 'TENANT-100';
```

Then normal queries are filtered by the policy.

Postgr([PostgreSQL][12])y policies to row visibility and modification, with permissive policies combined using `OR` and restrictive policies using `AND`. citeturn519252search1

### Pooling caution

Session state can leak between tenants if it is not reset.

Prefer:

```text
Begin transaction
→ SET LOCAL tenant context
→ execute work
→ commit or roll back
```

`SET LOCAL` limits the setting to the current transaction.

Also test:

- Table-owner and privileged-role behaviour
- Background jobs
- Migrations
- Native SQL
- Batch operations
- Cache-key tenant isolation
- Search indexes
- Object storage paths
- Logs and metrics

### Interview-ready answer

> I choose the model based on required isolation, tenant count, regulatory requirements, and operational cost. Database per tenant gives the strongest isolation, schema per tenant is a middle ground, and shared tables are the most efficient but require strict `tenant_id` enforcement. For shared PostgreSQL tables, I use composite tenant-aware keys, application checks, and Row-Level Security as defence in depth. Tenant identity comes from validated authentication, never directly from an untrusted request parameter.

---

# Q48: What is eventual consistency?

Eventual consistency means that after a write, different parts of a distributed system may temporarily show different states, but they converge when processing completes and no new updates occur.

## E-commerce example

```text
1. Order Service creates order as PENDING.
2. Inventory Service reserves stock.
3. Payment Service authorizes payment.
4. Order Service changes order to CONFIRMED.
5. Notification Service sends confirmation.
```

At one moment:

```text
Order database:
PENDING

Inventory database:
RESERVED

Payment database:
No payment yet
```

That intermediate state is expected, not necessarily an error.

## State model

```text
PENDING
├── INVENTORY_RESERVED
├── PAYMENT_PENDING
├── CONFIRMED
├── CANCELLATION_PENDING
├── CANCELLED
└── MANUAL_REVIEW_REQUIRED
```

Avoid representing every incomplete workflow as `FAILED`.

## Required mechanisms

### Local transactions

Each service atomically updates its own data.

### Transactional outbox

The business change and event record are committed together.

```text
Order update
+
Outbox insert
=
One local database transaction
```

### Idempotent consumers

Duplicate events must not duplicate business effects.

### Saga and compensation

```text
Payment fails
→ release inventory
→ cancel order
```

### Reconciliation

A scheduled process finds workflows that remain incomplete beyond the expected time.

```text
Order PAYMENT_PENDING for more than 10 minutes
→ investigate or retry
```

### User-visible status

Return the real state:

```json
{
  "orderId": "O-100",
  "status": "PAYMENT_PENDING"
}
```

The client can poll, receive a WebSocket update, or consume a webhook.

## Important nuance

Sending an email is not normally part of the order’s core consistency boundary.

```text
Order confirmed
+
Email failed
```

should generally result in a confirmed order and an independently retried notification, not cancellation of the order.

### Interview-ready answer

> Eventual consistency allows services to hold temporarily different views while an asynchronous workflow progresses. In checkout, an order may be pending while inventory is reserved and payment is still processing. I model those intermediate states explicitly and use local transactions, outbox events, idempotent consumers, Saga compensation, timeouts, and reconciliation so the system eventually reaches a valid terminal state.

---

# Q49: How do you synchronize a CQRS read model using Debezium CDC?

## Architecture

```text
PostgreSQL write database
        ↓ WAL / logical decoding
Debezium PostgreSQL Connector
        ↓
Kafka topics
        ↓
Read-model projector
        ↓
Elasticsearch / Redis / read database
```

The Debezium PostgreSQL connector initially takes a consistent ([Debezium][13])es committed row-level inserts, updates, and deletes from PostgreSQL logical decoding, publishing change records to Kafka topics. citeturn519252search2

---

## Example flow

### 1. Write-side transaction

```sql
UPDATE orders
SET status = 'CONFIRMED'
WHERE order_id = 'O-100';
```

### 2. PostgreSQL records the change in WAL

### 3. Debezium reads the change

Conceptual event:

```json
{
  "before": {
    "order_id": "O-100",
    "status": "PAYMENT_PENDING"
  },
  "after": {
    "order_id": "O-100",
    "status": "CONFIRMED"
  },
  "op": "u"
}
```

### 4. Projector updates the read model

```java
public void handle(OrderChange change) {
    if (change.isDelete()) {
        readRepository.deleteById(change.orderId());
        return;
    }

    readRepository.upsert(
            new OrderView(
                    change.orderId(),
                    change.status(),
                    change.updated(
                    change.orderId(),
                    change.status(),
                    change.updatedAt()
            )
    );
}
```

Use upserts so replaying the same change is safe.

---

## Important design concerns

### Idempotency

Kafka and connector processing can redeliver records.

The projector should use:

- Upserts
- Source position checks
- Event IDs where available
- Version or timestamp comparisons
- Idempotent deletion

### Ordering

Preserve ordering per aggregate key:

```text
Kafka key = orderId
```

This helps changes for one order remain in one partition.

Global ordering across all orders is usually unnecessary and expensive.

### Projection lag

Measure:

- Connector lag
- Kafka consumer lag
- Age of the newest projected record
- Failed projection count([Debezium][13]) replication-slot health

Debezium’s PostgreSQL connector uses a replication slot and resumes from its recorded WAL position, while heartbeat configuration can help prevent unnecessary WAL retention in low-event scenarios. citeturn519252search2turn519252search8

### Rebuilding

A read model should be disposable and rebuildable.

Possible approaches:

- Re-run a Debezium snapshot
- Replay retained Kafka topics
- Run a backfi([Debezium][13])mental events
- Build a new index and atomically switch aliases

### Schema changes

PostgreSQL logical decoding does not emit DDL changes through this connector, so database migrations and consumer compatibility must be managed separately. citeturn519252search2

---

## Direct CDC vs outbox

### Direct table CDC

```text
orders table change
→ Debezium row event
→ read model
```

Advantages:

- Minimal application changes
- Good for replication and projections

Risk:

- Consumers become coupled to database columns and table structure.

### Outbox CDC

```text
Business transaction
→ outbox row containing OrderConfirmed
→ Debezium
→ Kafka
```

Advantages:

- Publishes domain-oriented contracts
- Hides internal table structure
- Easier event versioning

For externally consumed business events, I generally prefer the outbox approach. Direct CDC remains useful for internal read-model replication.

### Interview-ready answer

> Debezium reads committed changes from the database transaction log and publishes them to Kafka. A projector consumes the records and idempotently upserts a denormalized read model such as Elasticsearch or Redis. I preserve per-aggregate ordering, monitor connector and consumer lag, plan projection rebuilds, and evolve schemas compatibly. For stable domain contracts, I prefer Debezium capturing an outbox table rather than exposing raw business-table changes directly.

---

# Q50: How do you rotate database secrets without downtime?

## Risk of a simple password replacement

Suppose every application uses one database username:

```text
app_user / old_password
```

Changing its password immediately can create:

```text
Existing pooled connections → may continue temporarily
New connections             → authentication failures
Restarting instances         → cannot connect
```

This causes a partial and difficult-to-diagnose outage.

---

## Strategy 1: Dual credentials

### Step 1: Create a second credential

```text
app_user_a → currently active
app_user_b → newly created
```

Grant `app_user_b` the same minimum required privileges.

### Step 2: Update the secret manager

Publish credential B as the new application credential.

### Step 3: Roll out gradually

```text
Old pods → use credential A
New pods → use credential B
```

Verify:

- New connections succeed.
- Application health remains normal.
- Queries have the expected permissions.
- Audit logs identify credential B usage.

### Step 4: Drain old connection pools

Wait until old pods and their connections are gone.

### Step 5: Revoke credential A

Only revoke A after confirming that nothing still uses it.

```text
Create B
→ deploy B
→ drain A
→ revoke A
```

This is similar to a blue-green credential rollout.

---

## Strategy 2: Vault dynamic credentials

The application authenticates to Vault and receives a temporary database username and password wit([HashiCorp Developer][14])enerates temporary DB credential
Database

````

Vault’s database secrets engine supports dynamically generated credentials with configurable TTLs, as well as static roles with automatic password rotation. citeturn603139search2turn603139search5turn603139search17

A dynamic role might issue:

```text
Username: v-app-order-98fa
TTL:      1 hour
````

The application must:

- Renew the lease or obtain new credentials.
- Build new connections using the new credential.
- Retire old pooled connections safely.
- Handle Vault unavailability.
- Avoid logging generated credentials.

---

## Connection-pool coordination

Credential rotation and pooling must be designed together.

Possible approach:

```text
1. Obtain new credential.
2. Validate it with a test connection.
3. Create or refresh the datasource.
4. Send new work to the refreshed pool.
5. Let old in-flight transactions finish.
6. Close the old pool.
7. Revoke the previous credential.
```

Align:

- Credential TTL
- Pool `maxLifetime`
- Lease-renewal timing
- Database session-expiry behaviour
- Rolling-deployment durat([HashiCorp Developer][14])t automatically updates an already-created `DataSource`.

## Static Vault rotation

Vault static roles map one Vault role to one database username and can rotate that user’s password automatically on a configured schedule. citeturn603139search2turn603139search8

The application must still know when to refresh its pool or credentials; rotating the database password alone does not reconfigure existing application objects.

---

## Failure handling

### New credential does not work

- Keep the old credential active.
- Stop the rollout.
- Restore the previous secret reference.
- Investigate grants and network access.

### Vault is temporarily unavailable

Decide whether the application:

- Continues using a still-valid lease
- Renews from a local agent
- Refuses to create new connections
- Enters degraded mode

### Rotation occurs during long transactions

Do not forcefully terminate valid transactions unless security policy requires it. Drain them within a bounded period and alert when old connections remain beyond the expected window.

## Monitoring

Track:

- Authentication failures
- Pool connection-creation failures
- Current credential version
- Credential lease expiry
- Renewal failures
- Old-credential usage
- Vault latency and availability
- Pool active, idle, and pending connections

### Interview-ready answer

> I avoid changing one shared password in place. For static credentials, I use dual users: create credential B, roll applications to it, drain pools using credential A, and then revoke A. For stronger automation, Vault can issue dynamic short-lived database credentials or rotate static roles. The application must refresh or replace its connection pool before the old lease expires, and every rotation needs validation, observability, rollback, and a bounded overlap period.

---

# Quick Interview Cheat Sheet

| Topic                   | Key answer                                                              |
| ----------------------- | ----------------------------------------------------------------------- |
| Database per Service    | Private data ownership; no uncontrolled cross-service table access      |
| Cross-service query     | API composition for simple queries; CQRS read model for complex queries |
| Redis lock              | `SET NX PX` plus unique owner token and compare-and-delete release      |
| Fencing                 | Reject operations from stale lock holders                               |
| Optimistic locking      | `@Version`; detect conflict and retry or reject                         |
| Pessimistic locking     | Database row lock; use for short high-contention operations             |
| Zero-downtime migration | Expand, migrate, contract                                               |
| N+1                     | One parent query plus one association query per row                     |
| N+1 fixes               | Fetch join, entity graph, projection, batch fetch                       |
| Pool sizing             | Database budget plus measured concurrency and load testing              |
| Multi-tenancy           | Database, schema, or tenan column with RLS                              |
| Eventual consistency    | Explicit intermediate states, Saga, outbox, reconciliation              |
| Debezium CQRS           | WAL/binlog → Kafka → idempotent read-model projector                    |
| Secret rotation         | Dual credentials or leased dynamic credentials with pool refresh        |

[1]: https://docs.aws.amazon.com/prescriptive-guidance/latest/modernization-data-persistence/database-per-service.html?utm_source=chatgpt.com "Database-per-service pattern - AWS Prescriptive Guidance"
[2]: https://docs.aws.amazon.com/prescriptive-guidance/latest/modernization-data-persistence/api-composition.html?utm_source=chatgpt.com "API composition pattern - AWS Prescriptive Guidance"
[3]: https://docs.aws.amazon.com/prescriptive-guidance/latest/modernization-data-persistence/cqrs-pattern.html?utm_source=chatgpt.com "CQRS pattern - AWS Prescriptive Guidance"
[4]: https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/?utm_source=chatgpt.com "Distributed Locks with Redis | Docs"
[5]: https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html?utm_source=chatgpt.com "Jakarta Persistence"
[6]: https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html?utm_source=chatgpt.com "Locking :: Spring Data JPA"
[7]: https://docs.hibernate.org/orm/5.2/userguide/html_single/chapters/locking/Locking.html?utm_source=chatgpt.com "Locking"
[8]: https://docs.aws.amazon.com/prescriptive-guidance/latest/modernization-data-persistence/shared-database.html?utm_source=chatgpt.com "Shared-database-per-service pattern"
[9]: https://docs.jboss.org/hibernate/orm/6.5/userguide/html_single/Hibernate_User_Guide.html?utm_source=chatgpt.com "Hibernate ORM User Guide"
[10]: https://docs.hibernate.org/orm/6.5/javadocs/org/hibernate/annotations/BatchSize.html?utm_source=chatgpt.com "BatchSize (Hibernate Javadocs) - Index of /"
[11]: https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing?utm_source=chatgpt.com "About Pool Sizing · brettwooldridge/HikariCP Wiki"
[12]: https://www.postgresql.org/docs/current/ddl-rowsecurity.html?utm_source=chatgpt.com "Documentation: 18: 5.9. Row Security Policies"
[13]: https://debezium.io/documentation/reference/stable/connectors/postgresql.html?utm_source=chatgpt.com "Debezium connector for PostgreSQL"
[14]: https://developer.hashicorp.com/vault/docs/secrets/databases?utm_source=chatgpt.com "Database secrets engine | Vault"
