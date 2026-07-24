# Advanced Questions — Spring Transactions, Spring Data JPA, Hibernate, and Databases

The repeated questions have been merged into a cleaner interview-ready structure.

---

# Part 1: Spring `@Transactional`

## Q1: How does `@Transactional` work internally?

Spring normally implements declarative transactions using an **AOP proxy** around a Spring-managed bean.

```text
External caller
      ↓
Spring proxy
      ↓
TransactionInterceptor
      ↓
Select PlatformTransactionManager
      ↓
Start, join, or suspend transaction
      ↓
Invoke target method
      ↓
Commit or roll back
```

Example:

```java
@Service
public class TransferService {

    private final AccountRepository accountRepository;

    public TransferService(
            AccountRepository accountRepository
    ) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void transfer(
            long sourceId,
            long destinationId,
            BigDecimal amount
    ) {
        Account source = accountRepository.findById(sourceId)
                .orElseThrow();

        Account destination = accountRepository.findById(destinationId)
                .orElseThrow();

        source.debit(amount);
        destination.credit(amount);
    }
}
```

Conceptually, the proxy behaves like:

```java
TransactionStatus transaction =
        transactionManager.getTransaction(definition);

try {
    target.transfer(sourceId, destinationId, amount);

    transactionManager.commit(transaction);
} catch (Throwable exception) {
    transactionManager.rollback(transaction);
    throw exception;
}
```

The actual implementation uses transaction interceptors, transaction attributes, and an appropriate transaction manager, such as `JpaTransactionManager`, `JdbcTransactionManager`, or a reactive transaction manager. Spring’s default mode is proxy-based, so only calls entering through the proxy are intercepted. ([Home][1])

### Interview answer

> Spring creates an AOP proxy around the transactional bean. When another bean calls an annotated method, the proxy reads the transaction metadata, asks the transaction manager to start or join a transaction, invokes the target method, and commits or rolls back according to the result.

---

## Q2: Why does `@Transactional` sometimes not work?

### 1. Self-invocation

```java
@Service
public class PaymentService {

    public void process() {
        savePayment();
    }

    @Transactional
    public void savePayment() {
        // Database operations
    }
}
```

The internal call is equivalent to:

```java
this.savePayment();
```

It does not pass through the Spring proxy, so `savePayment()` does not receive a new transactional interception. ([Home][1])

### 2. The object was created manually

```java
PaymentService service = new PaymentService();
```

This instance is not managed by Spring and has no transactional proxy.

### 3. Private method

```java
@Transactional
private void savePayment() {
}
```

A private method cannot normally be intercepted by Spring’s proxy-based method interception.

### 4. Calling it during initialization

```java
@PostConstruct
void initialize() {
    savePayment();
}
```

At initialization time, the proxy may not yet be fully available, and the call is also self-invocation. Spring explicitly advises against relying on transactional interception from initialization callbacks. ([Home][1])

### 5. Exception is caught and suppressed

```java
@Transactional
public void process() {
    try {
        repository.save(...);
    } catch (RuntimeException exception) {
        log.error("Operation failed", exception);
    }
}
```

Since the exception does not leave the transactional method, the proxy may see normal completion and commit.

### 6. Checked exception without a rollback rule

```java
@Transactional
public void process() throws IOException {
    throw new IOException("Failed");
}
```

By default, checked exceptions do not trigger rollback.

### 7. Work happens on another thread

```java
@Async
@Transactional
public void processAsync() {
}
```

Transactions are associated with their execution context. A transaction on the caller thread does not automatically propagate to an executor thread.

### 8. Wrong transaction manager

An application with multiple databases may use the wrong manager unless one is selected explicitly:

```java
@Transactional(transactionManager = "ordersTransactionManager")
public void saveOrder() {
}
```

---

## Q3: What exceptions cause rollback?

The traditional Spring default is:

| Exception           | Default result                     |
| ------------------- | ---------------------------------- |
| `RuntimeException`  | Rollback                           |
| `Error`             | Rollback                           |
| Checked `Exception` | Commit unless configured otherwise |

Spring allows custom rules:

```java
@Transactional(rollbackFor = Exception.class)
public void importFile() throws Exception {
}
```

```java
@Transactional(noRollbackFor = BusinessWarningException.class)
public void process() {
}
```

Spring Framework also supports changing the application-wide default rollback behavior in newer versions, but method-specific rules still take precedence. ([Home][1])

### Common mistake

```java
@Transactional
public void process() {
    try {
        performDatabaseWork();
    } catch (RuntimeException exception) {
        // Swallowed
    }
}
```

The proxy cannot roll back based on an exception it never receives.

A transaction can also be marked manually:

```java
TransactionAspectSupport
        .currentTransactionStatus()
        .setRollbackOnly();
```

This should be used sparingly because it couples business code to Spring transaction infrastructure.

---

# Transaction Propagation

## Q4: What are the transaction propagation types?

Propagation defines how a transactional method behaves when a transaction may already exist.

| Propagation     | Existing transaction                   | No existing transaction |
| --------------- | -------------------------------------- | ----------------------- |
| `REQUIRED`      | Join it                                | Start one               |
| `REQUIRES_NEW`  | Suspend it and start another           | Start one               |
| `SUPPORTS`      | Join it                                | Run without one         |
| `NOT_SUPPORTED` | Suspend it                             | Run without one         |
| `MANDATORY`     | Join it                                | Throw exception         |
| `NEVER`         | Throw exception                        | Run without one         |
| `NESTED`        | Create nested savepoint when supported | Behave like `REQUIRED`  |

Spring documents `REQUIRED` as the common default and distinguishes the logical transactional scope from the physical transaction underneath it. ([Home][2])

---

## Q5: What is the difference between `REQUIRED` and `REQUIRES_NEW`?

### `REQUIRED`

```java
@Transactional
public void createOrder() {
    inventoryService.reserve();
}
```

If `reserve()` is also `REQUIRED`, both methods normally share one physical transaction.

```text
createOrder transaction
└── reserve joins same transaction
```

If the inner operation marks the transaction rollback-only, the outer operation cannot safely commit it.

---

### `REQUIRES_NEW`

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveAuditLog() {
}
```

It suspends the current transaction and starts an independent transaction.

```text
Outer transaction suspended
        ↓
New audit transaction starts
        ↓
Audit commits or rolls back
        ↓
Outer transaction resumes
```

Use cases can include:

- Independent audit recording
- Independent retry-attempt records
- Work that must commit despite outer rollback

### Important risk

`REQUIRES_NEW` usually requires a separate physical database connection while the outer transaction keeps its connection.

```text
10 request threads
Each holds outer connection
Each requests another connection
Pool size = 10

Result:
Connection-pool exhaustion or deadlock-like waiting
```

Use it deliberately rather than as a general method-isolation mechanism. Spring warns that `REQUIRES_NEW` can require additional connection-pool capacity. ([Home][3])

---

## Q6: What happens with `NESTED` transactions?

`NESTED` usually creates a database savepoint inside the same physical transaction when the transaction manager and database support savepoints.

```text
Outer transaction
├── work A
├── savepoint
├── nested work B
└── work C
```

If nested work fails:

```text
Roll back to savepoint
```

The earlier work may remain in the outer transaction.

Example:

```java
@Transactional(propagation = Propagation.NESTED)
public void processItem(Item item) {
}
```

Important points:

- It is not the same as `REQUIRES_NEW`.
- It normally uses the same physical transaction.
- Support depends on the transaction manager and underlying resource.
- JPA behavior can differ from direct JDBC savepoint support.

---

# Transaction Isolation

## Q7: What is transaction isolation?

Isolation controls how much one transaction can observe changes made by concurrent transactions.

Spring supports:

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void updateInventory() {
}
```

`Isolation.DEFAULT` delegates to the database’s configured default. Isolation settings apply when a new transaction is actually started; a method joining an existing transaction normally inherits the outer transaction’s characteristics. ([Home][1])

---

## Q8: Explain the isolation levels

| Isolation level    | Dirty reads | Non-repeatable reads |             Phantom reads |
| ------------------ | ----------: | -------------------: | ------------------------: |
| `READ_UNCOMMITTED` |    Possible |             Possible |                  Possible |
| `READ_COMMITTED`   |   Prevented |             Possible |                  Possible |
| `REPEATABLE_READ`  |   Prevented |            Prevented | Standard permits phantoms |
| `SERIALIZABLE`     |   Prevented |            Prevented |                 Prevented |

Actual behavior can be stronger than the SQL-standard minimum depending on the database’s MVCC and locking implementation.

### Dirty read

Transaction B reads uncommitted data from transaction A.

```text
A updates balance to 0
B reads 0
A rolls back
```

B observed a value that never committed.

### Non-repeatable read

The same row returns different values within one transaction.

```text
A reads balance = 100
B commits balance = 50
A reads balance = 50
```

### Phantom read

Repeating a range query returns a different set of rows.

```sql
SELECT *
FROM orders
WHERE status = 'PENDING';
```

Another transaction inserts a matching order, and the query now returns an additional row.

### Serializable

Serializable attempts to produce results equivalent to transactions running one at a time. It provides stronger consistency but may introduce:

- Blocking
- Deadlocks
- Serialization failures
- Lower throughput
- Required transaction retries

---

# Transaction Design

## Q9: Why should remote API calls be avoided inside database transactions?

Problematic:

```java
@Transactional
public void createOrder() {
    orderRepository.save(...);

    paymentClient.charge(...); // Could take several seconds

    inventoryRepository.update(...);
}
```

While the remote call is running, the transaction may hold:

- A database connection
- Row locks
- Persistence-context state
- Other database resources

If the remote system is slow:

```text
Long transaction
→ longer lock duration
→ pool exhaustion
→ higher contention
→ more deadlocks
→ reduced throughput
```

There is also no atomic guarantee across the database and HTTP service:

```text
Payment succeeds
→ network response is lost
→ database transaction rolls back
```

Better approaches include:

- Keep the local transaction short.
- Commit business state and an outbox record together.
- Perform remote work asynchronously.
- Use idempotency.
- Use Saga compensation when workflows span services.

---

## Q10: What is transaction synchronization?

Transaction synchronization allows callbacks to execute at transaction lifecycle boundaries.

Conceptually:

```text
beforeCommit
beforeCompletion
afterCommit
afterCompletion
```

Common uses include:

- Publishing work only after a successful commit
- Releasing transaction-bound resources
- Clearing transaction-specific context
- Triggering post-commit processing

A higher-level Spring approach is:

```java
@TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
)
public void handle(OrderCreatedEvent event) {
}
```

Important limitation:

An in-memory `AFTER_COMMIT` event is not durable. If the process crashes after the database commit but before external publication, the event can be lost. Use a transactional outbox when durable cross-process delivery is required.

---

## Q11: How do you debug transaction issues?

### Enable transaction logs temporarily

```properties
logging.level.org.springframework.transaction=TRACE
logging.level.org.springframework.orm.jpa=DEBUG
```

Inspect:

- Whether a transaction was created
- Which transaction manager was selected
- Whether a method joined an existing transaction
- Whether the transaction was marked rollback-only
- Commit and rollback activity

### Verify proxying

```java
log.info("Class: {}", service.getClass());
```

A proxied class may appear as a JDK proxy or generated subclass.

### Check common causes

- Self-invocation
- Private method
- Manually constructed object
- Exception swallowed
- Wrong rollback rule
- Wrong transaction manager
- Async thread
- Early flush error
- Constraint error appearing only at commit
- `REQUIRES_NEW` exhausting the pool

### Force flush during diagnosis

```java
repository.save(entity);
entityManager.flush();
```

This can reveal database errors at a known line rather than at transaction commit.

---

## Q12: What is the difference between `@Transactional` in a monolith and in microservices?

The annotation itself still controls a transaction managed by one local transaction manager.

### Monolith

A monolith may have several modules sharing one database:

```text
Order module
Inventory module
Payment module
        ↓
One database transaction
```

A local transaction can update multiple tables atomically when they use the same transactional resource.

### Microservices

Independent services usually have independent databases:

```text
Order service DB
Inventory service DB
Payment service DB
```

A transaction in the order service does not automatically include the other databases, HTTP requests, or Kafka consumers.

Use:

- Saga orchestration or choreography
- Transactional outbox
- Idempotent consumers
- Compensation
- Eventual consistency
- Reconciliation

### Interview answer

> In a monolith, one Spring transaction can often cover multiple modules because they share a database and transaction manager. In microservices, `@Transactional` protects only the local resource. Cross-service consistency requires workflow patterns such as Saga, outbox, idempotency, and compensation rather than a normal local annotation.

---

# Part 2: Spring Data JPA and Hibernate

## Q13: What is Spring Data JPA, and how is it different from Hibernate?

### Jakarta Persistence

Jakarta Persistence is the standard API and specification for ORM concepts such as:

- `EntityManager`
- Entity mappings
- Relationships
- Persistence context
- JPQL
- Locking
- Entity lifecycle

### Hibernate

Hibernate is a persistence provider and ORM implementation. It implements Jakarta Persistence and provides additional Hibernate-specific features.

### Spring Data JPA

Spring Data JPA adds repository abstractions on top of Jakarta Persistence.

```text
Application repository interface
        ↓
Spring Data JPA
        ↓
Jakarta Persistence EntityManager
        ↓
Hibernate or another provider
        ↓
JDBC
        ↓
Database
```

Spring Data JPA generates repository implementations, derives queries from method names, and supports projections, specifications, paging, auditing, and repository integration. ([Home][4])

---

## Q14: What repository interfaces are available?

### `Repository`

Marker interface with no required CRUD methods.

```java
public interface ProductRepository
        extends Repository<Product, Long> {
}
```

### `CrudRepository`

Provides general CRUD operations:

```java
save(...)
findById(...)
findAll()
delete(...)
count()
```

### `ListCrudRepository`

Similar to `CrudRepository`, but collection-returning methods use `List`.

### `PagingAndSortingRepository`

Adds:

```java
findAll(Sort sort)
findAll(Pageable pageable)
```

In modern Spring Data, sorting repository interfaces do not themselves extend the CRUD interfaces, so a custom repository may need both when using the generic hierarchy. ([Home][5])

### `JpaRepository`

The common JPA-specific choice:

```java
public interface OrderRepository
        extends JpaRepository<OrderEntity, Long> {
}
```

It supplies CRUD, paging, sorting, and JPA-oriented functionality such as flushing and batch-oriented methods, subject to the Spring Data version.

---

## Q15: What is the difference between JPQL and native SQL?

### JPQL

JPQL operates on entity names and Java attributes.

```java
@Query("""
    select o
    from OrderEntity o
    where o.status = :status
    """)
List<OrderEntity> findByStatus(
        @Param("status") OrderStatus status
);
```

Advantages:

- Entity-oriented
- Generally portable
- Supports relationship navigation
- Validated against mappings
- Works with entity graphs and projections

### Native query

Native SQL operates on tables and columns.

```java
@Query(
        value = """
            SELECT *
            FROM orders
            WHERE status = :status
            """,
        nativeQuery = true
)
List<OrderEntity> findByStatusNative(
        @Param("status") String status
);
```

Use native queries for:

- Vendor-specific syntax
- Database functions
- Specialized locking
- Complex CTEs
- Window functions
- Performance-tuned reporting
- Database hints

Native SQL offers greater control but increases database coupling.

---

## Q16: What are JPA relationships?

The main relationship types are:

```java
@OneToOne
@OneToMany
@ManyToOne
@ManyToMany
```

Example:

```java
@Entity
public class Department {

    @OneToMany(mappedBy = "department")
    private List<Employee> employees =
            new ArrayList<>();
}
```

```java
@Entity
public class Employee {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
}
```

For bidirectional relationships:

- One side owns the foreign-key mapping.
- The other side uses `mappedBy`.
- Both sides of the Java object graph should be synchronized.

For many-to-many relationships with additional attributes, prefer a join entity instead of a direct `@ManyToMany`.

---

## Q17: What is the N+1 query problem?

N+1 happens when:

```text
1 query loads N parent entities
+
N separate queries load their related objects
```

Example:

```text
SELECT * FROM orders;

SELECT * FROM line_item WHERE order_id = 1;
SELECT * FROM line_item WHERE order_id = 2;
SELECT * FROM line_item WHERE order_id = 3;
...
```

Fixes include:

### Fetch join

```java
@Query("""
    select distinct o
    from OrderEntity o
    left join fetch o.lineItems
    where o.status = :status
    """)
List<OrderEntity> findWithItems(
        @Param("status") OrderStatus status
);
```

### Entity graph

```java
@EntityGraph(attributePaths = "lineItems")
List<OrderEntity> findByStatus(OrderStatus status);
```

### DTO projection

```java
@Query("""
    select new com.example.OrderSummary(
        o.id,
        o.status,
        o.total
    )
    from OrderEntity o
    """)
List<OrderSummary> findSummaries();
```

### Batch fetching

```java
@BatchSize(size = 25)
@OneToMany(mappedBy = "order")
private List<LineItemEntity> lineItems;
```

Detect it using SQL traces, Hibernate statistics, APM database spans, and query-count integration tests.

---

## Q18: What is the difference between `persist()`, `merge()`, and `save()`?

### `EntityManager.persist()`

Used for a new transient entity.

```java
OrderEntity order = new OrderEntity();

entityManager.persist(order);
```

The same object becomes managed.

```text
Transient object
→ persist
→ same object becomes managed
```

### `EntityManager.merge()`

Copies state from a detached or new instance into a managed instance.

```java
OrderEntity managed =
        entityManager.merge(detachedOrder);
```

Important:

```text
The returned object is managed.
The original detached object remains detached.
```

### Spring Data `save()`

```java
OrderEntity saved = repository.save(order);
```

Spring Data JPA determines whether the entity is new:

- New entity → `persist()`
- Existing entity → `merge()`

The result should be used because the merge path may return a different managed instance. ([Home][6])

### Managed entity update

```java
@Transactional
public void confirm(long id) {
    OrderEntity order = repository.findById(id)
            .orElseThrow();

    order.setStatus(OrderStatus.CONFIRMED);
}
```

An additional `save()` is generally unnecessary because dirty checking detects the change.

---

## Q19: What are Spring Data projections?

Projections retrieve only the data required by a read use case.

### Interface projection

```java
public interface OrderSummaryView {

    Long getId();

    OrderStatus getStatus();

    BigDecimal getTotal();
}
```

```java
List<OrderSummaryView> findByStatus(
        OrderStatus status
);
```

### DTO projection

```java
public record OrderSummary(
        Long id,
        OrderStatus status,
        BigDecimal total
) {
}
```

```java
@Query("""
    select new com.example.OrderSummary(
        o.id,
        o.status,
        o.total
    )
    from OrderEntity o
    where o.status = :status
    """)
List<OrderSummary> findSummaries(
        @Param("status") OrderStatus status
);
```

Advantages:

- Fewer selected columns
- Less persistence-context overhead
- Explicit API response model
- Reduced accidental lazy loading
- Better suitability for read endpoints

DTO projections need a suitable constructor or record canonical constructor. ([Home][7])

---

# Fetching and Entity Lifecycle

## Q20: What is the difference between `LAZY` and `EAGER` fetching?

### Lazy

The association is loaded when accessed.

```java
@ManyToOne(fetch = FetchType.LAZY)
private Customer customer;
```

Risks:

- N+1 queries
- Hidden database calls
- `LazyInitializationException`

### Eager

The provider must make the association available when the entity is loaded.

```java
@ManyToOne(fetch = FetchType.EAGER)
private Customer customer;
```

Risks:

- Unnecessary loading
- Large joins
- Secondary queries
- High memory usage
- Difficult-to-control SQL

Traditional JPA defaults are commonly:

| Relationship  | Default |
| ------------- | ------- |
| `@OneToMany`  | `LAZY`  |
| `@ManyToMany` | `LAZY`  |
| `@ManyToOne`  | `EAGER` |
| `@OneToOne`   | `EAGER` |

Prefer explicit use-case-specific fetch plans instead of changing everything to eager.

---

## Q21: Why does `LazyInitializationException` occur in production but not development?

Typical sequence:

```text
Transaction starts
→ entity loaded
→ transaction ends
→ controller/Jackson accesses lazy relationship
→ no active persistence context
→ LazyInitializationException
```

Why it may differ by environment:

- Development enables Open EntityManager in View.
- Test code runs inside a transaction.
- Development data already initialized the relationship.
- Production follows a different serialization path.
- Different mappings or configuration are deployed.
- Production uses pagination or projections differently.

Preferred solution:

```java
@Transactional(readOnly = true)
public OrderResponse find(long id) {
    OrderEntity order = repository.findWithItemsById(id)
            .orElseThrow();

    return mapper.toResponse(order);
}
```

Fetch the required data in the service layer and return a DTO.

Avoid fixing it by:

- Making every relationship eager
- Keeping transactions open around the entire HTTP request
- Calling getters merely to force initialization
- Returning JPA entities directly from controllers

---

## Q22: What are the entity lifecycle states?

| State              | Meaning                               |
| ------------------ | ------------------------------------- |
| Transient          | New object, not managed               |
| Managed/Persistent | Tracked by the persistence context    |
| Detached           | Previously managed, no longer tracked |
| Removed            | Marked for deletion                   |

Example:

```java
OrderEntity order = new OrderEntity();
// Transient

entityManager.persist(order);
// Managed

entityManager.detach(order);
// Detached

OrderEntity managed = entityManager.merge(order);
// Managed copy returned

entityManager.remove(managed);
// Removed
```

A persistence context maintains one managed entity instance for a given persistent identity. ([Jakarta EE][8])

---

## Q23: What is dirty checking?

Hibernate tracks managed entity state and detects changes.

```java
@Transactional
public void updateStatus(long id) {
    OrderEntity order = repository.findById(id)
            .orElseThrow();

    order.setStatus(OrderStatus.CONFIRMED);
}
```

At flush time, Hibernate detects the change and generates an update.

```text
Entity loaded
→ tracked by persistence context
→ entity modified
→ flush
→ UPDATE generated
```

Dirty checking applies to managed entities. Detached entities are not automatically tracked. Hibernate’s persistence context is also the first-level cache. ([Hibernate Documentation][9])

---

## Q24: What is the difference between Hibernate `get()` and `load()`?

Historically:

### `get()`

- Usually loads the entity immediately.
- Returns `null` when no row exists.
- Similar to JPA `EntityManager.find()`.

### `load()`

- Historically returned a proxy/reference when possible.
- Could delay database access.
- Failure might occur when the proxy was initialized.
- Commonly threw an object-not-found exception when the entity did not exist.

Modern Hibernate code should generally use:

```java
session.get(Entity.class, id);
```

or:

```java
session.getReference(Entity.class, id);
```

JPA equivalents are:

```java
entityManager.find(Entity.class, id);
```

```java
entityManager.getReference(Entity.class, id);
```

Use a reference when you only need an entity identity, such as assigning an existing foreign-key relationship without reading the entity immediately.

---

# Locking and Concurrent Updates

## Q25: What is optimistic locking?

Optimistic locking assumes conflicts are relatively uncommon.

```java
@Entity
public class Product {

    @Id
    private Long id;

    @Version
    private Long version;

    private int stock;
}
```

Conceptual update:

```sql
UPDATE product
SET stock = ?,
    version = version + 1
WHERE id = ?
  AND version = ?;
```

If another transaction updated the row first, zero rows are updated and an optimistic-lock exception is raised.

Use it when:

- Conflicts are uncommon
- Blocking should be minimized
- Requests may involve user think time
- Retrying or returning a conflict is acceptable

Jakarta Persistence uses version attributes to perform optimistic conflict detection. ([Jakarta EE][10])

---

## Q26: What is pessimistic locking?

Pessimistic locking obtains a database lock during the transaction.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    select p
    from Product p
    where p.id = :id
    """)
Optional<Product> findForUpdate(
        @Param("id") Long id
);
```

This may produce behavior similar to:

```sql
SELECT *
FROM product
WHERE id = ?
FOR UPDATE;
```

Use it when:

- Conflicts are frequent
- Critical sections are short
- Retrying would be expensive
- Immediate serialization is required

Risks:

- Blocking
- Deadlocks
- Lock timeouts
- Reduced concurrency
- Connection retention

---

## Q27: Two transactions overwrite each other. How do you prevent it?

Lost-update scenario:

```text
Transaction A reads stock = 10
Transaction B reads stock = 10

A writes stock = 8
B writes stock = 7

A's update is lost
```

### Solution 1: Optimistic locking

```java
@Version
private Long version;
```

Return a conflict or retry when the version is stale.

### Solution 2: Atomic database update

For stock reservation:

```sql
UPDATE product
SET stock = stock - :quantity
WHERE id = :id
  AND stock >= :quantity;
```

Check the affected row count:

```text
1 row → reservation succeeded
0 rows → insufficient stock or missing product
```

This is often better than reading, modifying, and writing stock in Java.

### Solution 3: Pessimistic lock

Lock the row, read it, validate it, and update it inside a short transaction.

### Recommended decision

| Situation                               | Technique                             |
| --------------------------------------- | ------------------------------------- |
| Rare conflict                           | Optimistic locking                    |
| Simple counter update                   | Atomic SQL                            |
| Frequent contention and short operation | Pessimistic lock                      |
| Cross-service inventory                 | Reservation workflow plus idempotency |

---

# Hibernate Caching

## Q28: What is the first-level cache?

The first-level cache is the persistence context associated with an `EntityManager` or Hibernate `Session`.

```java
Order first = entityManager.find(Order.class, 10L);
Order second = entityManager.find(Order.class, 10L);

System.out.println(first == second); // Normally true
```

Characteristics:

- Mandatory
- Scoped to the persistence context
- Not shared across transactions or sessions
- Tracks managed entities
- Supports dirty checking
- Ensures one managed instance per identity

Hibernate describes the session as maintaining a persistence context that acts as a first-level cache. ([Hibernate Documentation][11])

---

## Q29: What is the second-level cache?

The second-level cache is optional and associated with the `SessionFactory` or `EntityManagerFactory`.

```text
Transaction A persistence context
              ↓
Shared second-level cache
              ↑
Transaction B persistence context
```

Characteristics:

- Shared across persistence contexts
- Must be explicitly configured
- Usually stores entity or collection state
- Requires an appropriate cache provider
- Must use a suitable concurrency strategy

Possible strategies include:

- Read-only
- Non-strict read-write
- Read-write
- Transactional

Do not enable it globally without analyzing:

- Update frequency
- Staleness tolerance
- Memory usage
- Cluster invalidation
- Cache hit ratio
- Consistency requirements

### Query cache distinction

The query cache generally caches query-result identifiers, not complete entity state. It commonly relies on the second-level entity cache to avoid database loads.

---

# Database Migrations

## Q30: What are Flyway and Liquibase?

Flyway and Liquibase manage versioned database schema evolution.

Typical migration operations include:

- Creating tables
- Adding columns
- Creating indexes
- Data migrations
- Constraint changes
- Stored procedure changes

### Flyway example

```text
db/migration/
├── V1__create_order_table.sql
├── V2__add_order_status.sql
└── V3__create_status_index.sql
```

### Liquibase example

```yaml
databaseChangeLog:
  - changeSet:
      id: 1
      author: developer
      changes:
        - createTable:
            tableName: orders
```

Spring Boot can run configured Flyway or Liquibase migrations during startup when the appropriate integration is present. Current Boot documentation recommends using one schema migration mechanism rather than mixing Flyway or Liquibase with competing schema-initialization methods. ([Home][12])

### Production practices

- Never edit an already-applied versioned migration.
- Add a new migration.
- Test against the real database engine.
- Keep migrations backward-compatible during rolling deployments.
- Avoid large blocking migrations in the application startup path.
- Separate destructive schema removal from the first code deployment.

---

# Database Performance

## Q31: A query works with 100 rows but takes 45 seconds with one million rows. What might be wrong?

Possible causes include:

- Missing index
- Full table scan
- Deep offset pagination
- N+1 queries
- Returning too many rows
- Sorting without a supporting index
- Non-sargable predicates
- Functions applied to indexed columns
- Implicit type conversion
- Large joins
- Stale database statistics
- Lock contention
- Insufficient memory causing disk spill
- Selecting complete entities unnecessarily

### Investigation process

1. Capture the actual SQL.
2. Run the query with production-like parameters.
3. Use `EXPLAIN` or `EXPLAIN ANALYZE`.
4. Check estimated versus actual rows.
5. Inspect scans, joins, sorting, and temporary files.
6. Verify index selectivity.
7. Measure returned row count and payload size.
8. Check connection waiting and lock waits.
9. Test with production-like data volume.

Important:

```text
EXPLAIN ANALYZE executes the query.
```

Use it carefully for expensive or modifying queries.

---

## Q32: How do you find and fix a slow query?

### Step 1: Locate the slow operation

Use:

- Application traces
- Database slow-query log
- APM
- Actuator/Micrometer metrics
- Database monitoring
- Hibernate SQL logging in controlled environments

### Step 2: Capture the exact query

Include:

- Bind values
- Execution count
- Rows returned
- Transaction context
- Calling endpoint

### Step 3: Inspect the execution plan

Look for:

- Sequential scans
- Large nested loops
- Bad row estimates
- Sorting or hashing spills
- Missing index use
- Unselective predicates

### Step 4: Correct the access pattern

Possible fixes:

- Add or redesign an index.
- Select only required columns.
- Add pagination.
- Rewrite predicates.
- Change join order or query shape.
- Replace offset with cursor pagination.
- Eliminate N+1.
- Precompute an appropriate read model.
- Archive or partition extremely large historical data where justified.

### Step 5: Re-test

Measure:

- Execution time
- Rows scanned
- Database CPU
- I/O
- Query count
- Lock time
- Application P95/P99 latency

---

## Q33: How does indexing work?

An index is an additional data structure that allows the database to locate rows without scanning the entire table.

Example:

```sql
CREATE INDEX idx_orders_customer_created
ON orders(customer_id, created_at);
```

This may support:

```sql
SELECT *
FROM orders
WHERE customer_id = ?
ORDER BY created_at DESC;
```

### Composite index order matters

An index on:

```text
(customer_id, status, created_at)
```

is typically most useful when the query filters using the leading columns.

### Covering index

An index may contain all columns required by a query, allowing the database to avoid reading the main table in some cases.

---

## Q34: When can an index hurt performance?

Indexes add costs:

- Slower `INSERT`
- Slower `UPDATE`
- Slower `DELETE`
- More storage
- More memory pressure
- More maintenance
- Longer migrations
- Additional planner choices

An index may be ineffective when:

- The column has very low selectivity.
- The table is tiny.
- A function prevents normal index use.
- The query does not use the leading composite columns.
- Most of the table must be returned.
- Data types require implicit conversion.
- The index is duplicated by another index.

Do not add indexes merely because a column appears in a `WHERE` clause. Verify the execution plan and workload.

---

# ACID

## Q35: What are ACID properties?

### Atomicity

All operations succeed or all are rolled back.

```text
Debit account A
Credit account B
```

Both must complete together.

### Consistency

A transaction moves the database from one valid state to another while enforcing constraints and business invariants.

Examples:

- Primary keys remain unique.
- Foreign keys remain valid.
- Balance rules are preserved.

### Isolation

Concurrent transactions should not interfere beyond the chosen isolation level.

### Durability

After commit, the change should survive a process or system failure according to the database’s durability guarantees.

### Banking example

```java
@Transactional
public void transfer(
        long sourceId,
        long destinationId,
        BigDecimal amount
) {
    debit(sourceId, amount);
    credit(destinationId, amount);
}
```

ACID applies to the database transaction. It does not automatically include an HTTP payment provider, Kafka consumer, email system, or another service’s database.

---

# Quick Interview Answers

## How does `@Transactional` work?

> Spring normally creates an AOP proxy around the bean. The proxy uses transaction metadata and a transaction manager to start, join, suspend, commit, or roll back a transaction around the target method.

## Why does it fail on self-invocation?

> An internal call uses `this` and bypasses the proxy, so the called method’s own transaction metadata is not applied.

## `REQUIRED` vs `REQUIRES_NEW`?

> `REQUIRED` joins an existing transaction or starts one. `REQUIRES_NEW` suspends the outer transaction and starts an independent physical transaction.

## Spring Data JPA vs Hibernate?

> Spring Data JPA provides repository abstractions over Jakarta Persistence. Hibernate is a common ORM provider that implements Jakarta Persistence and performs mapping, dirty checking, fetching, caching, and SQL generation.

## First-level vs second-level cache?

> The first-level cache belongs to one persistence context and is mandatory. The second-level cache is optional and shared across persistence contexts associated with the same factory.

## How do you prevent lost updates?

> Use optimistic locking with `@Version`, an atomic conditional SQL update, or pessimistic locking, depending on contention and the type of operation.

## How do you fix N+1?

> Detect repeated queries using SQL traces or APM, then use a fetch join, entity graph, DTO projection, or batch fetching according to the use case.

## Optimistic vs pessimistic locking?

> Optimistic locking detects conflicts when updating and is best when conflicts are uncommon. Pessimistic locking obtains a database lock early and is useful for short, highly contended operations.

# Final Interview Summary

> Spring transactions are commonly proxy-based and local to one transaction manager. I account for self-invocation, rollback rules, propagation, isolation, timeouts, and transaction duration. In microservices, I use `@Transactional` for local atomicity and Saga, outbox, idempotency, and compensation for cross-service workflows. Spring Data JPA reduces repository boilerplate, while Hibernate implements persistence behavior such as dirty checking, lazy loading, first-level caching, and SQL generation. For performance and consistency, I watch for N+1 queries, unsafe pagination, missing indexes, long transactions, lost updates, and inappropriate fetch strategies.

[1]: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html "Using @Transactional :: Spring Framework"
[2]: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Propagation.html?utm_source=chatgpt.com "Enum Class propagation"
[3]: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html?utm_source=chatgpt.com "Transaction Propagation :: Spring Framework"
[4]: https://docs.spring.io/spring-data/jpa/reference/index.html "Spring Data JPA :: Spring Data JPA"
[5]: https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html "Core concepts :: Spring Data Commons"
[6]: https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html?utm_source=chatgpt.com "Persisting Entities :: Spring Data JPA"
[7]: https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html?utm_source=chatgpt.com "Projections :: Spring Data JPA"
[8]: https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html?utm_source=chatgpt.com "Jakarta Persistence"
[9]: https://docs.hibernate.org/orm/6.6/javadocs/org/hibernate/Session.html?utm_source=chatgpt.com "Session (Hibernate Javadocs) - Index of /"
[10]: https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-M1?utm_source=chatgpt.com "Jakarta Persistence"
[11]: https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com "Hibernate ORM User Guide"
[12]: https://docs.spring.io/spring-boot/how-to/data-initialization.html "Database Initialization :: Spring Boot"
