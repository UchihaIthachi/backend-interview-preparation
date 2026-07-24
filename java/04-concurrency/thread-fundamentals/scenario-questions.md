# Scenario-Based Questions — Thread Dumps, Pool Exhaustion, Deadlocks, and Production Debugging

## Scenario 1: The application is stuck. How do you analyze a thread dump?

A thread dump is a snapshot of every JVM thread, including:

- Thread name and ID
- Java thread state
- Current stack trace
- Monitors owned
- Monitors being awaited
- Synchronizers such as `ReentrantLock`
- Detected deadlock information

The goal is not merely to count thread states. The goal is to determine **what resource prevents progress**.

---

### Step 1: Capture multiple thread dumps

```bash
jcmd <pid> Thread.print -l
```

Alternatively:

```bash
jstack -l <pid>
```

Capture at least three dumps several seconds apart:

```bash
jcmd <pid> Thread.print -l > dump-1.txt
sleep 5

jcmd <pid> Thread.print -l > dump-2.txt
sleep 5

jcmd <pid> Thread.print -l > dump-3.txt
```

A single dump cannot reliably distinguish:

- Temporary waiting
- Slow execution
- Permanent deadlock
- Repeated retries
- A thread that is still making progress

---

### Step 2: Examine the thread-state distribution

| State           | Meaning                                                          |
| --------------- | ---------------------------------------------------------------- |
| `RUNNABLE`      | Executing, ready to execute, or inside certain native operations |
| `BLOCKED`       | Waiting to acquire an intrinsic monitor used by `synchronized`   |
| `WAITING`       | Waiting indefinitely for another action                          |
| `TIMED_WAITING` | Waiting with a timeout                                           |
| `TERMINATED`    | Execution has completed                                          |

Important nuance:

> A thread waiting for `ReentrantLock` commonly appears as `WAITING`, not `BLOCKED`.

`BLOCKED` specifically refers to intrinsic monitor acquisition.

---

### Step 3: Group threads by similar stack traces

Suppose many request threads show:

```text
java.lang.Thread.State: WAITING

at java.util.concurrent.locks.LockSupport.park(...)
at com.zaxxer.hikari.pool.HikariPool.getConnection(...)
at com.example.OrderRepository.save(...)
```

This suggests that request threads are waiting for database connections.

Another example:

```text
java.lang.Thread.State: BLOCKED

at com.example.InventoryService.reserve(...)
- waiting to lock <0x00000001>
```

If many threads wait for the same monitor, investigate:

- Who owns that monitor?
- What is the owner doing?
- Is it performing database or network I/O while holding the lock?
- Does the same owner appear unchanged across multiple dumps?

---

### Step 4: Look for common production patterns

#### Lock contention

```text
Many threads BLOCKED
→ same monitor
→ one owner
```

Likely causes:

- Large synchronized section
- Slow operation inside a lock
- One global lock protecting unrelated work
- Deadlock

---

#### Connection-pool exhaustion

```text
Request threads
→ waiting in HikariCP
→ database connections all active
```

Investigate:

- Slow queries
- Long transactions
- Connection leaks
- Pool size
- Database capacity
- Threads holding transactions during remote calls

---

#### Thread-pool starvation

```text
All worker threads
→ waiting on Future.get() or join()
→ dependent tasks queued in the same executor
```

Example:

```java
ExecutorService executor =
        Executors.newFixedThreadPool(2);

Future<String> first = executor.submit(() -> {
    Future<String> nested =
            executor.submit(this::loadData);

    return nested.get();
});
```

Both workers can become occupied waiting for queued work that has no free worker to execute it.

---

#### External dependency stall

```text
Many threads
→ SocketInputStream.read()
→ same downstream host
```

Investigate:

- Missing connect timeout
- Missing read timeout
- Downstream service latency
- Connection-pool limits
- Retry amplification

---

#### Busy loop

```text
RUNNABLE in the same application method
→ high CPU
→ stack remains unchanged
```

Possible causes:

- Infinite loop
- Spin loop
- Failed CAS retry loop
- Missing blocking operation
- Incorrect termination flag

---

### Step 5: Correlate with runtime metrics

Thread dumps should be combined with:

- CPU usage
- Heap usage
- Garbage-collection pauses
- Executor active-thread count
- Executor queue depth
- Database connection-pool pending count
- HTTP connection-pool pending count
- Request latency
- Timeout rate
- Kafka consumer lag

A thread dump gives the thread-level symptom. Metrics reveal its effect and duration.

---

### Interview-ready answer

> I capture several thread dumps a few seconds apart, group threads by state and stack trace, and identify the resource preventing progress. I look for monitor contention, connection-pool waits, executor starvation, socket calls, future joins, and deadlock cycles. I then correlate the dumps with CPU, queue, connection-pool, and latency metrics rather than diagnosing from one state alone.

---

# Scenario 2: The service becomes unresponsive under load because of thread exhaustion

## How does a Java thread pool work?

A `ThreadPoolExecutor` accepts tasks and manages reusable worker threads.

Its main configuration is:

```java
new ThreadPoolExecutor(
        corePoolSize,
        maximumPoolSize,
        keepAliveTime,
        timeUnit,
        workQueue,
        threadFactory,
        rejectionHandler
);
```

### Task-submission flow

```text
Task submitted
      ↓
Workers below corePoolSize?
      ├── Yes → create a worker
      └── No
            ↓
      Queue accepts task?
            ├── Yes → queue task
            └── No
                  ↓
      Workers below maximumPoolSize?
            ├── Yes → create another worker
            └── No → reject task
```

### Important unbounded-queue behavior

With an unbounded queue:

```java
Executors.newFixedThreadPool(10);
```

tasks are queued after ten workers exist.

The pool does not grow beyond ten workers, and the queue can grow indefinitely.

---

## What happens when every worker is blocked?

Suppose all workers wait for database connections:

```text
Pool workers:              100
Database connections:       10
Waiting executor tasks:  20,000
```

The likely result is:

```text
All workers become occupied
→ new tasks enter the queue
→ queue latency rises
→ requests time out
→ clients retry
→ even more tasks arrive
→ service becomes unresponsive
```

CPU usage may remain low because the workers are waiting rather than computing.

---

## Thread-starvation deadlock

A particularly dangerous case occurs when workers wait for tasks submitted to the same saturated executor:

```java
ExecutorService executor =
        Executors.newFixedThreadPool(2);

Callable<String> parentTask = () -> {
    Future<String> child =
            executor.submit(this::performChildWork);

    return child.get();
};
```

If both workers run parent tasks, both may wait for child tasks that remain queued forever.

---

## How would you tune the pool?

### CPU-bound workload

Starting point:

```text
Pool size ≈ available CPU cores
```

Example:

```java
int processors =
        Runtime.getRuntime()
                .availableProcessors();
```

Too many CPU workers cause:

- Context switching
- Cache disruption
- Scheduler overhead
- Higher latency

---

### Blocking or I/O-bound workload

A common starting estimate is:

```text
threads =
CPU cores
× target CPU utilization
× (1 + wait time / compute time)
```

This is only a heuristic.

The final pool size must also respect:

- Database connection count
- HTTP client connection limits
- Downstream concurrency limits
- Memory
- File descriptors
- Rate limits
- Required latency

A pool of 200 threads does not help when only 20 database connections exist.

---

## Use a bounded pool

```java
ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
                16,
                32,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1_000),
                new NamedThreadFactory(
                        "order-worker"
                ),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
```

This bounds:

- Active worker count
- Waiting task count

`CallerRunsPolicy` applies backpressure by making the submitting thread execute the task.

It is appropriate only when running the task on the caller is safe. For example, it can increase API request latency.

---

## Recovery actions

When a service is already exhausted:

1. Capture thread dumps before restarting.
2. Identify what workers are waiting for.
3. Stop uncontrolled retries.
4. Apply downstream timeouts.
5. Reject excess work rather than queueing indefinitely.
6. Reduce long transactions and lock scopes.
7. Align pool size with downstream capacity.
8. Separate unrelated workloads into different executors.
9. Add queue and saturation metrics.
10. Restart only after collecting diagnostic evidence.

### Interview-ready answer

> When all workers are blocked, queued tasks stop making progress and latency rises until requests time out or the queue exhausts memory. I inspect what the workers are waiting for, then bound the queue, configure rejection behavior, add downstream timeouts, separate blocking and CPU workloads, and tune concurrency according to the actual downstream resource limits.

---

# Scenario 3: A CPU-intensive method is called frequently. How would you optimize it?

The first step is to confirm that the method is actually the bottleneck.

## Step 1: Profile before changing concurrency

Use tools such as:

- Java Flight Recorder
- Java Mission Control
- CPU profiling
- Allocation profiling
- Method-level timing
- Flame graphs

Determine whether the cost comes from:

- Computation
- Object allocation
- Garbage collection
- Lock contention
- Repeated parsing
- Serialization
- Logging
- Database or network calls

A method that appears CPU-intensive may actually spend most of its time allocating objects or waiting for a lock.

---

## Step 2: Remove repeated work

Possible improvements include:

- Cache deterministic results
- Precompute immutable data
- Avoid repeated parsing
- Reuse compiled patterns
- Batch work
- Reduce allocations
- Avoid unnecessary boxing
- Remove expensive logging from hot loops

Example:

```java
private static final Pattern ORDER_PATTERN =
        Pattern.compile("[A-Z]{3}-\\d+");
```

Do not compile the same pattern for every invocation.

---

## Step 3: Keep CPU work out of blocking pools

Use a dedicated CPU executor:

```java
int processors =
        Runtime.getRuntime()
                .availableProcessors();

ExecutorService cpuExecutor =
        Executors.newFixedThreadPool(processors);
```

Separating CPU work prevents it from occupying threads needed for:

- HTTP calls
- Database access
- Kafka processing
- Scheduled tasks

---

## Step 4: Parallelize only independent work

```java
List<Result> results =
        requests.parallelStream()
                .map(this::calculate)
                .toList();
```

Parallel execution may help when:

- Input is large
- Each operation is expensive
- Tasks are independent
- Shared mutable state is absent
- Ordering requirements are limited
- Profiling shows an improvement

It may hurt performance when:

- Work is small
- Tasks compete for the same lock
- The common pool is already busy
- Operations perform blocking I/O
- The stream mutates shared state

---

## Step 5: Add admission control

Even optimized CPU work can overload the machine.

```java
Semaphore permits =
        new Semaphore(processors);
```

Reject, delay, or queue work when CPU capacity is exhausted rather than creating unlimited parallel tasks.

### Interview-ready answer

> I first profile the method to confirm whether the cost is computation, allocation, locking, or I/O. Then I remove repeated work, reduce allocations, cache only safe deterministic results, and isolate the method in a CPU-sized executor. I parallelize only independent work and only when benchmarks show a benefit.

---

# Scenario 4: A thread pool is exhausted. What would you do?

## Immediate diagnosis

Inspect:

```java
executor.getPoolSize();
executor.getActiveCount();
executor.getLargestPoolSize();
executor.getQueue().size();
executor.getTaskCount();
executor.getCompletedTaskCount();
```

Ask:

- Are all workers active?
- Is the queue growing?
- Are tasks executing slowly?
- Are tasks waiting on a downstream dependency?
- Are workers waiting on other tasks in the same pool?
- Are tasks blocked on database connections or locks?
- Are timeouts missing?

---

## Common causes

### Long-running tasks mixed with short tasks

```text
Large report jobs
+
small request tasks
→ short work waits behind long work
```

Fix by using separate executors.

---

### Blocking I/O in a CPU pool

```text
CPU pool workers
→ wait on HTTP or database calls
→ CPU work cannot execute
```

Separate CPU-bound and blocking workloads.

---

### Pool larger than downstream capacity

```text
100 workers
→ 10 database connections
→ 90 workers wait
```

Reduce concurrency or increase downstream capacity only after validating that the downstream system can support it.

---

### Unbounded queue

```text
Tasks accumulate
→ memory grows
→ latency grows
→ stale tasks execute too late
```

Use a bounded queue and an explicit overload policy.

---

### Nested waiting

Avoid:

```java
executor.submit(() ->
        executor.submit(this::load).get()
);
```

Prefer asynchronous composition or separate executors.

---

## Overload strategies

- Reject immediately
- Execute in caller
- Return `429 Too Many Requests`
- Return `503 Service Unavailable`
- Drop non-critical background work
- Prioritize critical tasks
- Apply tenant quotas
- Use bounded retries
- Degrade optional features

### Interview-ready answer

> I do not immediately increase the pool size. I inspect active workers, queue depth, execution time, and stack traces to find the resource causing saturation. Then I bound the queue, add an overload policy, separate workloads, remove nested blocking, and align concurrency with database and downstream limits.

---

# Scenario 5: How would you monitor thread performance in production?

## JVM-level metrics

Monitor:

- Current live-thread count
- Peak thread count
- Daemon-thread count
- Threads created over time
- Deadlocked-thread count
- CPU usage per thread
- Monitor contention
- Parked and waiting threads

Programmatic access:

```java
ThreadMXBean threadBean =
        ManagementFactory.getThreadMXBean();

int liveThreads =
        threadBean.getThreadCount();

int peakThreads =
        threadBean.getPeakThreadCount();

long[] deadlocked =
        threadBean.findDeadlockedThreads();
```

---

## Executor metrics

For every important executor, monitor:

| Metric             | Why it matters                 |
| ------------------ | ------------------------------ |
| Pool size          | Current worker count           |
| Active count       | Workers currently executing    |
| Maximum size       | Configured concurrency ceiling |
| Queue depth        | Waiting backlog                |
| Queue capacity     | Remaining backlog capacity     |
| Completed tasks    | Throughput                     |
| Rejected tasks     | Overload                       |
| Queue-wait time    | Scheduling delay               |
| Execution time     | Task duration                  |
| Total task latency | Queue delay plus execution     |

A pool can have fast task execution but poor total latency because tasks wait in the queue.

```text
Total latency
=
queue-wait time
+
execution time
```

---

## Resource metrics to correlate

- HikariCP active, idle, and pending connections
- HTTP client active and pending connections
- Kafka consumer lag
- CPU load
- Heap and garbage collection
- Request P95 and P99 latency
- Timeout count
- Retry count
- Circuit-breaker state
- Lock wait duration

---

## Diagnostic tooling

Use:

```bash
jcmd <pid> Thread.print -l
```

Java Flight Recorder:

```bash
jcmd <pid> JFR.start \
    name=production-diagnostic \
    duration=60s \
    filename=/tmp/diagnostic.jfr
```

JFR can reveal:

- CPU hotspots
- Java monitor blocking
- Thread parking
- Allocation pressure
- Socket and file I/O
- Garbage-collection pauses

### Interview-ready answer

> I monitor thread counts, pool active count, queue depth, rejected tasks, queue-wait time, execution time, deadlocks, and per-thread CPU. I correlate them with database, HTTP, Kafka, CPU, GC, and request-latency metrics. For incidents, I use repeated thread dumps and Java Flight Recorder.

---

# Scenario 6: How would you implement a custom thread pool?

In production, “custom thread pool” normally means configuring `ThreadPoolExecutor`, not building worker management from scratch.

## Named thread factory

```java
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class NamedThreadFactory
        implements ThreadFactory {

    private final String prefix;
    private final AtomicInteger sequence =
            new AtomicInteger();

    public NamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable task) {
        Thread thread = new Thread(
                task,
                prefix + "-"
                        + sequence.incrementAndGet()
        );

        thread.setUncaughtExceptionHandler(
                (failedThread, failure) ->
                        System.err.println(
                                "Uncaught failure in "
                                        + failedThread.getName()
                                        + ": "
                                        + failure
                        )
        );

        return thread;
    }
}
```

## Configured executor

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class OrderExecutorFactory {

    public static ThreadPoolExecutor create() {
        return new ThreadPoolExecutor(
                8,
                16,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1_000),
                new NamedThreadFactory(
                        "order-worker"
                ),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

## Design decisions to explain

### Core and maximum sizes

```text
core = normal concurrency
maximum = controlled burst capacity
```

### Queue type

- `ArrayBlockingQueue` for strict bounded capacity
- `LinkedBlockingQueue` when optional capacity is suitable
- `SynchronousQueue` for direct task handoff
- Priority queue only when starvation behavior is understood

### Rejection policy

| Policy                | Behavior                                 |
| --------------------- | ---------------------------------------- |
| `AbortPolicy`         | Throws `RejectedExecutionException`      |
| `CallerRunsPolicy`    | Caller executes the task                 |
| `DiscardPolicy`       | Silently drops the task                  |
| `DiscardOldestPolicy` | Drops the oldest queued task and retries |

For business-critical work, silently discarding tasks is usually dangerous.

### Shutdown

```java
executor.shutdown();

try {
    if (!executor.awaitTermination(
            30,
            TimeUnit.SECONDS
    )) {
        executor.shutdownNow();
    }
} catch (InterruptedException exception) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

### Interview-ready answer

> I implement a custom production pool by configuring `ThreadPoolExecutor` with deliberate worker limits, a bounded queue, named threads, rejection behavior, metrics, and graceful shutdown. I avoid writing worker coordination from scratch because `ThreadPoolExecutor` already handles complex lifecycle and concurrency concerns.

---

# Scenario 7: The system experiences frequent deadlocks. How would you identify and fix them?

## Identification

Capture repeated dumps:

```bash
jcmd <pid> Thread.print -l
```

A deadlock may appear as:

```text
Thread A:
- owns Lock 1
- waits for Lock 2

Thread B:
- owns Lock 2
- waits for Lock 1
```

Programmatic detection:

```java
ThreadMXBean bean =
        ManagementFactory.getThreadMXBean();

long[] threadIds =
        bean.findDeadlockedThreads();

if (threadIds != null) {
    ThreadInfo[] details =
            bean.getThreadInfo(
                    threadIds,
                    true,
                    true
            );

    for (ThreadInfo detail : details) {
        System.err.println(detail);
    }
}
```

---

## Frequent causes

- Inconsistent lock ordering
- Nested locking
- Calling external code while holding a lock
- Holding database and Java locks simultaneously
- Lock upgrading
- Callbacks that acquire unknown locks
- Long critical sections
- Error paths that do not release explicit locks

---

## Fix 1: Global lock ordering

```java
Resource first =
        left.id() < right.id()
                ? left
                : right;

Resource second =
        first == left
                ? right
                : left;

first.lock().lock();

try {
    second.lock().lock();

    try {
        performOperation();
    } finally {
        second.lock().unlock();
    }
} finally {
    first.lock().unlock();
}
```

Every code path must follow the same order.

---

## Fix 2: Timed acquisition

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
    performOperation();
} finally {
    lock.unlock();
}
```

Retries should be bounded and use backoff.

---

## Fix 3: Reduce overlapping lock ownership

Instead of:

```text
Lock A
→ call another component
→ acquire Lock B
```

prefer:

```text
Lock A
→ copy required state
→ release Lock A
→ perform external operation
```

This is safe only when releasing the lock does not violate the business invariant.

---

## Fix 4: Redesign ownership

Use:

- One state owner
- Message passing
- Partitioned state
- Immutable snapshots
- Concurrent collections
- Database atomic operations

### Interview-ready answer

> I identify deadlocks with repeated thread dumps or `ThreadMXBean`, map each thread to the locks it owns and awaits, and locate the circular dependency. I fix them with consistent lock ordering, reduced nested locking, timed acquisition where appropriate, small lock scopes, and by avoiding callbacks or I/O while holding locks.

---

# Scenario 8: How would you handle thread starvation?

Starvation occurs when one thread or task repeatedly fails to obtain a required resource while other work continues.

## Common causes

- Unfair lock acquisition
- Long critical sections
- One workload monopolizing a shared executor
- Small pool filled by long-running tasks
- Nested waiting in the same executor
- Priority queue without aging
- Reader-heavy locking that delays writers
- Database connection starvation
- Excessive task retries
- CPU oversubscription

---

## Mitigation strategies

### Separate workloads

```text
HTTP I/O executor
CPU executor
scheduled-task executor
background-report executor
```

A slow report should not consume every request-processing worker.

---

### Reduce critical-section duration

Avoid:

```java
lock.lock();

try {
    updateState();
    databaseClient.save();
    remoteClient.notify();
} finally {
    lock.unlock();
}
```

Keep only the shared-state transition under the lock when correctness permits.

---

### Fair locks

```java
ReentrantLock lock =
        new ReentrantLock(true);
```

Fairness can reduce starvation among queued lock waiters, but may reduce throughput.

It does not solve:

- CPU starvation
- Executor starvation
- Database starvation
- Queue starvation

---

### Bound task execution

Use:

- Bounded queues
- Tenant quotas
- Admission control
- Timeouts
- Cancellation
- Priority aging
- Backpressure

---

### Avoid blocking dependencies in the same pool

Use composition:

```java
loadRequestAsync()
        .thenCompose(this::loadDataAsync)
        .thenApply(this::createResult);
```

instead of submitting nested work and blocking with `get()`.

### Interview-ready answer

> I first identify which resource is causing starvation: a lock, executor worker, CPU, connection, or priority queue. I then separate workloads, reduce lock-hold time, bound queues, remove nested blocking, add fairness only where needed, and monitor wait duration rather than simply increasing thread count.

---

# Scenario 9: How would you design a system with minimal locking?

The best lock is often the one made unnecessary by the state design.

## 1. Prefer immutable objects

```java
public record Configuration(
        int timeoutSeconds,
        int retryCount
) {
}
```

Immutable objects can be safely shared after proper publication.

---

## 2. Use thread confinement

```java
public Report buildReport(
        List<Order> orders
) {
    Map<String, Integer> localCounts =
            new HashMap<>();

    return calculate(
            orders,
            localCounts
    );
}
```

Local mutable state belongs to one invocation and does not require locking.

---

## 3. Use immutable snapshots

```java
private volatile Configuration configuration;
```

Replace the complete reference:

```java
configuration =
        new Configuration(10, 3);
```

Readers see either the old snapshot or the new snapshot.

---

## 4. Partition shared state

Instead of one global lock:

```text
One lock for every customer
```

use:

```text
Independent state per customer or partition
```

Operations on unrelated partitions can proceed concurrently.

---

## 5. Use atomic collection operations

Avoid:

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

---

## 6. Use message passing

```text
Many request threads
        ↓
Bounded queue
        ↓
One state-owning worker
```

Only one owner mutates the state, eliminating many shared-memory races.

---

## 7. Use atomics for independent values

```java
AtomicInteger activeRequests =
        new AtomicInteger();
```

Do not use several independent atomics when multiple fields must change together.

---

## 8. Use database atomicity for distributed state

A Java lock works only inside one JVM.

For shared database state:

```sql
UPDATE inventory
SET quantity = quantity - 1
WHERE product_id = :productId
  AND quantity > 0;
```

### Interview-ready answer

> I minimize locking by reducing shared mutation. I prefer immutable data, local state, immutable snapshots, partitioned ownership, atomic collection methods, message passing, and database atomic operations. When locking is still necessary, I protect the smallest complete invariant rather than the smallest number of lines.

---

# Scenario 10: How would you debug one stuck thread?

## Step 1: Identify the thread

Use:

- Thread name from logs
- Request correlation ID
- Executor thread naming
- Thread ID from monitoring
- JFR events
- Thread dump search

Good thread names make incidents easier to diagnose:

```text
inventory-worker-12
payment-callback-4
report-scheduler-1
```

---

## Step 2: Capture repeated stack traces

```bash
jcmd <pid> Thread.print -l
```

Find the same thread in multiple dumps.

Ask:

- Is the stack changing?
- Is it waiting on the same lock?
- Is it blocked in one remote call?
- Is it sleeping or retrying?
- Is another thread expected to signal it?
- Who owns the lock?
- Is the owner itself waiting?

---

## Step 3: Classify the wait

### Monitor wait

```text
BLOCKED
→ waiting for synchronized monitor
```

Find the owner.

### Lock or queue wait

```text
WAITING
→ LockSupport.park()
→ ReentrantLock or BlockingQueue
```

Inspect the surrounding stack frames.

### Remote I/O

```text
Socket read
→ check client timeout
→ inspect downstream service
```

### Future wait

```text
Future.get()
CompletableFuture.join()
```

Determine which task should complete the future and where it is scheduled.

### Latch or barrier wait

```text
CountDownLatch.await()
CyclicBarrier.await()
Phaser.arriveAndAwaitAdvance()
```

Check whether all expected participants can arrive.

---

## Step 4: Check for progress

A thread is not necessarily stuck merely because it has waited for several seconds.

Look for:

- Unchanged stack across repeated dumps
- Increasing wait time
- No completion metrics
- Queue growth
- No owner progress
- Expired business deadline

---

## Step 5: Fix the root cause

Depending on the evidence:

- Add remote-call timeout
- Add lock timeout
- Correct missing signal
- Replace custom coordination with `BlockingQueue`
- Fix lock ordering
- Split executors
- Increase downstream capacity
- Remove nested future waiting
- Cancel stale work
- Add deadline propagation

### Interview-ready answer

> I identify the thread by name or ID, capture several dumps, and inspect the exact resource it is waiting for. I trace the dependency to the lock owner, future producer, latch participant, queue, connection pool, or external service. I then verify whether progress occurs and fix the missing timeout, signal, capacity, or lock-ordering problem.

---

# Refined Introduction: 70 Java Multithreading Scenarios

If you can explain how common multithreading scenarios behave under contention, you can handle most Java backend concurrency interviews.

The important skill is not memorizing APIs. It is explaining what happens when several threads interact with the same state or resource.

A candidate may know that:

```java
volatile boolean ready;
```

provides visibility.

The deeper question is:

> What could another thread observe without `volatile`, and which happens-before relationship fixes it?

A candidate may know how to write:

```java
list.parallelStream()
        .forEach(this::process);
```

The deeper question is:

> Is `process()` stateless, does it mutate shared data, which pool executes it, and will parallel execution actually improve performance?

The scenarios that matter include:

- Race conditions and lost updates
- Visibility and the Java Memory Model
- Deadlock, livelock, and starvation
- `synchronized`, `ReentrantLock`, and read-write locks
- `wait()`, `notifyAll()`, and lost notifications
- Executor sizing, queueing, and rejection
- Thread-pool starvation
- Latches, barriers, semaphores, and phasers
- `CompletableFuture` composition and failure handling
- Parallel streams and safe reduction
- `ThreadLocal` cleanup
- Atomic variables and CAS
- Concurrent collections
- Production thread-dump analysis

> The interview is usually testing whether you can predict system behavior after two threads collide—not whether you can recite another concurrency definition.

# Short Interview Summary

> When a Java service becomes stuck, I capture repeated thread dumps and identify what threads are waiting for: monitors, explicit locks, futures, queues, connections, or external I/O. For exhausted pools, I inspect active workers, queue depth, task duration, and downstream limits before changing the size. I use bounded queues, workload-specific executors, explicit rejection policies, timeouts, and production metrics. I prevent deadlocks with consistent lock ordering and minimal overlapping lock ownership, and I reduce locking through immutability, confinement, partitioning, atomic operations, and message passing.
