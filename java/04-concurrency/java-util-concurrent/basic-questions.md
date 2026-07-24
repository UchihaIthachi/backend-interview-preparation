# Basic Questions — Locks, Atomics, Synchronizers, and Concurrent Utilities

## Question 1: What is `ReentrantLock`? How does it differ from `synchronized`?

`ReentrantLock` is an explicit mutual-exclusion lock from `java.util.concurrent.locks`.

It is called **reentrant** because the thread that owns the lock can acquire it again without deadlocking itself. The thread must release it the same number of times it acquired it.

```java
private final ReentrantLock lock = new ReentrantLock();

public void update() {
    lock.lock();

    try {
        updateSharedState();
    } finally {
        lock.unlock();
    }
}
```

### Comparison

| Feature                   | `synchronized`            | `ReentrantLock`                               |
| ------------------------- | ------------------------- | --------------------------------------------- |
| Type                      | Language keyword          | Library class                                 |
| Reentrant                 | Yes                       | Yes                                           |
| Automatic release         | Yes, when the block exits | No                                            |
| Timed acquisition         | No                        | `tryLock(timeout, unit)`                      |
| Non-blocking attempt      | No                        | `tryLock()`                                   |
| Interruptible acquisition | No                        | `lockInterruptibly()`                         |
| Fairness option           | No                        | `new ReentrantLock(true)`                     |
| Condition queues          | One monitor wait set      | Multiple `Condition` objects                  |
| Ease of use               | Simpler                   | More flexible                                 |
| Risk                      | Lower misuse risk         | Forgetting `unlock()` can block other threads |

### Timed lock acquisition

```java
boolean acquired = lock.tryLock(
        500,
        TimeUnit.MILLISECONDS
);

if (!acquired) {
    handleLockTimeout();
    return;
}

try {
    updateSharedState();
} finally {
    lock.unlock();
}
```

### Interruptible acquisition

```java
lock.lockInterruptibly();

try {
    updateSharedState();
} finally {
    lock.unlock();
}
```

### Fairness nuance

```java
ReentrantLock fairLock = new ReentrantLock(true);
```

A fair lock generally favors threads that have waited longer, but it does not guarantee perfect FIFO execution. Fairness may also reduce throughput because it limits barging and increases thread handoff.

### When should each be used?

Use `synchronized` when:

- Simple mutual exclusion is sufficient.
- Timed or interruptible acquisition is unnecessary.
- One condition queue is sufficient.
- Simplicity is more valuable than additional control.

Use `ReentrantLock` when you require:

- `tryLock()`
- Lock-acquisition timeout
- Interruptible waiting
- Fairness
- Multiple `Condition` objects
- Explicit lock coordination across methods

### Interview-ready answer

> Both `synchronized` and `ReentrantLock` provide reentrant mutual exclusion and memory-visibility guarantees. `synchronized` is simpler and automatically releases the monitor. `ReentrantLock` requires explicit unlocking but supports timed, interruptible and optionally fair acquisition, along with multiple condition queues.

---

## Question 2: What is `ReadWriteLock`, and when should it be used?

`ReadWriteLock` separates access into:

- A **read lock**, which can be held by multiple readers concurrently
- A **write lock**, which requires exclusive access

The main implementation is `ReentrantReadWriteLock`.

```java
private final ReadWriteLock lock =
        new ReentrantReadWriteLock();

private Configuration configuration;

public Configuration read() {
    lock.readLock().lock();

    try {
        return configuration;
    } finally {
        lock.readLock().unlock();
    }
}

public void update(Configuration newConfiguration) {
    lock.writeLock().lock();

    try {
        configuration = newConfiguration;
    } finally {
        lock.writeLock().unlock();
    }
}
```

### Behavior

| Situation         | Allowed concurrently? |
| ----------------- | --------------------: |
| Multiple readers  |                   Yes |
| Reader and writer |                    No |
| Multiple writers  |                    No |

### Good use cases

- Reads greatly outnumber writes.
- Read operations are not trivial.
- Shared state contains multiple related fields.
- Concurrent reads meaningfully improve throughput.

Examples include:

- Configuration registries
- In-memory indexes
- Read-heavy lookup structures
- Metadata catalogs

### When it may not help

A read-write lock may perform worse than a normal lock when:

- Critical sections are extremely short.
- Writes are frequent.
- Contention is low.
- The extra lock-management overhead exceeds the benefit.

### Lock upgrade problem

Avoid acquiring the write lock while retaining a read lock:

```java
lock.readLock().lock();

try {
    if (requiresUpdate()) {
        lock.writeLock().lock(); // Dangerous
    }
} finally {
    lock.readLock().unlock();
}
```

Several readers attempting this upgrade can wait indefinitely for every reader—including themselves—to release the read lock.

### Interview-ready answer

> `ReadWriteLock` allows multiple readers but gives writers exclusive access. It is useful for sufficiently expensive, read-heavy shared state. It is not automatically faster than a normal lock, and read-to-write lock upgrades must be handled carefully.

---

## Question 3: What are atomic classes, and what operations are they useful for?

The `java.util.concurrent.atomic` package provides classes that support atomic operations on individual values.

Common classes include:

- `AtomicInteger`
- `AtomicLong`
- `AtomicBoolean`
- `AtomicReference<T>`
- `AtomicIntegerArray`
- `AtomicLongArray`
- `AtomicReferenceArray<T>`
- `AtomicStampedReference<T>`
- `AtomicMarkableReference<T>`
- `LongAdder`
- `LongAccumulator`

### Basic counter

```java
AtomicInteger counter = new AtomicInteger();

int updated = counter.incrementAndGet();
```

### Common operations

```java
counter.get();
counter.set(10);

counter.incrementAndGet();
counter.getAndIncrement();

counter.addAndGet(5);
counter.getAndAdd(5);

counter.compareAndSet(expected, update);

counter.updateAndGet(value -> value * 2);
counter.accumulateAndGet(5, Integer::sum);
```

### Suitable use cases

- Independent counters
- Sequence numbers
- State flags
- Atomic reference replacement
- Simple state machines
- Lock-free-style retry loops
- High-contention metrics

### Limitation

Atomic classes make operations on one atomic variable safe. They do not automatically protect invariants across several fields.

```java
AtomicInteger balance = new AtomicInteger();
AtomicInteger version = new AtomicInteger();
```

Updating both independently does not make this workflow atomic:

```text
Update balance
and
increment version
as one indivisible transaction
```

Use a lock when multiple pieces of state must change together.

### `LongAdder`

`LongAdder` performs better than `AtomicLong` under heavy update contention by distributing updates internally.

```java
LongAdder requestCount = new LongAdder();

requestCount.increment();

long total = requestCount.sum();
```

Use it for statistics and metrics, not for logic that requires one exact atomic value during concurrent updates.

### Interview-ready answer

> Atomic classes provide thread-safe read-modify-write operations on individual values, usually using compare-and-set internally. They are ideal for counters, flags and simple state transitions. For multi-field invariants or complex operations, a lock is usually clearer and safer.

---

## Question 4: What is CAS—Compare and Set?

CAS is an atomic conditional update operation.

Conceptually:

```text
If current value equals expected value:
    replace it with the new value
    return true
Otherwise:
    leave it unchanged
    return false
```

Example:

```java
AtomicInteger value = new AtomicInteger(5);

boolean updated =
        value.compareAndSet(5, 10);

System.out.println(updated);   // true
System.out.println(value.get()); // 10
```

### CAS retry loop

```java
AtomicInteger counter = new AtomicInteger();

public void increment() {
    int current;
    int updated;

    do {
        current = counter.get();
        updated = current + 1;
    } while (!counter.compareAndSet(
            current,
            updated
    ));
}
```

If another thread changes the value between `get()` and `compareAndSet()`, the CAS fails and the operation retries.

### Advantages

- Avoids explicit lock acquisition for simple updates.
- Threads do not wait for a lock owner.
- Performs well under low or moderate contention.
- Used internally by many concurrent algorithms.

### Limitations

- Repeated retries consume CPU.
- High contention can reduce performance.
- Starvation is possible for a thread that repeatedly loses the CAS race.
- Multi-field invariants remain difficult.
- CAS may be affected by the ABA problem.

### ABA problem

```text
Value changes:

A → B → A
```

A thread checking only the current value sees `A` and cannot tell that the value changed temporarily.

Use a version or stamp when the transition history matters:

```java
AtomicStampedReference<Node> reference =
        new AtomicStampedReference<>(
                initialNode,
                0
        );
```

### Interview-ready answer

> CAS atomically replaces a value only when it still matches an expected value. Atomic classes use CAS to implement thread-safe read-modify-write operations without explicit application-level locks. Failed updates retry, which can become expensive under heavy contention.

---

## Question 5: What are concurrent collections? When should they be preferred over synchronized wrappers?

Concurrent collections are designed for safe and efficient access by multiple threads.

### Important concurrent collections

| Class                   | Main use                          |
| ----------------------- | --------------------------------- |
| `ConcurrentHashMap`     | Concurrent key-value lookup       |
| `ConcurrentSkipListMap` | Concurrent sorted map             |
| `ConcurrentSkipListSet` | Concurrent sorted set             |
| `ConcurrentLinkedQueue` | Non-blocking FIFO queue           |
| `ConcurrentLinkedDeque` | Non-blocking double-ended queue   |
| `CopyOnWriteArrayList`  | Read-heavy list with rare writes  |
| `CopyOnWriteArraySet`   | Read-heavy set with rare writes   |
| `ArrayBlockingQueue`    | Bounded producer-consumer queue   |
| `LinkedBlockingQueue`   | Optionally bounded blocking queue |
| `PriorityBlockingQueue` | Unbounded priority queue          |
| `DelayQueue`            | Delayed-element queue             |
| `SynchronousQueue`      | Direct producer-consumer handoff  |

### Important correction: `ConcurrentHashMap`

Modern `ConcurrentHashMap` should not be described as using segment-level locking.

Java 8+ implementations use a combination of techniques such as:

- CAS
- Localized bin synchronization
- Cooperative resizing
- Tree bins for heavy collisions

The important application-level contract is that operations are designed for concurrent access without one global map lock.

### `ConcurrentHashMap` example

```java
ConcurrentMap<String, Integer> counts =
        new ConcurrentHashMap<>();

counts.merge(
        "SUCCESS",
        1,
        Integer::sum
);
```

Prefer atomic compound methods:

```java
map.putIfAbsent(key, value);
map.computeIfAbsent(key, this::loadValue);
map.computeIfPresent(key, this::updateValue);
map.merge(key, value, this::combine);
map.remove(key, expectedValue);
```

This is not atomic:

```java
if (!map.containsKey(key)) {
    map.put(key, value);
}
```

### Synchronized wrapper

```java
Map<String, User> map =
        Collections.synchronizedMap(
                new HashMap<>()
        );
```

A synchronized wrapper generally serializes access using one monitor.

Iteration also requires explicit synchronization:

```java
synchronized (map) {
    for (Map.Entry<String, User> entry
            : map.entrySet()) {

        process(entry);
    }
}
```

### Concurrent collection iteration

A `ConcurrentHashMap` iterator is weakly consistent:

- It tolerates concurrent updates.
- It does not normally throw `ConcurpZEAWYtiB6bJ16NuLbGCc6CZ6jJdKfb63`.
- It may see some updates and not others.
- It does not provide a transactionally isolated snapshot.

### Which should be preferred?

Use a concurrent collection when:

- Concurrent reads and writes are expected.
- Atomic compound methods are useful.
- Higher concurrent throughput matters.
- Weakly consistent iteration is acceptable.

Use a synchronized wrapper when:

- The collection is small.
- Contention is low.
- One monitor around all collection access is acceptable.
- External synchronization of a larger workflow is required.

Concurrent collections are not universally better; select them according to their contracts.

### Interview-ready answer

> Concurrent collections are designed for shared access without serializing every operation through one global monitor. They also provide atomic methods such as `computeIfAbsent()` and weakly consistent iteration. Synchronized wrappers are simpler but normally use one lock and require explicit synchronization during iteration.

---

## Question 6: Explain `CountDownLatch`, `CyclicBarrier`, and `Semaphore`

## `CountDownLatch`

A `CountDownLatch` allows one or more threads to wait until a fixed number of operations have completed.

```java
CountDownLatch latch =
        new CountDownLatch(3);

for (int index = 0; index < 3; index++) {
    executor.submit(() -> {
        try {
            initialize();
        } finally {
            latch.countDown();
        }
    });
}

boolean completed =
        latch.await(
                30,
                TimeUnit.SECONDS
        );
```

Characteristics:

- One-shot
- Count only decreases
- Cannot be reset
- Waiting threads do not decrement the count
- Useful for startup or completion coordination

---

## `CyclicBarrier`

A `CyclicBarrier` makes a fixed number of participants wait for each other at a common point.

```java
CyclicBarrier barrier =
        new CyclicBarrier(
                3,
                () -> System.out.println(
                        "All participants arrived"
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

Characteristics:

- Reusable
- Fixed participant count
- Supports an optional barrier action
- Useful for repeated multi-phase work

---

## `Semaphore`

A semaphore controls the number of permits available for a resource.

```java
Semaphore permits =
        new Semaphore(5);

boolean acquired =
        permits.tryAcquire(
                2,
                TimeUnit.SECONDS
        );

if (!acquired) {
    handleOverload();
    return;
}

try {
    useLimitedResource();
} finally {
    permits.release();
}
```

Characteristics:

- Limits concurrent access
- Can be fair or unfair
- A permit is not tied permanently to a particular thread
- Useful for admission control and scarce resources

### Important distinction

A semaphore limits active concurrency:

```text
At most five operations at the same time
```

It does not directly enforce:

```text
At most five operations per second
```

Requests-per-second control requires a rate limiter.

### Comparison

| Utility          | Main purpose                     | Reusable? | Participants    |
| ---------------- | -------------------------------- | --------: | --------------- |
| `CountDownLatch` | Wait for N completions           |        No | Fixed count     |
| `CyclicBarrier`  | Participants wait for each other |       Yes | Fixed           |
| `Semaphore`      | Limit concurrent access          |       Yes | Not phase-based |

---

## Question 7: What is `Phaser`? How does it improve on `CountDownLatch` and `CyclicBarrier`?

`Phaser` is a reusable multi-phase synchronizer that supports dynamic registration and deregistration.

```java
Phaser phaser = new Phaser(1); // Main participant

phaser.register();

executor.submit(() -> {
    try {
        performPhaseOne();
        phaser.arriveAndAwaitAdvance();

        performPhaseTwo();
        phaser.arriveAndAwaitAdvance();
    } finally {
        phaser.arriveAndDeregister();
    }
});
```

Main participant:

```java
phaser.arriveAndAwaitAdvance();
System.out.println("Phase one completed");

phaser.arriveAndAwaitAdvance();
System.out.println("Phase two completed");

phaser.arriveAndDeregister();
```

### Comparison

| Feature                  | `CountDownLatch` | `CyclicBarrier` | `Phaser`      |
| ------------------------ | ---------------- | --------------- | ------------- |
| One-shot                 | Yes              | No              | No            |
| Reusable phases          | No               | Yes             | Yes           |
| Dynamic participants     | No               | No              | Yes           |
| Deregistration           | No               | No              | Yes           |
| Hierarchical composition | No               | No              | Yes           |
| Optional phase hook      | No               | Barrier action  | `onAdvance()` |

Use `Phaser` for:

- Simulations
- Iterative algorithms
- Variable worker groups
- Multi-stage processing
- Tasks that may join or leave between phases

### Interview-ready answer

> `Phaser` is a reusable barrier with dynamic participant registration. It is more flexible than a one-shot latch and a fixed-party cyclic barrier, especially for workflows where workers join, leave or coordinate over several phases.

---

## Question 8: Explain `StampedLock` and optimistic reading

`StampedLock` supports three modes:

1. Exclusive write lock
2. Shared read lock
3. Optimistic read

```java
private final StampedLock lock =
        new StampedLock();

private double x;
private double y;
```

### Write operation

```java
public void move(double newX, double newY) {
    long stamp = lock.writeLock();

    try {
        x = newX;
        y = newY;
    } finally {
        lock.unlockWrite(stamp);
    }
}
```

### Optimistic read

```java
public Point readPoint() {
    long stamp = lock.tryOptimisticRead();

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

    return new Point(observedX, observedY);
}
```

### How optimistic reading works

1. Obtain an optimistic stamp without acquiring a traditional read lock.
2. Copy all related shared fields into local variables.
3. Validate the stamp.
4. If a write interfered, repeat the read while holding a real read lock.

### Important limitations

- It is not reentrant.
- It does not directly support `Condition`.
- Stamp management is error-prone.
- Optimistic values must not be trusted before validation.
- It does not guarantee fairness.
- It is more complex than `ReentrantReadWriteLock`.

Use it only when:

- Reads heavily outnumber writes.
- Several related fields must be read consistently.
- Profiling shows that read locking is a bottleneck.

### Interview-ready answer

> `StampedLock` adds optimistic reading to read and write locking. The reader copies the state and validates that no write occurred; otherwise, it repeats the read under a normal read lock. It can improve read-heavy workloads but is non-reentrant and more difficult to use correctly.

---

## Question 9: What is `Exchanger`?

`Exchanger<V>` is a two-party synchronizer that allows two threads to exchange object references.

```java
Exchanger<List<Integer>> exchanger =
        new Exchanger<>();
```

Producer:

```java
List<Integer> buffer = fillBuffer();

List<Integer> emptyBuffer =
        exchanger.exchange(buffer);
```

Consumer:

```java
List<Integer> emptyBuffer =
        new ArrayList<>();

List<Integer> fullBuffer =
        exchanger.exchange(emptyBuffer);

consume(fullBuffer);
```

Both threads wait until the other arrives. Their provided objects are then exchanged.

### Use cases

- Double buffering
- Producer-consumer batch handoff
- Pipeline stage coordination
- Reusing buffers without copying elements

### Timeout version

```java
List<Integer> received =
        exchanger.exchange(
                buffer,
                5,
                TimeUnit.SECONDS
        );
```

`Exchanger` is designed for exactly two participants. For multiple producers and consumers, a queue is usually more appropriate.

### Interview-ready answer

> `Exchanger` synchronizes two threads and atomically swaps the object references they provide. It is useful for two-stage pipelines and double buffering where one thread hands off a full buffer and receives an empty one.

---

## Question 10: What is a `Condition` variable?

A `Condition` is associated with a `Lock` and allows threads to wait until a specific state condition becomes true.

It provides methods analogous to monitor operations:

| Monitor method | `Condition` method |
| -------------- | ------------------ |
| `wait()`       | `await()`          |
| `notify()`     | `signal()`         |
| `notifyAll()`  | `signalAll()`      |

### Bounded queue example

```java
private final ReentrantLock lock =
        new ReentrantLock();

private final Condition notEmpty =
        lock.newCondition();

private final Condition notFull =
        lock.newCondition();
```

Producer:

```java
public void put(Task task)
        throws InterruptedException {

    lock.lockInterruptibly();

    try {
        while (queue.size() == capacity) {
            notFull.await();
        }

        queue.add(task);
        notEmpty.signal();
    } finally {
        lock.unlock();
    }
}
```

Consumer:

```java
public Task take()
        throws InterruptedException {

    lock.lockInterruptibly();

    try {
        while (queue.isEmpty()) {
            notEmpty.await();
        }

        Task task = queue.remove();
        notFull.signal();

        return task;
    } finally {
        lock.unlock();
    }
}
```

### Why is it more flexible than `wait()` and `notify()`?

An intrinsic monitor has one wait set. A `ReentrantLock` can have several conditions:

```text
notEmpty condition
notFull condition
shutdown condition
```

This enables targeted signalling instead of waking unrelated waiters.

Always wait inside a `while` loop because:

- Spurious wakeups are possible.
- Another thread may change the condition before the lock is reacquired.
- Several waiters may wake simultaneously.

### Interview-ready answer

> A `Condition` is a wait set attached to an explicit lock. It provides `await`, `signal` and `signalAll`, similar to monitor waiting, but one lock can have several condition queues for more precise thread coordination.

---

# Additional Questions

## Question 31: What are concurrent collections?

Concurrent collections are thread-safe collections specifically designed for shared concurrent access.

They include:

- `ConcurrentHashMap`
- `ConcurrentSkipListMap`
- `ConcurrentLinkedQueue`
- `ConcurrentLinkedDeque`
- `CopyOnWriteArrayList`
- `CopyOnWriteArraySet`
- Blocking queue implementations

They are preferable to ordinary collections plus ad hoc synchronization when their atomic operations and iteration guarantees match the workload.

---

## Question 32: `HashMap` vs `ConcurrentHashMap`

| Feature                 | `HashMap`              | `ConcurrentHashMap`                     |
| ----------------------- | ---------------------- | --------------------------------------- |
| Thread-safe             | No                     | Yes                                     |
| Null key                | One allowed            | Not allowed                             |
| Null values             | Allowed                | Not allowed                             |
| Concurrent modification | Unsafe                 | Supported                               |
| Iterator                | Fail-fast, best effort | Weakly consistent                       |
| Atomic compound methods | Limited                | `putIfAbsent`, `compute`, `merge`, etc. |
| Ordering guarantee      | None                   | None                                    |

### Why does `ConcurrentHashMap` reject nulls?

For concurrent access, this result would be ambiguous:

```java
map.get(key) == null
```

It could mean:

- The key is absent.
- The key maps to `null`.

The mapping could also change immediately after the check. Rejecting nulls keeps the API’s absence semantics clearer.

---

## Question 33: What is a good use case for `CopyOnWriteArrayList`?

It is useful when:

- Reads and iterations are very frequent.
- Writes are rare.
- The collection is relatively small.
- Iterators should observe a stable snapshot.

Example:

```java
CopyOnWriteArrayList<EventListener> listeners =
        new CopyOnWriteArrayList<>();

listeners.add(listener);

for (EventListener current : listeners) {
    current.onEvent(event);
}
```

Good examples:

- Event listeners
- Plugin registries
- Rarely updated configuration lists

Avoid it for write-heavy workloads because every structural modification copies the backing array.

---

## Question 34: `BlockingQueue` vs `Queue`

| `Queue`                               | `BlockingQueue`                                 |
| ------------------------------------- | ----------------------------------------------- |
| Basic queue operations                | Adds blocking and timed operations              |
| `offer()` and `poll()`                | `put()`, `take()`, timed `offer()` and `poll()` |
| No producer-consumer waiting contract | Designed for producer-consumer coordination     |
| May be non-thread-safe                | Implementations are thread-safe                 |

Example:

```java
BlockingQueue<Task> tasks =
        new ArrayBlockingQueue<>(100);

tasks.put(task);   // Waits when full
Task next = tasks.take(); // Waits when empty
```

---

## Question 35: Explain the Producer-Consumer problem

Producers generate work, consumers process it, and a shared buffer coordinates them.

```java
BlockingQueue<Job> queue =
        new ArrayBlockingQueue<>(100);
```

Producer:

```java
queue.put(job);
```

Consumer:

```java
Job job = queue.take();
process(job);
```

A bounded queue provides backpressure when producers are faster than consumers.

---

## Question 36: `CountDownLatch` vs `CyclicBarrier`

| `CountDownLatch`                    | `CyclicBarrier`                           |
| ----------------------------------- | ----------------------------------------- |
| Waits for events or operations      | Participants wait for each other          |
| One-shot                            | Reusable                                  |
| Count decreased with `countDown()`  | Participants call `await()`               |
| Waiting thread need not be a worker | Every participating worker normally waits |
| No barrier action                   | Optional barrier action                   |

---

## Question 37: What is a semaphore?

A semaphore manages permits to restrict concurrent resource usage.

```java
Semaphore permits = new Semaphore(10);

permits.acquire();

try {
    useResource();
} finally {
    permits.release();
}
```

It limits simultaneous operations, not request rate over time.

---

## Question 38: What is a `Phaser`?

A `Phaser` is a reusable phase synchronizer supporting dynamic registration and deregistration.

```java
Phaser phaser = new Phaser();

phaser.register();
phaser.arriveAndAwaitAdvance();
phaser.arriveAndDeregister();
```

It is suitable for multi-phase workflows with changing participant counts.

---

## Question 39: `wait()` vs `sleep()`

| `wait()`                                             | `sleep()`                            |
| ---------------------------------------------------- | ------------------------------------ |
| Method of `Object`                                   | Static method of `Thread`            |
| Must own the object monitor                          | No monitor ownership required        |
| Releases that monitor                                | Does not release held locks          |
| Used for condition coordination                      | Used for time-based pausing          |
| Wakes through notification, interruption, or timeout | Wakes after duration or interruption |

```java
synchronized (lock) {
    lock.wait();
}
```

```java
Thread.sleep(1_000);
```

---

## Question 40: `wait()` vs `notify()` vs `notifyAll()`

- `wait()` releases the monitor and suspends the calling thread.
- `notify()` wakes one arbitrary thread waiting on that monitor.
- `notifyAll()` wakes all waiting threads.

All must be called while owning the same monitor.

```java
synchronized (lock) {
    while (!conditionIsTrue()) {
        lock.wait();
    }
}
```

Use `while`, not `if`, to recheck the condition after waking.

---

## Question 41: What is `ForkJoinPool`?

`ForkJoinPool` is optimized for CPU-bound divide-and-conquer work.

Tasks split recursively:

```java
left.fork();
Result rightResult = right.compute();
Result leftResult = left.join();
```

Idle workers steal tasks from busy workers to improve load balancing.

Avoid uncontrolled blocking I/O in fork/join tasks.

---

## Question 42: What is a parallel stream?

A parallel stream partitions stream processing across multiple workers.

```java
long total = values.parallelStream()
        .mapToLong(this::calculate)
        .sum();
```

It may help when:

- Data is large.
- Work is CPU-intensive.
- Operations are stateless.
- Reduction is associative.
- The source splits efficiently.

It may perform worse for:

- Small collections
- Cheap operations
- Blocking I/O
- Shared mutable state
- Strict ordering
- Contended server environments

Parallel streams commonly use the shared Fork/Join common pool.

---

## Question 43: How does `CompletableFuture` improve performance?

`CompletableFuture` does not automatically improve performance.

It can improve latency or resource utilization when:

- Independent operations run concurrently.
- Blocking is avoided between stages.
- Work is assigned to an appropriate executor.
- Results are composed rather than synchronously waited for.

Example:

```java
CompletableFuture<Price> price =
        CompletableFuture.supplyAsync(
                pricingClient::fetch,
                executor
        );

CompletableFuture<Shipping> shipping =
        CompletableFuture.supplyAsync(
                shippingClient::fetch,
                executor
        );

CompletableFuture<OrderSummary> summary =
        price.thenCombine(
                shipping,
                OrderSummary::new
        );
```

It can reduce performance when:

- Too many tasks are created.
- The common pool is blocked.
- Downstream systems are overloaded.
- Every stage uses unnecessary `Async` scheduling.
- Workers wait on dependent tasks in the same pool.

---

## Question 44: How do you handle exceptions in `CompletableFuture`?

### `exceptionally()`

Converts a failure to a fallback result:

```java
CompletableFuture<User> recovered =
        future.exceptionally(
                failure -> defaultUser()
        );
```

### `handle()`

Runs for success or failure:

```java
CompletableFuture<User> handled =
        future.handle((user, failure) ->
                failure == null
                        ? user
                        : defaultUser()
        );
```

### `whenComplete()`

Observes completion without normally replacing the result:

```java
future.whenComplete((result, failure) -> {
    if (failure != null) {
        log.error("Operation failed", failure);
    }
});
```

### Explicit recovery stage

```java
future.exceptionallyCompose(
        failure -> loadFallbackAsync()
);
```

Add timeouts where appropriate:

```java
future.orTimeout(
        5,
        TimeUnit.SECONDS
);
```

---

## Question 45: What are common `ThreadLocal` use cases?

`ThreadLocal<T>` stores a separate value for each thread.

```java
private static final ThreadLocal<RequestContext> CONTEXT =
        new ThreadLocal<>();
```

Set and remove:

```java
try {
    CONTEXT.set(requestContext);
    processRequest();
} finally {
    CONTEXT.remove();
}
```

Common use cases include:

- Request correlation IDs
- Security context
- Transaction-associated context
- Legacy non-thread-safe formatters
- Per-thread diagnostic information

### Important thread-pool risk

Worker threads are reused. A value left in `ThreadLocal` may leak into another task or retain memory indefinitely.

Always call:

```java
threadLocal.remove();
```

in `finally`.

Prefer explicit parameter passing when practical because it makes dependencies easier to understand and test.

---

## Question 46: Daemon thread vs user thread

| User thread                         | Daemon thread                                 |
| ----------------------------------- | --------------------------------------------- |
| Keeps the JVM alive                 | Does not keep the JVM alive alone             |
| Used for application work           | Often used for background support             |
| JVM waits while user threads remain | JVM may stop while only daemon threads remain |

```java
Thread thread = new Thread(task);
thread.setDaemon(true);
thread.start();
```

Do not use daemon threads for work that must complete reliably, because the JVM may terminate without waiting for them to finish.

---

## Question 47: How does the JVM schedule threads?

For platform threads:

- Java threads are normally mapped to operating-system threads.
- The operating system performs most CPU scheduling.
- JVM safepoints, garbage collection and runtime mechanisms also affect execution.
- Thread priority is only a scheduling hint and is platform-dependent.

For virtual threads:

- The Java runtime schedules many virtual threads onto a smaller set of carrier platform threads.
- Blocking virtual threads can often be suspended so their carrier can run other virtual threads.

Applications must not depend on a specific execution order unless they explicitly coordinate it.

---

## Question 48: How do you debug multithreading problems?

### 1. Capture repeated thread dumps

```bash
jcmd <pid> Thread.print -l
```

or:

```bash
jstack -l <pid>
```

Capture several dumps a few seconds apart.

### 2. Classify thread states

- `RUNNABLE`
- `BLOCKED`
- `WAITING`
- `TIMED_WAITING`

### 3. Group similar stack traces

Look for:

- Many threads waiting on one monitor
- Database connection-pool waits
- Socket reads
- Executor queue waits
- Future joins
- Deadlock cycles

### 4. Use Java Flight Recorder

Inspect:

- Lock contention
- Thread parking
- CPU hotspots
- Allocation
- Long pauses
- Socket and file I/O

### 5. Add concurrency metrics

- Executor queue size
- Active worker count
- Rejected tasks
- Lock wait time
- Connection-pool pending threads
- Timeout counts
- Retry counts
- Thread count

### 6. Build targeted tests

Use:

- Repeated concurrent execution
- Barriers to force specific interleavings
- Timeouts
- Stress tests
- Dedicated concurrency testing tools when needed

A single successful test run does not prove thread safety.

---

## Question 49: What are real-world uses of multithreading in Spring Boot?

Common examples include:

### Asynchronous operations

```java
@Async("notificationExecutor")
public CompletableFuture<Void> sendNotification(
        Notification notification
) {
    notificationClient.send(notification);
    return CompletableFuture.completedFuture(null);
}
```

### Scheduled jobs

```java
@Scheduled(fixedDelay = 60_000)
public void refreshCache() {
    cacheService.refresh();
}
```

### Kafka listeners

Different listener threads may process separate partitions concurrently.

### Parallel downstream calls

```java
CompletableFuture<Price> price =
        pricingService.fetchAsync(order);

CompletableFuture<Stock> stock =
        inventoryService.fetchAsync(order);
```

### Background file processing

A bounded executor can process uploaded files independently.

### Request processing

Spring MVC servers use worker threads. Reactive servers use event-loop-based execution and require blocking work to be isolated.

### Important production concerns

- Executor sizing
- Queue capacity
- Transaction boundaries
- Context propagation
- Timeouts
- Rejection handling
- Graceful shutdown
- Downstream resource limits

---

## Question 50: Explain a production multithreading issue you solved

Use a genuine example from your experience. A strong answer should follow this structure:

1. Symptom
2. Investigation
3. Root cause
4. Fix
5. Validation
6. Prevention

### Sample interview answer

> We observed increasing API latency while CPU usage remained low. Executor queue depth was continuously rising, and thread dumps showed most workers waiting for database connections. The executor had 100 workers, but the connection pool contained only 10 connections. Tasks occupied worker threads while waiting, causing queue growth and timeout amplification.
>
> I separated queue-wait time from execution time, reviewed thread dumps and connection-pool metrics, and confirmed that database concurrency was the bottleneck. We reduced executor concurrency, bounded the queue, added a rejection policy and aligned worker concurrency with the database connection budget. We also added database timeouts and metrics for active, idle and pending connections.
>
> Load testing showed lower tail latency, stable queue depth and no connection starvation. The key lesson was that increasing threads does not increase downstream capacity.

Do not invent production metrics or claim experience you did not have. Replace the example with the actual incident and measurable outcome.

---

# Additional Concurrency Questions

## Question 16: What is the trade-off between `synchronized` and `ReentrantLock`?

`synchronized` provides simpler and safer structured locking. `ReentrantLock` provides more control but introduces more opportunities for misuse.

The main trade-off is:

```text
Simplicity and automatic release
versus
timed, interruptible, fair and multi-condition locking
```

Do not use `ReentrantLock` merely because it appears more advanced.

---

## Question 17: What does `volatile` do?

`volatile` provides visibility and ordering guarantees for one variable.

```java
private volatile boolean running = true;
```

One thread:

```java
running = false;
```

Another thread:

```java
while (running) {
    performWork();
}
```

A volatile write happens-before a later volatile read of the same variable.

It does not provide mutual exclusion and does not make compound operations atomic.

```java
private volatile int count;

count++; // Not thread-safe
```

---

## Question 18: `volatile` vs atomic classes

| `volatile`                          | Atomic class                               |
| ----------------------------------- | ------------------------------------------ |
| Provides visibility and ordering    | Provides visibility plus atomic operations |
| Simple reads and writes are visible | Supports atomic read-modify-write          |
| `count++` is unsafe                 | `incrementAndGet()` is atomic              |
| No retry API                        | Supports CAS and update functions          |
| Good for flags and snapshots        | Good for counters and state transitions    |

```java
volatile boolean running;
AtomicInteger counter;
```

### No standard `AtomicFloat`

Java does not provide an `AtomicFloat` class.

Possible alternatives include:

- `AtomicReference<Float>`
- Storing float bit patterns in `AtomicInteger`
- Synchronizing access
- Designing the state around integer minor units where appropriate

Example using `AtomicReference<Double>`:

```java
AtomicReference<Double> value =
        new AtomicReference<>(0.0);

value.updateAndGet(current -> current + 1.5);
```

For numeric accumulation, consider whether floating-point arithmetic is suitable for the domain.

---

## Question 19: Important `CompletableFuture` methods

| Method                | Purpose                                         |
| --------------------- | ----------------------------------------------- |
| `runAsync()`          | Run an asynchronous action without a result     |
| `supplyAsync()`       | Run an asynchronous supplier                    |
| `thenApply()`         | Transform a result                              |
| `thenApplyAsync()`    | Schedule an asynchronous transformation         |
| `thenCompose()`       | Flatten a dependent asynchronous operation      |
| `thenCombine()`       | Combine two independent results                 |
| `thenAccept()`        | Consume a result                                |
| `thenRun()`           | Run an action after completion                  |
| `allOf()`             | Complete after all stages                       |
| `anyOf()`             | Complete after one stage                        |
| `exceptionally()`     | Recover from failure                            |
| `handle()`            | Process success or failure                      |
| `whenComplete()`      | Observe completion                              |
| `orTimeout()`         | Fail after a timeout                            |
| `completeOnTimeout()` | Return a fallback after timeout                 |
| `join()`              | Wait and wrap failure in an unchecked exception |
| `get()`               | Wait and expose checked exceptions              |

### `thenApply()` vs `thenCompose()`

```java
CompletableFuture<User> userFuture =
        loadUserAsync();
```

If the callback returns a normal value:

```java
CompletableFuture<String> name =
        userFuture.thenApply(User::name);
```

If the callback returns another future:

```java
CompletableFuture<Address> address =
        userFuture.thenCompose(
                this::loadAddressAsync
        );
```

`thenCompose()` avoids:

```java
CompletableFuture<CompletableFuture<Address>>
```

---

# Quick Selection Guide

| Requirement                          | Suitable utility                |
| ------------------------------------ | ------------------------------- |
| Simple mutual exclusion              | `synchronized`                  |
| Timed or interruptible locking       | `ReentrantLock`                 |
| Many readers, occasional writer      | `ReadWriteLock`                 |
| Optimistic read-heavy state          | `StampedLock`                   |
| Independent atomic counter           | `AtomicInteger` or `AtomicLong` |
| High-contention statistics           | `LongAdder`                     |
| Concurrent key-value access          | `ConcurrentHashMap`             |
| Producer-consumer with backpressure  | Bounded `BlockingQueue`         |
| Wait for N tasks                     | `CountDownLatch`                |
| Fixed workers across phases          | `CyclicBarrier`                 |
| Dynamic phase participants           | `Phaser`                        |
| Limit concurrent resource usage      | `Semaphore`                     |
| Exchange buffers between two threads | `Exchanger`                     |
| Compose asynchronous tasks           | `CompletableFuture`             |
| CPU divide-and-conquer               | `ForkJoinPool`                  |

# Short Interview Summary

> Java concurrency utilities provide different coordination contracts. I use `synchronized` for simple locking, `ReentrantLock` for timed or interruptible acquisition, read-write locks for read-heavy compound state, atomics for independent values, concurrent collections for shared structures, blocking queues for producer-consumer workflows and synchronizers such as latches, barriers, phasers and semaphores for thread coordination. The key is choosing the highest-level abstraction that directly models the problem rather than building custom synchronization.
