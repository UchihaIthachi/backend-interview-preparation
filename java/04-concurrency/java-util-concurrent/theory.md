# `java.util.concurrent` Utilities

## 1. Definition

The `java.util.concurrent` package provides high-level building blocks for writing concurrent Java applications.

It includes:

- Executor services and thread pools
- Concurrent collections
- Blocking queues
- Atomic variables
- Explicit locks
- Synchronization aids
- Asynchronous computation utilities
- Fork/join processing

These utilities reduce the need to implement low-level synchronization manually with `Thread`, `wait()`, `notify()`, and custom locking protocols.

> Not every class in `java.util.concurrent` is lock-free. Some use locks, some use compare-and-set operations, and others combine multiple techniques internally.

---

## 2. Why Does It Exist?

Writing correct multithreaded code from scratch is difficult because developers must handle:

- Atomicity
- Memory visibility
- Ordering
- Lock acquisition and release
- Waiting and notification
- Interruption
- Deadlock prevention
- Cancellation
- Resource limits
- Graceful shutdown

For example, implementing a producer-consumer queue manually requires careful synchronization:

```java
public synchronized void put(Task task)
        throws InterruptedException {

    while (queue.size() == capacity) {
        wait();
    }

    queue.add(task);
    notifyAll();
}
```

Java already provides `BlockingQueue`, which handles this coordination:

```java
BlockingQueue<Task> queue =
        new ArrayBlockingQueue<>(100);

queue.put(task);
Task next = queue.take();
```

The second version is clearer, less error-prone, and based on a well-tested concurrency abstraction.

---

# Package Overview

## 3. Main Categories

| Category                  | Important classes                                                                                      |
| ------------------------- | ------------------------------------------------------------------------------------------------------ |
| Executors                 | `Executor`, `ExecutorService`, `ThreadPoolExecutor`                                                    |
| Scheduling                | `ScheduledExecutorService`                                                                             |
| Asynchronous computation  | `Future`, `CompletableFuture`                                                                          |
| Concurrent maps           | `ConcurrentHashMap`, `ConcurrentSkipListMap`                                                           |
| Concurrent sets           | `ConcurrentHashMap.newKeySet()`, `ConcurrentSkipListSet`                                               |
| Concurrent queues         | `ConcurrentLinkedQueue`, `ConcurrentLinkedDeque`                                                       |
| Blocking queues           | `ArrayBlockingQueue`, `LinkedBlockingQueue`, `PriorityBlockingQueue`, `DelayQueue`, `SynchronousQueue` |
| Copy-on-write collections | `CopyOnWriteArrayList`, `CopyOnWriteArraySet`                                                          |
| Synchronizers             | `CountDownLatch`, `CyclicBarrier`, `Phaser`, `Semaphore`, `Exchanger`                                  |
| Locks                     | `ReentrantLock`, `ReadWriteLock`, `StampedLock`                                                        |
| Atomics                   | `AtomicInteger`, `AtomicLong`, `AtomicReference`                                                       |
| High-contention counters  | `LongAdder`, `LongAccumulator`                                                                         |
| Fork/join                 | `ForkJoinPool`, `RecursiveTask`, `RecursiveAction`                                                     |

---

# Executors

## 4. `ExecutorService`

`ExecutorService` separates tasks from thread management.

```java
ExecutorService executor =
        Executors.newFixedThreadPool(4);

Future<Integer> future =
        executor.submit(() -> calculate());

try {
    Integer result = future.get();
    System.out.println(result);
} finally {
    executor.shutdown();
}
```

It provides:

- Task submission
- Worker management
- Result tracking
- Cancellation
- Bulk execution
- Shutdown control

For production systems, explicitly configuring `ThreadPoolExecutor` often provides better resource control:

```java
ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
                8,
                16,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1_000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
```

This controls both:

- Active worker count
- Waiting task backlog

---

## 5. `ScheduledExecutorService`

Use it for delayed or recurring work.

```java
ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(2);

scheduler.schedule(
        this::refreshCache,
        10,
        TimeUnit.SECONDS
);
```

Periodic execution:

```java
scheduler.scheduleAtFixedRate(
        this::collectMetrics,
        0,
        30,
        TimeUnit.SECONDS
);
```

### Fixed rate vs fixed delay

| Method                     | Behavior                                           |
| -------------------------- | -------------------------------------------------- |
| `scheduleAtFixedRate()`    | Attempts to maintain a regular start-time schedule |
| `scheduleWithFixedDelay()` | Waits for a delay after one execution finishes     |

Use fixed delay when the next execution should depend on completion of the previous execution.

---

# Concurrent Collections

## 6. `ConcurrentHashMap`

`ConcurrentHashMap` supports concurrent access without locking the entire map for every operation.

```java
ConcurrentMap<String, Integer> counts =
        new ConcurrentHashMap<>();

counts.merge(
        "SUCCESS",
        1,
        Integer::sum
);
```

Useful atomic methods include:

```java
map.putIfAbsent(key, value);
map.computeIfAbsent(key, this::loadValue);
map.computeIfPresent(key, this::updateValue);
map.merge(key, value, this::combine);
map.replace(key, oldValue, newValue);
map.remove(key, expectedValue);
```

### Why atomic map methods matter

This is not atomic:

```java
if (!map.containsKey(key)) {
    map.put(key, value);
}
```

Another thread may insert the key between the two calls.

Prefer:

```java
map.putIfAbsent(key, value);
```

### Iteration behavior

`ConcurrentHashMap` iterators are **weakly consistent**.

They:

- Do not normally throw `ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63`
- May observe some concurrent updates
- Do not provide a fully isolated snapshot
- Can continue while other threads modify the map

---

## 7. `ConcurrentSkipListMap`

`ConcurrentSkipListMap` is a concurrent sorted map.

```java
ConcurrentNavigableMap<Long, Order> orders =
        new ConcurrentSkipListMap<>();

orders.put(order.id(), order);
```

It supports operations such as:

```java
orders.firstEntry();
orders.lastEntry();
orders.floorEntry(id);
orders.ceilingEntry(id);
orders.subMap(from, true, to, false);
```

Use it when both concurrency and key ordering are required.

For unordered key lookup, `ConcurrentHashMap` is generally the simpler choice.

---

## 8. `ConcurrentLinkedQueue`

`ConcurrentLinkedQueue` is a non-blocking, thread-safe FIFO queue.

```java
Queue<Task> queue =
        new ConcurrentLinkedQueue<>();

queue.offer(task);

Task next = queue.poll();
```

It is useful when:

- Producers should not block
- Consumers can handle an empty result
- Unbounded growth is acceptable or externally controlled

It does not provide backpressure.

If producers must wait when capacity is reached, use a bounded `BlockingQueue`.

---

## 9. `CopyOnWriteArrayList`

`CopyOnWriteArrayList` creates a new internal array whenever it is structurally modified.

```java
CopyOnWriteArrayList<Listener> listeners =
        new CopyOnWriteArrayList<>();

listeners.add(listener);

for (Listener current : listeners) {
    current.onEvent(event);
}
```

### Suitable workloads

- Reads are extremely frequent.
- Writes are rare.
- Iteration must remain stable during concurrent modifications.
- Collections are relatively small.

Common examples:

- Event listeners
- Configuration snapshots
- Subscription lists

### Trade-off

Writes are expensive because the internal array is copied.

Do not use it for:

- Frequent insertions
- Large collections with regular updates
- High-volume producer workloads

Its iterators observe a snapshot and do not support iterator mutation.

---

# Blocking Queues

## 10. `BlockingQueue`

`BlockingQueue` is designed for producer-consumer coordination.

```java
BlockingQueue<Job> jobs =
        new ArrayBlockingQueue<>(100);
```

Producer:

```java
jobs.put(job);
```

Consumer:

```java
Job job = jobs.take();
```

Behavior:

- `put()` waits when full.
- `take()` waits when empty.
- Queue operations provide the required thread-safety and visibility.
- A bounded queue provides backpressure.

---

## 11. Common Blocking Queue Implementations

| Queue                   | Main characteristic                             |
| ----------------------- | ----------------------------------------------- |
| `ArrayBlockingQueue`    | Bounded array-based FIFO queue                  |
| `LinkedBlockingQueue`   | Optionally bounded linked FIFO queue            |
| `PriorityBlockingQueue` | Unbounded priority-ordered queue                |
| `DelayQueue`            | Elements become available after a delay         |
| `SynchronousQueue`      | Direct handoff with no storage                  |
| `LinkedTransferQueue`   | Supports transfer directly to waiting consumers |

### `ArrayBlockingQueue`

```java
BlockingQueue<Task> queue =
        new ArrayBlockingQueue<>(1_000);
```

Good when strict capacity is required.

### `PriorityBlockingQueue`

```java
BlockingQueue<Job> jobs =
        new PriorityBlockingQueue<>(
                100,
                Comparator.comparingInt(Job::priority)
        );
```

It is unbounded from an API-capacity perspective. Supplying an initial capacity does not make it bounded.

### `SynchronousQueue`

```java
BlockingQueue<Task> handoff =
        new SynchronousQueue<>();
```

Each insertion must wait for a corresponding removal. It does not store elements.

---

# Synchronization Aids

## 12. `CountDownLatch`

A `CountDownLatch` allows threads to wait until a fixed number of operations finish.

```java
int workerCount = 5;

CountDownLatch ready =
        new CountDownLatch(workerCount);

for (int index = 0;
     index < workerCount;
     index++) {

    executor.submit(() -> {
        try {
            initialize();
        } finally {
            ready.countDown();
        }
    });
}

boolean completed =
        ready.await(30, TimeUnit.SECONDS);
```

Characteristics:

- One-shot
- Count only decreases
- Cannot be reset
- Good for startup coordination and waiting for task completion

---

## 13. `CyclicBarrier`

A `CyclicBarrier` makes a fixed number of threads wait for one another at a common phase boundary.

```java
CyclicBarrier barrier =
        new CyclicBarrier(
                4,
                () -> System.out.println(
                        "All workers reached the barrier"
                )
        );
```

Worker:

```java
performPhaseOne();
barrier.await();

performPhaseTwo();
barrier.await();
```

Unlike `CountDownLatch`, a cyclic barrier can be reused.

Use it for:

- Multi-phase algorithms
- Parallel simulations
- Repeated batch synchronization

---

## 14. `Phaser`

`Phaser` supports reusable phases with dynamic registration and deregistration.

```java
Phaser phaser = new Phaser(1);

phaser.register();

executor.submit(() -> {
    try {
        performPhase();
        phaser.arriveAndAwaitAdvance();
    } finally {
        phaser.arriveAndDeregister();
    }
});
```

Use it when:

- Participants can join or leave
- Several synchronization phases exist
- `CountDownLatch` is too static
- `CyclicBarrier` participant count is too rigid

---

## 15. `Semaphore`

A semaphore maintains a number of permits.

```java
Semaphore permits =
        new Semaphore(10);
```

```java
boolean acquired =
        permits.tryAcquire(
                2,
                TimeUnit.SECONDS
        );

if (!acquired) {
    throw new ServiceBusyException();
}

try {
    callExternalService();
} finally {
    permits.release();
}
```

Common uses:

- Limit API-call concurrency
- Limit access to database-like resources
- Protect a scarce resource
- Implement admission control

### Important distinction

A semaphore limits concurrent activity, not operations per second.

For example:

```text
Semaphore with 10 permits
→ maximum 10 active operations
```

It does not mean:

```text
maximum 10 operations per second
```

A token-bucket or similar algorithm is required for rate limiting.

---

## 16. `Exchanger`

`Exchanger<T>` lets two threads exchange objects at a synchronization point.

```java
Exchanger<List<Event>> exchanger =
        new Exchanger<>();

List<Event> received =
        exchanger.exchange(currentBuffer);
```

It is useful for:

- Double buffering
- Producer-consumer batch exchange
- Two-party pipeline coordination

Both threads wait until the other participant reaches the exchange.

---

# Explicit Locks

## 17. `ReentrantLock`

`ReentrantLock` provides explicit mutual exclusion.

```java
private final ReentrantLock lock =
        new ReentrantLock();

public void update() {
    lock.lock();

    try {
        updateSharedState();
    } finally {
        lock.unlock();
    }
}
```

It supports:

- Reentrant acquisition
- `tryLock()`
- Timed lock attempts
- Interruptible lock acquisition
- Optional fairness
- Multiple `Condition` objects

```java
boolean acquired =
        lock.tryLock(
                500,
                TimeUnit.MILLISECONDS
        );
```

Always release it in `finally`.

---

## 18. `Condition`

A `Condition` provides explicit wait sets associated with a `Lock`.

```java
private final ReentrantLock lock =
        new ReentrantLock();

private final Condition notEmpty =
        lock.newCondition();

private final Condition notFull =
        lock.newCondition();
```

Waiting:

```java
while (queue.isEmpty()) {
    notEmpty.await();
}
```

Signalling:

```java
queue.add(item);
notEmpty.signal();
```

Unlike an intrinsic monitor, one lock can have several conditions, allowing more targeted signalling.

---

## 19. `ReadWriteLock`

`ReadWriteLock` separates shared reads from exclusive writes.

```java
private final ReadWriteLock lock =
        new ReentrantReadWriteLock();

public Value read() {
    lock.readLock().lock();

    try {
        return value;
    } finally {
        lock.readLock().unlock();
    }
}
```

```java
public void write(Value newValue) {
    lock.writeLock().lock();

    try {
        value = newValue;
    } finally {
        lock.writeLock().unlock();
    }
}
```

Use it when:

- Reads are frequent
- Writes are relatively rare
- Read operations are substantial enough to benefit from concurrency

For very small critical sections, a normal lock may be faster and simpler.

---

## 20. `StampedLock`

`StampedLock` supports:

- Write locking
- Pessimistic read locking
- Optimistic reading

```java
long stamp =
        lock.tryOptimisticRead();

double observedX = x;
double observedY = y;

if (!lock.validate(stamp)) {
    stamp = lock.readLock();

    try {
        observedX = x;
        observedY = y;
    } finally {
        lock.unlockRead(stamp);
    }
}
```

Important characteristics:

- Not reentrant
- More complex than `ReadWriteLock`
- Optimistic reads must be validated
- Useful for highly read-dominated compound state
- Does not directly provide `Condition`

Use it only when measurement justifies the extra complexity.

---

# Atomic Variables

## 21. Atomic Classes

Atomic classes provide atomic operations on individual values.

Common examples:

```java
AtomicInteger
AtomicLong
AtomicBoolean
AtomicReference<T>
AtomicStampedReference<T>
AtomicMarkableReference<T>
```

Example:

```java
AtomicInteger counter =
        new AtomicInteger();

counter.incrementAndGet();
```

Compare-and-set:

```java
boolean updated =
        counter.compareAndSet(
                expectedValue,
                newValue
        );
```

---

## 22. Compare and Set

Compare-and-set conceptually performs:

```text
if current value equals expected value:
    replace it with new value
    return true
otherwise:
    do not modify it
    return false
```

Example retry loop:

```java
int current;
int updated;

do {
    current = counter.get();
    updated = current + 1;
} while (!counter.compareAndSet(
        current,
        updated
));
```

CAS is useful for simple independent state transitions.

It is less suitable when several fields must change together as one invariant.

---

## 23. `LongAdder`

`LongAdder` is designed for counters with high concurrent update contention.

```java
LongAdder requests =
        new LongAdder();

requests.increment();

long currentTotal =
        requests.sum();
```

It distributes updates across internal cells and combines them when read.

### `AtomicLong` vs `LongAdder`

| `AtomicLong`                        | `LongAdder`                                                             |
| ----------------------------------- | ----------------------------------------------------------------------- |
| One atomic value                    | Distributed internal counters                                           |
| Exact result after each operation   | `sum()` may not represent one atomic snapshot during concurrent updates |
| Good for state transitions          | Good for statistics and metrics                                         |
| More contention under heavy updates | Better update scalability                                               |

Use `AtomicLong` when the exact current value participates in logic.

Use `LongAdder` for metrics such as request counters.

---

# Asynchronous Computation

## 24. `Future`

A `Future<T>` represents a task that may complete later.

```java
Future<Result> future =
        executor.submit(this::calculate);

Result result =
        future.get(
                5,
                TimeUnit.SECONDS
        );
```

It supports:

- Waiting
- Timeout
- Cancellation
- Completion checks
- Exception retrieval

Limitations include:

- Blocking `get()`
- No direct transformation
- No direct composition
- No callback-based recovery
- No manual completion

---

## 25. `CompletableFuture`

`CompletableFuture` supports completion-driven composition.

```java
CompletableFuture<OrderSummary> result =
        CompletableFuture
                .supplyAsync(
                        () -> loadOrder(orderId),
                        executor
                )
                .thenCompose(order ->
                        paymentClient.chargeAsync(order)
                )
                .thenApply(payment ->
                        createSummary(payment)
                )
                .orTimeout(
                        5,
                        TimeUnit.SECONDS
                );
```

Important methods include:

| Method                | Purpose                                  |
| --------------------- | ---------------------------------------- |
| `thenApply()`         | Transform a result                       |
| `thenCompose()`       | Chain a dependent asynchronous operation |
| `thenCombine()`       | Combine independent results              |
| `thenAccept()`        | Consume a result                         |
| `thenRun()`           | Run a completion action                  |
| `allOf()`             | Wait for all futures                     |
| `anyOf()`             | Complete when one future completes       |
| `exceptionally()`     | Convert a failure to a fallback          |
| `handle()`            | Process success or failure               |
| `whenComplete()`      | Observe completion                       |
| `orTimeout()`         | Complete exceptionally after timeout     |
| `completeOnTimeout()` | Supply a fallback after timeout          |

Avoid blocking workers on nested tasks submitted to the same saturated executor.

---

# Fork/Join Framework

## 26. `ForkJoinPool`

`ForkJoinPool` is intended for CPU-bound work that can be recursively divided.

```java
public final class SumTask
        extends RecursiveTask<Long> {

    private static final int THRESHOLD = 10_000;

    private final long[] values;
    private final int start;
    private final int end;

    @Override
    protected Long compute() {
        int length = end - start;

        if (length <= THRESHOLD) {
            return calculateDirectly();
        }

        int middle = start + length / 2;

        SumTask left =
                new SumTask(values, start, middle);

        SumTask right =
                new SumTask(values, middle, end);

        left.fork();

        long rightResult = right.compute();
        long leftResult = left.join();

        return leftResult + rightResult;
    }
}
```

Workers use work stealing to help balance tasks across the pool.

Good candidates include:

- Array processing
- Tree traversal
- Recursive calculations
- CPU-heavy divide-and-conquer work

Avoid uncontrolled blocking I/O in fork/join workers.

---

# Visibility and Happens-Before

## 27. Memory-Visibility Guarantees

The utilities do more than prevent simultaneous mutation. They also establish memory-ordering relationships.

For example:

```java
queue.put(task);
```

in one thread and:

```java
Task task = queue.take();
```

in another thread establish the visibility required for safely handing the task between threads.

Similarly:

- Unlocking a lock happens-before a later successful acquisition of that lock.
- A volatile write happens-before a later read of that variable.
- Actions before `CountDownLatch.countDown()` become visible after a successful `await()`.
- Task submission and result retrieval establish relevant visibility guarantees.

This is why replacing a concurrent utility with a random collection plus polling is often incorrect even when it appears to work.

---

# Choosing the Right Utility

## 28. Selection Guide

| Requirement                               | Recommended utility        |
| ----------------------------------------- | -------------------------- |
| Run tasks through managed workers         | `ExecutorService`          |
| Run delayed or periodic tasks             | `ScheduledExecutorService` |
| Producer-consumer with backpressure       | Bounded `BlockingQueue`    |
| Concurrent key-value lookup               | `ConcurrentHashMap`        |
| Concurrent sorted map                     | `ConcurrentSkipListMap`    |
| Frequent reads and rare writes            | `CopyOnWriteArrayList`     |
| Wait for fixed operations to complete     | `CountDownLatch`           |
| Fixed participants across repeated phases | `CyclicBarrier`            |
| Dynamic participants across phases        | `Phaser`                   |
| Limit concurrent resource access          | `Semaphore`                |
| Exchange buffers between two threads      | `Exchanger`                |
| Explicit locking with timeout             | `ReentrantLock`            |
| Multiple readers, exclusive writer        | `ReadWriteLock`            |
| Optimistic read-heavy compound state      | `StampedLock`              |
| Independent atomic integer state          | `AtomicInteger`            |
| Highly contended metrics counter          | `LongAdder`                |
| Compose asynchronous results              | `CompletableFuture`        |
| Recursive CPU-bound processing            | `ForkJoinPool`             |

---

# Common Mistakes

## 29. Assuming Every Concurrent Class Is Lock-Free

`java.util.concurrent` contains different implementation strategies.

Examples:

- Some atomics use CAS.
- `ReentrantLock` explicitly uses locking.
- `ArrayBlockingQueue` uses locks and conditions.
- `ConcurrentHashMap` combines CAS and localized synchronization.
- `CopyOnWriteArrayList` copies its internal array under write coordination.

Choose utilities by contract and workload, not by assuming “lock-free means faster.”

---

## 30. Using Concurrent Collections but Non-Atomic Workflows

This is still unsafe:

```java
if (!map.containsKey(key)) {
    map.put(key, createValue());
}
```

Use:

```java
map.computeIfAbsent(
        key,
        this::createValue
);
```

Thread-safe methods do not automatically make a sequence of methods atomic.

---

## 31. Using an Unbounded Queue Under Unbounded Load

```java
BlockingQueue<Task> queue =
        new LinkedBlockingQueue<>();
```

When producers are faster than consumers:

```text
queue growth
→ increasing latency
→ memory pressure
→ possible OutOfMemoryError
```

Use a bounded queue when the system requires backpressure.

---

## 32. Forgetting Interruption

Avoid swallowing `InterruptedException`:

```java
catch (InterruptedException exception) {
    // Ignored
}
```

Either propagate it or restore the interrupt flag:

```java
catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
}
```

Interruption is often a shutdown or cancellation request.

---

## 33. Forgetting to Release Permits or Locks

Incorrect semaphore use:

```java
semaphore.acquire();
callService();
semaphore.release();
```

Correct:

```java
semaphore.acquire();

try {
    callService();
} finally {
    semaphore.release();
}
```

The same principle applies to explicit locks.

---

## 34. Using `CopyOnWriteArrayList` for Write-Heavy Workloads

Every structural write creates a new array copy.

Use it only when reads greatly outnumber writes.

---

## 35. Assuming Concurrent Iteration Is a Snapshot

A `ConcurrentHashMap` iterator is weakly consistent, not a transactionally isolated snapshot.

When a snapshot is required:

- Copy the data under suitable coordination.
- Use immutable snapshots.
- Use application versioning.
- Use a database transaction where appropriate.

---

## 36. Using Synchronizers Without Timeouts

This may wait forever:

```java
latch.await();
```

Production code may require:

```java
boolean completed =
        latch.await(
                30,
                TimeUnit.SECONDS
        );
```

Timeout behavior must then be handled explicitly.

---

## 37. Selecting Complexity Before Measurement

Do not immediately replace:

```java
synchronized
```

with:

```java
StampedLock
```

or a custom CAS algorithm.

The simpler mechanism may be:

- Easier to verify
- Easier to maintain
- Fast enough
- Less likely to contain subtle correctness bugs

Measure contention before increasing complexity.

---

# Interview Questions

## Question 1: What is the purpose of `java.util.concurrent`?

> It provides tested high-level abstractions for task execution, concurrent collections, atomic updates, locking and thread coordination. These utilities reduce the need to implement low-level synchronization manually.

## Question 2: What is the difference between `ConcurrentHashMap` and `Collections.synchronizedMap()`?

> A synchronized map generally serializes operations through one monitor, while `ConcurrentHashMap` is designed for higher concurrency using more localized coordination. Its iterators are weakly consistent and it also provides atomic compound methods such as `computeIfAbsent()` and `merge()`.

## Question 3: When should `BlockingQueue` be used?

> It is appropriate for producer-consumer workflows where producers and consumers need safe coordination. A bounded implementation also provides backpressure.

## Question 4: `CountDownLatch` vs `CyclicBarrier`?

> A latch is a one-shot counter used to wait for operations to finish. A cyclic barrier repeatedly makes a fixed number of participants wait for each other at phase boundaries.

## Question 5: When should `Phaser` be used?

> Use it for reusable multi-phase coordination when participants may dynamically register or deregister.

## Question 6: What does a semaphore control?

> It controls the number of threads or tasks that can access a resource concurrently. It limits active concurrency, not requests per second.

## Question 7: `AtomicInteger` vs `LongAdder`?

> `AtomicInteger` represents one exact atomic value and supports state transitions such as compare-and-set. `LongAdder` spreads updates to reduce contention and is better for high-volume metrics where `sum()` need not be an atomic snapshot.

## Question 8: Why use `ReentrantLock` instead of `synchronized`?

> Use it when timed, interruptible or fair acquisition or multiple condition queues are required. Otherwise, `synchronized` is often simpler.

## Question 9: Are concurrent collection iterators strongly consistent?

> Not necessarily. For example, `ConcurrentHashMap` iterators are weakly consistent: they tolerate concurrent updates but do not provide an isolated snapshot.

## Question 10: Why prefer high-level utilities?

> They encode established concurrency patterns, provide memory-visibility guarantees and reduce the chance of errors involving waiting, notification, locking, interruption and cancellation.

---

# Short Interview Answer

> `java.util.concurrent` provides high-level concurrency building blocks. I use executors for task management, concurrent collections for shared data, blocking queues for producer-consumer workflows, atomics for independent state updates, synchronizers such as latches and semaphores for coordination, and explicit locks when advanced lock behavior is required. I choose the highest-level abstraction that matches the problem because it is usually safer and easier to maintain than writing custom synchronization.

## Related Topics

- [Race Conditions](../race-conditions/theory.md)
- [Thread Fundamentals](../thread-fundamentals/theory.md)
- [Executors](../executors/theory.md)
- [Atomic Variables](../atomic-variables/theory.md)
- [Locks](../locks/theory.md)
- [Java Memory Model](../java-memory-model/theory.md)
