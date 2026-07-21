# Basic Questions

## Question 1: What is a race condition?

When the outcome depends on the relative timing/interleaving of threads accessing shared state.

## Question 2: What is synchronization and why is it needed?

Synchronization is the mechanism that controls access to shared resources by multiple threads, preventing race conditions and ensuring data consistency. It is needed because thread scheduling is non-deterministic and multiple threads can interleave in unpredictable ways.

Example – race condition without sync:
```java
int counter = 0;
// Thread 1 and Thread 2 both run: counter++;
// Read-Increment-Write is NOT atomic → lost updates!
```

Fix with synchronized:
```java
synchronized(this) { counter++; }
```

## Question 3: Explain the synchronized keyword – methods vs blocks.

**Synchronized instance method** – lock on 'this'
```java
public synchronized void increment() { count++; }
```

**Synchronized static method** – lock on Class object
```java
public static synchronized void reset() { count = 0; }
```

**Synchronized block** – finer-grained, preferred
```java
public void add(int val) {
    synchronized(lock) { // lock is a dedicated Object
        count += val;
    }
    // code here runs without holding the lock
}
```

✅ **Best Practices**
- Prefer synchronized blocks over methods – minimize the critical section.
- Never lock on 'this' in public APIs – callers can deadlock you externally.
- Use a private final `Object` as the lock object.

## Question 4: What is a deadlock? How do you detect and prevent it?

Deadlock occurs when two or more threads each hold a lock the other needs and both are permanently blocked waiting.

```java
// Thread 1: synchronized(A) { synchronized(B) { ... } }
// Thread 2: synchronized(B) { synchronized(A) { ... } } → DEADLOCK
```

Four Coffman conditions (ALL must hold for deadlock):
- **Mutual Exclusion** – resource held exclusively.
- **Hold and Wait** – thread holds one resource while waiting for another.
- **No Preemption** – locks cannot be forcibly taken away.
- **Circular Wait** – circular chain of threads waiting for each other.

**Prevention strategies:**
- **Lock ordering** – always acquire locks in the same global order.
- **tryLock with timeout** – use `ReentrantLock.tryLock(timeout)` to avoid indefinite waiting.
- **Lock-free data structures** – use `java.util.concurrent` classes.
- **Single lock strategy** – avoid acquiring multiple locks where possible.

**Detection:** `jstack <pid>` prints a thread dump identifying deadlocked threads. Also programmable via `ThreadMXBean.findDeadlockedThreads()`.

## Question 5: What is livelock and starvation?

- **Livelock:** Threads continuously change state in response to each other but never make progress (both keep retrying simultaneously).
- **Starvation:** A thread never gets CPU time because higher-priority threads or greedy threads monopolize resources. Fix with fair locks (`new ReentrantLock(true)`).

## Question 6: Explain the volatile keyword in Java.

`volatile` guarantees:
- **Visibility** – writes to a volatile variable are immediately visible to all threads (no CPU caching).
- **Ordering** – prevents instruction reordering around the volatile access.
- **NOT atomicity** – `volatile int x; x++;` is still a race condition!

```java
private volatile boolean running = true;
// Thread 1 reads 'running' – always sees latest value
// Thread 2 sets running = false – immediately visible
```

Use `volatile` for: simple flags, double-checked locking (Java 5+), published references.

## Question 7: What is the volatile write guarantee?

A write to a volatile variable flushes to main memory immediately and establishes a happens-before for subsequent readers.

## Question 8: What is the Java Memory Model (JMM)?

The JMM defines the rules for how threads interact through memory. Key concepts:
- **happens-before** – If action A happens-before B, then A's writes are visible to B.
- **Main Memory vs Working Memory** – Each thread has its own working memory (cache); volatile/synchronized force sync with main memory.
- **Reordering** – CPU and compiler may reorder instructions for performance; JMM constrains this.

**happens-before rules:**
- **Program order** – each action in a thread happens-before the next in that thread.
- **Monitor lock** – unlock happens-before subsequent lock on the same monitor.
- **volatile** – write to volatile happens-before subsequent read of same variable.
- **Thread start** – `Thread.start()` happens-before any action in the started thread.
- **Thread join** – all actions in a thread happen-before `Thread.join()` returns.

## Question 9: What is the happens-before relationship in detail?

happens-before is the formal JMM guarantee that if action A happens-before action B, then all writes made by A (and anything that happened before A) are visible to B.

Establishing happens-before:
- `Thread.start()` → all actions in the started thread.
- `Thread.join()` → all actions in the joined thread happen-before `join()` returns.
- Unlock of monitor → subsequent lock of same monitor.
- Write to volatile → subsequent read of same volatile.
- Actions on objects passed to executor → their execution.

## Question 10: How does double-checked locking work and why is volatile required?

Without `volatile`, the JVM may reorder: (1) allocate memory, (2) assign reference, (3) initialize object. Another thread can see a non-null but incompletely initialized object. `volatile` prevents this reordering by establishing a happens-before between write and read.

Without volatile (BROKEN):
```java
private static Singleton instance; // No volatile – BROKEN
// Thread 2 might see partially initialized object!
```

With volatile (CORRECT):
```java
private static volatile Singleton instance; // volatile REQUIRED
```
