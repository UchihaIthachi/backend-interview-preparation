# Basic Questions

## Question 1: What is the Executor framework? Why use it instead of raw threads?

The Executor framework (`java.util.concurrent`) separates task submission from thread management, enabling thread reuse, resource control, and richer lifecycle management.

- **Avoids thread creation overhead** – threads are reused from a pool.
- **Resource control** – cap the number of concurrent threads.
- **Task queue** – tasks queue up when all threads are busy.
- **Lifecycle management** – orderly shutdown, await termination.

## Question 2: Explain the different types of thread pools in Executors factory.

| Factory Method | Behavior | When to Use |
| :--- | :--- | :--- |
| `newFixedThreadPool(n)` | n threads, unbounded queue | Stable, predictable load |
| `newCachedThreadPool()` | Grows/shrinks, 60s idle TTL | Many short-lived tasks |
| `newSingleThreadExecutor()` | 1 thread, sequential | Ordered, serial tasks |
| `newScheduledThreadPool(n)` | Scheduled / periodic tasks | Cron-like scheduling |
| `newWorkStealingPool()` | ForkJoinPool, parallelism level | Parallel divide-and-conquer |

❌ **Common Mistakes**
- Using `newCachedThreadPool()` for long-running tasks – can create unlimited threads and cause OOM.
- Using `newFixedThreadPool` with an unbounded queue – can cause unbounded task accumulation.

## Question 3: How does ThreadPoolExecutor work internally?

`ThreadPoolExecutor` is the backbone of all Executors. Key constructor parameters:

```java
new ThreadPoolExecutor(
    corePoolSize,     // min threads kept alive
    maximumPoolSize,  // max threads allowed
    keepAliveTime,    // idle time before non-core thread dies
    unit,             // time unit
    workQueue,        // task queue
    threadFactory,    // how threads are created
    rejectionHandler  // what to do when full
)
```

**Execution flow:**
- If threads < corePoolSize → create new thread.
- If threads >= core → put in workQueue.
- If queue full and threads < max → create new thread.
- If queue full and threads = max → execute RejectionHandler.

**Rejection policies:**
- **AbortPolicy** (default) – throws `RejectedExecutionException`.
- **CallerRunsPolicy** – caller thread executes the task (slows producer naturally).
- **DiscardPolicy** – silently drops the task.
- **DiscardOldestPolicy** – drops oldest queued task, retries submission.

## Question 4: What is the difference between submit() and execute() in ExecutorService?

- `execute(Runnable)` – fire-and-forget; no return value; throws unchecked exception directly.
- `submit(Runnable/Callable)` – returns a Future; exceptions captured in `future.get()`; safer for error handling.

```java
Future<?> f1 = executor.submit(runnable); // Future<Void>
Future<Integer> f2 = executor.submit(callable);
executor.execute(runnable);               // no Future returned
```

## Question 5: What are the limitations of Future?

- **Blocking get()** – `future.get()` blocks the calling thread, defeating async purpose.
- **No chaining** – cannot chain multiple async operations easily.
- **No combine** – cannot combine results of multiple Futures without blocking.
- **No exception handling** – no callback on failure; exceptions wrapped in `ExecutionException`.
- **Cannot complete manually** – cannot set result from outside the computation.

## Question 6: Explain CompletableFuture and its key methods.

`CompletableFuture<T>` (Java 8+) implements both `Future<T>` and `CompletionStage<T>`, providing a rich non-blocking API for async composition.

**Creating**
```java
CompletableFuture.runAsync(() -> { /* no result */ });
CompletableFuture.supplyAsync(() -> fetchData(), executor);
```

**Transforming**
```java
cf.thenApply(result -> result.toUpperCase())   // sync transform
  .thenApplyAsync(s -> callApi(s), executor)   // async transform
  .thenAccept(s -> System.out.println(s))      // consume, no return
  .thenRun(() -> System.out.println("done"));  // no input/output
```

**Combining**
```java
cf1.thenCombine(cf2, (r1, r2) -> r1 + r2)  // combine two CFs
CompletableFuture.allOf(cf1, cf2, cf3)     // wait for ALL
CompletableFuture.anyOf(cf1, cf2, cf3)     // first to complete
```

**Error Handling**
```java
cf.exceptionally(ex -> defaultValue)
cf.handle((result, ex) -> ex != null ? fallback : result)
```

## Question 7: What is the ForkJoinPool and how does work stealing work?

`ForkJoinPool` (Java 7+) is optimized for recursive divide-and-conquer tasks. Each thread has its own deque; idle threads steal tasks from the tail of busy threads' deques.

```java
class SumTask extends RecursiveTask<Long> {
    protected Long compute() {
        if (size <= THRESHOLD) return computeDirectly();

        SumTask left = new SumTask(data, start, mid);
        left.fork(); // async execution

        SumTask right = new SumTask(data, mid, end);
        return right.compute() + left.join(); // join result
    }
}
```

## Question 8: What does shutdown() vs shutdownNow() do?

- `shutdown()`: stop accepting new tasks, wait for running ones to finish.
- `shutdownNow()`: attempt to interrupt running threads and return the list of pending tasks.

## Question 9: How should a thread pool be sized for CPU-bound vs I/O-bound work?

Sizing thread pools depends on whether work is CPU-bound or I/O-bound:
- **CPU-bound:** Size near the core count (e.g., `Runtime.getRuntime().availableProcessors() + 1`) to maximize CPU utilization without excessive context switching.
- **I/O-bound:** Size much higher, since threads spend time waiting. Can be approximated using Little's Law or the formula: `Target CPU Utilization * Core Count * (1 + Wait Time / Compute Time)`.

## Question 10: Why can `CompletableFuture` chains cause thread starvation, and how do you avoid it?

Starvation can occur if all threads in the common pool are blocked waiting for other stages to complete, or if nested asynchronous stages run out of available threads to execute. To avoid this, use a dedicated `ExecutorService` for distinct or heavily blocking operations rather than relying entirely on the default `ForkJoinPool.commonPool()`.
