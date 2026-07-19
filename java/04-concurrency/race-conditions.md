# Race Conditions: Why count++ Is Not Thread-Safe

## 1. Definition

A race condition occurs when the correctness of a program depends on the
relative timing of multiple threads, and at least one thread modifies shared
state.

## 2. Why it exists (as a problem)

`count++` looks like a single operation but compiles into three: read `count`,
add 1, write `count` back. If two threads interleave these three steps, one
increment can be lost entirely.

## 3. How it works

```text
Thread A reads count = 5
Thread B reads count = 5
Thread A computes 6, writes count = 6
Thread B computes 6, writes count = 6   <- one increment is lost!
```

## 4. Code example — fix with AtomicInteger or synchronized

```java
// Unsafe
int count = 0;
void increment() { count++; }

// Fixed with AtomicInteger (lock-free, CAS-based)
AtomicInteger count = new AtomicInteger(0);
void increment() { count.incrementAndGet(); }

// Fixed with synchronized (mutual exclusion)
int count = 0;
synchronized void increment() { count++; }
```

## 5. Production use case

Request counters, in-memory rate limiters, and shared caches under concurrent
load are classic places race conditions silently corrupt data — often only
showing up under production traffic, never in single-threaded tests.

## 6. Common mistakes

- Believing `volatile` fixes `count++` (it only fixes visibility, not the
  read-modify-write race).
- Over-synchronizing entire methods when only a small critical section needs
  protection, hurting throughput.
- Not testing concurrent code under actual concurrent load.

## 7. Trade-offs

| AtomicInteger | synchronized |
|---|---|
| Lock-free, generally faster under contention | Simpler to reason about for complex multi-step logic |
| Good for single-variable updates | Needed when multiple variables must update together |

## 8. Interview questions

1. Why exactly is `count++` not thread-safe?
2. How does `AtomicInteger` achieve thread safety without locks?
3. When would `synchronized` be necessary even though atomics exist?
4. How would you test for a race condition?

## 9. Short interview answer

"count++ is really three steps — read, add, write — so two threads can
interleave and lose an update. I fix single-variable counters with
AtomicInteger, which uses compare-and-swap to retry atomically without
blocking, and reach for synchronized or a lock when multiple related pieces of
state need to change together consistently."

## 10. Related topics

- [Thread lifecycle & JMM](threads.md)
- [ExecutorService](executor-service.md)
