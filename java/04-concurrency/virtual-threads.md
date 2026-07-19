# Virtual Threads: What They Solve, and What They Don't

## 1. Definition

Virtual threads (Project Loom, standard since Java 21) are lightweight threads
managed by the JVM rather than mapped 1:1 to OS threads. Millions can exist
concurrently.

## 2. Why it exists

Platform (OS) threads are expensive — each has a large stack and OS-level
context-switch cost, so thread-per-request architectures don't scale past a few
thousand concurrent connections. Virtual threads let a blocking, imperative
programming style (no reactive/async code needed) scale to huge levels of
concurrency for I/O-bound workloads.

## 3. How it works

```text
Virtual thread blocks on I/O (e.g., a JDBC call, HTTP request)
   -> JVM "unmounts" it from its carrier OS thread
   -> the OS thread is freed to run other virtual threads
   -> when the I/O completes, the virtual thread is "remounted" onto a
      carrier thread and resumes
```

This means you can write simple blocking code (`resultSet = query.execute()`)
and still get near-async-level concurrency, because the JVM handles the
unmounting transparently.

## 4. Code example

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 100_000; i++) {
        executor.submit(() -> {
            callSlowDownstreamService(); // blocking call, totally fine here
        });
    }
}
```

## 5. Production use case

High-concurrency I/O-bound services (API gateways fanning out to many
downstream calls, request handlers waiting on database/network I/O) benefit
most. CPU-bound work (heavy computation, tight loops) sees no benefit — virtual
threads don't add CPU parallelism, they add cheap concurrency for waiting.

## 6. Common mistakes

- Expecting virtual threads to speed up CPU-bound work — they don't; the
  bottleneck there is CPU cores, not thread creation cost.
- Using `synchronized` blocks around blocking I/O inside virtual threads before
  Java pinning fixes — this can "pin" a virtual thread to its carrier thread,
  defeating the benefit (largely resolved in newer JDK versions, but worth
  knowing).
- Assuming virtual threads replace the need for connection pool limits — a
  downstream database can still only handle so many concurrent connections.

## 7. Trade-offs

| Platform threads | Virtual threads |
|---|---|
| Expensive, OS-managed | Cheap, JVM-managed |
| Good for CPU-bound work | Good for I/O-bound, high-concurrency work |

## 8. Interview questions

1. What problem do virtual threads solve that thread pools don't?
2. Why don't virtual threads help with CPU-bound workloads?
3. What does it mean for a virtual thread to be "pinned," and why is it a problem?
4. Would you still need a bounded database connection pool with virtual threads? Why?

## 9. Short interview answer

"Virtual threads make blocking I/O cheap by letting the JVM unmount a blocked
virtual thread from its OS carrier thread and reuse that carrier for other work.
That lets you write simple blocking code and still scale to huge concurrency for
I/O-bound workloads — but it doesn't help CPU-bound work, since the actual
bottleneck there is CPU cores, not thread count."

## 10. Related topics

- [ExecutorService](executor-service.md)
