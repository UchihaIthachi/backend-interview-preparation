# Top Java Concurrency Questions — Scenario-Based Answers

## 1. A synchronized block reduces throughput by 70%. What would you inspect first?

Start by confirming whether the synchronized block has become a **high-contention lock**.

```java
synchronized (sharedLock) {
    updateSharedState();
}
```

Inspect:

- How many threads compete for the same monitor?
- How long is the lock held?
- Is network, database, file, or logging I/O performed while holding it?
- Does the critical section contain expensive computation?
- Is the lock protecting more state than necessary?
- Can independent operations use separate locks?
- Is one frequently accessed object being used as a global lock?
- Are threads repeatedly reacquiring the same monitor?

### Problematic example

```java
synchronized (lock) {
    repository.save(order);
    remoteClient.notify(order);
}
```

The lock remains held while waiting for the database and remote service.

A better structure may be:

```java
OrderEvent event;

synchronized (lock) {
    event = updateSharedState(order);
}

remoteClient.notify(event);
```

Only move work outside the lock when doing so preserves correctness.

### Diagnostic tools

Use:

- Java Flight Recorder lock events
- Thread dumps
- Monitor contention metrics
- Profiler lock views
- Request latency percentiles
- Time spent waiting versus time holding the lock

Capture several thread dumps rather than relying on one snapshot.

### Possible fixes

- Reduce the critical-section size.
- Move slow I/O outside the lock.
- Partition shared state.
- Use lock striping.
- Use immutable or thread-confined data.
- Replace simple counters with atomic variables or `LongAdder`.
- Use concurrent collections and their atomic methods.
- Remove unnecessary synchronization.
- Redesign around message passing where appropriate.

### Interview-ready answer

> I would first measure contention and lock-hold time. I would check whether slow I/O or unrelated work is inside the synchronized block and whether one global monitor protects independent operations. Then I would reduce the critical section, partition the state, or use a higher-level concurrent structure while preserving the required invariant.

---

## 2. Increasing a thread pool from 50 to 500 makes the API slower. Why?

More threads do not create more CPU, database connections, network bandwidth, or downstream capacity.

Increasing the pool may introduce:

- CPU oversubscription
- Excessive context switching
- Larger thread-stack memory usage
- More garbage-collection pressure
- Lock contention
- CPU-cache disruption
- Database connection-pool contention
- Downstream-service overload
- Longer timeout and retry queues

Suppose 500 workers compete for a database pool containing 20 connections:

```text
500 worker threads
        ↓
20 database connections
        ↓
480 threads waiting
```

The additional threads increase memory and scheduling overhead without increasing useful database concurrency.

### CPU-bound workload

If the machine has eight available cores, 500 CPU-intensive workers normally create severe oversubscription.

```text
Runnable threads >> CPU cores
→ context switching
→ cache misses
→ less useful work per second
```

### I/O-bound workload

More threads can improve I/O concurrency only until another resource becomes saturated:

- Connection pool
- Socket limit
- Remote-service capacity
- Disk throughput
- Rate limit
- Memory

### Interview-ready answer

> The larger pool probably exceeded the capacity of the CPU or a downstream resource. The extra threads caused context switching, lock contention, memory overhead, or connection-pool waiting. I would inspect thread states, queue delay, CPU saturation, connection-pool metrics, and downstream latency, then size the pool against the actual bottleneck rather than the request rate alone.

---

## 3. A `ThreadPoolExecutor` has idle capacity, but tasks remain queued. How is that possible?

One common cause is the interaction between `corePoolSize`, `maximumPoolSize`, and the work queue.

A `ThreadPoolExecutor` broadly handles a submitted task as follows:

1. Create a worker when the current worker count is below `corePoolSize`.
2. Otherwise, enqueue the task.
3. Create workers beyond the core size only when the queue cannot accept the task.
4. Reject the task when both the pool and queue are at capacity.

Consider:

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
        10,
        100,
        60,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>()
);
```

Because the queue is effectively unbounded, the executor normally grows only to ten core workers. Tasks accumulate in the queue even though `maximumPoolSize` is 100.

The other 90 potential threads are not created because the queue never becomes full.

### “Idle threads” may also be misleading

A monitoring tool may show idle threads that:

- Do not belong to this executor
- Were observed during a different moment than the queue measurement
- Are waiting on another condition
- Belong to another executor
- Are paused by custom executor logic
- Are blocked inside task code rather than available to dequeue work

A truly available worker belonging to the executor should normally attempt to take queued work.

### Possible solutions

Depending on the requirement:

- Increase `corePoolSize`.
- Use a bounded queue.
- Prestart core threads.
- Choose an appropriate rejection policy.
- Reduce task blocking time.
- Use separate pools for different workloads.

```java
executor.prestartAllCoreThreads();
```

Do not simply set a very large maximum size without bounding downstream concurrency.

### Interview-ready answer

> With an unbounded queue, `ThreadPoolExecutor` normally queues tasks after reaching the core size and does not create threads up to the maximum size. Therefore, `maximumPoolSize` may have no practical effect. I would inspect the core size, queue type, worker states, and whether the apparently idle threads actually belong to that executor.

---

## 4. Why does `volatile` solve visibility but not make `count++` thread-safe?

A volatile field provides visibility and ordering guarantees.

```java
private volatile boolean running = true;
```

When one thread writes:

```java
running = false;
```

another thread reading the volatile field can observe that update according to the Java Memory Model.

However:

```java
private volatile int count;

count++;
```

is a compound operation.

It conceptually performs:

```text
Read count
→ add one
→ write count
```

Two threads can interleave:

```text
Thread A reads 10
Thread B reads 10
Thread A writes 11
Thread B writes 11
```

The correct result should be 12, but one update is lost.

### Correct alternatives

Atomic counter:

```java
private final AtomicInteger count =
        new AtomicInteger();

count.incrementAndGet();
```

Synchronization:

```java
private int count;

public synchronized void increment() {
    count++;
}
```

High-contention statistics counter:

```java
private final LongAdder count =
        new LongAdder();

count.increment();
```

### Interview-ready answer

> `volatile` guarantees that threads observe current writes to the field, but it does not combine a read-modify-write sequence into one atomic action. Since `count++` performs a read, increment, and write, threads can lose updates. I would use an atomic variable or a lock.

---

## 5. The service suddenly has 10,000 threads. CPU is low, but latency is terrible. What is your first hypothesis?

The first hypothesis should be that most threads are **waiting rather than computing**.

Likely causes include:

- Slow database queries
- Database connection-pool exhaustion
- Remote API latency
- Socket reads without suitable timeouts
- Lock contention
- Queue waits
- Thread-per-request creation
- Unbounded executor growth
- Retry amplification
- Deadlocked or indefinitely blocked tasks

Low CPU with many threads often means:

```text
Threads exist
but
threads are blocked, waiting, parked, or sleeping
```

### First checks

Capture thread dumps and classify states:

- `RUNNABLE`
- `BLOCKED`
- `WAITING`
- `TIMED_WAITING`

Then group threads by stack trace.

Examples:

```text
Thousands waiting in HikariPool.getConnection()
→ database connection bottleneck
```

```text
Thousands blocked in socketRead()
→ slow downstream service or missing timeout
```

```text
Thousands waiting in ThreadPoolExecutor.getTask()
→ many idle pool workers
```

```text
Thousands blocked on the same monitor
→ lock contention
```

Also inspect:

- Thread-creation rate
- Executor pool sizes
- Active and queued task counts
- Database pool metrics
- HTTP connection pools
- Remote-service latency
- Timeout and retry configuration

### Interview-ready answer

> My first hypothesis is a waiting or blocking bottleneck rather than CPU saturation. I would capture several thread dumps, group identical stack traces, and check database connections, network calls, locks, queue waits, timeouts, and unbounded thread creation.

---

## 6. Hundreds of threads are `BLOCKED`. What does that tell you, and what does it not tell you?

`BLOCKED` means a thread is waiting to enter a Java monitor used by `synchronized`.

Example:

```java
synchronized (lock) {
    update();
}
```

If one thread owns `lock`, other threads attempting to enter the same synchronized region may appear as `BLOCKED`.

### What it tells you

- Monitor contention exists.
- Threads are waiting to acquire an intrinsic lock.
- A synchronized block or method is involved.
- The lock owner may be found in the thread dump.

### What it does not tell you

A single `BLOCKED` state does not prove:

- There is a deadlock.
- The lock is held for a long time.
- The lock is the primary performance bottleneck.
- The application is incorrectly synchronized.
- Every blocked thread is waiting on the same monitor.
- The monitor owner is permanently stuck.
- A `ReentrantLock` is involved.

Threads waiting for `ReentrantLock` commonly appear as `WAITING` or `TIMED_WAITING` because it is built using parking mechanisms rather than an intrinsic monitor.

### Investigation

1. Identify the monitor address in the thread dump.
2. Find the thread that owns it.
3. Inspect the owner’s stack.
4. Capture repeated dumps several seconds apart.
5. Determine whether ownership changes.
6. Measure monitor wait and hold times with JFR.
7. Check for nested locks and circular dependencies.

### Interview-ready answer

> `BLOCKED` means the threads are waiting to enter a synchronized monitor. It indicates contention but does not by itself prove deadlock, excessive hold time, or the root cause. I would find the lock owner, inspect its stack, and compare several thread dumps or JFR lock events.

---

## 7. The task queue keeps growing, but CPU remains below 30%. Where do you start?

Begin by comparing the task arrival rate with the task completion rate.

```text
Arrival rate > service rate
→ queue grows
```

Low CPU indicates that workers may be blocked on something other than computation.

### Investigation order

#### 1. Inspect worker thread states

Are workers:

- Waiting for database connections?
- Blocked on network calls?
- Waiting on locks?
- Sleeping due to retry backoff?
- Waiting for another task in the same pool?
- Stuck on slow disk operations?

#### 2. Measure task phases

Record:

- Queue wait time
- Task execution time
- Database duration
- Remote-call duration
- Lock-wait time
- Serialization time
- Retry count

A task may spend 20 milliseconds computing and five seconds waiting for a database connection.

#### 3. Inspect downstream pools

Check:

- Database active, idle, and pending connections
- HTTP client connection-pool usage
- Kafka producer or consumer blocking
- Rate limits
- External-service saturation
- Disk latency

#### 4. Inspect executor configuration

- Is the core pool too small?
- Is the queue unbounded?
- Are long and short tasks mixed in one pool?
- Are tasks waiting on futures submitted to the same executor?
- Is the rejection policy hiding overload?

#### 5. Check locks

Low CPU and queue growth may result from many workers waiting on one serialized critical section.

### Interview-ready answer

> I would first determine whether workers are blocked on I/O, locks, connection pools, or task dependencies. Then I would compare task arrival and completion rates and separate queue delay from execution time. Low CPU suggests the bottleneck is probably waiting on another resource rather than insufficient processor capacity.

---

## 8. How do you size thread pools for CPU-bound and I/O-bound workloads?

## CPU-bound work

For CPU-intensive tasks, a starting point is approximately:

```text
Number of usable CPU cores
```

or sometimes:

```text
CPU cores + 1
```

The extra thread may help when one worker occasionally pauses, but too many runnable threads cause context switching.

Example:

```java
int processors =
        Runtime.getRuntime().availableProcessors();

ExecutorService executor =
        Executors.newFixedThreadPool(processors);
```

The reported processor count may need adjustment in containers or when the application shares CPU with other workloads.

## I/O-bound work

I/O-bound tasks spend part of their time waiting.

A common estimation model is:

```text
threads ≈ cores × (1 + wait time / service time)
```

For example:

```text
8 cores
wait time = 90 ms
CPU service time = 10 ms

threads ≈ 8 × (1 + 90 / 10)
        ≈ 80
```

This is only a starting estimate.

The real limit may be:

- Database connection count
- HTTP connection count
- Remote-service quota
- Memory
- File descriptors
- Desired queue latency

### Bounded pool example

```java
ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
                20,
                80,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1_000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
```

The exact values require load testing.

### Mixed workloads

Do not place CPU-heavy and blocking tasks in the same executor when they have different capacity requirements.

```text
CPU pool
I/O pool
scheduled-task pool
```

### Virtual threads

Virtual threads can simplify high-concurrency blocking I/O, but they do not remove downstream limits.

Ten thousand virtual threads still cannot efficiently use ten database connections simultaneously.

### Interview-ready answer

> For CPU-bound work, I start near the number of available cores. For blocking I/O, I estimate from the wait-to-compute ratio, but I cap concurrency according to downstream resources. I use bounded queues and validate the size through load testing, throughput, latency, and saturation metrics.

---

## 9. A task submitted to an `ExecutorService` never runs. What could cause it?

Several conditions can make a task appear to disappear.

## Executor was shut down

```java
executor.shutdown();
executor.submit(task);
```

Submission should cause rejection, but the rejection may be swallowed by surrounding code.

Check:

```java
executor.isShutdown();
executor.isTerminated();
```

---

## Silent rejection policy

A custom executor may use:

```java
new ThreadPoolExecutor.DiscardPolicy()
```

Rejected tasks are silently discarded.

`DiscardOldestPolicy` may silently remove a queued task to make room for a new one.

Prefer observable rejection:

```java
new ThreadPoolExecutor.AbortPolicy()
```

or explicitly instrument the chosen overload behavior.

---

## Task is stuck behind long-running tasks

In a single-thread executor:

```java
ExecutorService executor =
        Executors.newSingleThreadExecutor();

executor.submit(this::neverEndingTask);
executor.submit(this::expectedTask);
```

The second task never runs while the first task never completes.

---

## Thread-pool starvation deadlock

A task submits another task to the same bounded executor and waits for its result:

```java
Future<Result> outer = executor.submit(() -> {
    Future<Result> inner =
            executor.submit(this::calculate);

    return inner.get();
});
```

If all workers do this, no worker remains to execute the inner tasks.

---

## Task is queued far behind other work

With an unbounded queue and slow workers, the task may technically be waiting but not run within a useful time.

Inspect:

```java
executor.getQueue().size();
executor.getActiveCount();
executor.getCompletedTaskCount();
```

---

## Core pool has no active workers

Unusual custom configurations may use:

- `corePoolSize` of zero
- Core-thread timeout
- Custom queues
- Paused executors
- Broken worker factories

Normally, `ThreadPoolExecutor` ensures a worker exists when queued work is present, but custom behavior should be inspected.

---

## Scheduled too far in the future

For a scheduled executor:

```java
scheduler.schedule(
        task,
        24,
        TimeUnit.HOURS
);
```

A wrong unit or timestamp calculation can make the task appear missing.

---

## The task runs but logging is absent

Possible reasons:

- Logging level disabled
- Exception captured in a `Future`
- Task returns before reaching the log
- Log buffering
- Correlation ID missing
- The wrong application instance was inspected

Exceptions from `submit()` tasks are captured in the returned `Future`:

```java
Future<?> future = executor.submit(task);

future.get(); // Surfaces task exception
```

By comparison, exceptions from `execute()` reach the worker’s uncaught-exception handling path.

### Interview-ready answer

> I would check executor shutdown state, rejection policy, queue depth, long-running tasks, starvation caused by tasks waiting on work submitted to the same pool, and scheduled delays. I would also inspect the returned `Future`, because `submit()` captures task exceptions rather than necessarily logging them.

---

## 10. A thread acquires a `ReentrantLock` and throws before unlocking. What happens?

The lock remains held unless application code explicitly unlocks it.

Problematic:

```java
lock.lock();

updateState(); // Throws exception

lock.unlock();
```

If `updateState()` throws, `unlock()` is skipped.

Other threads attempting to acquire the lock may then block indefinitely or until a timed acquisition expires.

Unlike exiting a `synchronized` block through an exception, a `ReentrantLock` is not automatically released by block structure.

### Correct pattern

```java
lock.lock();

try {
    updateState();
} finally {
    lock.unlock();
}
```

For interruptible acquisition:

```java
lock.lockInterruptibly();

try {
    updateState();
} finally {
    lock.unlock();
}
```

For conditional acquisition:

```java
boolean acquired =
        lock.tryLock(500, TimeUnit.MILLISECONDS);

if (!acquired) {
    handleContention();
    return;
}

try {
    updateState();
} finally {
    lock.unlock();
}
```

### Additional danger

If the owning thread terminates without unlocking, other threads can remain unable to acquire the lock. The lock does not recover simply because the owner thread died.

### Interview-ready answer

> If the exception bypasses `unlock()`, the `ReentrantLock` remains held and other threads may wait indefinitely. Every successful `lock()` must therefore be paired with `unlock()` in a `finally` block.

---

# Quick Diagnostic Summary

| Symptom                                | First suspicion                                    |
| -------------------------------------- | -------------------------------------------------- |
| Throughput falls after synchronization | Lock contention or long critical section           |
| Larger pool makes API slower           | Oversubscription or downstream saturation          |
| Tasks queue below maximum pool size    | Unbounded queue and core-pool behavior             |
| `volatile int` loses increments        | Compound operation is not atomic                   |
| Thousands of threads and low CPU       | Blocking I/O, locks, or resource-pool waits        |
| Hundreds of `BLOCKED` threads          | Contention on intrinsic monitors                   |
| Queue grows while CPU stays low        | Workers waiting on downstream resources            |
| CPU workload needs pool sizing         | Start near available processor count               |
| Submitted task never executes          | Rejection, backlog, starvation, shutdown, or delay |
| Exception after `lock()`               | Lock remains held unless released in `finally`     |

# Short Interview Summary

> Concurrency performance problems are usually resource-capacity problems rather than simply thread-count problems. I inspect thread states, executor configuration, queue delay, lock contention, connection pools, downstream latency, and task dependencies. I size CPU pools near the available cores, bound I/O concurrency by downstream capacity, use `volatile` only for visibility, and always release explicit locks in `finally`.
