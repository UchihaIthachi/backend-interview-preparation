# Scenario-Based Questions — Collections and Caching

## Scenario 1: Your application randomly throws `ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63`. How would you debug and fix it?

### Problem

The correct exception name is:

```java
ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63
```

It usually occurs when a fail-fast collection is structurally modified while it is being iterated, except through the iterator’s supported modification methods.

Example:

```java
List<String> values =
        new ArrayList<>(
                List.of("Java", "", "Go")
        );

for (String value : values) {
    if (value.isBlank()) {
        values.remove(value);
    }
}
```

This may throw:

```text
java.util.ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63
```

---

### Why does it happen?

Many collection iterators track the collection’s structural modification count.

Conceptually:

```text
Iterator created
    → stores expected modification count

Collection structurally modified
    → modification count changes

Iterator continues
    → expected count does not match actual count
    → ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63
```

A structural modification usually means adding or removing elements or otherwise changing the collection’s size or internal structure.

Replacing an existing value may or may not be considered structural, depending on the collection and operation.

---

## Debugging approach

### Step 1: Inspect the complete stack trace

Find the line where iteration fails.

Typical stack trace:

```text
java.util.ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63
    at java.util.ArrayList$Itr.checkForComodification(...)
    at java.util.ArrayList$Itr.next(...)
    at OrderService.removeInvalidOrders(...)
```

The exception is commonly thrown by `next()`, not necessarily by the exact line that originally modified the collection.

Therefore, inspect the entire iteration block.

---

### Step 2: Look for modification during enhanced iteration

Search for patterns such as:

```java
for (Order order : orders) {
    orders.remove(order);
}
```

```java
orders.forEach(order -> {
    if (order.isInvalid()) {
        orders.remove(order);
    }
});
```

```java
Iterator<Order> iterator = orders.iterator();

while (iterator.hasNext()) {
    Order order = iterator.next();
    orders.add(createReplacement(order));
}
```

---

### Step 3: Check indirect modifications

The modification may happen inside another method.

```java
for (Order order : orders) {
    validate(order, orders);
}
```

```java
private void validate(
        Order order,
        List<Order> orders
) {
    if (order.isInvalid()) {
        orders.remove(order);
    }
}
```

The iteration method may appear harmless, while a helper, callback, listener, or event handler modifies the same collection.

---

### Step 4: Check multiple threads

A second thread may modify a non-thread-safe collection while another thread iterates over it.

```java
private final List<Order> orders =
        new ArrayList<>();
```

Thread A:

```java
for (Order order : orders) {
    process(order);
}
```

Thread B:

```java
orders.add(newOrder);
```

Fail-fast collections are not made thread-safe by the exception. `ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63` is only best-effort bug detection.

A race may also produce:

- Stale data
- Lost updates
- Inconsistent results
- No exception at all

---

### Step 5: Check map views

Modifying a map while iterating over one of its views can also trigger the exception.

```java
for (String key : map.keySet()) {
    if (key.startsWith("TEMP")) {
        map.remove(key);
    }
}
```

The same applies to:

```java
map.entrySet();
map.values();
```

These are backed by the original map.

---

## Correct solutions

### Solution 1: Use `Iterator.remove()`

```java
Iterator<String> iterator =
        values.iterator();

while (iterator.hasNext()) {
    String value = iterator.next();

    if (value.isBlank()) {
        iterator.remove();
    }
}
```

This is the standard solution when removing the current element during iteration.

---

### Solution 2: Use `removeIf()`

```java
values.removeIf(String::isBlank);
```

This is usually the clearest solution for predicate-based removal.

```java
orders.removeIf(Order::isInvalid);
```

---

### Solution 3: Collect changes and apply them later

```java
List<Order> ordersToRemove =
        new ArrayList<>();

for (Order order : orders) {
    if (order.isInvalid()) {
        ordersToRemove.add(order);
    }
}

orders.removeAll(ordersToRemove);
```

This is useful when the decision requires complex processing.

---

### Solution 4: Build a new collection

```java
List<Order> validOrders =
        orders.stream()
              .filter(order -> !order.isInvalid())
              .toList();
```

This avoids mutating the original collection.

Be aware that `Stream.toList()` returns an unmodifiable result in modern Java.

Use a mutable result when required:

```java
List<Order> validOrders =
        orders.stream()
              .filter(order -> !order.isInvalid())
              .collect(Collectors.toCollection(
                      ArrayList::new
              ));
```

---

### Solution 5: Use the map iterator

Incorrect:

```java
for (Map.Entry<String, Order> entry :
        ordersById.entrySet()) {

    if (entry.getValue().isInvalid()) {
        ordersById.remove(entry.getKey());
    }
}
```

Correct:

```java
Iterator<Map.Entry<String, Order>> iterator =
        ordersById.entrySet().iterator();

while (iterator.hasNext()) {
    Map.Entry<String, Order> entry =
            iterator.next();

    if (entry.getValue().isInvalid()) {
        iterator.remove();
    }
}
```

Or:

```java
ordersById.entrySet()
          .removeIf(entry ->
                  entry.getValue().isInvalid()
          );
```

---

### Solution 6: Use an appropriate concurrent collection

When concurrent modification is expected by design, consider:

```java
ConcurrentHashMap<K, V>
CopyOnWriteArrayList<E>
ConcurrentLinkedQueue<E>
BlockingQueue<E>
```

Example:

```java
ConcurrentMap<String, Order> orders =
        new ConcurrentHashMap<>();

for (Map.Entry<String, Order> entry :
        orders.entrySet()) {

    orders.put(
            "new-order",
            createOrder()
    );
}
```

`ConcurrentHashMap` iterators are weakly consistent. They can continue during updates without throwing `ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63` merely because the map changed.

However, they do not provide a fully isolated snapshot and may observe some updates but not others.

---

### Do not catch and ignore the exception

Avoid:

```java
try {
    for (Order order : orders) {
        process(order);
    }
} catch (ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63 exception) {
    // Ignore
}
```

This hides the underlying correctness problem and may leave only part of the collection processed.

---

## Production debugging checklist

1. Capture the complete stack trace.
2. Find the iterator or enhanced-loop location.
3. Search for `add()`, `remove()`, `clear()`, and bulk operations in that block.
4. Inspect helper methods and callbacks called from the loop.
5. Check whether the collection is shared between threads.
6. Identify the required consistency:
   - Remove current element
   - Apply changes after traversal
   - Read a snapshot
   - Allow concurrent updates

7. Select the appropriate collection or synchronization mechanism.
8. Add a concurrent test reproducing the problem.
9. Avoid fixing it by merely catching the exception.

---

### Interview-ready answer

> I would start with the full stack trace and identify which fail-fast iterator is involved. Then I would check for direct, indirect, or cross-thread structural modifications to the same collection. For single-threaded removal, I would use `Iterator.remove()`, `removeIf()`, or build a new collection. If concurrent updates are expected, I would choose a suitable concurrent collection and define whether weak consistency or snapshot iteration is acceptable. I would not catch and ignore the exception because it indicates an incorrect modification strategy.

---

# Scenario 2: How would you design a cache with an eviction strategy?

## Requirement clarification

Before designing the cache, clarify:

1. Is it local or distributed?
2. Is the maximum size based on entry count or memory usage?
3. Which eviction policy is required?
4. Do entries expire after write or after access?
5. Is the cache thread-safe?
6. How are cache misses loaded?
7. Can multiple threads load the same missing key?
8. Must entries survive application restarts?
9. How stale may cached data become?
10. What should happen when loading fails?

For an interview, start with a bounded in-memory cache and then explain production extensions.

---

## Core cache interface

```java
public interface Cache<K, V> {

    V get(K key);

    void put(K key, V value);

    V remove(K key);

    boolean containsKey(K key);

    int size();

    void clear();
}
```

A loading cache may use:

```java
public interface LoadingCache<K, V> {

    V get(
            K key,
            Function<? super K, ? extends V> loader
    );

    void put(K key, V value);

    void invalidate(K key);
}
```

---

## Common eviction strategies

| Strategy          | Evicts                                | Good for                       | Limitation                         |
| ----------------- | ------------------------------------- | ------------------------------ | ---------------------------------- |
| FIFO              | Oldest inserted entry                 | Simple bounded buffers         | Ignores usage                      |
| LRU               | Least recently accessed entry         | Temporal locality              | Scanning traffic can pollute cache |
| LFU               | Least frequently accessed entry       | Repeatedly popular items       | Requires frequency tracking        |
| TTL               | Entry after fixed lifetime            | Time-sensitive data            | Popular entries can still expire   |
| TTI               | Entry after inactivity period         | Session-like data              | Access updates add overhead        |
| Random            | Random entry                          | Very simple high-scale designs | Lower predictability               |
| Size/weight based | Entries with total weight above limit | Unequal object sizes           | Requires weight estimation         |

A production cache commonly combines policies:

```text
Maximum weight + expire-after-write + LRU-like admission/eviction
```

---

# Simple LRU Cache Using `LinkedHashMap`

`LinkedHashMap` supports access-order iteration.

```java
public final class LruCache<K, V> {

    private final int maximumSize;
    private final LinkedHashMap<K, V> entries;

    public LruCache(int maximumSize) {
        if (maximumSize <= 0) {
            throw new IllegalArgumentException(
                    "Maximum size must be positive"
            );
        }

        this.maximumSize = maximumSize;

        this.entries =
                new LinkedHashMap<>(
                        16,
                        0.75f,
                        true
                );
    }

    public synchronized V get(K key) {
        return entries.get(key);
    }

    public synchronized void put(
            K key,
            V value
    ) {
        Objects.requireNonNull(key, "Key is required");
        Objects.requireNonNull(value, "Value is required");

        entries.put(key, value);

        if (entries.size() > maximumSize) {
            Iterator<Map.Entry<K, V>> iterator =
                    entries.entrySet().iterator();

            iterator.next();
            iterator.remove();
        }
    }

    public synchronized V remove(K key) {
        return entries.remove(key);
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized void clear() {
        entries.clear();
    }
}
```

### Usage

```java
LruCache<String, User> cache =
        new LruCache<>(3);

cache.put("A", new User("A"));
cache.put("B", new User("B"));
cache.put("C", new User("C"));

cache.get("A");

cache.put("D", new User("D"));
```

After accessing `"A"`, `"B"` is the least recently used entry and is evicted when `"D"` is inserted.

---

## Alternative using `removeEldestEntry()`

```java
public final class LruMap<K, V>
        extends LinkedHashMap<K, V> {

    private final int maximumSize;

    public LruMap(int maximumSize) {
        super(16, 0.75f, true);

        if (maximumSize <= 0) {
            throw new IllegalArgumentException(
                    "Maximum size must be positive"
            );
        }

        this.maximumSize = maximumSize;
    }

    @Override
    protected boolean removeEldestEntry(
            Map.Entry<K, V> eldest
    ) {
        return size() > maximumSize;
    }
}
```

Usage:

```java
Map<String, User> cache =
        Collections.synchronizedMap(
                new LruMap<>(1_000)
        );
```

### Limitation

Iteration and compound operations still require synchronization on the wrapper.

```java
synchronized (cache) {
    for (Map.Entry<String, User> entry :
            cache.entrySet()) {

        process(entry);
    }
}
```

---

# Strategy-Based Cache Design

To support multiple eviction policies, separate eviction decisions from storage.

```java
public interface EvictionPolicy<K> {

    void onGet(K key);

    void onPut(K key);

    void onRemove(K key);

    K selectEvictionCandidate();
}
```

Cache implementation:

```java
public final class BoundedCache<K, V> {

    private final int maximumSize;
    private final Map<K, V> values;
    private final EvictionPolicy<K> evictionPolicy;
    private final ReentrantLock lock =
            new ReentrantLock();

    public BoundedCache(
            int maximumSize,
            EvictionPolicy<K> evictionPolicy
    ) {
        if (maximumSize <= 0) {
            throw new IllegalArgumentException(
                    "Maximum size must be positive"
            );
        }

        this.maximumSize = maximumSize;
        this.evictionPolicy =
                Objects.requireNonNull(
                        evictionPolicy,
                        "Eviction policy is required"
                );

        this.values = new HashMap<>();
    }

    public Optional<V> get(K key) {
        lock.lock();

        try {
            V value = values.get(key);

            if (value != null) {
                evictionPolicy.onGet(key);
            }

            return Optional.ofNullable(value);
        } finally {
            lock.unlock();
        }
    }

    public void put(K key, V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        lock.lock();

        try {
            boolean existing =
                    values.containsKey(key);

            values.put(key, value);
            evictionPolicy.onPut(key);

            if (!existing &&
                    values.size() > maximumSize) {

                K candidate =
                        evictionPolicy
                                .selectEvictionCandidate();

                if (candidate == null) {
                    throw new IllegalStateException(
                            "No eviction candidate available"
                    );
                }

                values.remove(candidate);
                evictionPolicy.onRemove(candidate);
            }
        } finally {
            lock.unlock();
        }
    }

    public void remove(K key) {
        lock.lock();

        try {
            if (values.remove(key) != null) {
                evictionPolicy.onRemove(key);
            }
        } finally {
            lock.unlock();
        }
    }
}
```

---

## LRU policy

```java
public final class LruEvictionPolicy<K>
        implements EvictionPolicy<K> {

    private final LinkedHashSet<K> order =
            new LinkedHashSet<>();

    @Override
    public void onGet(K key) {
        moveToMostRecent(key);
    }

    @Override
    public void onPut(K key) {
        moveToMostRecent(key);
    }

    @Override
    public void onRemove(K key) {
        order.remove(key);
    }

    @Override
    public K selectEvictionCandidate() {
        Iterator<K> iterator =
                order.iterator();

        return iterator.hasNext()
                ? iterator.next()
                : null;
    }

    private void moveToMostRecent(K key) {
        order.remove(key);
        order.add(key);
    }
}
```

This design demonstrates the Strategy pattern, but `LinkedHashMap` is simpler for a basic LRU implementation.

The strategy abstraction becomes useful when supporting multiple policies.

---

# TTL Expiration

An entry can store its expiry time:

```java
public record CacheEntry<V>(
        V value,
        long expiresAtNanos
) {

    public boolean isExpired(long nowNanos) {
        return nowNanos >= expiresAtNanos;
    }
}
```

Cache lookup:

```java
public Optional<V> get(K key) {
    CacheEntry<V> entry = entries.get(key);

    if (entry == null) {
        return Optional.empty();
    }

    long now = System.nanoTime();

    if (entry.isExpired(now)) {
        entries.remove(key, entry);
        return Optional.empty();
    }

    return Optional.of(entry.value());
}
```

Use a monotonic time source such as `System.nanoTime()` for measuring elapsed duration within one JVM.

---

## Expiration approaches

### Lazy expiration

Remove expired entries when accessed.

Advantages:

- Simple
- No background thread

Disadvantages:

- Unused expired entries may remain in memory

### Periodic cleanup

A scheduled task scans and removes expired entries.

Advantages:

- Expired data is eventually reclaimed

Disadvantages:

- Full scans may be expensive
- Cleanup scheduling requires lifecycle management

### Expiry queue

Maintain entries ordered by expiry time.

Possible structures:

```java
PriorityQueue<ExpiryRecord<K>>
DelayQueue<ExpiryRecord<K>>
```

Advantages:

- Removes entries close to expiry time
- Avoids scanning the entire cache

Disadvantages:

- Updating expiration can leave stale expiry records
- Requires validation against the current cache entry

---

# Preventing a Cache Stampede

Suppose many threads miss the same key:

```java
User user = cache.get(userId);

if (user == null) {
    user = repository.findById(userId);
    cache.put(userId, user);
}
```

Every thread may query the database simultaneously.

Use a per-key atomic loading operation:

```java
ConcurrentHashMap<K, V> cache =
        new ConcurrentHashMap<>();

V value = cache.computeIfAbsent(
        key,
        this::loadValue
);
```

### Important warning

The loader should not perform uncontrolled long-running work inside `computeIfAbsent()` because other operations involving that key may be delayed.

For advanced systems, use:

- Per-key futures
- Request coalescing
- Single-flight loading
- Refresh-ahead
- Distributed locks only when justified

---

## Cache-aside pattern

```java
public User findUser(UserId userId) {
    User cached = cache.get(userId);

    if (cached != null) {
        return cached;
    }

    User loaded =
            repository.findRequired(userId);

    cache.put(userId, loaded);

    return loaded;
}
```

Write path:

```java
public User updateUser(
        UserId userId,
        UpdateUserRequest request
) {
    User updated =
            repository.update(userId, request);

    cache.remove(userId);

    return updated;
}
```

This is cache-aside with invalidation after a successful database update.

---

# Thread-Safety Considerations

A cache operation often involves multiple related structures:

```text
Value map
+ eviction metadata
+ expiry metadata
+ statistics
```

Protecting only the map is insufficient if eviction metadata can become inconsistent.

The following actions may need one logical atomic boundary:

1. Insert value.
2. Update access order.
3. Check capacity.
4. Select victim.
5. Remove victim.
6. Update metrics.

Possible approaches:

- One lock around cache state
- Segmented caches
- Per-key coordination plus a global eviction structure
- Established cache libraries with tested concurrency designs

For interview implementations, a single lock is often acceptable if its trade-off is clearly explained.

---

# Production Cache Requirements

A production cache should consider:

## Capacity

- Maximum entries
- Maximum estimated memory weight
- Per-entry size
- JVM heap pressure

## Expiration

- Expire after write
- Expire after access
- Refresh after write
- Manual invalidation

## Loading behavior

- Synchronous load
- Asynchronous refresh
- Request coalescing
- Failure handling
- Negative caching

## Consistency

- Stale-data tolerance
- Database update invalidation
- Event-driven invalidation
- Version checking
- Multi-instance consistency

## Observability

Track:

```text
Cache hits
Cache misses
Hit ratio
Eviction count
Load duration
Load failures
Current entry count
Current estimated weight
Expired entries
```

## Reliability

- Loader timeout
- Fallback behavior
- Bounded memory
- Protection from stampedes
- No caching of unexpected failures
- Controlled negative-cache duration

---

# Local Cache vs Distributed Cache

| Local cache                      | Distributed cache                            |
| -------------------------------- | -------------------------------------------- |
| Very low latency                 | Network call required                        |
| Per-instance data                | Shared across instances                      |
| No serialization required        | Serialization usually required               |
| Simple deployment                | Additional infrastructure                    |
| Invalidation differs by instance | Centralized state                            |
| Limited by process memory        | Can scale independently                      |
| Lost when process restarts       | Durability depends on platform configuration |

Examples:

```text
Local: Caffeine-style in-process cache
Distributed: Redis-style shared cache
```

A distributed cache still requires:

- Expiration
- Eviction
- Connection-pool management
- Failure handling
- Serialization compatibility
- Hot-key protection
- Stale-data strategy

---

# Common Mistakes

## 1. Building an unbounded cache

```java
private final Map<String, User> cache =
        new ConcurrentHashMap<>();
```

Without size or expiry controls, this is an unbounded map rather than a safe cache and may cause `OutOfMemoryError`.

---

## 2. Assuming `ConcurrentHashMap` provides eviction

`ConcurrentHashMap` provides concurrent mapping operations but does not provide:

- LRU eviction
- TTL expiration
- Maximum-size enforcement
- Cache statistics
- Automatic loading

---

## 3. Evicting entries without synchronizing metadata

Removing an entry from the value map but not from the LRU structure creates inconsistent cache state.

---

## 4. Using one global lock without discussing contention

A single lock may be correct for a small interview implementation, but it limits concurrent throughput.

State the trade-off:

> I would begin with one lock for correctness, measure contention, and then consider segmentation or a proven cache library if throughput requires it.

---

## 5. Treating eviction and expiration as identical

- **Eviction** usually removes entries because capacity is exceeded.
- **Expiration** removes entries because their lifetime has ended.
- **Invalidation** removes entries because application data changed.

---

## 6. Caching mutable objects without protection

```java
User user = cache.get(userId);
user.setStatus(Status.BLOCKED);
```

The caller has modified the cached object.

Prefer:

- Immutable cached values
- Defensive copies
- Controlled update methods
- Clearly documented ownership

---

# Interview Questions

## Question 1: Why use `LinkedHashMap` for LRU?

Access-order `LinkedHashMap` maintains entries from least recently accessed to most recently accessed, allowing the eldest entry to be selected efficiently.

---

## Question 2: Is a synchronized LRU cache scalable?

It is thread-safe but may not scale under heavy contention because every operation acquires the same lock. It is suitable as a simple correct design, not necessarily as a high-throughput production implementation.

---

## Question 3: How do you prevent duplicate loads?

Use per-key coordination, request coalescing, a future representing the active load, or an appropriate atomic loading operation.

---

## Question 4: What happens when the backing service fails?

Possible approaches include:

- Return a valid stale value
- Propagate the failure
- Use a fallback
- Cache selected negative results briefly
- Apply a timeout and circuit breaker

The correct policy depends on business requirements.

---

## Question 5: How would you make the cache distributed?

Move shared entries to a distributed store, define serialization and key ownership, use atomic operations where needed, introduce expiry and eviction policies, and design invalidation and failure behavior across application instances.

---

# Short Interview Answers

## Debugging `ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63`

> I inspect the stack trace, identify the fail-fast iterator, and search for direct, indirect, or cross-thread structural changes to the same collection. I fix current-element removal using `Iterator.remove()` or `removeIf()`, defer modifications until after iteration, or use a concurrent collection when concurrent changes are part of the design.

## Designing a cache

> I first define capacity, eviction, expiration, concurrency, and consistency requirements. For a simple local LRU cache, I use access-order `LinkedHashMap` with bounded size and synchronization. For production, I add TTL, weight-based limits, request coalescing, metrics, invalidation, immutable values, and a clear strategy for stale data and backing-service failures.
