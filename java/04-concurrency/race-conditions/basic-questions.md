# Basic Questions — Race Conditions, Synchronization, `volatile`, and the JMM

## Question 1: What is a race condition?

A **race condition** occurs when a program’s correctness depends on the unpredictable timing or interleaving of concurrent operations.

Race conditions commonly involve:

- Shared mutable state
- Multiple concurrent threads
- At least one state-changing operation
- Missing or incorrect coordination

Example:

```java
public final class Counter {

    private int count;

    public void increment() {
        count++;
    }

    public int get() {
        return count;
    }
}
```

`count++` is a read-modify-write operation:

```text
Read count
→ add one
→ write updated count
```

Two threads can read the same old value and overwrite one another:

```text
Initial value: 5

Thread A reads 5
Thread B reads 5

Thread A writes 6
Thread B writes 6

Expected: 7
Actual:   6
```

This is called a **lost update**.

### Race condition vs data race

A **data race** is a specific low-level situation where threads access the same memory location concurrently, at least one access is a write, and the accesses are not correctly ordered by synchronization.

A **race condition** is broader. A workflow may have a race even when individual operations use thread-safe APIs.

```java
if (!concurrentQueue.isEmpty()) {
    concurrentQueue.remove();
}
```

Each method is thread-safe, but another thread may remove the last item between the two calls.

Prefer one atomic operation:

```java
Task task = concurrentQueue.poll();

if (task != null) {
    process(task);
}
```

### Interview-ready answer

> A race condition occurs when correctness depends on thread timing. For example, `count++` is not atomic, so two threads can read the same value and lose an update. I prevent races using synchronization, locks, atomic operations, immutability, confinement, or concurrent collections.

---

## Question 2: What is synchronization, and why is it needed?

Synchronization coordinates concurrent access to shared state.

In Java, synchronization can provide:

- **Mutual exclusion:** Only one thread executes a critical section at a time.
- **Visibility:** Changes made by one thread become visible to another.
- **Ordering:** Operations are observed according to Java Memory Model guarantees.
- **Invariant protection:** Related checks and updates execute as one logical operation.

Unsafe example:

```java
private int counter;

public void increment() {
    counter++;
}
```

Synchronized version:

```java
private int counter;

public synchronized void increment() {
    counter++;
}
```

Or with a dedicated lock:

```java
private final Object lock = new Object();
private int counter;

public void increment() {
    synchronized (lock) {
        counter++;
    }
}
```

### Protect the entire invariant

Incorrect:

```java
if (balance >= amount) {
    synchronized (lock) {
        balance -= amount;
    }
}
```

The balance may change after the check but before the lock is acquired.

Correct:

```java
synchronized (lock) {
    if (balance >= amount) {
        balance -= amount;
    }
}
```

### Synchronization is broader than `synchronized`

Java coordination mechanisms also include:

- `ReentrantLock`
- Atomic variables
- Volatile variables
- Concurrent collections
- Blocking queues
- Latches, barriers, and semaphores
- Immutable objects
- Thread confinement

### Interview-ready answer

> Synchronization coordinates access to shared mutable state. It is needed because thread scheduling is nondeterministic, so related reads and writes can interleave incorrectly. A synchronized critical section provides mutual exclusion and also establishes the required visibility and ordering guarantees.

---

## Question 3: Explain the `synchronized` keyword—methods vs blocks

The `synchronized` keyword acquires an intrinsic monitor before entering protected code and releases it automatically when the code exits, including when an exception is thrown.

---

### Synchronized instance method

```java
public synchronized void increment() {
    count++;
}
```

This is equivalent to synchronizing on the current object:

```java
public void increment() {
    synchronized (this) {
        count++;
    }
}
```

Different instances use different monitors.

```java
Counter first = new Counter();
Counter second = new Counter();
```

A thread locking `first` does not block another thread locking `second`.

---

### Synchronized static method

```java
public static synchronized void reset() {
    count = 0;
}
```

This locks the corresponding `Class` object.

Conceptually:

```java
public static void reset() {
    synchronized (Counter.class) {
        count = 0;
    }
}
```

A static synchronized method and an instance synchronized method use different locks:

```text
Static method   → Counter.class
Instance method → this
```

They do not automatically exclude each other.

---

### Synchronized block

```java
private final Object lock = new Object();
private int count;

public void add(int value) {
    validate(value);

    synchronized (lock) {
        count += value;
    }

    publishMetrics();
}
```

A synchronized block allows explicit control over:

- Which monitor is used
- How much code is protected
- Which operations may run concurrently

---

### Best practices

- Protect every access participating in the same invariant with the same locking policy.
- Keep critical sections as small as correctness permits.
- Avoid slow network, database, or file operations while holding a monitor.
- Avoid synchronizing on mutable lock references.
- Avoid locking on interned strings, boxed values, or public class objects you do not control.
- Prefer a private final lock when external synchronization could interfere.

```java
private final Object lock = new Object();
```

Using `this` is not inherently incorrect. It is appropriate when the object’s monitor is intentionally part of its synchronization contract. A private lock simply reduces accidental interference from external code.

### Are synchronized blocks always better?

No.

A synchronized method may be clearer when the whole method represents one atomic operation:

```java
public synchronized boolean withdraw(long amount) {
    if (balance < amount) {
        return false;
    }

    balance -= amount;
    return true;
}
```

Do not split a critical section merely to make it smaller if doing so breaks the invariant.

### Interview-ready answer

> An instance synchronized method locks `this`, while a static synchronized method locks the class object. A synchronized block lets me choose a private monitor and limit the lock scope. Blocks offer more control, but a synchronized method can be clearer when the whole method is one atomic operation.

---

## Question 4: What is deadlock? How do you detect and prevent it?

A **deadlock** occurs when threads wait indefinitely for resources held by one another.

Example:

```java
// Thread A
synchronized (firstLock) {
    synchronized (secondLock) {
        performWork();
    }
}

// Thread B
synchronized (secondLock) {
    synchronized (firstLock) {
        performOtherWork();
    }
}
```

Possible execution:

```text
Thread A owns firstLock and waits for secondLock.
Thread B owns secondLock and waits for firstLock.

Neither thread can continue.
```

---

### Coffman conditions

Deadlock is possible when all four conditions exist:

1. **Mutual exclusion**
   A resource can be held by only one thread at a time.

2. **Hold and wait**
   A thread holds one resource while waiting for another.

3. **No preemption**
   A held lock cannot be forcibly taken away safely.

4. **Circular wait**
   A cycle exists in which each thread waits for a resource held by the next.

Preventing any one of these conditions can prevent that deadlock pattern.

---

### Detection using thread dumps

```bash
jcmd <pid> Thread.print -l
```

or:

```bash
jstack -l <pid>
```

A thread dump may show:

- Thread names
- Thread states
- Locks owned
- Locks being awaited
- Detected deadlock cycles

Capture multiple thread dumps several seconds apart. One snapshot may show temporary contention rather than permanent deadlock.

---

### Programmatic detection

```java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public final class DeadlockDetector {

    private final ThreadMXBean threadMxBean =
            ManagementFactory.getThreadMXBean();

    public void detect() {
        long[] threadIds =
                threadMxBean.findDeadlockedThreads();

        if (threadIds == null) {
            return;
        }

        ThreadInfo[] threadInfos =
                threadMxBean.getThreadInfo(
                        threadIds,
                        true,
                        true
                );

        for (ThreadInfo threadInfo : threadInfos) {
            System.err.println(threadInfo);
        }
    }
}
```

`findDeadlockedThreads()` can detect cycles involving intrinsic monitors and ownable synchronizers such as `ReentrantLock`.

---

### Prevention strategy 1: Consistent lock ordering

```java
public void transfer(
        Account source,
        Account destination,
        long amount
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

Every thread acquires resources in the same order, removing circular wait.

---

### Prevention strategy 2: Timed lock acquisition

```java
boolean acquired =
        lock.tryLock(
                500,
                TimeUnit.MILLISECONDS
        );

if (!acquired) {
    handleContention();
    return;
}

try {
    performWork();
} finally {
    lock.unlock();
}
```

Timed acquisition allows a thread to abandon, retry, or fail instead of waiting forever.

Retry logic should be:

- Bounded
- Backed off
- Jittered when many threads may retry together

---

### Other prevention techniques

- Avoid nested locks where possible.
- Keep lock scope small.
- Do not call unknown callbacks while holding locks.
- Avoid slow I/O while holding locks.
- Use one higher-level lock when suitable.
- Use immutable data or message passing.
- Use concurrent collections and blocking queues.
- Partition state so unrelated operations do not share one lock.

### Interview-ready answer

> Deadlock is a permanent circular wait between threads holding resources needed by one another. I detect it using repeated thread dumps, Java Flight Recorder, or `ThreadMXBean`. I prevent it mainly through consistent lock ordering, reduced nested locking, small critical sections, and timed lock acquisition where retries are acceptable.

---

## Question 5: What are livelock and starvation?

## Livelock

In a **livelock**, threads remain active but repeatedly react to one another without completing useful work.

```text
Thread A detects a conflict and backs off.
Thread B detects a conflict and backs off.

Both retry simultaneously.
Both conflict again.
```

Unlike deadlocked threads, livelocked threads continue running.

### Mitigation

- Randomized backoff
- Exponential backoff with jitter
- Bounded retries
- Stable ownership rules
- Consistent resource ordering
- Centralized coordination

---

## Starvation

**Starvation** occurs when one thread repeatedly fails to obtain the CPU, lock, worker slot, connection, permit, or another required resource while other work continues progressing.

Possible causes include:

- One thread repeatedly reacquiring an unfair lock
- Very long critical sections
- High-priority work continuously arriving
- Reader-heavy policies indefinitely delaying writers
- Executor saturation
- Connection-pool exhaustion
- Priority queues without aging

### Fair lock

```java
ReentrantLock lock =
        new ReentrantLock(true);
```

A fair lock generally favors threads that have waited longer.

However, it:

- Does not guarantee perfect FIFO execution
- Does not solve CPU scheduling starvation
- Does not solve executor or database starvation
- May reduce throughput

### Interview-ready answer

> In deadlock, threads stop because they wait cyclically. In livelock, they remain active but repeatedly interfere without progress. In starvation, the system progresses, but one thread repeatedly fails to obtain the resource it needs.

---

## Question 6: Explain the `volatile` keyword

A volatile field provides Java Memory Model guarantees for visibility and ordering.

```java
private volatile boolean running = true;
```

Thread one:

```java
public void stop() {
    running = false;
}
```

Thread two:

```java
public void run() {
    while (running) {
        performWork();
    }
}
```

A write to `running` by one thread happens-before a later read of that same volatile variable by another thread.

---

### What `volatile` guarantees

- A thread reading the volatile field can observe a properly ordered previous volatile write.
- Writes before the volatile write become visible to a thread performing the corresponding later volatile read.
- The compiler and runtime must respect the Java Memory Model’s ordering constraints around that access.

---

### What `volatile` does not guarantee

It does not provide mutual exclusion.

This is unsafe:

```java
private volatile int count;

public void increment() {
    count++;
}
```

Two threads can still read the same value and lose an update.

Use:

```java
private final AtomicInteger count =
        new AtomicInteger();

public void increment() {
    count.incrementAndGet();
}
```

or:

```java
private int count;

public synchronized void increment() {
    count++;
}
```

---

### Good uses

- Stop flags
- State indicators
- Immutable configuration references
- Safely published snapshots
- Double-checked locking
- Independent reads and writes where each access is the complete state transition

### Avoid this explanation

> Volatile disables CPU caching and forces immediate main-memory access.

That is an oversimplified physical explanation. The Java Memory Model specifies observable visibility and ordering behavior, not a requirement for one particular hardware cache implementation.

### Interview-ready answer

> `volatile` provides visibility and ordering guarantees for a variable. A write to a volatile field happens-before a subsequent read of that field. It is suitable for flags and published references, but it does not make compound operations such as `count++` atomic.

---

## Question 7: What is the volatile write guarantee?

The formal guarantee is:

> A write to a volatile variable happens-before every subsequent read of that same variable that observes it according to synchronization order.

Example:

```java
private int configurationVersion;
private volatile boolean ready;
```

Publisher:

```java
configurationVersion = 10;
ready = true;
```

Reader:

```java
if (ready) {
    System.out.println(configurationVersion);
}
```

When the reader observes `ready == true`, the earlier write to `configurationVersion` is also visible through the established happens-before relationship.

```text
Write configurationVersion
        ↓
Write volatile ready
        ↓ happens-before
Read volatile ready
        ↓
Read configurationVersion
```

### Important nuance

Avoid describing this solely as “flushing to main memory.” The essential contract is visibility and ordering under the JMM.

### Interview-ready answer

> A volatile write happens-before a later volatile read of the same variable. Therefore, writes performed before the volatile write become visible to the thread after it performs the corresponding volatile read.

---

## Question 8: What is the Java Memory Model?

The **Java Memory Model**, or JMM, defines the legal interactions between threads through memory.

It specifies:

- When writes by one thread must become visible to another
- Which operation reorderings are legal
- What synchronization actions guarantee
- How `volatile`, monitors, final fields, thread start, and thread completion behave
- What outcomes are permitted in correctly and incorrectly synchronized programs

The JMM is an abstract specification. It should not be understood as a literal rule that every Java thread owns a physical private copy of every variable.

Actual implementations may involve:

- CPU registers
- CPU caches
- Compiler optimizations
- Runtime optimizations
- Main memory

The JMM defines what Java programs are allowed to observe despite those implementation details.

---

### Happens-before

If action A happens-before action B, then:

1. The effects of A are visible to B.
2. A is ordered before B for the purposes of the memory model.

This does not necessarily mean A occurred immediately before B in wall-clock time.

---

### Important happens-before rules

#### Program order

Within one thread, each action happens-before later actions according to program order.

```java
value = 10;
ready = true;
```

Within that thread, the assignment to `value` occurs before the assignment to `ready`.

---

#### Monitor rule

An unlock of a monitor happens-before a later successful lock of the same monitor.

```java
synchronized (lock) {
    sharedValue = 10;
}
```

Later:

```java
synchronized (lock) {
    System.out.println(sharedValue);
}
```

---

#### Volatile rule

A write to a volatile variable happens-before a later read of that same variable.

---

#### Thread-start rule

Actions before calling `Thread.start()` happen-before actions performed by the started thread.

```java
message = "ready";
thread.start();
```

The started thread can observe the correctly published preceding state.

---

#### Thread-termination rule

All actions performed by a thread happen-before another thread successfully detects its termination, such as when `join()` returns.

```java
worker.start();
worker.join();

System.out.println(result);
```

---

#### Transitivity

If:

```text
A happens-before B
B happens-before C
```

then:

```text
A happens-before C
```

### Interview-ready answer

> The Java Memory Model defines when shared-memory effects become visible across threads and which reorderings are permitted. Its central concept is happens-before: when A happens-before B, A’s effects are visible to B and are ordered before it.

---

## Question 9: Explain happens-before in more detail

Happens-before is the formal ordering relationship used by the Java Memory Model to establish safe visibility between threads.

If A happens-before B:

- Writes visible to A before it completes become visible to B.
- B cannot legally observe a state that violates that ordering guarantee.

---

### Common happens-before relationships

| Action A                                    | Action B                                           |
| ------------------------------------------- | -------------------------------------------------- |
| Earlier action in one thread                | Later action in that thread                        |
| Monitor unlock                              | Later lock of the same monitor                     |
| Volatile write                              | Later read of the same volatile variable           |
| Actions before `Thread.start()`             | Actions in the started thread                      |
| Actions in a thread                         | Successful return from `join()`                    |
| Actions before task submission              | Beginning of that task’s execution                 |
| Task actions                                | Successful result retrieval through `Future.get()` |
| Actions before `CountDownLatch.countDown()` | Return from a corresponding successful `await()`   |

---

### Executor example

```java
List<String> values =
        new ArrayList<>();

values.add("before submission");

Future<Integer> future =
        executor.submit(values::size);

int size = future.get();
```

The state prepared before submission is safely made available to the task according to the executor’s memory-consistency guarantees.

The task’s actions are then visible after successful result retrieval.

---

### Synchronization must connect the threads

This is insufficient:

```java
private boolean ready;
private int value;
```

Writer:

```java
value = 42;
ready = true;
```

Reader:

```java
if (ready) {
    System.out.println(value);
}
```

Without synchronization, there is no suitable cross-thread happens-before relationship.

Fix with volatile publication:

```java
private volatile boolean ready;
private int value;
```

or by using the same lock in both threads.

### Interview-ready answer

> Happens-before is the JMM’s visibility and ordering rule. It is established through mechanisms such as monitor locking, volatile access, thread start and join, executor submission, futures, and synchronizers. Without such a relationship, another thread is not guaranteed to observe shared updates correctly.

---

## Question 10: How does double-checked locking work, and why is `volatile` required?

Double-checked locking implements lazy initialization while avoiding synchronization after the instance has been created.

```java
public final class Singleton {

    private static volatile Singleton instance;

    private Singleton() {
    }

    public static Singleton getInstance() {
        Singleton result = instance;

        if (result == null) {
            synchronized (Singleton.class) {
                result = instance;

                if (result == null) {
                    result = new Singleton();
                    instance = result;
                }
            }
        }

        return result;
    }
}
```

---

### Why are there two checks?

The first check avoids locking after initialization:

```java
if (result == null)
```

The second check ensures only one thread creates the object:

```java
synchronized (Singleton.class) {
    if (result == null) {
        result = new Singleton();
        instance = result;
    }
}
```

Two threads may pass the first check before either acquires the lock. The second check handles that race.

---

### Why is `volatile` required?

Without the required publication and ordering guarantees, another thread could observe the reference without being guaranteed to observe the object’s fully initialized state.

A simplified conceptual construction sequence is:

```text
Allocate storage
Initialize object
Publish reference
```

The program requires safe publication so a thread that observes the non-null reference also observes initialization effects.

The volatile write:

```java
instance = result;
```

happens-before a later volatile read that observes the initialized reference.

---

### Broken version

```java
private static Singleton instance;
```

Without volatile, double-checked locking does not provide the required publication guarantee.

---

### Simpler alternative: holder idiom

```java
public final class Singleton {

    private Singleton() {
    }

    private static final class Holder {
        private static final Singleton INSTANCE =
                new Singleton();
    }

    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

Advantages:

- Lazy initialization
- Thread-safe class initialization
- No explicit synchronization
- Easier to reason about

### Interview-ready answer

> Double-checked locking checks the instance before and after acquiring the class lock. The second check prevents duplicate construction, and `volatile` safely publishes the initialized object so other threads cannot observe an inadequately published reference. The holder idiom is usually simpler.

---

# Common Mistakes

## 1. Treating visibility as atomicity

```java
volatile int count;

count++;
```

Still unsafe.

---

## 2. Synchronizing only the writer

```java
public synchronized void increment() {
    count++;
}

public int get() {
    return count;
}
```

The read does not follow the same locking protocol.

Correct:

```java
public synchronized int get() {
    return count;
}
```

---

## 3. Holding a lock during slow I/O

```java
synchronized (lock) {
    remoteClient.call();
}
```

This can serialize many requests behind a slow dependency.

Move work outside the lock only when the invariant remains correct.

---

## 4. Assuming `sleep()` establishes visibility

```java
Thread.sleep(1_000);
```

Sleeping does not create a happens-before relationship between arbitrary threads.

Use explicit synchronization.

---

## 5. Using fair locks as a universal starvation fix

A fair lock only affects contention on that lock. It does not solve:

- CPU starvation
- Thread-pool starvation
- Connection-pool starvation
- Priority-queue starvation
- Downstream resource exhaustion

---

## 6. Assuming a passing test proves thread safety

Race conditions may disappear under:

- Debugging
- Logging
- Different hardware
- Different JVM optimizations
- Lower contention

Correctness must come from the synchronization design, supported by stress tests rather than inferred from one successful run.

---

# Quick Comparison

| Concept                | Main meaning                                               |
| ---------------------- | ---------------------------------------------------------- |
| Race condition         | Correctness depends on timing                              |
| Synchronization        | Coordinates visibility, ordering, and access               |
| Deadlock               | Threads wait cyclically and cannot progress                |
| Livelock               | Threads remain active but accomplish nothing               |
| Starvation             | One participant repeatedly fails to obtain a resource      |
| `volatile`             | Visibility and ordering for a variable                     |
| `synchronized`         | Mutual exclusion plus visibility and ordering              |
| Happens-before         | Formal cross-thread visibility and ordering relationship   |
| JMM                    | Rules governing observable shared-memory behavior          |
| Double-checked locking | Lazy initialization using locking and volatile publication |

# Short Interview Summary

> A race condition occurs when concurrent correctness depends on thread interleaving. Java synchronization provides the visibility, ordering, and mutual exclusion required to protect shared invariants. `volatile` establishes visibility and happens-before guarantees but does not make compound operations atomic. The Java Memory Model formally defines these guarantees through relationships such as monitor unlock-to-lock, volatile write-to-read, thread start, thread join, executor submission, and future completion.
