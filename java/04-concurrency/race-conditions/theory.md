# Race Conditions: Why `count++` Is Not Thread-Safe

## 1. Definition

A **race condition** occurs when a program’s correctness depends on the unpredictable timing or interleaving of multiple threads accessing shared state.

Race conditions usually involve:

- Shared mutable state
- Multiple concurrent threads
- At least one write operation
- Missing or insufficient synchronization

The same code may:

- Work correctly during development
- Fail only occasionally
- Fail more often under production traffic
- Produce incorrect data without throwing an exception

---

## 2. Why `count++` Is Unsafe

The expression:

```java
count++;
```

looks like one operation, but it is a **read-modify-write** sequence.

Conceptually, it performs:

```text
1. Read the current value of count
2. Add one
3. Write the new value back
```

These steps are not atomic as a group.

### Lost-update example

Suppose `count` is initially `5`:

```text
Thread A reads count = 5
Thread B reads count = 5

Thread A calculates 6
Thread B calculates 6

Thread A writes count = 6
Thread B writes count = 6
```

Two increments were attempted, but the final result is `6` instead of `7`.

One update was lost.

---

## 3. Unsafe Counter Example

```java
public final class UnsafeCounter {

    private int count;

    public void increment() {
        count++;
    }

    public int get() {
        return count;
    }
}
```

Multiple threads can invoke `increment()` concurrently and overwrite one another’s changes.

The problem is not limited to `++`. Similar read-modify-write operations include:

```java
count--;
count += 10;
balance = balance - amount;
map.put(key, map.get(key) + 1);
```

Each operation may depend on a previously read value.

---

# Correct Solutions

## 4. Fix with `synchronized`

```java
public final class SynchronizedCounter {

    private int count;

    public synchronized void increment() {
        count++;
    }

    public synchronized int get() {
        return count;
    }
}
```

A synchronized method provides:

- Mutual exclusion
- Visibility
- Ordering guarantees

Only one thread at a time can execute a synchronized instance method on the same object monitor.

### Smaller critical section

When the method performs unrelated work, synchronize only the shared-state update:

```java
public final class CounterService {

    private final Object lock = new Object();
    private int count;

    public void process() {
        performIndependentWork();

        synchronized (lock) {
            count++;
        }

        performMoreIndependentWork();
    }

    public int getCount() {
        synchronized (lock) {
            return count;
        }
    }

    private void performIndependentWork() {
    }

    private void performMoreIndependentWork() {
    }
}
```

Keep critical sections as small as correctness allows.

---

## 5. Fix with `AtomicInteger`

```java
import java.util.concurrent.atomic.AtomicInteger;

public final class AtomicCounter {

    private final AtomicInteger count =
            new AtomicInteger();

    public void increment() {
        count.incrementAndGet();
    }

    public int get() {
        return count.get();
    }
}
```

Common atomic operations include:

```java
count.incrementAndGet();
count.getAndIncrement();
count.addAndGet(10);
count.getAndAdd(10);
count.updateAndGet(value -> value * 2);
count.compareAndSet(expectedValue, newValue);
```

### `incrementAndGet()` vs `getAndIncrement()`

```java
int updated = count.incrementAndGet();
```

Returns the value **after** incrementing.

```java
int previous = count.getAndIncrement();
```

Returns the value **before** incrementing.

---

## 6. How `AtomicInteger` Works

Atomic classes commonly use **compare-and-set**, or CAS.

Conceptually:

```text
Read the current value
Calculate the new value
Attempt to replace the current value
only if it has not changed
```

A simplified increment loop looks like this:

```java
public void increment(AtomicInteger value) {
    int current;
    int updated;

    do {
        current = value.get();
        updated = current + 1;
    } while (!value.compareAndSet(current, updated));
}
```

If another thread modifies the value before the CAS operation succeeds, the operation retries with the new value.

### CAS characteristics

- No explicit application-level lock
- Atomic conditional update
- Good performance under low or moderate contention
- May retry repeatedly under heavy contention
- Does not automatically protect several related variables

It is more accurate to say `AtomicInteger` provides atomic operations using CAS-style mechanisms than to assume every implementation maps directly to one specific CPU instruction.

---

# Why `volatile` Is Not Enough

## 7. Visibility vs Atomicity

This is still unsafe:

```java
public final class VolatileCounter {

    private volatile int count;

    public void increment() {
        count++;
    }
}
```

`volatile` provides:

- Visibility of writes between threads
- Ordering guarantees around volatile reads and writes
- A happens-before relationship between a volatile write and a later volatile read of the same variable

It does **not** turn a compound read-modify-write sequence into one atomic operation.

Two threads may still:

```text
Read the same visible value
Calculate the same result
Overwrite one another
```

### Good use of `volatile`

```java
public final class Worker {

    private volatile boolean running = true;

    public void run() {
        while (running) {
            performWork();
        }
    }

    public void stop() {
        running = false;
    }

    private void performWork() {
    }
}
```

Here, each read and write of `running` is the complete state operation.

---

# Atomicity, Visibility, and Ordering

## 8. Three Different Concurrency Requirements

### Atomicity

An operation appears indivisible.

```java
counter.incrementAndGet();
```

No other thread observes a partial increment.

### Visibility

A thread can observe changes made by another thread.

```java
volatile boolean running;
```

### Ordering

Operations are observed according to the guarantees defined by the Java Memory Model.

These concepts are related but not interchangeable.

| Mechanism       |                                                  Atomicity |                           Visibility | Mutual exclusion |
| --------------- | ---------------------------------------------------------: | -----------------------------------: | ---------------: |
| Plain variable  | Limited to individual primitive/reference reads and writes | Not safely guaranteed across threads |               No |
| `volatile`      |                                 Individual read/write only |                                  Yes |               No |
| `AtomicInteger` |                                Supported atomic operations |                                  Yes | No blocking lock |
| `synchronized`  |                                           Critical section |                                  Yes |              Yes |
| `ReentrantLock` |                                           Critical section |                                  Yes |              Yes |

---

# When Atomics Are Not Enough

## 9. Multi-Variable Invariants

Suppose an account has:

```java
private int balance;
private int version;
```

The following two atomic variables do not make the combined update atomic:

```java
private final AtomicInteger balance =
        new AtomicInteger();

private final AtomicInteger version =
        new AtomicInteger();
```

```java
balance.addAndGet(-amount);
version.incrementAndGet();
```

Another thread may observe:

```text
New balance
Old version
```

or:

```text
Old balance
New version
```

when both values must change together.

Use one lock to protect the invariant:

```java
public final class Account {

    private final Object lock = new Object();

    private int balance;
    private int version;

    public void withdraw(int amount) {
        synchronized (lock) {
            if (amount <= 0) {
                throw new IllegalArgumentException(
                        "Amount must be positive"
                );
            }

            if (balance < amount) {
                throw new IllegalStateException(
                        "Insufficient balance"
                );
            }

            balance -= amount;
            version++;
        }
    }

    public AccountSnapshot snapshot() {
        synchronized (lock) {
            return new AccountSnapshot(
                    balance,
                    version
            );
        }
    }

    public record AccountSnapshot(
            int balance,
            int version
    ) {
    }
}
```

### Rule

> Use atomic classes for independent values. Use a lock when several values or operations form one logical transaction.

---

# `LongAdder` for High-Contention Metrics

## 10. `AtomicLong` vs `LongAdder`

For a heavily contended metrics counter:

```java
import java.util.concurrent.atomic.LongAdder;

public final class RequestMetrics {

    private final LongAdder requests =
            new LongAdder();

    public void recordRequest() {
        requests.increment();
    }

    public long totalRequests() {
        return requests.sum();
    }
}
```

`LongAdder` distributes updates across internal cells and combines them when read.

| `AtomicLong`                             | `LongAdder`                                                  |
| ---------------------------------------- | ------------------------------------------------------------ |
| One atomic value                         | Internally distributed values                                |
| Exact atomic updates                     | Better update scalability under contention                   |
| Supports CAS-based state transitions     | Primarily suited to counters and statistics                  |
| `get()` returns the current atomic value | `sum()` is not one atomic snapshot during concurrent updates |

Use `LongAdder` for:

- Request metrics
- Event counters
- Success/failure statistics

Do not use it when the value controls business logic that requires one exact atomic state.

---

# Production Examples

## 11. Request Counter

Unsafe:

```java
public final class Metrics {

    private long requestCount;

    public void recordRequest() {
        requestCount++;
    }
}
```

Safer:

```java
public final class Metrics {

    private final LongAdder requestCount =
            new LongAdder();

    public void recordRequest() {
        requestCount.increment();
    }

    public long requestCount() {
        return requestCount.sum();
    }
}
```

---

## 12. In-Memory Rate Limiter

This check-and-update sequence is not safe:

```java
if (requestCount < limit) {
    requestCount++;
    allowRequest();
}
```

Two threads may both observe that the limit has not been reached.

A lock can protect the complete decision:

```java
public final class SimpleRateLimiter {

    private final Object lock = new Object();

    private final int limit;
    private int requestCount;

    public SimpleRateLimiter(int limit) {
        this.limit = limit;
    }

    public boolean tryAcquire() {
        synchronized (lock) {
            if (requestCount >= limit) {
                return false;
            }

            requestCount++;
            return true;
        }
    }
}
```

A real rate limiter also needs:

- Time-window management
- Clock behavior
- Reset or replenishment
- Distributed coordination when several instances exist

An in-memory lock protects only one JVM.

---

## 13. Concurrent Map Update

Unsafe:

```java
Map<String, Integer> counts =
        new HashMap<>();

counts.put(
        key,
        counts.getOrDefault(key, 0) + 1
);
```

Using `ConcurrentHashMap` alone does not make the two-step operation atomic.

Use an atomic map method:

```java
ConcurrentMap<String, Integer> counts =
        new ConcurrentHashMap<>();

counts.merge(
        key,
        1,
        Integer::sum
);
```

For highly contended per-key counters:

```java
ConcurrentMap<String, LongAdder> counts =
        new ConcurrentHashMap<>();

counts.computeIfAbsent(
        key,
        ignored -> new LongAdder()
).increment();
```

---

## 14. Shared Cache Initialization

Unsafe check-then-act sequence:

```java
if (!cache.containsKey(key)) {
    cache.put(key, loadValue(key));
}
```

Two threads may both load and insert the value.

Prefer:

```java
Value value = cache.computeIfAbsent(
        key,
        this::loadValue
);
```

The mapping function should remain short and avoid recursively modifying the same map in unsafe ways.

For expensive remote loads, consider cache-stampede controls and failure handling rather than assuming `computeIfAbsent()` solves every caching concern.

---

# How to Test for Race Conditions

## 15. Basic Concurrent Counter Test

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public final class CounterRaceTest {

    public static void main(String[] args)
            throws InterruptedException {

        int threadCount = 20;
        int incrementsPerThread = 100_000;

        UnsafeCounter counter =
                new UnsafeCounter();

        CountDownLatch start =
                new CountDownLatch(1);

        CountDownLatch finished =
                new CountDownLatch(threadCount);

        List<Thread> threads =
                new ArrayList<>();

        for (int index = 0;
             index < threadCount;
             index++) {

            Thread thread = new Thread(() -> {
                try {
                    start.await();

                    for (int increment = 0;
                         increment < incrementsPerThread;
                         increment++) {

                        counter.increment();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            });

            threads.add(thread);
            thread.start();
        }

        start.countDown();
        finished.await();

        int expected =
                threadCount * incrementsPerThread;

        System.out.println(
                "Expected: " + expected
        );

        System.out.println(
                "Actual:   " + counter.get()
        );
    }

    private static final class UnsafeCounter {

        private int count;

        public void increment() {
            count++;
        }

        public int get() {
            return count;
        }
    }
}
```

The start latch increases the chance that threads execute concurrently.

### Important limitation

A test passing once does not prove thread safety.

Race conditions are timing-dependent and may disappear when:

- Logging is added
- Debugging is enabled
- The workload changes
- The machine has fewer cores
- The JVM optimizes code differently

---

## 16. Better Testing Strategy

Use several complementary techniques:

1. Run the operation repeatedly.
2. Increase thread count and iteration count.
3. Use barriers or latches to align thread execution.
4. Test invariants, not only exceptions.
5. Add timeouts so deadlocks do not hang the test suite.
6. Run stress tests on multicore systems.
7. Use concurrency-specific testing tools where appropriate.
8. Inspect thread dumps and Java Flight Recorder data for production issues.

### What not to do

Do not use arbitrary sleeps as proof of concurrency correctness:

```java
Thread.sleep(100);
```

Sleep may influence timing, but it does not create a reliable synchronization relationship.

---

# Common Mistakes

## 17. Believing `volatile` Makes Increment Atomic

```java
private volatile int count;

count++;
```

Visibility is not atomicity.

---

## 18. Synchronizing Writes but Not Reads

Incomplete protection:

```java
public synchronized void increment() {
    count++;
}

public int get() {
    return count;
}
```

The unsynchronized read may not participate in the same locking protocol.

Use:

```java
public synchronized int get() {
    return count;
}
```

or another mechanism that provides the required visibility.

---

## 19. Locking on Publicly Accessible Objects

Avoid:

```java
synchronized (this) {
    count++;
}
```

when external code can also synchronize on the same object and interfere with the locking protocol.

Prefer a private lock:

```java
private final Object lock = new Object();
```

```java
synchronized (lock) {
    count++;
}
```

Using `this` is not inherently incorrect, but private lock ownership reduces accidental interference.

---

## 20. Locking Only Part of an Invariant

Unsafe:

```java
if (balance >= amount) {
    synchronized (lock) {
        balance -= amount;
    }
}
```

The balance can change after the check but before acquiring the lock.

Correct:

```java
synchronized (lock) {
    if (balance >= amount) {
        balance -= amount;
    }
}
```

The check and update must use the same critical section.

---

## 21. Assuming Thread-Safe Components Make a Workflow Thread-Safe

```java
if (!concurrentQueue.isEmpty()) {
    Task task = concurrentQueue.remove();
}
```

The queue may become empty between calls.

Use one atomic operation:

```java
Task task = concurrentQueue.poll();

if (task != null) {
    process(task);
}
```

---

## 22. Over-Synchronizing

This may unnecessarily serialize slow operations:

```java
public synchronized void process(Order order) {
    validate(order);
    repository.save(order);
    remoteClient.notify(order);
}
```

Database and network calls may hold the monitor for a long time.

A better design may update only the shared in-memory state under the lock:

```java
OrderEvent event;

synchronized (lock) {
    event = updateSharedState(order);
}

repository.save(order);
remoteClient.notify(event);
```

Move work outside the lock only when doing so preserves the business invariant.

---

## 23. Assuming Atomics Are Always Faster

Atomic retry loops can perform poorly under heavy contention.

```text
Many threads
→ repeated CAS failures
→ repeated retries
→ wasted CPU
```

Depending on the workload, better choices may include:

- `LongAdder`
- A lock
- Partitioned counters
- Thread-local accumulation followed by merging
- Message passing
- Reducing shared-state access

Measure rather than assuming.

---

# Trade-Offs

| Approach           | Strength                             | Limitation                                            |
| ------------------ | ------------------------------------ | ----------------------------------------------------- |
| `synchronized`     | Simple, structured mutual exclusion  | Serializes access under contention                    |
| `ReentrantLock`    | Timed and interruptible acquisition  | Must unlock manually                                  |
| `AtomicInteger`    | Efficient independent atomic updates | Cannot directly protect multi-field invariants        |
| `LongAdder`        | Scales well for contended metrics    | No single atomic snapshot during concurrent updates   |
| Thread confinement | Avoids synchronization               | State must remain owned by one thread                 |
| Immutability       | Naturally safe to share              | Updates require new objects or controlled replacement |
| Message passing    | Reduces shared mutation              | Requires queues and ownership design                  |

---

# Interview Questions

## Question 1: Why exactly is `count++` not thread-safe?

> `count++` is a read-modify-write operation. Two threads can read the same old value, calculate the same new value and overwrite each other, causing a lost update.

## Question 2: Does making `count` volatile solve the problem?

> No. `volatile` provides visibility and ordering, but it does not make the complete read-modify-write sequence atomic.

## Question 3: How does `AtomicInteger` provide thread safety?

> It provides atomic methods such as `incrementAndGet()` and commonly uses compare-and-set retry logic so an update succeeds only if the value has not changed since it was read.

## Question 4: When should `synchronized` be used instead of an atomic variable?

> Use synchronization or a lock when several fields or operations must change together as one invariant, or when the critical section contains logic that cannot be expressed as one atomic operation.

## Question 5: Is `AtomicInteger` always faster than synchronization?

> No. It often works well for simple values, but heavy contention can cause repeated CAS retries. Performance depends on the workload and must be measured.

## Question 6: When should `LongAdder` be preferred?

> Use `LongAdder` for heavily contended metrics counters where update throughput matters more than obtaining one exact atomic snapshot during concurrent updates.

## Question 7: How would you test for a race condition?

> I would start many threads together using a latch or barrier, execute the operation repeatedly, verify invariants and run the test many times. A passing test does not prove correctness, so I also review the synchronization design and use stress-testing tools.

## Question 8: What is the difference between a race condition and a data race?

> A data race is unsynchronized conflicting access to shared memory, with at least one write. A race condition is broader: correctness depends on timing. A race condition can also occur through incorrectly ordered higher-level operations even when individual memory accesses use thread-safe methods.

---

# Short Interview Answer

> `count++` is not thread-safe because it performs a read, increment and write rather than one indivisible action. Two threads can read the same value and overwrite one another’s updates. `volatile` does not solve this because it provides visibility, not atomic read-modify-write behavior. I use `AtomicInteger` for independent counters, `LongAdder` for highly contended metrics and synchronization or explicit locks when several values must remain consistent together.

## Related Topics

- [Thread Lifecycle and Java Memory Model](threads.md)
- [ExecutorService](executor-service.md)
- [Atomic Variables](atomic-variables.md)
- [Locks](locks.md)
- [Thread Safety](thread-safety.md)
