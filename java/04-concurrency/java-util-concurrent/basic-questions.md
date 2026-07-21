# Basic Questions

## Question 1: What is ReentrantLock? How does it differ from synchronized?

| Feature | `synchronized` | `ReentrantLock` |
| :--- | :--- | :--- |
| **Fairness** | Not configurable | `new ReentrantLock(true)` |
| **tryLock** | Not available | `tryLock(timeout)` |
| **Condition vars**| `wait()`/`notify()` | `lock.newCondition()` |
| **Interruptible** | No | `lockInterruptibly()` |
| **Explicit unlock**| Not needed | MUST call `unlock()` in finally |

```java
ReentrantLock lock = new ReentrantLock();
lock.lock();
try { /* critical section */ }
finally { lock.unlock(); } // ALWAYS unlock in finally!
```

## Question 2: What is ReadWriteLock and when should you use it?

`ReadWriteLock` (`ReentrantReadWriteLock`) allows multiple concurrent readers but exclusive writers. Ideal for read-heavy workloads like caches.

```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();

// Multiple threads can read concurrently:
rwLock.readLock().lock();
try { /* read */ }
finally { rwLock.readLock().unlock(); }

// Only one thread can write:
rwLock.writeLock().lock();
try { /* write */ }
finally { rwLock.writeLock().unlock(); }
```

## Question 3: Explain Atomic classes. What operations are they useful for?

`java.util.concurrent.atomic` classes use CPU-level CAS (Compare-And-Swap) for lock-free thread safety.

- `AtomicInteger`, `AtomicLong`, `AtomicBoolean` – for primitive types
- `AtomicReference<V>` – for object references
- `AtomicIntegerArray`, `AtomicLongArray` – for arrays
- `LongAdder` / `LongAccumulator` – better performance under high contention

```java
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet(); // atomic counter++
counter.compareAndSet(expected, update); // CAS
```

## Question 4: What is CAS (Compare-And-Swap)? How does it relate to atomic classes?

CAS is a CPU-level atomic instruction: `compareAndSwap(variable, expected, new)` – sets `variable` to `new` only if it equals `expected`. Returns `true` if successful.

- **Non-blocking** – threads retry instead of blocking.
- **ABA problem** – value changed from A to B back to A; CAS succeeds incorrectly. Use `AtomicStampedReference`.

```java
AtomicInteger ai = new AtomicInteger(5);
boolean updated = ai.compareAndSet(5, 10); // true
```

## Question 5: What are the concurrent collections? When to prefer them over synchronized wrappers?

| Class | Use Case | Notes |
| :--- | :--- | :--- |
| `ConcurrentHashMap` | Thread-safe Map | Segment/bucket level locking |
| `CopyOnWriteArrayList` | Read-heavy List | Copies on every write – expensive |
| `CopyOnWriteArraySet` | Read-heavy Set | Based on `CopyOnWriteArrayList` |
| `LinkedBlockingQueue` | Producer-consumer | Bounded/unbounded blocking |
| `PriorityBlockingQueue`| Priority task queue| Unbounded, thread-safe |
| `ConcurrentLinkedQueue`| Non-blocking queue | Lock-free CAS operations |
| `BlockingDeque` | Double-ended blocking queue | Both ends can block |

Use concurrent collections over `Collections.synchronizedXxx()` – they offer better throughput through fine-grained locking.

## Question 6: Explain CountDownLatch, CyclicBarrier, and Semaphore.

**CountDownLatch – wait for N events to complete**
```java
CountDownLatch latch = new CountDownLatch(3);
// 3 worker threads each call latch.countDown()
latch.await(); // main thread waits until count reaches 0
```
Note: One-time use – cannot be reset.

**CyclicBarrier – all threads wait at a common point**
```java
CyclicBarrier barrier = new CyclicBarrier(3, () -> System.out.println("All arrived!"));
// Each thread calls barrier.await() – all block until 3 have arrived
```
Note: Reusable – resets automatically after each cycle.

**Semaphore – control concurrent access count**
```java
Semaphore sem = new Semaphore(5); // max 5 concurrent accesses
sem.acquire(); // blocks if 0 permits
try { /* limited resource */ } finally { sem.release(); }
```

## Question 7: What is Phaser? How does it improve on CountDownLatch and CyclicBarrier?

`Phaser` is a flexible barrier that supports dynamic registration of parties, multiple phases, and hierarchical composition. Unlike `CyclicBarrier` (fixed parties) and `CountDownLatch` (one-shot), `Phaser` allows parties to register/deregister dynamically.

```java
Phaser phaser = new Phaser(3); // 3 parties
phaser.arriveAndAwaitAdvance(); // similar to barrier.await()
phaser.arriveAndDeregister(); // leave the phaser
```

## Question 8: Explain StampedLock and optimistic reading.

`StampedLock` (Java 8+) improves read scalability with three modes:
- **Writing** – exclusive write lock (like `writeLock`)
- **Reading** – shared read lock (like `readLock`)
- **Optimistic Reading** – lock-free read; validate after; retry if invalidated

```java
StampedLock sl = new StampedLock();
long stamp = sl.tryOptimisticRead(); // no lock taken
int value = sharedValue; // read

if (!sl.validate(stamp)) { // check if write occurred
    stamp = sl.readLock(); // fall back to read lock
    try { value = sharedValue; }
    finally { sl.unlockRead(stamp); }
}
```

## Question 9: Explain the Exchanger class.

`Exchanger<V>` allows two threads to exchange objects at a synchronization point. Both threads block until the other arrives, then swap.

```java
Exchanger<List<Integer>> exchanger = new Exchanger<>();

// Thread 1 (producer):
List<Integer> full = fillBuffer();
List<Integer> empty = exchanger.exchange(full);

// Thread 2 (consumer):
List<Integer> full = exchanger.exchange(emptyBuffer);
```

## Question 10: What is a Condition variable?

A condition associated with a `ReentrantLock`. `Lock.newCondition()` provides await/signal analogous to `wait/notify` but more flexible.


## More Questions
31. What are Concurrent Collections? 
32. HashMap vs ConcurrentHashMap. 
33. CopyOnWriteArrayList use case. 
34. BlockingQueue vs Queue. 
35. Producer-Consumer Problem. 
36. CountDownLatch vs CyclicBarrier. 
37. What is Semaphore? 
38. What is Phaser? 
39. wait() vs sleep(). 
40. wait() vs notify() vs notifyAll(). 
41. What is ForkJoinPool? 
42. Parallel Stream in Java. 
43. How does CompletableFuture improve performance? 
44. How do you handle exceptions in CompletableFuture? 
45. ThreadLocal use cases. 
46. Daemon Thread vs User Thread. 
47. How does JVM schedule threads? 
48. How do you debug multithreading issues? 
49. Real-world use cases of multithreading in Spring Boot applications. 
50. Explain a production issue related to multithreading that you solved.

Concurrency & Multithreading:
16. Diff between synchronized keyword and ReentrantLock. What is the trade-off?
17. What is a volatile keyword?
18. What is the difference between the volatile keyword and the Atomic class (AtomicInteger & AtomicFloat)?
19. Explain CompletableFuture methods.