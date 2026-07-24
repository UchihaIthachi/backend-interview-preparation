# Basic Questions

### 1. `ThreadPoolExecutor` is not the foundation of every executor

This statement is too broad:

> `ThreadPoolExecutor` is the backbone of all Executors.

A more accurate version is:

> `ThreadPoolExecutor` is the main configurable implementation for traditional platform-thread pools. Other executors may use different implementations, such as `ForkJoinPool` for work-stealing pools.

For example:

- `newFixedThreadPool()` → `ThreadPoolExecutor`
- `newCachedThreadPool()` → `ThreadPoolExecutor`
- `newSingleThreadExecutor()` → wrapped `ThreadPoolExecutor`
- `newWorkStealingPool()` → `ForkJoinPool`

---

### 2. `corePoolSize` is not always the minimum number of live threads

Replace:

```java
corePoolSize // minimum threads kept alive
```

with:

```java
corePoolSize // number of workers created before tasks are queued
```

Core workers are normally created on demand as tasks arrive. They can be started beforehand using:

```java
executor.prestartAllCoreThreads();
```

They may also be allowed to time out:

```java
executor.allowCoreThreadTimeOut(true);
```

---

### 3. `execute()` does not throw task exceptions directly to the submitting thread

Replace:

> `execute(Runnable)` throws unchecked exceptions directly.

with:

> `execute(Runnable)` returns no task handle. An uncaught task exception occurs in the worker thread and may reach that worker’s uncaught-exception handler.

```java
executor.execute(() -> {
    throw new IllegalStateException("Failed");
});
```

The caller of `execute()` does not receive that task exception through the method call.

By comparison, `submit()` captures the failure:

```java
Future<?> future = executor.submit(() -> {
    throw new IllegalStateException("Failed");
});

try {
    future.get();
} catch (ExecutionException exception) {
    Throwable taskFailure = exception.getCause();
}
```

---

### 4. `Future<?>` is not `Future<Void>`

This comment is inaccurate:

```java
Future<?> future = executor.submit(runnable); // Future<Void>
```

Use:

```java
Future<?> future = executor.submit(runnable);
```

A successful `Runnable` submission returns `null` from `get()`, but its static type is usually `Future<?>`.

A result can be supplied explicitly:

```java
Future<String> future =
        executor.submit(runnable, "completed");
```

---

### 5. `Future.get()` does not always defeat asynchronous execution

Replace:

> Blocking `get()` defeats the async purpose.

with:

> `get()` blocks the calling thread. Excessive or poorly placed blocking can reduce the advantages of asynchronous execution and may cause starvation when workers wait for tasks queued to the same executor.

Waiting at a controlled application boundary can be valid:

```java
Future<Report> future =
        executor.submit(this::generateReport);

Report report =
        future.get(5, TimeUnit.SECONDS);
```

---

### 6. `Future` does support exception retrieval, but not callback-based recovery

Replace:

> Future has no exception handling.

with:

> `Future` exposes task failures through `ExecutionException`, but it does not provide convenient callback, transformation, fallback, or composition methods.

```java
try {
    return future.get();
} catch (ExecutionException exception) {
    throw new TaskFailedException(
            exception.getCause()
    );
}
```

---

### 7. `shutdown()` does not wait by itself

Replace:

> `shutdown()` stops accepting tasks and waits for running tasks to finish.

with:

> `shutdown()` stops accepting new tasks and allows previously submitted tasks to finish, but the method returns immediately.

Use `awaitTermination()` to wait:

```java
executor.shutdown();

if (!executor.awaitTermination(
        30,
        TimeUnit.SECONDS
)) {
    executor.shutdownNow();
}
```

---

### 8. `shutdownNow()` cannot forcibly terminate tasks

Use this wording:

> `shutdownNow()` attempts to interrupt running tasks and returns queued tasks that never started. Tasks that ignore interruption may continue running.

```java
List<Runnable> neverStarted =
        executor.shutdownNow();
```

Java cancellation is cooperative.

---

### 9. Thread-pool sizing formulas are starting estimates

Avoid presenting this as an exact formula:

```text
threads =
target utilization × cores ×
(1 + wait time / compute time)
```

Use:

> For blocking workloads, this formula provides an initial estimate. The final limit must respect database connections, HTTP connection pools, downstream capacity, memory, rate limits, and measured queue latency.

For CPU-bound tasks:

```text
Starting point ≈ usable CPU cores
```

Not always:

```text
cores + 1
```

---

### 10. `newCachedThreadPool()` does not provide a safe upper bound

Its practical risk should be stated clearly:

```text
No idle worker
→ create another platform thread
→ sustained blocking load
→ potentially very large thread count
```

It should not be described as suitable for arbitrary “many short-lived tasks” unless submission volume and blocking behavior are externally controlled.

---

### 11. A fixed thread pool controls workers but not queued work

```java
ExecutorService executor =
        Executors.newFixedThreadPool(10);
```

This limits active worker threads to ten, but its queue is unbounded.

A production service commonly needs:

```java
ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
                10,
                20,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1_000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
```

This controls both:

- Active concurrency
- Waiting backlog

---

### 12. Work stealing is an implementation strategy, not strict scheduling

A good explanation is:

> Fork/join workers maintain local task queues. A worker generally processes its own tasks while idle workers steal available tasks from other workers. This helps balance uneven divide-and-conquer workloads.

Avoid relying on exact deque-end behavior as an application guarantee.

---

### 13. `CompletableFuture` is composable, but not automatically non-blocking

This statement needs nuance:

> `CompletableFuture` provides a rich non-blocking API.

Better:

> `CompletableFuture` supports completion-driven composition without requiring each stage to block, but the functions executed by its stages may still perform blocking work.

For example:

```java
CompletableFuture<User> future =
        CompletableFuture.supplyAsync(
                () -> blockingHttpClient.fetchUser(id),
                ioExecutor
        );
```

The composition is asynchronous, but the HTTP operation itself is blocking.

---

### 14. Avoid blocking dependent tasks inside the same small executor

Problematic:

```java
ExecutorService executor =
        Executors.newFixedThreadPool(2);

CompletableFuture<String> result =
        CompletableFuture.supplyAsync(() -> {
            CompletableFuture<String> nested =
                    CompletableFuture.supplyAsync(
                            this::loadData,
                            executor
                    );

            return nested.join();
        }, executor);
```

Prefer composition:

```java
CompletableFuture<String> result =
        CompletableFuture
                .supplyAsync(
                        this::loadRequest,
                        executor
                )
                .thenCompose(request ->
                        loadDataAsync(
                                request,
                                executor
                        )
                );
```

---

## Recommended Repository Placement

```text
04-concurrency/
├── executors/
│   ├── README.md
│   ├── basic-questions.md
│   ├── advanced-questions.md
│   ├── thread-pool-executor.md
│   ├── completable-future.md
│   └── fork-join-pool.md
├── locks/
│   ├── synchronized-vs-lock.md
│   ├── read-write-lock.md
│   └── stamped-lock.md
├── atomics/
│   ├── volatile.md
│   ├── atomic-classes.md
│   └── compare-and-swap.md
└── concurrency-problems/
    ├── race-condition.md
    ├── deadlock.md
    ├── livelock.md
    ├── starvation.md
    └── thread-safety.md
```

The first ten questions belong in:

```text
04-concurrency/executors/basic-questions.md
```
