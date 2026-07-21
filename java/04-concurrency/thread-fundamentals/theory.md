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


## Visual Interview Series

Introducing the Java Multithreading — The Ultimate Visual Interview Series (Part 1/5). 
 
Many developers learn Java Multithreading through books or lengthy videos. The challenge is that by the time you finish, you often forget what you learned at the start. 
 
To address this, I created a visual Java Multithreading handbook that you can review in just 2–3 minutes before an interview. 
 
In this first part of a 5-part series, we will cover Java Multithreading from the fundamentals to production-level concepts utilized in Spring Boot. 
 
📌 In this post, you'll learn: 
 
✅ Why Multithreading exists  
✅ Program → Process → JVM → Thread  
✅ Process vs Thread  
✅ JVM Memory Areas  
✅ CPU Scheduling & Context Switching  
✅ Thread Life Cycle  
✅ 2-Minute Interview Cheat Sheet  
 
📚 What's coming next? 
 
🔹 Part 2 — Thread Creation, Runnable, Thread APIs & Best Practices  
🔹 Part 3 — Thread Safety, Race Conditions, volatile & Synchronization  
🔹 Part 4 — Inter-thread Communication, Locks & Deadlocks  
🔹 Part 5 — Executor Framework, Thread Pools, CompletableFuture & Spring Boot Multithreading  
 
The aim of this series is not just to explain the APIs, but to provide insights into: 
 
- How Java works internally  
- What interviewers actually ask  
- How these concepts are applied in real Spring Boot applications  
 
If you're preparing for Java Backend interviews, I hope these notes assist you as much as they helped me during their creation.


## Multithreading Lessons

Multithreading in Java isn’t just an interview topic — it’s the difference between a system that scales and one that falls over under load.
A few lessons I’ve picked up building high-throughput services in finance and healthcare:
🔹 Stop creating raw Threads. ExecutorService + a properly sized thread pool beats new Thread() every time. Unbounded thread creation is a silent production killer.
🔹 CompletableFuture > Future. Chaining async calls (thenApply, thenCompose, allOf) makes non-blocking pipelines readable instead of a callback maze.
🔹 Not everything needs a lock. Before reaching for synchronized, check if java.util.concurrent (ConcurrentHashMap, AtomicInteger, CopyOnWriteArrayList) already solves it lock-free.
🔹 Deadlocks hide in code review, not production — if you’re disciplined. Consistent lock ordering and timeouts (tryLock) catch 90% of issues before they ever reach staging.
🔹 Virtual threads (Java 21) change the math. For I/O-bound workloads, they make thousands of concurrent tasks cheap without the thread-pool tuning gymnastics.

## Thread States and Debugging

Most Java concurrency bugs don't start with complex logic.

They start with not understanding what a thread is actually doing.

Early on, I used to think of threads in a very simple way:
→ Created
→ Running
→ Done

But while working with backend services, async processing, and thread pools, that mental model was not enough.

Because a thread is not always "running."

Sometimes it is:
→ Waiting for a lock
→ Sleeping
→ Blocked on I/O
→ Waiting in a queue
→ Ready to run but not scheduled yet

And that difference matters.

A service can look healthy from the outside but still have hidden thread issues inside.

Some signs:
→ Request latency increases
→ CPU does not explain the slowdown
→ Thread pool queue keeps growing
→ Database calls start piling up
→ APIs timeout under load

One useful concept every Java backend engineer should know:

A Java thread moves through multiple states.
→ NEW
→ RUNNABLE
→ BLOCKED
→ WAITING
→ TIMED_WAITING
→ TERMINATED

Understanding these states helps when debugging production issues using thread dumps.

For example:
If many threads are BLOCKED, the issue may be lock contention.
If many threads are WAITING, the issue may be coordination or resource dependency.

If many threads are TIMED_WAITING, the system may be waiting on sleep, timeout, or scheduled operations.

This becomes even more important when working with:
→ ExecutorService
→ ThreadPoolTaskExecutor
→ Kafka consumers
→ Async API calls
→ Scheduled jobs

One lesson I learned:

Concurrency debugging is not about guessing.

It is about observing where threads are stuck.

A thread dump can tell a story that logs sometimes cannot.