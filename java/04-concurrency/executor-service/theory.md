# ExecutorService: Managing Tasks, Not Threads

## 1. Definition

`ExecutorService` is a higher-level API over raw `Thread` management — you submit
`Runnable`/`Callable` tasks to a pool, and the pool decides how/when to run them.

## 2. Why it exists

Manually creating a `new Thread()` per task doesn't scale (thread creation is
expensive, and unbounded thread creation can exhaust system resources).
Executors reuse a fixed or bounded pool of worker threads across many tasks.

## 3. How it works

```text
execute(Runnable)   -> fire and forget, no result, no way to detect failure
submit(Callable)    -> returns a Future<T> you can .get() a result or exception from
```

```java
ExecutorService pool = Executors.newFixedThreadPool(4);

pool.execute(() -> System.out.println("fire and forget"));

Future<Integer> future = pool.submit(() -> {
    Thread.sleep(100);
    return 42;
});
int result = future.get(); // blocks until done, throws if the task threw

pool.shutdown(); // stop accepting new tasks, let running ones finish
```

`ThreadPoolExecutor` (which `Executors` factory methods wrap) has a task queue —
once the queue fills and no threads are free, the configured `RejectedExecutionHandler`
kicks in (default: throw an exception).

## 4. Code example — CompletableFuture composition

```java
CompletableFuture<String> result = CompletableFuture
    .supplyAsync(() -> fetchUser(id), pool)
    .thenApply(User::getName)
    .thenApply(String::toUpperCase);
```

## 5. Production use case

Parallel file processing, fan-out API calls to multiple downstream services, and
async request handling in Spring WebFlux/reactive stacks all build on
`ExecutorService`/`CompletableFuture`.

## 6. Common mistakes

- Using `Executors.newCachedThreadPool()` in production without bounds — it can
  create unlimited threads under load and exhaust memory.
- Forgetting to call `shutdown()`, leaking threads that keep the JVM alive.
- Not handling exceptions from `Future.get()` (they're wrapped in
  `ExecutionException`).
- Sizing thread pools without considering whether work is CPU-bound (size near
  core count) or I/O-bound (size much higher, since threads spend time waiting).

## 7. Trade-offs

| Fixed pool | Cached pool | Virtual threads |
|---|---|---|
| Predictable resource use | Can grow unbounded | Cheap, scale to huge counts for I/O-bound work |

## 8. Interview questions

1. `execute()` vs `submit()` — what's the practical difference?
2. How does `ThreadPoolExecutor` decide what to do when its queue is full?
3. How should you size a thread pool for CPU-bound vs I/O-bound work?
4. Why can `CompletableFuture` chains cause thread starvation, and how do you avoid it?
5. What problem do virtual threads solve that platform threads don't?

## 9. Short interview answer

"ExecutorService lets me submit tasks to a managed pool instead of creating raw
threads per task. I size pools differently depending on whether work is
CPU-bound — near the core count — or I/O-bound, where a much larger pool (or
virtual threads) makes sense since threads spend most of their time waiting, not
computing."

## 10. Related topics

- [Race conditions](race-conditions.md)
- [Virtual threads](virtual-threads.md)


## Thread Pool Internal Execution Strategy

Java Backend Interview Series #11/31 | Thread Pool 
 
Every Java backend application eventually reaches a point where handling tasks sequentially is no longer sufficient. Whether it's processing incoming HTTP requests, executing background jobs, sending emails, consuming Kafka messages, or calling multiple external services, creating a new thread for every task quickly becomes expensive and doesn't scale. 
 
A thread pool addresses this by reusing a fixed set of worker threads instead of continuously creating and destroying them.  
 
When a task is submitted to a ThreadPoolExecutor, Java doesn't immediately create a new thread every time. It follows a well-defined execution strategy: 
 
Create new worker threads until the core pool size is reached. 
Queue incoming tasks when all core threads are busy. 
Create additional threads only if the queue is full and the maximum pool size hasn't been reached. 
Reject new tasks using the configured Rejection Policy when both the queue and thread pool have reached their limits. 
 
Another important aspect is choosing the right queue and pool configuration. A large queue may reduce thread creation but can increase request latency, whereas a small queue may improve responsiveness but create more worker threads. Finding the right balance depends on the application's workload and traffic patterns. 
 
In production systems, it's generally recommended to configure ThreadPoolExecutor explicitly instead of relying solely on the convenience methods provided by Executors. This gives developers complete control over corePoolSize, maximumPoolSize, keepAliveTime, BlockingQueue, ThreadFactory, and RejectedExecutionHandler, allowing thread pools to be tuned for specific workloads. 
 
Understanding these internals is one of the most common topics in Java backend interviews because thread pools directly impact application performance, scalability, and system stability. 
 
💡 Interview Tip 
One of the most frequently asked interview questions is: 
"What happens internally when a task is submitted to a ThreadPoolExecutor?" 
A task is submitted. If the current worker count is below corePoolSize, a new worker thread is created. 
Otherwise, the task is placed into the BlockingQueue. 
If the queue is full and the worker count is below maximumPoolSize, another worker thread is created. 
If both the queue and maximum pool size are exhausted, the configured RejectedExecutionHandler determines how the task is handled. 
 
Being able to explain this flow clearly often distinguishes candidates who understand concurrency from those who only know the API. 
 
How do you configure thread pools in your production applications? Do you prefer using the Executors utility methods, or do you configure ThreadPoolExecutor explicitly for better control? 
