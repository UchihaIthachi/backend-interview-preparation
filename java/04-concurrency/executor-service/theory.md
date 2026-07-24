# Basic Questions — Executors, Locks, Atomics, and Concurrency Problems

## Question 1: What is the Executor Framework? Why use it instead of raw threads?

The Executor Framework is part of `java.util.concurrent`. It separates **task submission** from **thread creation, scheduling, and lifecycle management**.

Instead of creating a thread directly:

```java
Thread thread = new Thread(this::processOrder);
thread.start();
```

submit the task to an executor:

```java
ExecutorService executor =
        Executors.newFixedThreadPool(4);

executor.submit(this::processOrder);
```

### Advantages

- **Thread reuse:** Worker threads can process multiple tasks.
- **Concurrency control:** Limits how many tasks run simultaneously.
- **Task queuing:** Tasks can wait when all workers are busy.
- **Result handling:** `Future` can represent a task result.
- **Cancellation:** Submitted tasks can be cancelled cooperatively.
- **Lifecycle management:** Supports orderly and immediate shutdown.
- **Scheduling:** Scheduled executors support delayed and periodic tasks.
- **Overload handling:** `ThreadPoolExecutor` supports rejection policies.

### When are raw threads reasonable?

Raw threads may still be appropriate for:

- A dedicated, long-lived application thread
- Small educational examples
- Special thread configuration not represented by an executor

For most application task execution, an executor provides safer resource and lifecycle management.

### Interview-ready answer

> The Executor Framework separates what work should run from how threads are created and managed. It supports thread reuse, bounded concurrency, task queuing, results, cancellation, overload handling, and graceful shutdown, making it more scalable and maintainable than creating a raw thread for every task.

---

## Question 2: Explain the thread pools provided by `Executors`

| Factory method                      | General behavior                                    | Typical use                                                     |
| ----------------------------------- | --------------------------------------------------- | --------------------------------------------------------------- |
| `newFixedThreadPool(n)`             | Fixed workers with an unbounded queue               | Controlled worker count when task submission is already bounded |
| `newCachedThreadPool()`             | Reuses idle threads and can create many new threads | Short asynchronous tasks under externally bounded load          |
| `newSingleThreadExecutor()`         | One worker and sequential execution                 | Ordered task processing                                         |
| `newScheduledThreadPool(n)`         | Supports delayed and periodic execution             | Maintenance, polling, delayed jobs                              |
| `newWorkStealingPool()`             | Fork/join pool using work stealing                  | CPU-oriented independent or divide-and-conquer tasks            |
| `newVirtualThreadPerTaskExecutor()` | One virtual thread per submitted task               | High-concurrency blocking workloads on modern Java              |

---

### `newFixedThreadPool(n)`

```java
ExecutorService executor =
        Executors.newFixedThreadPool(10);
```

It uses:

- Exactly `n` platform worker threads
- An effectively unbounded `LinkedBlockingQueue`

Risk:

```text
Tasks arrive faster than they complete
        ↓
Queue grows continuously
        ↓
Increasing latency and memory usage
```

A fixed worker count does not imply a bounded system because its queue is unbounded.

---

### `newCachedThreadPool()`

```java
ExecutorService executor =
        Executors.newCachedThreadPool();
```

It:

- Reuses available idle threads
- Creates another platform thread when no idle worker is available
- Removes idle workers after the keep-alive period
- Uses a `SynchronousQueue`, which does not retain tasks

It can create a very large number of threads under sustained blocking load.

Possible consequences include:

- High thread-stack memory use
- Context switching
- Connection-pool exhaustion
- Downstream overload
- Poor latency

---

### `newSingleThreadExecutor()`

```java
ExecutorService executor =
        Executors.newSingleThreadExecutor();
```

It guarantees that tasks execute sequentially.

Useful for:

- Ordered event processing
- Serial file updates
- Single-owner state mutation

A slow or stuck task delays every task behind it, and the queue is unbounded.

---

### `newScheduledThreadPool(n)`

```java
ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(2);
```

Delayed execution:

```java
scheduler.schedule(
        this::refreshCache,
        10,
        TimeUnit.SECONDS
);
```

Periodic execution:

```java
scheduler.scheduleAtFixedRate(
        this::collectMetrics,
        0,
        30,
        TimeUnit.SECONDS
);
```

Use `ScheduledExecutorService` rather than manually combining `Thread.sleep()` with loops.

---

### `newWorkStealingPool()`

```java
ExecutorService executor =
        Executors.newWorkStealingPool();
```

It creates a `ForkJoinPool` with work-stealing behavior.

It is appropriate for independent CPU-oriented tasks that can be efficiently divided. Execution order is not guaranteed.

---

### Production recommendation

For important production workloads, explicit configuration often provides better control:

```java
ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
                8,
                16,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1_000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
```

This allows explicit control over:

- Worker limits
- Queue capacity
- Backpressure
- Rejection policy
- Thread creation
- Metrics

---

## Question 3: How does `ThreadPoolExecutor` work internally?

A `ThreadPoolExecutor` is configured using:

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

### Parameters

| Parameter          | Meaning                                         |
| ------------------ | ----------------------------------------------- |
| `corePoolSize`     | Normal number of workers maintained by the pool |
| `maximumPoolSize`  | Maximum number of platform workers              |
| `keepAliveTime`    | How long excess idle workers remain alive       |
| `workQueue`        | Stores tasks waiting to execute                 |
| `threadFactory`    | Creates and configures worker threads           |
| `rejectionHandler` | Handles tasks that cannot be accepted           |

### Simplified submission flow

```text
Task submitted
      ↓
workerCount < corePoolSize?
      ├── Yes → create a core worker for the task
      └── No
            ↓
Can the queue accept the task?
      ├── Yes → enqueue the task
      └── No
            ↓
workerCount < maximumPoolSize?
      ├── Yes → create a non-core worker
      └── No → reject the task
```

### Important consequence of an unbounded queue

```java
new ThreadPoolExecutor(
        10,
        500,
        60,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>()
);
```

After ten core workers become busy, new tasks are normally queued. Since the queue does not fill, the executor usually does not create workers toward the maximum of 500.

Therefore, `maximumPoolSize` may have no practical effect with an unbounded queue.

---

### Rejection policies

#### `AbortPolicy`

```java
new ThreadPoolExecutor.AbortPolicy()
```

Throws `RejectedExecutionException`. This is the default.

#### `CallerRunsPolicy`

```java
new ThreadPoolExecutor.CallerRunsPolicy()
```

The submitting thread runs the task, slowing task submission and providing basic backpressure.

#### `DiscardPolicy`

```java
new ThreadPoolExecutor.DiscardPolicy()
```

Silently discards the task. Use only when losing work is explicitly acceptable and monitored.

#### `DiscardOldestPolicy`

```java
new ThreadPoolExecutor.DiscardOldestPolicy()
```

Removes an older queued task and retries the new submission. This can silently lose work and violate ordering.

### Interview-ready answer

> `ThreadPoolExecutor` creates workers until the core size is reached, then queues tasks. If the queue is full, it creates workers up to the maximum size. When both the queue and maximum worker count are exhausted, it invokes the configured rejection policy.

---

## Question 4: What is the difference between `submit()` and `execute()`?

| `execute()`                                          | `submit()`                               |
| ---------------------------------------------------- | ---------------------------------------- |
| Accepts a `Runnable`                                 | Accepts `Runnable` or `Callable<T>`      |
| Returns `void`                                       | Returns `Future<T>`                      |
| No returned cancellation handle                      | Future supports cancellation             |
| No returned completion handle                        | `Future.get()` waits for completion      |
| Task exception may reach uncaught-exception handling | Task exception is captured by the Future |

### `execute()`

```java
executor.execute(() ->
        processMessage()
);
```

It does not return a handle for waiting, cancellation, or retrieving an exception.

An uncaught exception occurs in the worker thread, not directly in the thread that called `execute()`.

---

### `submit()` with `Callable`

```java
Future<Integer> future =
        executor.submit(() -> {
            Thread.sleep(100);
            return 42;
        });
```

```java
try {
    Integer result = future.get();
} catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
} catch (ExecutionException exception) {
    Throwable taskFailure = exception.getCause();
}
```

### `submit()` with `Runnable`

```java
Future<?> future =
        executor.submit(this::sendNotification);
```

A successfully completed `Runnable` normally produces `null` through `get()`.

### Common mistake

```java
executor.submit(() -> {
    throw new IllegalStateException("Failed");
});
```

When the returned `Future` is ignored, the exception may appear to disappear because it is captured inside the future.

### Interview-ready answer

> `execute()` is suitable when no result or task handle is required. `submit()` returns a `Future`, supports `Callable`, cancellation, waiting, and exception retrieval. Exceptions from submitted tasks are exposed through `Future.get()` as `ExecutionException`.

---

## Question 5: What are the limitations of `Future`?

`Future<T>` represents the eventual result of an asynchronous computation.

```java
Future<User> future =
        executor.submit(this::loadUser);
```

### Limitations

#### 1. `get()` is blocking

```java
User user = future.get();
```

The calling thread waits until the task finishes.

Blocking is not always wrong, but it limits composability and can cause pool starvation if used inside the wrong executor.

#### 2. No transformation API

A plain `Future` cannot directly express:

```text
Load user
→ extract email
→ send notification
```

The caller must manually wait and submit more work.

#### 3. No convenient combination

Combining several futures usually requires manual blocking:

```java
Result result =
        combine(first.get(), second.get());
```

#### 4. Limited error composition

Exceptions are wrapped in `ExecutionException`, but `Future` provides no built-in callback or fallback pipeline.

#### 5. No manual completion

A normal `Future` cannot be completed externally with a value or error.

#### 6. No completion callback

There is no built-in `thenApply()`, `thenAccept()`, or similar callback API.

These limitations motivated `CompletableFuture`.

---

## Question 6: Explain `CompletableFuture` and its important methods

`CompletableFuture<T>` implements:

- `Future<T>`
- `CompletionStage<T>`

It supports asynchronous execution, transformation, composition, combination, manual completion, and error handling.

---

### Creating futures

No result:

```java
CompletableFuture<Void> future =
        CompletableFuture.runAsync(
                this::refreshCache,
                executor
        );
```

Produces a result:

```java
CompletableFuture<User> future =
        CompletableFuture.supplyAsync(
                () -> fetchUser(userId),
                executor
        );
```

Already completed:

```java
CompletableFuture<String> future =
        CompletableFuture.completedFuture("ready");
```

Manual completion:

```java
CompletableFuture<String> future =
        new CompletableFuture<>();

future.complete("success");
```

---

### Transforming results

#### `thenApply()`

Transforms a result synchronously relative to stage completion:

```java
CompletableFuture<String> name =
        userFuture.thenApply(User::getName);
```

#### `thenApplyAsync()`

Schedules the transformation asynchronously:

```java
CompletableFuture<String> name =
        userFuture.thenApplyAsync(
                User::getName,
                executor
        );
```

---

### Consuming results

#### `thenAccept()`

```java
future.thenAccept(
        value -> System.out.println(value)
);
```

Consumes the value and returns `CompletableFuture<Void>`.

#### `thenRun()`

```java
future.thenRun(
        () -> System.out.println("Completed")
);
```

Receives neither the preceding value nor a return value.

---

### Dependent asynchronous operations

Use `thenCompose()` when the callback already returns a future.

```java
CompletableFuture<Payment> payment =
        orderFuture.thenCompose(
                order -> paymentService.chargeAsync(order)
        );
```

Without `thenCompose()`, the result would be nested:

```java
CompletableFuture<CompletableFuture<Payment>>
```

---

### Combining independent futures

```java
CompletableFuture<OrderSummary> summary =
        priceFuture.thenCombine(
                shippingFuture,
                OrderSummary::new
        );
```

---

### Wait for all futures

```java
CompletableFuture<Void> all =
        CompletableFuture.allOf(
                first,
                second,
                third
        );
```

`allOf()` returns `CompletableFuture<Void>`, so individual results must still be obtained from the original futures.

---

### First future to complete

```java
CompletableFuture<Object> first =
        CompletableFuture.anyOf(
                primary,
                mirror
        );
```

It completes when any supplied future completes, including exceptional completion.

---

### Error handling

#### `exceptionally()`

Runs only after exceptional completion and returns a fallback value:

```java
CompletableFuture<User> recovered =
        future.exceptionally(
                exception -> defaultUser()
        );
```

#### `handle()`

Runs for both success and failure:

```java
CompletableFuture<User> handled =
        future.handle((user, failure) ->
                failure == null
                        ? user
                        : defaultUser()
        );
```

#### `whenComplete()`

Observes the outcome without normally transforming it:

```java
future.whenComplete((result, failure) -> {
    if (failure != null) {
        log.error("Operation failed", failure);
    }
});
```

---

### Timeout support

```java
future.orTimeout(
        5,
        TimeUnit.SECONDS
);
```

Fallback after a delay:

```java
future.completeOnTimeout(
        defaultValue,
        5,
        TimeUnit.SECONDS
);
```

### Interview-ready answer

> `CompletableFuture` extends the Future model with non-blocking composition. I use `thenApply` for transformations, `thenCompose` for dependent asynchronous calls, `thenCombine` for independent results, `allOf` for synchronization, and `exceptionally`, `handle`, or `whenComplete` for failures.

---

## Question 7: What is `ForkJoinPool`, and how does work stealing operate?

`ForkJoinPool` is designed for tasks that can be recursively divided into smaller independent subtasks.

Typical classes include:

- `RecursiveTask<V>` — returns a result
- `RecursiveAction` — returns no result

### Example

```java
public final class SumTask
        extends RecursiveTask<Long> {

    private static final int THRESHOLD = 10_000;

    private final long[] values;
    private final int start;
    private final int end;

    public SumTask(
            long[] values,
            int start,
            int end
    ) {
        this.values = values;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {
        int length = end - start;

        if (length <= THRESHOLD) {
            long total = 0;

            for (int index = start;
                 index < end;
                 index++) {

                total += values[index];
            }

            return total;
        }

        int middle = start + length / 2;

        SumTask left =
                new SumTask(values, start, middle);

        SumTask right =
                new SumTask(values, middle, end);

        left.fork();
        long rightResult = right.compute();
        long leftResult = left.join();

        return leftResult + rightResult;
    }
}
```

### Work stealing

Each worker broadly maintains a deque of tasks.

- A worker normally processes its own recently created tasks.
- An idle worker steals older tasks from another worker’s deque.
- This redistributes work without one heavily contended central queue.

```text
Worker A deque: [task1, task2, task3]
Worker B deque: []

Worker B steals task1
Worker A continues with task3
```

### Suitable workloads

- Recursive array processing
- Tree traversal
- Divide-and-conquer algorithms
- CPU-bound independent transformations

### Poor workloads

- Long blocking database calls
- Remote API calls without managed blocking
- Tasks with heavy shared-state contention
- Tasks too small to justify splitting

### Interview-ready answer

> `ForkJoinPool` is optimized for recursive divide-and-conquer work. Workers keep local deques and idle workers steal tasks from busy workers, helping balance uneven workloads. It is best suited to CPU-bound tasks that split naturally.

---

## Question 8: What is the difference between `shutdown()` and `shutdownNow()`?

## `shutdown()`

```java
executor.shutdown();
```

It:

- Stops accepting new tasks.
- Allows already submitted tasks to execute.
- Returns immediately.
- Does not itself wait for termination.

Wait explicitly:

```java
executor.awaitTermination(
        30,
        TimeUnit.SECONDS
);
```

---

## `shutdownNow()`

```java
List<Runnable> neverStarted =
        executor.shutdownNow();
```

It:

- Stops accepting new tasks.
- Attempts to interrupt running tasks.
- Removes queued tasks that have not started.
- Returns those unstarted tasks.
- Does not forcibly terminate tasks that ignore interruption.

### Graceful shutdown pattern

```java
executor.shutdown();

try {
    if (!executor.awaitTermination(
            30,
            TimeUnit.SECONDS
    )) {
        executor.shutdownNow();

        if (!executor.awaitTermination(
                30,
                TimeUnit.SECONDS
        )) {
            System.err.println(
                    "Executor did not terminate"
            );
        }
    }
} catch (InterruptedException exception) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

### Interview-ready answer

> `shutdown()` initiates an orderly shutdown in which submitted tasks can finish. `shutdownNow()` attempts to interrupt running tasks and returns tasks that never started. Neither method guarantees that an uncooperative task will stop immediately.

---

## Question 9: How should a pool be sized for CPU-bound and I/O-bound work?

## CPU-bound workload

CPU-bound tasks spend most of their time computing.

Examples:

- Compression
- Encryption
- Image transformation
- Parsing
- Mathematical calculations

A reasonable starting point is:

```text
threads ≈ usable processor cores
```

```java
int processors =
        Runtime.getRuntime()
                .availableProcessors();
```

Sometimes `cores + 1` is used when occasional pauses occur, but it is not a universal rule.

Too many runnable workers cause:

- Context switching
- CPU-cache disruption
- Scheduling overhead
- Lower throughput

---

## I/O-bound workload

I/O-bound tasks spend much of their time waiting for:

- Databases
- Network responses
- Files
- Locks
- External services

A starting estimate is:

```text
threads ≈ cores × target utilization ×
          (1 + wait time / compute time)
```

Example:

```text
cores             = 8
target utilization = 0.9
wait time          = 90 ms
compute time       = 10 ms

threads ≈ 8 × 0.9 × (1 + 90 / 10)
        ≈ 72
```

This is only an estimate.

Actual concurrency must also respect:

- Database connection count
- HTTP connection-pool size
- External-service concurrency
- Rate limits
- Memory
- Queue latency
- File-descriptor limits

### Virtual threads

For large numbers of blocking tasks, virtual threads can reduce the cost of having one thread per task. They do not remove downstream limits.

```text
10,000 virtual threads
do not create
10,000 database connections
```

### Interview-ready answer

> For CPU-bound work, I start near the number of usable cores. For blocking I/O, I estimate from the wait-to-compute ratio, but I cap concurrency according to downstream resources. The final size should be validated with queue delay, throughput, CPU, latency, and saturation metrics.

---

## Question 10: Why can `CompletableFuture` chains cause thread starvation?

Starvation can occur when every available worker is blocked waiting for work that must run on the same executor.

```java
ExecutorService executor =
        Executors.newFixedThreadPool(2);

CompletableFuture<String> first =
        CompletableFuture.supplyAsync(() -> {
            CompletableFuture<String> nested =
                    CompletableFuture.supplyAsync(
                            this::loadValue,
                            executor
                    );

            return nested.join();
        }, executor);
```

If both workers execute an outer task and wait for nested tasks, the nested tasks remain queued forever.

```text
Worker 1 waits for nested task 1
Worker 2 waits for nested task 2
Nested tasks wait in executor queue
No worker remains available
```

This is **thread-pool starvation deadlock**.

### Other cause: blocking the common pool

Without an explicit executor:

```java
CompletableFuture.supplyAsync(
        remoteClient::call
);
```

the task generally uses `ForkJoinPool.commonPool()`. Long blocking operations can occupy common-pool workers and delay unrelated tasks.

### Prevention

- Avoid blocking calls such as `get()` or `join()` inside asynchronous stages.
- Use `thenCompose()` rather than manually waiting for another future.
- Use explicit executors for blocking workloads.
- Separate CPU-bound and I/O-bound work.
- Use timeouts.
- Avoid submitting dependent work to the same saturated bounded pool.
- Consider virtual threads for suitable blocking task-per-thread workflows.

### Interview-ready answer

> Starvation occurs when executor workers block waiting for dependent tasks that are queued to the same executor, leaving no free worker to execute them. I avoid blocking inside completion stages, use `thenCompose`, separate workloads, provide explicit executors, and add timeouts.

---

# Locks, Visibility, Atomics, and Concurrency Problems

## Question 15: What is `ReentrantLock`?

`ReentrantLock` is an explicit mutual-exclusion lock from `java.util.concurrent.locks`.

```java
private final ReentrantLock lock =
        new ReentrantLock();

public void update() {
    lock.lock();

    try {
        modifySharedState();
    } finally {
        lock.unlock();
    }
}
```

### Why is it called reentrant?

A thread that already owns the lock can acquire it again.

```java
public void outer() {
    lock.lock();

    try {
        inner();
    } finally {
        lock.unlock();
    }
}

private void inner() {
    lock.lock();

    try {
        performWork();
    } finally {
        lock.unlock();
    }
}
```

The thread must release the lock the same number of times it acquired it.

### Additional capabilities

- `tryLock()`
- Timed acquisition
- Interruptible acquisition
- Optional fairness
- Multiple `Condition` objects
- Lock-state inspection

```java
if (lock.tryLock()) {
    try {
        performWork();
    } finally {
        lock.unlock();
    }
}
```

### Critical rule

Every successful acquisition must be paired with `unlock()` in `finally`.

---

## Question 16: `synchronized` vs `ReentrantLock`

| `synchronized`                               | `ReentrantLock`                       |
| -------------------------------------------- | ------------------------------------- |
| Language keyword                             | Library class                         |
| Lock released automatically when block exits | Must call `unlock()` explicitly       |
| Simple and readable                          | More configurable                     |
| No timed acquisition                         | Supports timed `tryLock()`            |
| No interruptible monitor acquisition         | Supports `lockInterruptibly()`        |
| One monitor wait set                         | Supports multiple `Condition` objects |
| No fairness option                           | Optional fair policy                  |
| JVM manages monitor                          | Application manages lock lifecycle    |

### `synchronized`

```java
synchronized (lockObject) {
    updateSharedState();
}
```

It is often preferable when straightforward mutual exclusion is sufficient.

### `ReentrantLock`

```java
lock.lockInterruptibly();

try {
    updateSharedState();
} finally {
    lock.unlock();
}
```

Use it when the design requires:

- Timeout
- Cancellation while waiting
- Fairness
- Multiple conditions
- Conditional acquisition

### Interview-ready answer

> `synchronized` is simpler and automatically releases its monitor when the block exits. `ReentrantLock` requires explicit unlocking but supports timed, interruptible and fair acquisition and multiple conditions. I use the simplest mechanism that satisfies the coordination requirement.

---

## Question 17: What is `ReadWriteLock`?

A `ReadWriteLock` separates access into:

- A shared read lock
- An exclusive write lock

```java
private final ReadWriteLock lock =
        new ReentrantReadWriteLock();

public Value read() {
    lock.readLock().lock();

    try {
        return value;
    } finally {
        lock.readLock().unlock();
    }
}

public void update(Value newValue) {
    lock.writeLock().lock();

    try {
        value = newValue;
    } finally {
        lock.writeLock().unlock();
    }
}
```

### Behavior

- Multiple readers may hold the read lock concurrently.
- Only one writer may hold the write lock.
- A writer excludes both readers and other writers.

### Suitable workload

- Reads are frequent.
- Writes are relatively rare.
- Read operations take enough time to justify the extra lock complexity.

It may perform worse than a simple lock for short critical sections or frequent writes.

### Lock upgrade warning

Attempting to acquire the write lock while retaining a read lock can cause deadlock or indefinite waiting.

---

## Question 18: What is `StampedLock`?

`StampedLock` supports three access modes:

- Write locking
- Read locking
- Optimistic reading

```java
private final StampedLock lock =
        new StampedLock();

private double x;
private double y;
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

### Important characteristics

- Optimistic reads initially avoid locking.
- Validation detects whether a write occurred.
- It is not reentrant.
- Lock ownership is represented by a stamp.
- It does not directly provide `Condition`.
- Incorrect stamp handling can cause failures.
- It is more complex than `ReentrantReadWriteLock`.

Use it only when read-heavy performance benefits justify the complexity.

---

## Question 19: What does the `volatile` keyword do?

A volatile field provides:

- Visibility between threads
- Ordering guarantees around volatile reads and writes
- A happens-before relationship between a write and subsequent reads of that variable

```java
private volatile boolean running = true;
```

Thread one:

```java
running = false;
```

Thread two:

```java
while (running) {
    performWork();
}
```

The second thread can observe the update without acquiring a lock.

### What `volatile` does not provide

It does not make compound operations atomic:

```java
private volatile int count;

count++;
```

`count++` consists of:

```text
read
→ calculate
→ write
```

Multiple threads can lose updates.

### Good uses

- Stop flags
- Published immutable snapshots
- Configuration references
- State indicators with independent reads and writes

---

## Question 20: `volatile` vs `synchronized`

| `volatile`                            | `synchronized`                            |
| ------------------------------------- | ----------------------------------------- |
| Visibility and ordering               | Visibility, ordering and mutual exclusion |
| Does not lock                         | Uses a monitor                            |
| Suitable for independent reads/writes | Suitable for compound invariants          |
| Does not make `count++` atomic        | Can make a critical section atomic        |
| Low coordination overhead             | May block under contention                |

### `volatile`

```java
private volatile boolean active;
```

Use when one write or read is itself the complete state transition.

### `synchronized`

```java
public synchronized void transfer(
        Account source,
        Account destination,
        long amount
) {
    source.withdraw(amount);
    destination.deposit(amount);
}
```

Use when multiple reads, writes or fields must be updated as one logical operation.

### Interview-ready answer

> `volatile` provides visibility and ordering but no mutual exclusion. `synchronized` provides both visibility and exclusive execution, so it is suitable for compound operations and multi-field invariants.

---

## Question 21: What is `AtomicInteger`?

`AtomicInteger` supports thread-safe atomic operations on an integer value.

```java
private final AtomicInteger counter =
        new AtomicInteger();

public int increment() {
    return counter.incrementAndGet();
}
```

Common operations include:

```java
counter.get();
counter.set(10);
counter.incrementAndGet();
counter.getAndIncrement();
counter.addAndGet(5);
counter.compareAndSet(expected, update);
counter.updateAndGet(value -> value * 2);
```

It commonly uses compare-and-set operations rather than an application-level lock.

### Suitable uses

- Sequence numbers
- Independent counters
- State transitions
- Atomic flags represented as integers
- Simple lock-free updates

For highly contended statistics that do not require an immediate exact value after each update, `LongAdder` may scale better.

---

## Question 22: Atomic classes vs `synchronized`

| Atomic classes                         | `synchronized`                       |
| -------------------------------------- | ------------------------------------ |
| Best for one independent value         | Best for several related values      |
| Usually based on CAS                   | Monitor-based mutual exclusion       |
| No blocking on uncontended retry       | Threads may block                    |
| Limited to supported atomic operations | Supports arbitrary critical sections |
| Can require retry loops                | Easier for complex invariants        |

### Atomic counter

```java
AtomicInteger counter =
        new AtomicInteger();

counter.incrementAndGet();
```

### Multi-field invariant

```java
synchronized (account) {
    account.balance -= amount;
    account.version++;
    account.lastUpdated = Instant.now();
}
```

Multiple atomic variables do not automatically make a multi-variable workflow atomic.

### Interview-ready answer

> Atomic classes are effective for independent variables and simple read-modify-write operations. A lock is preferable when several fields or operations must remain consistent as one transaction.

---

## Question 23: What is CAS—Compare and Swap?

CAS is an atomic conditional update operation.

Conceptually:

```text
if current value == expected value:
    replace it with new value
    return success
else:
    do not modify it
    return failure
```

Java example:

```java
AtomicInteger value =
        new AtomicInteger(10);

boolean updated =
        value.compareAndSet(10, 11);
```

CAS is commonly used in retry loops:

```java
int previous;
int updated;

do {
    previous = value.get();
    updated = previous + 1;
} while (!value.compareAndSet(
        previous,
        updated
));
```

### Advantages

- Avoids explicit lock acquisition for simple operations
- Can provide good scalability under low or moderate contention
- Forms the basis of many concurrent structures

### Limitations

- Repeated failures can waste CPU.
- High contention can cause many retries.
- Complex multi-field invariants remain difficult.
- It can experience the ABA problem.

### ABA problem

```text
Value changes A → B → A
```

A CAS operation checking only the current value sees `A` and may not know that it changed in between.

Versioned or stamped references can help when that history matters.

---

## Question 24: What is a race condition?

A race condition occurs when the correctness of a result depends on unpredictable thread interleaving.

```java
private int count;

public void increment() {
    count++;
}
```

Two threads can both read the same old value and overwrite one another.

```text
Thread A reads 10
Thread B reads 10
Thread A writes 11
Thread B writes 11
```

Expected value: `12`
Actual value: `11`

### Prevention techniques

- Immutability
- Thread confinement
- Synchronization
- Explicit locks
- Atomic classes
- Concurrent collections
- Message passing
- Partitioned ownership

A race condition is not guaranteed to fail every time, which makes it difficult to reproduce.

---

## Question 25: What is deadlock?

Deadlock occurs when threads wait indefinitely for resources held by one another.

```text
Thread A owns Lock 1 and waits for Lock 2
Thread B owns Lock 2 and waits for Lock 1
```

Example:

```java
// Thread A path
synchronized (first) {
    synchronized (second) {
        performWork();
    }
}

// Thread B path
synchronized (second) {
    synchronized (first) {
        performWork();
    }
}
```

### Common deadlock conditions

- Mutual exclusion
- Hold and wait
- No forced preemption
- Circular waiting

Deadlock prevention usually removes at least one of these conditions.

---

## Question 26: How do you detect deadlocks?

### Thread dumps

```bash
jcmd <pid> Thread.print -l
```

or:

```bash
jstack -l <pid>
```

A JVM thread dump may explicitly report detected deadlocks.

Capture several dumps to distinguish permanent deadlock from temporary contention.

---

### Programmatic detection

```java
ThreadMXBean threadMxBean =
        ManagementFactory.getThreadMXBean();

long[] threadIds =
        threadMxBean.findDeadlockedThreads();

if (threadIds != null) {
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
```

### Other tools

- Java Flight Recorder
- Java Mission Control
- Lock-contention profilers
- Application watchdogs
- Thread-dump monitoring

### Investigation steps

1. Identify waiting threads.
2. Identify each owned and requested lock.
3. Find the circular dependency.
4. Map stack frames to application code.
5. Compare lock acquisition order across code paths.

---

## Question 27: How do you prevent deadlocks?

### 1. Consistent lock ordering

```java
Resource first =
        resourceA.id() < resourceB.id()
                ? resourceA
                : resourceB;

Resource second =
        first == resourceA
                ? resourceB
                : resourceA;

synchronized (first) {
    synchronized (second) {
        performOperation();
    }
}
```

All threads acquire resources in the same global order.

### 2. Avoid nested locks

Reduce the number of locks held simultaneously.

### 3. Keep critical sections small

Do not perform slow database, network or file operations while holding locks unless correctness requires it.

### 4. Use timed acquisition

```java
if (firstLock.tryLock(
        500,
        TimeUnit.MILLISECONDS
)) {
    try {
        // Attempt second lock
    } finally {
        firstLock.unlock();
    }
}
```

### 5. Avoid calling external code while locked

Callbacks or extension code may acquire unknown locks.

### 6. Use higher-level concurrency constructs

Examples:

- `BlockingQueue`
- Concurrent collections
- Message passing
- Actor or single-owner designs
- Database transactions

### Interview-ready answer

> I prevent deadlocks primarily with consistent resource ordering, minimal lock scope, fewer nested locks, and avoiding slow or unknown code inside critical sections. Timed `tryLock()` can provide recovery when acquisition failure is acceptable.

---

## Question 28: What is livelock?

In livelock, threads are not blocked. They remain active and continually react to one another but make no useful progress.

Example:

```text
Thread A detects conflict and backs off.
Thread B detects conflict and backs off.
Both retry at the same time.
Both conflict again.
```

### Deadlock vs livelock

| Deadlock                         | Livelock                                   |
| -------------------------------- | ------------------------------------------ |
| Threads wait without progressing | Threads actively retry without progressing |
| Often blocked or waiting         | Often runnable or repeatedly sleeping      |
| Circular resource dependency     | Repeated reaction or retry symmetry        |

### Prevention

- Randomized backoff
- Exponential backoff with jitter
- Bounded retries
- Ownership rules
- Priority or ordering
- Central coordination

---

## Question 29: What is starvation?

Starvation occurs when one thread repeatedly fails to receive the CPU, lock, permit or other resource it needs while other threads continue progressing.

Possible causes include:

- Unfair lock acquisition
- Long-running critical sections
- Higher-priority work continually arriving
- Reader preference indefinitely delaying writers
- Thread-pool saturation
- One task type monopolizing workers
- Priority queues without aging

### Mitigation

- Fair `ReentrantLock` or semaphore where appropriate
- Shorter critical sections
- Separate executors for different workloads
- Queue aging or priority adjustment
- Bounded tasks
- Lock partitioning
- Monitoring wait-time distribution

A fair lock reduces some lock starvation risks but does not guarantee complete scheduling fairness.

---

## Question 30: What is thread safety?

Code is thread-safe when it remains correct when accessed concurrently by multiple threads under every valid thread interleaving.

Thread safety includes:

- No data races
- Preserved invariants
- Safe publication
- Required visibility
- Atomic state transitions
- Correct coordination
- Progress properties appropriate to the design

### Common techniques

#### Immutability

```java
public record Money(
        BigDecimal amount,
        Currency currency
) {
}
```

#### Thread confinement

A mutable object is used by only one thread.

#### Synchronization

```java
public synchronized void update() {
    modifySharedState();
}
```

#### Atomic variables

```java
AtomicInteger counter =
        new AtomicInteger();
```

#### Concurrent collections

```java
ConcurrentMap<String, User> users =
        new ConcurrentHashMap<>();
```

#### Stateless services

```java
public Result calculate(Input input) {
    return new Result(input.value() * 2);
}
```

### Important nuance

Using a thread-safe collection does not automatically make a multi-step workflow thread-safe.

```java
if (!map.containsKey(key)) {
    map.put(key, value);
}
```

Use an atomic operation:

```java
map.putIfAbsent(key, value);
```

### Interview-ready answer

> Thread safety means concurrent use cannot violate the component’s correctness or invariants. I achieve it through immutability, confinement, synchronization, atomic operations, concurrent collections, and carefully defined ownership of mutable state.
