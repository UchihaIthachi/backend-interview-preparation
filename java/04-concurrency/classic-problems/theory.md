# Classic Multithreading Problems and Lock Fairness

## 1. Definition

Classic multithreading problems are standard concurrency scenarios used to demonstrate:

- Race conditions
- Mutual exclusion
- Visibility
- Thread coordination
- Deadlocks
- Starvation
- Livelock
- Resource ordering
- Safe publication
- Fairness and throughput trade-offs

Common examples include:

- Producer–Consumer
- Dining Philosophers
- Readers–Writers
- Thread-safe Singleton
- Alternating odd and even threads
- Bounded buffer
- Cache initialization
- Concurrent counters

These problems are useful because the same coordination challenges appear in production systems such as worker queues, connection pools, schedulers, caches, and transaction-processing services.

---

## 2. Why These Problems Matter

Writing multiple threads is easy. Correctly coordinating shared state is difficult.

A concurrent implementation must answer questions such as:

1. Which state is shared?
2. Which operations must be atomic?
3. How are changes made visible between threads?
4. What happens when a required resource is unavailable?
5. Can threads wait forever?
6. Can all threads make progress?
7. Is ordering required?
8. Should waiting threads be treated fairly?
9. How does the design behave under heavy contention?
10. What happens when a waiting thread is interrupted?

The goal is not only to avoid exceptions. A concurrent program must remain correct for every valid thread interleaving.

---

# Core Concurrency Problems

## 3. Race Condition

A race condition occurs when multiple threads access shared mutable state and the result depends on their execution order.

```java
public final class Counter {

    private int value;

    public void increment() {
        value++;
    }

    public int get() {
        return value;
    }
}
```

`value++` is not one indivisible operation. It conceptually performs:

```text
Read current value
→ calculate value + 1
→ write new value
```

Two threads may read the same old value and overwrite one another’s updates.

### Fix using synchronization

```java
public final class Counter {

    private int value;

    public synchronized void increment() {
        value++;
    }

    public synchronized int get() {
        return value;
    }
}
```

### Fix using an atomic variable

```java
public final class Counter {

    private final AtomicInteger value =
            new AtomicInteger();

    public void increment() {
        value.incrementAndGet();
    }

    public int get() {
        return value.get();
    }
}
```

Use an atomic variable when the invariant concerns one independently updated value. Use a lock when several operations or fields must change together.

---

## 4. Deadlock

A deadlock occurs when threads wait permanently for resources held by one another.

```java
public void transfer(
        Account source,
        Account destination,
        BigDecimal amount
) {
    synchronized (source) {
        synchronized (destination) {
            source.withdraw(amount);
            destination.deposit(amount);
        }
    }
}
```

Thread A may execute:

```text
Lock Account 1
→ wait for Account 2
```

Thread B may execute:

```text
Lock Account 2
→ wait for Account 1
```

Neither thread can continue.

### Common deadlock conditions

Deadlock is possible when these conditions exist:

- Mutual exclusion
- Hold and wait
- No forced resource preemption
- Circular waiting

### Prevent using consistent lock ordering

```java
public void transfer(
        Account source,
        Account destination,
        BigDecimal amount
) {
    Account first =
            source.id() < destination.id()
                    ? source
                    : destination;

    Account second =
            first == source
                    ? destination
                    : source;

    synchronized (first) {
        synchronized (second) {
            source.withdraw(amount);
            destination.deposit(amount);
        }
    }
}
```

All threads acquire account locks in the same order, removing circular wait.

Other approaches include:

- `tryLock()` with timeout
- Reducing nested locking
- Avoiding calls to unknown code while holding a lock
- Using one higher-level lock
- Using transactional database coordination
- Message passing instead of shared mutable state

---

## 5. Starvation

Starvation occurs when a thread repeatedly fails to acquire CPU time, a lock, or another required resource while other threads continue progressing.

Possible causes include:

- Unfair lock acquisition
- Excessive thread priorities
- Long critical sections
- One thread repeatedly reacquiring a lock
- Readers indefinitely delaying writers
- Unbounded high-priority work

A fair lock can reduce lock starvation, but fairness does not solve every scheduling or resource-starvation problem.

---

## 6. Livelock

In a livelock, threads are active and repeatedly react to each other, but no useful progress is made.

Example:

```text
Thread A detects conflict and backs off.
Thread B detects conflict and backs off.
Both retry simultaneously.
Both conflict again.
```

Possible solutions include:

- Randomized backoff
- Exponential backoff with jitter
- Ownership rules
- Limiting retries
- Central coordination

Unlike deadlocked threads, livelocked threads continue executing.

---

# Producer–Consumer Problem

## 7. Definition

The Producer–Consumer problem contains:

- One or more producers creating work
- One or more consumers processing work
- A shared buffer between them

The design must coordinate:

- Producers when the buffer is full
- Consumers when the buffer is empty
- Thread-safe insertion and removal
- Shutdown and interruption
- Backpressure

---

## 8. Preferred Solution: `BlockingQueue`

Java provides `BlockingQueue` specifically for producer-consumer workflows.

```java
public final class JobProcessor {

    private final BlockingQueue<Job> queue =
            new ArrayBlockingQueue<>(100);

    public void produce(Job job)
            throws InterruptedException {

        queue.put(job);
    }

    public Job consume()
            throws InterruptedException {

        return queue.take();
    }
}
```

Behavior:

- `put()` waits while the queue is full.
- `take()` waits while the queue is empty.
- Queue operations are thread-safe.
- A bounded queue provides backpressure.

### Producer

```java
Runnable producer = () -> {
    try {
        for (int index = 0; index < 1_000; index++) {
            queue.put(new Job(index));
        }
    } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
    }
};
```

### Consumer

```java
Runnable consumer = () -> {
    try {
        while (!Thread.currentThread().isInterrupted()) {
            Job job = queue.take();
            process(job);
        }
    } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
    }
};
```

### Why a bounded queue matters

An unbounded queue allows producers to create work faster than consumers process it.

```text
Producer rate > Consumer rate
→ queue continuously grows
→ memory consumption increases
→ OutOfMemoryError
```

A bounded queue forces the system to slow down, reject work, or apply another overload policy.

---

## 9. Producer–Consumer Using `wait()` and `notifyAll()`

This implementation is useful for understanding monitor coordination, although production code should normally use `BlockingQueue`.

```java
public final class BoundedBuffer<T> {

    private final Queue<T> queue =
            new ArrayDeque<>();

    private final int capacity;

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Capacity must be positive"
            );
        }

        this.capacity = capacity;
    }

    public synchronized void put(T value)
            throws InterruptedException {

        while (queue.size() == capacity) {
            wait();
        }

        queue.add(value);
        notifyAll();
    }

    public synchronized T take()
            throws InterruptedException {

        while (queue.isEmpty()) {
            wait();
        }

        T value = queue.remove();
        notifyAll();

        return value;
    }
}
```

### Why use `while` instead of `if`?

Incorrect:

```java
if (queue.isEmpty()) {
    wait();
}
```

Correct:

```java
while (queue.isEmpty()) {
    wait();
}
```

A waiting thread must recheck the condition because:

- Another thread may consume the item first.
- The thread may wake without the required condition becoming true.
- Multiple waiting threads may be notified together.

This pattern is called waiting in a condition loop.

### Why call `wait()` while synchronized?

`wait()`, `notify()`, and `notifyAll()` must be called while owning the object’s monitor.

`wait()`:

1. Releases the monitor.
2. Suspends the thread.
3. Reacquires the monitor before returning.

---

# Dining Philosophers Problem

## 10. Definition

Several philosophers sit around a table. Each philosopher needs two forks to eat, but adjacent philosophers share forks.

A naive solution can deadlock when every philosopher picks up one fork and waits for the second.

```text
Philosopher 1 holds Fork 1 and waits for Fork 2
Philosopher 2 holds Fork 2 and waits for Fork 3
...
Philosopher N holds Fork N and waits for Fork 1
```

This creates circular wait.

---

## 11. Solution: Global Resource Ordering

Always acquire lower-numbered fork first.

```java
public void eat(
        ReentrantLock leftFork,
        int leftId,
        ReentrantLock rightFork,
        int rightId
) {
    ReentrantLock first =
            leftId < rightId
                    ? leftFork
                    : rightFork;

    ReentrantLock second =
            first == leftFork
                    ? rightFork
                    : leftFork;

    first.lock();

    try {
        second.lock();

        try {
            consumeMeal();
        } finally {
            second.unlock();
        }
    } finally {
        first.unlock();
    }
}
```

Because every philosopher follows the same ordering rule, a circular lock dependency cannot form.

---

## 12. Solution: Limit Concurrent Philosophers

With five philosophers, allow only four to attempt eating simultaneously.

```java
Semaphore waiter =
        new Semaphore(4, true);
```

```java
waiter.acquire();

try {
    leftFork.lock();

    try {
        rightFork.lock();

        try {
            eat();
        } finally {
            rightFork.unlock();
        }
    } finally {
        leftFork.unlock();
    }
} finally {
    waiter.release();
}
```

At least one philosopher can obtain both forks, preventing the all-hold-one-fork condition.

---

## 13. Solution: `tryLock()` With Backoff

```java
if (leftFork.tryLock()) {
    try {
        if (rightFork.tryLock()) {
            try {
                eat();
                return;
            } finally {
                rightFork.unlock();
            }
        }
    } finally {
        leftFork.unlock();
    }
}

Thread.sleep(randomBackoff());
```

This avoids waiting indefinitely while holding one fork.

However, a poor retry strategy can cause livelock. Randomized or jittered backoff helps prevent synchronized retries.

---

# Readers–Writers Problem

## 14. Definition

The Readers–Writers problem involves shared data where:

- Multiple readers may read concurrently.
- Writers require exclusive access.
- Readers must not observe partial writes.

Java provides `ReadWriteLock` for this pattern.

```java
public final class ConfigurationStore {

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

    public void update(
            Configuration newConfiguration
    ) {
        lock.writeLock().lock();

        try {
            configuration = newConfiguration;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
```

### Advantages

- Multiple readers can proceed concurrently.
- Writers receive exclusive access.
- Useful for read-heavy shared state.

### Trade-offs

A read-write lock may perform worse than a normal lock when:

- Critical sections are very small.
- Writes are frequent.
- Contention is low.
- Lock-management overhead dominates.

It should be chosen based on measurement, not merely because a workload has readers and writers.

---

# Thread-Safe Singleton

## 15. Initialization-on-Demand Holder

A simple lazy, thread-safe singleton can use class initialization guarantees.

```java
public final class ConfigurationManager {

    private ConfigurationManager() {
    }

    private static final class Holder {
        private static final ConfigurationManager INSTANCE =
                new ConfigurationManager();
    }

    public static ConfigurationManager getInstance() {
        return Holder.INSTANCE;
    }
}
```

Advantages:

- Lazy initialization
- Thread safety
- No explicit synchronization
- Simple implementation

---

## 16. Enum Singleton

```java
public enum ConfigurationManager {
    INSTANCE;

    public void reload() {
        // Reload configuration
    }
}
```

An enum singleton provides strong protection against:

- Duplicate construction through ordinary reflection
- Serialization creating another instance

It is often the simplest singleton implementation when lazy timing and inheritance requirements do not conflict with the design.

### Important limitation

A Java singleton is normally one instance per:

- JVM
- Class loader

It is not automatically one instance across multiple processes, containers, or servers.

Distributed singleton behavior requires coordination such as:

- Leader election
- Distributed locking
- Database ownership
- Lease-based coordination

---

## 17. Double-Checked Locking

```java
public final class ServiceRegistry {

    private static volatile ServiceRegistry instance;

    private ServiceRegistry() {
    }

    public static ServiceRegistry getInstance() {
        ServiceRegistry result = instance;

        if (result == null) {
            synchronized (ServiceRegistry.class) {
                result = instance;

                if (result == null) {
                    result = new ServiceRegistry();
                    instance = result;
                }
            }
        }

        return result;
    }
}
```

### Why is `volatile` required?

Object construction can conceptually involve:

```text
Allocate memory
→ initialize object
→ assign reference
```

Without the required memory-ordering guarantees, another thread could observe a published reference before observing fully initialized state.

`volatile` provides safe publication and prevents problematic reordering around the instance assignment.

The holder idiom or enum approach is normally simpler.

---

# Alternating Odd and Even Threads

## 18. Problem

Two threads must print numbers in sequence:

```text
Odd thread:  1, 3, 5, 7
Even thread: 2, 4, 6, 8
```

The output must remain:

```text
1 2 3 4 5 6 7 8
```

---

## 19. Solution Using `Condition`

```java
public final class AlternatingPrinter {

    private final Lock lock =
            new ReentrantLock();

    private final Condition turnChanged =
            lock.newCondition();

    private int current = 1;
    private final int maximum;

    public AlternatingPrinter(int maximum) {
        this.maximum = maximum;
    }

    public void printOdd() {
        printWhen(true);
    }

    public void printEven() {
        printWhen(false);
    }

    private void printWhen(boolean odd) {
        while (true) {
            lock.lock();

            try {
                while (current <= maximum
                        && isOdd(current) != odd) {

                    turnChanged.await();
                }

                if (current > maximum) {
                    turnChanged.signalAll();
                    return;
                }

                System.out.println(current);
                current++;

                turnChanged.signalAll();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                lock.unlock();
            }
        }
    }

    private boolean isOdd(int value) {
        return value % 2 != 0;
    }
}
```

This demonstrates:

- Explicit locking
- Condition waiting
- Condition rechecking
- Signalling
- Interruption handling

For real application logic, prefer higher-level task decomposition instead of coordinating threads merely to alternate execution.

---

# Fairness in Java Concurrency

## 20. What Is Fairness?

Fairness is a lock or synchronizer policy intended to give waiting threads a more predictable opportunity to acquire a resource.

A fair `ReentrantLock` can be created with:

```java
Lock lock = new ReentrantLock(true);
```

The default is non-fair:

```java
Lock lock = new ReentrantLock();
```

or:

```java
Lock lock = new ReentrantLock(false);
```

---

## 21. Fair vs Unfair Locks

| Fair lock                                   | Unfair lock                                                                |
| ------------------------------------------- | -------------------------------------------------------------------------- |
| Generally favors longer-waiting threads     | Allows a newly arriving thread to acquire the lock ahead of queued threads |
| Reduces lock starvation risk                | May provide higher throughput                                              |
| More predictable acquisition behavior       | Less predictable acquisition order                                         |
| More thread handoff and scheduling overhead | Allows efficient lock reacquisition                                        |
| Useful when bounded waiting matters         | Good default for many high-throughput workloads                            |

The default non-fair strategy allows **barging**.

Barging occurs when a newly arriving thread acquires the lock before a thread that has already been waiting.

---

## 22. Fairness Is Not Strict Execution Ordering

A fair lock does not guarantee:

```text
Thread A executes first
Thread B executes second
Thread C executes third
```

It only influences lock acquisition under contention.

The following still affect execution:

- Operating-system scheduling
- JVM scheduling
- Thread suspension
- CPU availability
- Thread priorities
- Work done inside the critical section
- Interruptions

Therefore:

> Fair lock acquisition does not imply globally fair thread execution.

---

## 23. Fair `ReentrantLock` Example

```java
public final class FairResource {

    private final Lock lock =
            new ReentrantLock(true);

    public void use() {
        lock.lock();

        try {
            performOperation();
        } finally {
            lock.unlock();
        }
    }
}
```

When several threads are queued, the lock generally favors the longest-waiting thread.

However, fairness should not be described as an absolute first-in-first-out guarantee for all scheduling situations.

---

## 24. Fair Semaphore

A semaphore can also be configured for fairness.

```java
Semaphore connections =
        new Semaphore(10, true);
```

This can be useful for a limited resource pool:

```java
connections.acquire();

try {
    useLimitedResource();
} finally {
    connections.release();
}
```

Fair mode generally serves queued acquisition requests in arrival order.

It may reduce starvation but can decrease throughput.

---

## 25. `tryLock()` Fairness Nuance

An untimed `tryLock()` can acquire a fair `ReentrantLock` immediately when available, even when other threads have been waiting.

```java
if (lock.tryLock()) {
    try {
        performOperation();
    } finally {
        lock.unlock();
    }
}
```

Therefore, using untimed `tryLock()` may bypass the expected fair acquisition policy.

Code requiring fairness should not assume that every lock-acquisition API behaves identically.

---

## 26. Why Can Fairness Reduce Performance?

Fairness may reduce throughput because it limits scheduling flexibility.

With a non-fair lock, the thread that released the lock may still be running and can sometimes reacquire it efficiently.

A fair lock may instead need to:

1. Wake a queued thread.
2. Schedule that thread.
3. Perform a context switch.
4. Transfer lock ownership.

This can increase:

- Context switching
- Thread parking and unparking
- Scheduling overhead
- CPU cache disruption
- Lock handoff latency

Fairness is therefore a trade-off:

```text
Predictability and reduced starvation
                    vs
Maximum throughput
```

---

## 27. When Should a Fair Lock Be Considered?

Fair locking may be useful when:

- Threads have waited for highly variable durations.
- Lock starvation is observed.
- Bounded waiting is an important requirement.
- Requests should receive more predictable treatment.
- One thread repeatedly reacquires a contended lock.
- Fair access is more important than maximum throughput.

Examples may include:

- Shared resource allocation
- Admission controls
- Certain connection or permit managers
- Long-running worker coordination
- Interactive systems sensitive to starvation

A fair lock should not be selected only because fairness sounds safer. Measure the actual workload and contention.

---

## 28. When Is an Unfair Lock Appropriate?

An unfair lock is often appropriate when:

- Throughput is the primary requirement.
- Critical sections are short.
- Starvation is not observed.
- Lock contention is moderate.
- Workloads are homogeneous.
- Predictable acquisition order is unnecessary.

This is why `ReentrantLock` uses non-fair mode by default.

---

# Fairness Across Java Synchronizers

| Mechanism                | Fairness support                                       |
| ------------------------ | ------------------------------------------------------ |
| `synchronized`           | No fairness configuration or strict ordering guarantee |
| `ReentrantLock`          | Optional fair mode                                     |
| `Semaphore`              | Optional fair mode                                     |
| `ReentrantReadWriteLock` | Optional fair mode                                     |
| `ArrayBlockingQueue`     | Optional fairness setting                              |
| `LinkedBlockingQueue`    | No public fairness constructor                         |
| `ConcurrentHashMap`      | No lock-acquisition fairness guarantee                 |

Fairness semantics vary by class. They should be checked at the specific API level rather than generalized across all concurrency utilities.

---

# Choosing the Correct Concurrency Tool

| Requirement                        | Suitable tool                   |
| ---------------------------------- | ------------------------------- |
| Atomic counter                     | `AtomicInteger`, `LongAdder`    |
| Exclusive critical section         | `synchronized`, `ReentrantLock` |
| Multiple readers, exclusive writer | `ReadWriteLock`                 |
| Producer-consumer queue            | `BlockingQueue`                 |
| Limit concurrent access            | `Semaphore`                     |
| Wait for several tasks             | `CountDownLatch`                |
| Reusable phase coordination        | `CyclicBarrier`, `Phaser`       |
| Per-thread contextual state        | `ThreadLocal`                   |
| Concurrent key-value operations    | `ConcurrentHashMap`             |
| Wait for a condition               | `Condition`                     |
| Asynchronous result                | `CompletableFuture`, `Future`   |

Prefer the highest-level abstraction that correctly models the problem.

---

# Common Mistakes

## 29. Using `if` Instead of `while` Around `wait()`

Incorrect:

```java
if (queue.isEmpty()) {
    wait();
}
```

Correct:

```java
while (queue.isEmpty()) {
    wait();
}
```

Always recheck the condition after waking.

---

## 30. Calling `wait()` Without Owning the Monitor

Incorrect:

```java
queue.wait();
```

when the thread is not synchronized on `queue`.

This throws `IllegalMonitorStateException`.

---

## 31. Forgetting to Unlock in `finally`

Incorrect:

```java
lock.lock();
performOperation();
lock.unlock();
```

If `performOperation()` throws, the lock remains held.

Correct:

```java
lock.lock();

try {
    performOperation();
} finally {
    lock.unlock();
}
```

---

## 32. Swallowing `InterruptedException`

Avoid:

```java
try {
    queue.take();
} catch (InterruptedException exception) {
    // Ignore
}
```

When the method cannot propagate interruption, restore the interrupt status:

```java
try {
    queue.take();
} catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
}
```

---

## 33. Holding Locks During Slow I/O

Avoid:

```java
lock.lock();

try {
    remoteService.call();
} finally {
    lock.unlock();
}
```

Slow I/O keeps other threads blocked and increases contention.

Prepare state under the lock, release it, and perform slow work outside the critical section when correctness permits.

---

## 34. Assuming a Fair Lock Prevents Every Starvation Case

Fairness only affects acquisition of that particular synchronizer.

A thread may still starve because of:

- CPU scheduling
- Another resource
- Thread-pool exhaustion
- Queue priority
- Application logic
- Database locks

---

## 35. Making Every Lock Fair

Fair mode can reduce throughput and increase context switching.

Use it only when predictable access or starvation avoidance is a genuine requirement.

---

## 36. Mixing Lock Order

```java
// Thread A
lock(first);
lock(second);

// Thread B
lock(second);
lock(first);
```

This can deadlock.

Define one consistent global lock order.

---

## 37. Sharing Mutable State Unnecessarily

The best synchronization strategy is often eliminating shared mutable state through:

- Immutability
- Thread confinement
- Message passing
- Partitioned ownership
- Copy-on-write snapshots

---

# Interview Questions

## Question 1: What is the Producer–Consumer problem?

> Producers create work, consumers process it, and a shared bounded buffer coordinates them. In Java, I normally solve it with a `BlockingQueue`, which handles waiting and provides backpressure.

## Question 2: Why use `while` around `wait()`?

> A thread can wake before the condition it needs is true, or another thread can change the condition first. The condition must therefore be rechecked after every wake-up.

## Question 3: How do you prevent Dining Philosophers deadlock?

> I can impose a consistent resource-acquisition order, limit the number of philosophers competing at once, or use timed lock attempts with backoff.

## Question 4: What is lock fairness?

> Fairness is a policy that generally favors threads that have waited longer. It reduces starvation risk but may lower throughput because it limits barging and introduces additional thread handoffs.

## Question 5: Does a fair lock guarantee FIFO execution?

> No. It influences contended lock acquisition but does not control the complete execution order because JVM and operating-system scheduling still apply.

## Question 6: Why are unfair locks often faster?

> They allow a running or newly arriving thread to acquire an available lock without always handing it to the longest-waiting thread, reducing scheduling and context-switch overhead.

## Question 7: What is the difference between starvation and deadlock?

> In deadlock, a set of threads cannot progress because they wait cyclically for one another. In starvation, the system continues progressing, but one particular thread repeatedly fails to receive the required resource.

## Question 8: Why is `volatile` required in double-checked locking?

> It provides visibility and ordering guarantees so another thread cannot observe the singleton reference without also observing its fully initialized state.

---

# Short Interview Answer

> Classic multithreading problems demonstrate how to coordinate shared state safely. I use `BlockingQueue` for Producer–Consumer, consistent resource ordering or bounded admission for Dining Philosophers, `ReadWriteLock` for read-heavy shared state, and the holder idiom or enum for a thread-safe singleton. Fair locks reduce starvation by generally favoring longer-waiting threads, but they do not guarantee complete execution order and may reduce throughput because of additional scheduling and lock-handoff overhead.

---

## Related Topics

- [`java.util.concurrent` Utilities](../java-util-concurrent/theory.md)
- [Race Conditions](../race-conditions/theory.md)
- [Deadlocks](../deadlocks/theory.md)
- [Atomic Variables](../atomic-variables/theory.md)
- [Java Memory Model](../java-memory-model/theory.md)
- [Thread Pools](../executors/theory.md)
