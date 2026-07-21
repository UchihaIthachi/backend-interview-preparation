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


## Virtual Threads Deep Dive

our Spring Boot application doesn't always need more CPU.

Sometimes...

It just needs fewer platform threads.

One of the biggest performance improvements in modern Java isn't a new framework.

It's Virtual Threads.

Instead of creating thousands of heavyweight OS threads, Java 21 lets the JVM create lightweight virtual threads that are perfect for applications waiting on I/O.

Think about a typical Spring Boot service.

A request arrives.

➡️ Validate request
➡️ Call another REST API
➡️ Query the database
➡️ Read Redis
➡️ Publish a Kafka event
➡️ Return the response

Most of that time isn't spent using the CPU.

It's spent waiting.

With traditional platform threads:

• Each request occupies an expensive thread.
• Thousands of concurrent requests require thousands of OS threads.
• Context switching increases.
• Memory usage grows rapidly.

With Virtual Threads:

• Each request gets its own lightweight virtual thread.
• When waiting for I/O, the thread simply parks.
• The carrier thread immediately starts executing another virtual thread.
• Thousands—even tens of thousands—of concurrent requests become practical.

Production Example

Imagine an Order Service processing:

✔ Database queries
✔ External payment APIs
✔ Inventory service calls
✔ Redis lookups
✔ Kafka publishing

These are mostly I/O-bound operations.

Virtual Threads can dramatically improve concurrency without rewriting your business logic.

But here's the catch...

Virtual Threads are NOT magic.

They won't speed up CPU-intensive work like:

❌ Image processing
❌ Encryption
❌ Large mathematical computations
❌ Video transcoding

Production Best Practices

✔ Use Virtual Threads for REST APIs and microservices.
✔ Ideal for database and HTTP calls.
✔ Avoid holding synchronized locks for long periods.
✔ Configure proper request timeouts.
✔ Continue monitoring latency and thread metrics.
✔ Benchmark before and after enabling them.

The biggest mistake?

Treating Virtual Threads as a replacement for good architecture.

They're an execution model—not a performance shortcut.

When used for the right workload, they can significantly increase concurrency while reducing infrastructure costs.

Have you enabled Virtual Threads in production yet? What kind of workload saw the biggest improvement?

🚀 Virtual Threads in Java (Project Loom): The Future of High-Concurrency Applications
Modern backend systems are expected to handle thousands of concurrent requests while remaining fast, scalable, and resource-efficient.
This is where Virtual Threads, introduced in Java 21 (Project Loom), make a significant impact.
Instead of creating an expensive operating system thread for every request, Virtual Threads are lightweight and managed by the JVM. This allows Java applications to process a massive number of concurrent tasks while consuming far fewer system resources.
💡 Why are Virtual Threads important?
✅ High concurrency with minimal memory usage
✅ Simpler synchronous programming model
✅ Better scalability for REST APIs and microservices
✅ Reduced overhead compared to traditional platform threads
✅ Production-ready in Java 21+