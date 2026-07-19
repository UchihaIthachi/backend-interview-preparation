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
