# Thread Lifecycle and the Java Memory Model

## 1. Definition

A thread moves through states: NEW -> RUNNABLE -> (BLOCKED / WAITING /
TIMED_WAITING) -> TERMINATED. The Java Memory Model (JMM) defines when writes by
one thread become visible to reads by another.

## 2. Why it exists

Modern CPUs and compilers reorder instructions and cache values in registers for
performance. Without a memory model, one thread's writes might never become
visible to another thread, or might become visible in a different order than
they were written — the JMM defines the rules that make concurrent code
predictable.

## 3. How it works — happens-before

A "happens-before" relationship guarantees that memory effects of one action are
visible to another. Key sources of happens-before:

- A `synchronized` block's unlock happens-before the next thread's lock on the
  same monitor.
- A write to a `volatile` field happens-before every subsequent read of that
  field.
- Starting a thread happens-before any action in that thread.
- All actions in a thread happen-before another thread successfully joins it.

Without one of these relationships, there's no guarantee another thread ever
sees your write — this is why plain (non-volatile, non-synchronized) shared
variables are unsafe across threads.

## 4. Code example — visibility bug

```java
class Flag {
    private boolean running = true;   // NOT volatile

    void stop() { running = false; }  // written by thread A

    void run() {
        while (running) {             // read by thread B — may loop forever!
            // do work
        }
    }
}
```

Fix with `volatile`:

```java
private volatile boolean running = true;
```

## 5. Production use case

Feature flags, shutdown signals, and cached configuration values that are
written by one thread (e.g., an admin action or config reloader) and read by
worker threads all need either `volatile`, `synchronized`, or atomic classes to
guarantee visibility.

## 6. Common mistakes

- Assuming a plain boolean flag will "eventually" be seen by another thread —
  the JMM makes no such guarantee without a happens-before edge.
- Using `volatile` for compound operations like `count++` (not atomic — read,
  increment, write are three separate steps).
- Confusing "atomic" with "visible" — an operation can be visible but not
  atomic, or atomic but still subject to reordering without proper synchronization.

## 7. Trade-offs

| volatile | synchronized | Atomic classes |
|---|---|---|
| Visibility only, no atomicity | Visibility + atomicity + mutual exclusion | Visibility + atomicity, lock-free (CAS) |
| Cheapest | Most expensive (can block) | Cheap for simple counters/references |

## 8. Interview questions

1. What is a race condition, concretely?
2. What is "happens-before" and why does it matter?
3. Why is `volatile` insufficient for something like a counter?
4. What are the possible thread states and what causes each transition?

## 9. Short interview answer

"The JMM defines which writes by one thread are guaranteed visible to another.
Without a happens-before relationship — from synchronized, volatile, or thread
start/join — a thread may never see another thread's update, or see it out of
order. volatile gives visibility but not atomicity, so it's not enough for
compound operations like increment; those need synchronized, a lock, or an
atomic class."

## 10. Related topics

- [Race conditions](race-conditions.md)
- [ExecutorService](executor-service.md)
