# Scenario-Based Questions: Concurrent Updates and Inventory Race Conditions

## Scenario 1: Multiple users update the same database record and cause inconsistent data. How would you fix it in Spring Boot?

The first step is to identify where consistency must be enforced.

A Java lock protects threads only inside one JVM:

```java
synchronized (lock) {
    updateRecord();
}
```

In a production system with several Spring Boot instances, different requests may run in different JVMs. Therefore, database consistency should normally be enforced through:

- Atomic SQL operations
- Transactions
- Optimistic locking
- Pessimistic locking
- Unique constraints
- Idempotency rules

---

## Optimistic Locking

Optimistic locking assumes conflicts are relatively rare. Each record contains a version value.

```java
@Entity
public class ProductInventory {

    @Id
    private Long productId;

    private int availableQuantity;

    @Version
    private long version;
}
```

When Hibernate updates the entity, it conceptually executes:

```sql
UPDATE product_inventory
SET available_quantity = ?,
    version = version + 1
WHERE product_id = ?
  AND version = ?;
```

If another transaction has already updated the row, the version no longer matches. The update affects zero rows, and Spring or Hibernate raises an optimistic-locking exception.

### Service example

```java
@Service
public class InventoryService {

    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void reserve(Long productId, int quantity) {
        ProductInventory inventory = repository.findById(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Product not found"));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new OutOfStockException();
        }

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() - quantity
        );
    }
}
```

A concurrent update may fail with an optimistic-locking exception. The application must then decide whether to:

- Retry
- Return a conflict
- Re-read and re-evaluate the business condition
- Reject the operation

### Retry warning

A retry must repeat the complete business decision:

```text
Reload current inventory
→ check availability again
→ attempt the update
```

Do not blindly retry an old update using stale state.

---

## Pessimistic Locking

Pessimistic locking acquires a database lock before modifying the row.

```java
public interface InventoryRepository
        extends JpaRepository<ProductInventory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select inventory
           from ProductInventory inventory
           where inventory.productId = :productId
           """)
    Optional<ProductInventory> findForUpdate(
            @Param("productId") Long productId
    );
}
```

Service:

```java
@Transactional
public void reserve(Long productId, int quantity) {
    ProductInventory inventory = repository.findForUpdate(productId)
            .orElseThrow(() ->
                    new IllegalArgumentException("Product not found"));

    if (inventory.getAvailableQuantity() < quantity) {
        throw new OutOfStockException();
    }

    inventory.setAvailableQuantity(
            inventory.getAvailableQuantity() - quantity
    );
}
```

The database prevents another conflicting transaction from updating the locked row until the current transaction completes.

---

## Optimistic vs Pessimistic Locking

| Optimistic locking                                | Pessimistic locking                            |
| ------------------------------------------------- | ---------------------------------------------- |
| Detects conflicts during update                   | Prevents conflicting updates by locking        |
| Suitable when conflicts are uncommon              | Suitable when conflicts are frequent           |
| Better concurrency under low contention           | Serializes access to the locked record         |
| Requires conflict handling or retry               | May block waiting transactions                 |
| Uses a version column                             | Uses database row locks                        |
| Can produce retry storms under extreme contention | Can increase lock waits and deadlock risk      |
| Good for ordinary CRUD updates                    | Useful for very hot, correctness-critical rows |

### Interview-ready answer

> I use optimistic locking when collisions are uncommon because it allows greater concurrency and detects stale updates using a version column. I use pessimistic locking when conflicts are frequent and the cost of retrying is high. For inventory decrement operations, an atomic conditional SQL update is often even simpler and more scalable.

---

# Prefer Atomic Conditional Updates for Inventory

For a simple stock decrement, reading and then writing the entity may be unnecessary.

Use one atomic database statement:

```sql
UPDATE product_inventory
SET available_quantity = available_quantity - 1
WHERE product_id = :productId
  AND available_quantity > 0;
```

The update count determines the result:

- `1` row updated → reservation succeeded
- `0` rows updated → no inventory remained

Spring Data example:

```java
public interface InventoryRepository
        extends JpaRepository<ProductInventory, Long> {

    @Modifying
    @Query("""
           update ProductInventory inventory
           set inventory.availableQuantity =
               inventory.availableQuantity - :quantity
           where inventory.productId = :productId
             and inventory.availableQuantity >= :quantity
           """)
    int reserve(
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );
}
```

Service:

```java
@Transactional
public void reserve(Long productId, int quantity) {
    int updatedRows = repository.reserve(
            productId,
            quantity
    );

    if (updatedRows == 0) {
        throw new OutOfStockException();
    }
}
```

This avoids the unsafe sequence:

```text
Read stock
→ check stock
→ update stock
```

The check and decrement happen atomically inside the database.

---

# Scenario 2: A multithreaded service causes inconsistent updates. How would you fix it?

## Investigation process

### 1. Identify the shared mutable state

Examples:

```java
private int counter;
private final Map<String, Order> orders = new HashMap<>();
private ProductInventory cachedInventory;
```

Check whether the state is:

- Static
- Stored in a singleton Spring bean
- Shared through a cache
- Shared across scheduled jobs and requests
- Updated by Kafka listeners
- Accessed by multiple application instances

Spring services are singleton-scoped by default. Mutable instance fields inside them may therefore be shared by many request threads.

Unsafe example:

```java
@Service
public class PricingService {

    private BigDecimal currentDiscount;

    public Price calculate(Order order) {
        currentDiscount = loadDiscount(order);
        return applyDiscount(order, currentDiscount);
    }
}
```

Different requests can overwrite `currentDiscount`.

Prefer local variables:

```java
public Price calculate(Order order) {
    BigDecimal discount = loadDiscount(order);
    return applyDiscount(order, discount);
}
```

---

### 2. Determine the required invariant

Examples:

```text
Inventory must never become negative.

A payment must be charged at most once.

Balance and version must change together.

Only one active reservation may exist for an order.
```

The synchronization mechanism must protect the complete invariant, not only one assignment.

---

### 3. Choose the correct scope of coordination

| Scope                              | Possible solution                                                             |
| ---------------------------------- | ----------------------------------------------------------------------------- |
| One independent value in one JVM   | Atomic variable                                                               |
| Several fields in one JVM          | `synchronized` or `ReentrantLock`                                             |
| Producer-consumer workflow         | Bounded `BlockingQueue`                                                       |
| One database row across instances  | Atomic SQL or database locking                                                |
| Duplicate request across instances | Idempotency key plus unique constraint                                        |
| Distributed resource coordination  | Database, transactional store, or carefully designed distributed coordination |

---

### 4. Keep service objects stateless where possible

```java
@Service
public class OrderCalculator {

    public OrderTotal calculate(Order order) {
        BigDecimal subtotal = calculateSubtotal(order);
        BigDecimal tax = calculateTax(subtotal);

        return new OrderTotal(subtotal, tax);
    }
}
```

Stateless services are naturally safer for concurrent use.

---

# Scenario 3: How would you handle race conditions in a high-concurrency environment?

Use layers of protection based on the operation:

1. Make services stateless where possible.
2. Use immutable data transfer objects and snapshots.
3. Use atomic operations for independent values.
4. Protect multi-step in-memory invariants with locks.
5. Use concurrent collections and their atomic methods.
6. Enforce database invariants with SQL and constraints.
7. Use idempotency for retried requests.
8. Bound concurrency and queue capacity.
9. Add timeouts and overload handling.
10. Test under realistic concurrent load.

Example of an unsafe map workflow:

```java
if (!orders.containsKey(orderId)) {
    orders.put(orderId, createOrder());
}
```

Even with `ConcurrentHashMap`, the compound sequence is unsafe.

Prefer:

```java
orders.computeIfAbsent(
        orderId,
        this::createOrder
);
```

For a business operation spanning a database, rely on the database rather than only an in-memory map.

---

# Scenario 4: How would you handle shared mutable state safely?

The safest approach is often to remove or reduce shared mutation.

## Preferred techniques

### Immutability

```java
public record InventorySnapshot(
        long productId,
        int availableQuantity,
        long version
) {
}
```

### Thread confinement

Keep mutable state inside one task or thread.

```java
public Report generate(List<Order> orders) {
    Map<String, Integer> localCounts =
            new HashMap<>();

    // Only this invocation uses localCounts.
    return buildReport(localCounts);
}
```

### Atomic variables

```java
private final AtomicInteger activeRequests =
        new AtomicInteger();
```

### Locks

```java
private final Object lock = new Object();

synchronized (lock) {
    validateState();
    updateRelatedFields();
}
```

### Concurrent collections

```java
private final ConcurrentMap<String, LongAdder> counters =
        new ConcurrentHashMap<>();

counters.computeIfAbsent(
        key,
        ignored -> new LongAdder()
).increment();
```

### Message passing

Instead of several threads modifying one object, send commands to one owner through a queue.

```text
Request threads
      ↓
BlockingQueue
      ↓
Single state-owning worker
```

### Database constraints

Examples include:

```sql
UNIQUE (idempotency_key)
```

and:

```sql
CHECK (available_quantity >= 0)
```

Constraints provide a final layer of protection even when application logic contains a defect.

---

# Big Sale Scenario: One Item, 10,000 Buyers

## Naive implementation

```java
@Transactional
public Order buy(Long productId) {
    Inventory inventory = repository.findById(productId)
            .orElseThrow();

    if (inventory.getAvailableQuantity() <= 0) {
        throw new OutOfStockException();
    }

    inventory.setAvailableQuantity(
            inventory.getAvailableQuantity() - 1
    );

    return createOrder(productId);
}
```

Under concurrent execution, several transactions may read the same stock value before any commits.

However, the claim that all 10,000 requests will always be confirmed is also too absolute. The result depends on:

- Transaction isolation
- ORM behavior
- Database locking
- Version columns
- Constraints
- Update statements
- Commit timing

The real risk is that a naive read-check-write implementation permits lost updates or overselling unless the database operation is properly coordinated.

---

## Correct database-first solution

```sql
UPDATE inventory
SET available_quantity = available_quantity - 1
WHERE product_id = :productId
  AND available_quantity > 0;
```

Exactly one transaction can successfully decrement the final unit.

The application then creates the order only when the update count is one.

```java
@Transactional
public OrderResult buy(
        Long productId,
        String idempotencyKey
) {
    Order existing =
            orderRepository.findByIdempotencyKey(
                    idempotencyKey
            );

    if (existing != null) {
        return OrderResult.from(existing);
    }

    int updated =
            inventoryRepository.reserveOne(productId);

    if (updated == 0) {
        return OrderResult.outOfStock();
    }

    Order order = orderRepository.save(
            Order.pending(
                    productId,
                    idempotencyKey
            )
    );

    return OrderResult.from(order);
}
```

Use a database uniqueness constraint for the idempotency key:

```sql
ALTER TABLE orders
ADD CONSTRAINT uk_orders_idempotency
UNIQUE (idempotency_key);
```

This protects against concurrent duplicate submissions across application instances.

---

# Redis Atomic Inventory

## Problem with plain `DECR`

A plain command:

```text
DECR inventory:iphone
```

is atomic, but it can decrement below zero:

```text
1 → 0 → -1 → -2
```

The application must either compensate or use a conditional atomic operation.

## Conditional Lua script

```lua
local stock = tonumber(redis.call("GET", KEYS[1]) or "0")

if stock <= 0 then
    return 0
end

redis.call("DECR", KEYS[1])
return 1
```

Application meaning:

```text
1 → reservation accepted
0 → out of stock
```

The check and decrement execute atomically inside Redis.

## Important consistency problem

Redis stock and database stock are two separate states.

Possible failure:

```text
Redis decrement succeeds
→ application crashes
→ order is never persisted
```

Now Redis reports less inventory than the database.

Another failure:

```text
Redis decrement succeeds
→ order persistence fails
→ inventory must be released
```

Therefore, a complete solution needs a reservation lifecycle such as:

```text
AVAILABLE
→ RESERVED
→ CONFIRMED
or
→ RELEASED
```

Possible supporting techniques include:

- Reservation expiration
- Compensation
- Durable event publication
- Reconciliation jobs
- Database source of truth
- Outbox or inbox patterns
- Idempotent reservation commands

### Interview-ready answer

> Redis can perform a conditional inventory decrement atomically, but plain `DECR` can produce negative values. More importantly, Redis and the order database can diverge, so I model inventory as a reservation lifecycle and add compensation and reconciliation rather than claiming that one Redis command eliminates every overselling risk.

---

# Kafka During a Traffic Spike

Kafka can buffer accepted commands so downstream services process them at a sustainable rate.

```text
Checkout request
      ↓
Validation and idempotency
      ↓
Order command published
      ↓
Consumer processes inventory and order
```

However, publishing an event does not necessarily mean the order is confirmed.

A safer user-visible state is:

```text
ORDER_PENDING
```

The final result becomes:

```text
ORDER_CONFIRMED
```

or:

```text
OUT_OF_STOCK
```

after inventory processing succeeds.

## Why “Kafka accepts, therefore show Order Confirmed” is unsafe

Kafka publication confirms that a message was accepted according to the producer acknowledgement configuration. It does not prove that:

- Inventory was reserved
- Payment succeeded
- The consumer processed the message
- The database transaction committed
- A downstream side effect did not fail

## Partitioning

To serialize inventory operations for one product, commands can be keyed by product or SKU:

```java
new ProducerRecord<>(
        "inventory-commands",
        productId,
        command
);
```

Events with the same key are routed consistently to a partition, preserving order within that partition.

Trade-off:

```text
One extremely popular product
→ one hot partition
→ limited processing parallelism for that product
```

That may be acceptable when strict serialization is required, but it must be included in capacity planning.

## Delivery semantics

Consumers must expect redelivery.

```text
Reserve inventory
→ process crashes before offset commit
→ message is delivered again
```

Use:

- Idempotency keys
- Unique constraints
- Inbox tables
- State-transition validation
- Transactional processing where appropriate

### Interview-ready answer

> Kafka can absorb a traffic spike and decouple intake from database processing, but publishing a command should normally produce a pending state, not immediate confirmation. Consumers must be idempotent, and partition keys must be chosen according to ordering and hot-key requirements.

---

# Idempotency Keys

An idempotency key represents one logical checkout attempt.

Request:

```http
Idempotency-Key: checkout-8b74...
```

Database table:

```sql
CREATE TABLE idempotency_record (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    request_hash VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    response_body TEXT
);
```

Workflow:

```text
Receive key
→ attempt unique insert
→ if new, process request
→ store final response
→ if duplicate, return existing result
```

## Why Redis alone may be insufficient

A Redis entry can expire, be evicted, or be unavailable while the durable order remains.

For financial or order operations, a database uniqueness constraint is generally the final correctness guard.

Also validate that the same key is not reused with a different request body.

```text
Same idempotency key
+ different product or amount
→ reject as invalid reuse
```

---

# Centralized Sale State

A shared sale flag can help application instances agree on whether a sale is active, but polling Redis every 100 milliseconds does not make activation perfectly simultaneous.

Differences can still arise because of:

- Polling intervals
- Network delay
- Process pauses
- Clock differences
- Redis unavailability
- Cached values

A more robust approach is:

```text
Store sale start timestamp centrally
→ use synchronized server clocks
→ validate on every authoritative checkout request
→ optionally broadcast configuration changes
```

Example:

```java
public boolean isSaleActive(
        Instant now,
        SaleConfiguration configuration
) {
    return !now.isBefore(configuration.startTime())
            && now.isBefore(configuration.endTime());
}
```

Do not rely only on a frontend flag. The backend must enforce the sale window.

---

# What if Redis Fails?

This claim is unsafe:

> A replica promotes in under a second and there is zero inventory corruption.

Failover duration varies, and asynchronous replication may allow recently acknowledged writes to be lost during promotion.

The application must define a failure policy.

Possible options:

### Fail closed

```text
Redis unavailable
→ temporarily stop new purchases
```

Best when overselling is unacceptable.

### Fall back to the database

```text
Redis unavailable
→ use atomic database decrement
→ accept lower throughput
```

### Continue with reduced guarantees

Usually inappropriate for scarce inventory unless the business explicitly accepts overselling risk.

### Reconcile after recovery

Compare:

- Redis reservations
- Database orders
- Payment records
- Expired reservations
- Published events

### Interview-ready answer

> Redis replication improves availability but does not guarantee instantaneous failover or zero data loss in every configuration. For scarce inventory, I would fail closed or fall back to an atomic database operation, then reconcile reservations after recovery.

---

# Corrected High-Concurrency Purchase Flow

```text
Client
  ↓
Rate and abuse controls
  ↓
Idempotency validation
  ↓
Atomic inventory reservation
  ↓
Create pending order
  ↓
Publish durable event
  ↓
Payment and downstream processing
  ↓
Confirm or release reservation
```

The exact order depends on the business contract, especially whether payment or inventory is reserved first.

Important safeguards include:

- Atomic conditional decrement
- Idempotency key
- Unique database constraints
- Reservation timeout
- Durable event publication
- Idempotent consumers
- Compensation
- Reconciliation
- Backpressure
- Monitoring

---

# How Would You Test Concurrent Update Problems?

## 1. Repository integration test

Use the real database engine rather than only an in-memory replacement.

```java
@SpringBootTest
class InventoryConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void onlyOneRequestReservesTheFinalItem()
            throws Exception {

        long productId = 1001L;
        int requestCount = 100;

        inventoryRepository.save(
                new ProductInventory(
                        productId,
                        1
                )
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(20);

        CountDownLatch ready =
                new CountDownLatch(requestCount);

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<Boolean>> results =
                new ArrayList<>();

        for (int index = 0;
             index < requestCount;
             index++) {

            results.add(executor.submit(() -> {
                ready.countDown();
                start.await();

                try {
                    inventoryService.reserveOne(productId);
                    return true;
                } catch (OutOfStockException exception) {
                    return false;
                }
            }));
        }

        ready.await();
        start.countDown();

        int successes = 0;

        for (Future<Boolean> result : results) {
            if (result.get()) {
                successes++;
            }
        }

        executor.shutdown();

        ProductInventory finalInventory =
                inventoryRepository.findById(productId)
                        .orElseThrow();

        assertEquals(1, successes);
        assertEquals(
                0,
                finalInventory.getAvailableQuantity()
        );
    }
}
```

### Important test rule

Each concurrent operation must use its own transaction. Do not wrap the entire test in one transaction and assume it reproduces production behavior.

---

## 2. Test optimistic-lock conflicts

Create two transactions that:

1. Read the same version.
2. Wait at a barrier.
3. Update the entity concurrently.
4. Verify that only one commits without conflict.
5. Verify that retry logic re-reads the current state.

---

## 3. Test pessimistic locking

Verify that:

- One transaction acquires the lock.
- A second transaction blocks or times out.
- Lock timeout behavior is handled.
- Rollback releases the lock.
- No deadlock appears when multiple rows are involved.

---

## 4. Test idempotency

Submit the same idempotency key concurrently.

Assert:

- Only one order exists.
- Only one inventory reservation exists.
- All callers receive the same logical result.
- Reusing the key with a different request is rejected.

---

## 5. Load testing

Measure:

- Successful reservations
- Rejections
- Stock remaining
- Database lock time
- Optimistic conflict rate
- Retry count
- Queue depth
- Connection-pool waiting
- Kafka lag
- Redis latency
- Duplicate-processing count
- P95 and P99 latency

The final invariant should always hold:

```text
initial inventory
=
confirmed units
+ active reservations
+ available inventory
```

No test should end with negative inventory.

---

# Common Mistakes

## Using `synchronized` in a distributed service

```java
public synchronized void reserve() {
}
```

This protects only calls reaching the same Spring Boot instance.

---

## Reading before updating without a lock or version

```text
SELECT stock
→ application check
→ UPDATE stock
```

This creates a race window.

---

## Retrying optimistic conflicts without limits

Under high contention:

```text
Conflict
→ retry immediately
→ conflict again
→ retry storm
```

Use bounded retries with backoff, or switch to a more appropriate strategy.

---

## Holding a pessimistic lock during remote calls

```java
@Transactional
public void reserveAndCharge() {
    inventoryRepository.findForUpdate(productId);
    paymentClient.charge();
}
```

The database lock remains held while the network call runs.

This can cause:

- Long lock waits
- Connection exhaustion
- Deadlocks
- Poor throughput

Keep transactions short and coordinate external side effects through a reliable workflow.

---

## Treating order acceptance as order confirmation

A queued command means:

```text
Request accepted for processing
```

not necessarily:

```text
Inventory reserved and payment completed
```

Use explicit states.

---

# Short Interview Answer

> For concurrent updates, I first define the invariant and enforce it at the correct scope. Inside one JVM, atomics or locks may be sufficient, but across Spring Boot instances I rely on atomic SQL, optimistic versions, pessimistic row locks, unique constraints, and idempotency. Optimistic locking is suitable when collisions are rare; pessimistic locking is useful for highly contended rows but introduces blocking. For inventory, I prefer a conditional atomic update such as `UPDATE ... SET stock = stock - 1 WHERE stock > 0`. I test it using concurrent integration tests against the real database, synchronize request starts with a latch, and assert that stock never becomes negative and only the allowed number of reservations succeeds.
